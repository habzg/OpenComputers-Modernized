package li.cil.oc.core.impl.server.component;

import com.google.common.net.InetAddresses;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.core.Constants;
import li.cil.oc.core.Tags;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.InternetFilteringRule;
import li.cil.oc.core.impl.util.Log;
import li.cil.oc.core.impl.util.SideTracker;
import li.cil.oc.core.impl.util.ThreadPoolFactory;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class InternetCard extends li.cil.oc.api.prefab.ManagedEnvironment implements DeviceInfo {
    private static final ExecutorService threadPool = ThreadPoolFactory.create("Internet",
            Settings.get() != null ? Settings.get().internetThreads : 1);
    public final Node node = Network.newNode(this, Visibility.Network)
            .withComponent("internet", Visibility.Neighbors)
            .create();
    protected final Set<Closable> connections = new HashSet<>();
    private final Map<String, String> deviceInfo;
    protected Context owner = null;

    public InternetCard() {
        deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Communication, DeviceAttribute.Description, "Internet modem", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "SuperLink X-D4NK");
        setNode(this.node);
    }

    public static boolean isRequestAllowed(Settings settings, InetAddress inetAddress, String host) {
        if (settings.internetAccessDenied()) return false;
        InternetFilteringRule[] rules = settings.internetFilteringRules;
        if (inetAddress instanceof Inet6Address inet6) {
            if (InetAddresses.hasEmbeddedIPv4ClientAddress(inet6)) {
                InetAddress inet4 = InetAddresses.getEmbeddedIPv4ClientAddress(inet6);
                for (InternetFilteringRule r : rules) {
                    Boolean result = r.apply(inet4, host);
                    if (result != null && !result) return false;
                }
            }
            for (InternetFilteringRule r : rules) {
                Boolean result = r.apply(inet6, host);
                if (result != null) return result;
            }
            return false;
        } else if (inetAddress instanceof Inet4Address) {
            for (InternetFilteringRule r : rules) {
                Boolean result = r.apply(inetAddress, host);
                if (result != null) return result;
            }
            return false;
        }
        Log.get().warn("Internet Card blocked unrecognized address type: {}", inetAddress);
        return false;
    }

    public static void checkLists(InetAddress inetAddress, String host) {
        if (!isRequestAllowed(Settings.get(), inetAddress, host))
            throw new RuntimeException(new FileNotFoundException("address is not allowed"));
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether HTTP requests can be made.")
    public Object[] isHttpEnabled(Context context, Arguments args) {
        return ResultWrapper.result(Settings.get().httpEnabled);
    }

    @Callback(doc = "function(url:string[, postData:string[, headers:table[, method:string]]]):userdata -- Starts an HTTP request.")
    public synchronized Object[] request(Context context, Arguments args) {
        checkOwner(context);
        String address = args.checkString(0);
        if (Settings.get().internetAccessDenied())
            return ResultWrapper.result(null, "internet access is unavailable");
        if (!Settings.get().httpEnabled)
            return ResultWrapper.result(null, "http requests are unavailable");
        if (connections.size() >= Settings.get().maxConnections)
            throw new RuntimeException(new IOException("too many open connections"));

        String post = args.isString(1) ? args.checkString(1) : null;
        Map<String, String> headers = new HashMap<>();
        if (args.isTable(2)) {
            Map<?, ?> table = args.checkTable(2);
            for (Object key : table.keySet()) {
                Object val = table.get(key);
                if (key instanceof String && val != null)
                    headers.put((String) key, val.toString());
            }
        }
        if (!Settings.get().httpHeadersEnabled && !headers.isEmpty())
            return ResultWrapper.result(null, "http request headers are unavailable");
        String method = args.isString(3) ? args.checkString(3) : null;
        HTTPRequest request = new HTTPRequest(this, checkAddress(address), post, headers, method);
        connections.add(request);
        return ResultWrapper.result(request);
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether TCP connections can be made.")
    public Object[] isTcpEnabled(Context context, Arguments args) {
        return ResultWrapper.result(Settings.get().tcpEnabled);
    }

    @Callback(doc = "function(address:string[, port:number]):userdata -- Opens a new TCP connection.")
    public synchronized Object[] connect(Context context, Arguments args) {
        checkOwner(context);
        String address = args.checkString(0);
        int port = args.optInteger(1, -1);
        if (Settings.get().internetAccessDenied())
            return ResultWrapper.result(null, "internet access is unavailable");
        if (!Settings.get().tcpEnabled)
            return ResultWrapper.result(null, "tcp connections are unavailable");
        if (connections.size() >= Settings.get().maxConnections)
            throw new RuntimeException(new IOException("too many open connections"));
        URI uri = checkUri(address, port);
        TCPSocket socket = new TCPSocket(this, uri, port);
        connections.add(socket);
        return ResultWrapper.result(socket);
    }

    private void checkOwner(Context context) {
        if (owner == null || context.node() != owner.node())
            throw new IllegalArgumentException("can only be used by the owning computer");
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (owner == null && node.host() instanceof Context && node.isNeighborOf(this.node)) {
            owner = (Context) node.host();
        }
    }

    @Override
    public synchronized void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (owner != null && (node == this.node || (node.host() instanceof Context && node.host() == owner))) {
            owner = null;
            for (Closable c : new HashSet<>(connections)) c.close();
            connections.clear();
        }
    }

    @Override
    public synchronized void onMessage(Message message) {
        super.onMessage(message);
        if (message.data().length == 0 && ("computer.stopped".equals(message.name()) || "computer.started".equals(message.name()))
                && owner != null && message.source().address().equals(owner.node().address())) {
            for (Closable c : new HashSet<>(connections)) c.close();
            connections.clear();
        }
    }

    private URI checkUri(String address, int port) {
        try {
            URI parsed = new URI(address);
            if (parsed.getHost() != null && (parsed.getPort() > 0 || port > 0)) return parsed;
        } catch (URISyntaxException ignored) {
        }
        try {
            URI simple = new URI("oc://" + address);
            if (simple.getHost() != null) {
                if (simple.getPort() > 0) return simple;
                if (port > 0) return new URI(simple + ":" + port);
            }
        } catch (URISyntaxException ignored) {
        }
        throw new IllegalArgumentException("address could not be parsed or no valid port given");
    }

    private URL checkAddress(String address) {
        try {
            URL url = URI.create(address).toURL();
            String protocol = url.getProtocol();
            if (!protocol.matches("^https?$"))
                throw new RuntimeException(new FileNotFoundException("unsupported protocol"));
            return url;
        } catch (MalformedURLException e) {
            throw new RuntimeException(new FileNotFoundException("invalid address"));
        }
    }

    public interface Closable {
        void close();
    }

    public static class TCPNotifier extends Thread {
        private static final Queue<RegisteredAction> toAccept = new ConcurrentLinkedQueue<>();
        private static Selector selector;

        static {
            try {
                selector = Selector.open();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            TCPNotifier notifier = new TCPNotifier();
            notifier.start();
        }

        public static void add(SocketChannel channel, Runnable action) {
            toAccept.offer(new RegisteredAction(channel, action));
            selector.wakeup();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    RegisteredAction ra;
                    while ((ra = toAccept.poll()) != null) {
                        ra.channel.register(selector, SelectionKey.OP_READ, ra.action);
                    }
                    selector.select();
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Set<SelectionKey> readableKeys = new HashSet<>();
                    for (SelectionKey key : selectedKeys) {
                        if (key.isReadable()) {
                            ((Runnable) key.attachment()).run();
                            readableKeys.add(key);
                        }
                    }
                    if (!readableKeys.isEmpty()) {
                        Selector newSelector = Selector.open();
                        for (SelectionKey key : selector.keys()) {
                            if (!readableKeys.contains(key)) {
                                key.channel().register(newSelector, SelectionKey.OP_READ, key.attachment());
                            }
                        }
                        selector.close();
                        selector = newSelector;
                    }
                } catch (Exception e) {
                    Log.get().error("Error in TCP selector loop.", e);
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        private record RegisteredAction(SocketChannel channel, Runnable action) {

        }
    }

    public static class TCPSocket extends AbstractValue implements Closable {
        private final UUID id = UUID.randomUUID();
        private InternetCard owner;
        private Future<InetAddress> addressFuture;
        private SocketChannel channel;
        private boolean isAddressResolved = false;

        @SuppressWarnings("unused")
        public TCPSocket() {
        }

        public TCPSocket(InternetCard owner, URI uri, int port) {
            this.owner = owner;
            try {
                channel = SocketChannel.open();
                channel.configureBlocking(false);
                addressFuture = threadPool.submit(new AddressResolver(uri, port));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void setupSelector() {
            if (channel == null) return;
            TCPNotifier.add(channel, () -> {
                if (owner != null) {
                    owner.node.sendToVisible("computer.signal", "internet_ready", id.toString());
                } else {
                    try {
                        channel.close();
                    } catch (IOException ignored) {
                    }
                }
            });
        }

        @Callback(doc = "function():boolean -- Ensures a socket is connected.")
        public synchronized Object[] finishConnect(Context context, Arguments args) {
            boolean result = checkConnected();
            setupSelector();
            return ResultWrapper.result(result);
        }

        @Callback(doc = "function([n:number]):string -- Tries to read data from the socket stream.")
        public synchronized Object[] read(Context context, Arguments args) {
            int n = Math.clamp(args.optInteger(0, Integer.MAX_VALUE), 0, Settings.get().maxReadBuffer);
            if (checkConnected()) {
                try {
                    ByteBuffer buffer = ByteBuffer.allocate(n);
                    int read = channel.read(buffer);
                    if (read == -1)
                        return ResultWrapper.result((Object) null);
                    setupSelector();
                    byte[] data = new byte[read];
                    System.arraycopy(buffer.array(), 0, data, 0, read);
                    return ResultWrapper.result((Object) data);
                } catch (IOException e) {
                    return ResultWrapper.result((Object) new byte[0]);
                }
            }
            return ResultWrapper.result((Object) new byte[0]);
        }

        @Callback(doc = "function(data:string):number -- Tries to write data to the socket stream.")
        public synchronized Object[] write(Context context, Arguments args) {
            if (checkConnected()) {
                try {
                    byte[] value = args.checkByteArray(0);
                    return ResultWrapper.result((double) channel.write(ByteBuffer.wrap(value)));
                } catch (IOException e) {
                    return ResultWrapper.result(0);
                }
            }
            return ResultWrapper.result(0);
        }

        @SuppressWarnings("SameReturnValue")
        @Callback(direct = true, doc = "function() -- Closes an open socket stream.")
        public synchronized Object @Nullable [] close(Context context, Arguments args) {
            close();
            return null;
        }

        @Callback(direct = true, doc = "function():string -- Returns connection ID.")
        public synchronized Object[] id(Context context, Arguments args) {
            return ResultWrapper.result(id.toString());
        }

        @Override
        public void dispose(Context context) {
            super.dispose(context);
            close();
        }

        @Override
        public void close() {
            if (owner != null) {
                owner.connections.remove(this);
                if (addressFuture != null) addressFuture.cancel(true);
                try {
                    if (channel != null) channel.close();
                } catch (IOException ignored) {
                }
                owner = null;
                addressFuture = null;
                channel = null;
            }
        }

        private boolean checkConnected() {
            if (owner == null) throw new RuntimeException(new IOException("connection lost"));
            try {
                if (isAddressResolved) {
                    return channel.finishConnect();
                } else if (addressFuture.isCancelled()) {
                    channel.close();
                    throw new RuntimeException(new IOException("bad connection descriptor"));
                } else if (addressFuture.isDone()) {
                    try {
                        addressFuture.get();
                    } catch (ExecutionException e) {
                        if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
                        throw new RuntimeException(e.getCause());
                    }
                    isAddressResolved = true;
                    return false;
                }
                return false;
            } catch (Throwable t) {
                close();
                return false;
            }
        }

        private class AddressResolver implements Callable<InetAddress> {
            private final URI uri;
            private final int port;

            AddressResolver(URI uri, int port) {
                this.uri = uri;
                this.port = port;
            }

            @Override
            public InetAddress call() {
                try {
                    InetAddress resolved = InetAddress.getByName(uri.getHost());
                    checkLists(resolved, uri.getHost());
                    InetSocketAddress address = new InetSocketAddress(resolved, uri.getPort() != -1 ? uri.getPort() : port);
                    channel.connect(address);
                    return resolved;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static class HTTPRequest extends AbstractValue implements Closable {
        private final ConcurrentLinkedQueue<Byte> queue = new ConcurrentLinkedQueue<>();
        private InternetCard owner;
        private int responseCode;
        private String responseMessage;
        private Object responseHeaders;
        private Future<InputStream> stream;
        private Future<?> reader;
        private boolean eof = false;

        @SuppressWarnings("unused")
        public HTTPRequest() {
        }

        public HTTPRequest(InternetCard owner, URL url, String post, Map<String, String> headers, String method) {
            this.owner = owner;
            this.stream = threadPool.submit(new RequestSender(url, post, headers, method));
        }

        @Callback(doc = "function():boolean -- Ensures a response is available.")
        public synchronized Object[] finishConnect(Context context, Arguments args) {
            return ResultWrapper.result(checkResponse());
        }

        @Callback(direct = true, doc = "function():number, string, table -- Get response code, message and headers.")
        public synchronized Object[] response(Context context, Arguments args) {
            Object result = responseHeaders;
            if (result != null) return ResultWrapper.result((double) responseCode, responseMessage, result);
            return ResultWrapper.result((Object) null);
        }

        @Callback(doc = "function([n:number]):string -- Tries to read data from the response.")
        public synchronized Object[] read(Context context, Arguments args) {
            int n = Math.clamp(args.optInteger(0, Integer.MAX_VALUE), 0, Settings.get().maxReadBuffer);
            if (checkResponse()) {
                if (eof && queue.isEmpty()) return ResultWrapper.result((Object) null);
                byte[] buffer = new byte[n];
                int read = 0;
                while (!queue.isEmpty() && read < n) {
                    buffer[read++] = queue.poll();
                }
                if (read == 0) readMore();
                if (read == n) return ResultWrapper.result((Object) buffer);
                byte[] data = new byte[read];
                System.arraycopy(buffer, 0, data, 0, read);
                return ResultWrapper.result((Object) data);
            }
            return ResultWrapper.result((Object) new byte[0]);
        }

        @SuppressWarnings("SameReturnValue")
        @Callback(direct = true, doc = "function() -- Closes an open socket stream.")
        public synchronized Object @Nullable [] close(Context context, Arguments args) {
            close();
            return null;
        }

        @Override
        public void dispose(Context context) {
            super.dispose(context);
            close();
        }

        @Override
        public void close() {
            if (owner != null) {
                owner.connections.remove(this);
                if (stream != null) stream.cancel(true);
                if (reader != null) reader.cancel(true);
                owner = null;
                stream = null;
                reader = null;
            }
        }

        private boolean checkResponse() {
            if (owner == null) throw new RuntimeException(new IOException("connection lost"));
            if (stream.isDone()) {
                if (reader == null) {
                    try {
                        stream.get();
                    } catch (Exception e) {
                        if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
                        throw new RuntimeException(e.getCause());
                    }
                    readMore();
                }
                return true;
            }
            return false;
        }

        private void readMore() {
            if (reader == null || reader.isCancelled() || reader.isDone()) {
                if (!eof) {
                    reader = threadPool.submit(() -> {
                        try {
                            byte[] buffer = new byte[Settings.get().maxReadBuffer];
                            int count = stream.get().read(buffer);
                            if (count < 0) eof = true;
                            else for (int i = 0; i < count; i++) queue.add(buffer[i]);
                        } catch (Exception ignored) {
                        }
                    });
                }
            }
        }

        private class RequestSender implements Callable<InputStream> {
            private final URL url;
            private final String post;
            private final Map<String, String> headers;
            private final String method;

            RequestSender(URL url, String post, Map<String, String> headers, String method) {
                this.url = url;
                this.post = post;
                this.headers = headers;
                this.method = method;
            }

            @Override
            public InputStream call() {
                try {
                    checkLists(InetAddress.getByName(url.getHost()), url.getHost());
                    MinecraftServer server = SideTracker.getCurrentServer();
                    Proxy proxy = server != null ? server.getProxy() : Proxy.NO_PROXY;
                    URLConnection conn = url.openConnection(proxy);
                    if (!(conn instanceof HttpURLConnection http))
                        throw new RuntimeException(new IOException("unexpected connection type"));
                    try {
                        http.setDoInput(true);
                        http.setDoOutput(post != null);
                        http.setRequestMethod(method != null ? method : (post != null ? "POST" : "GET"));
                        http.setRequestProperty("User-Agent",
                                Settings.get().httpUserAgent.replace("$version", Tags.VERSION));
                        for (Map.Entry<String, String> h : headers.entrySet())
                            http.setRequestProperty(h.getKey(), h.getValue());
                        if (post != null) {
                            http.setReadTimeout(Settings.get().httpTimeout);
                            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(http.getOutputStream()));
                            out.write(post);
                            out.close();
                        }
                        try {
                            http.getInputStream();
                        } catch (Exception ignored) {
                        }
                        synchronized (HTTPRequest.this) {
                            responseCode = http.getResponseCode();
                            responseMessage = http.getResponseMessage();
                            responseHeaders = http.getHeaderFields();
                        }
                        return http.getInputStream();
                    } catch (Throwable t) {
                        http.disconnect();
                        throw t;
                    }
                } catch (UnknownHostException e) {
                    throw new RuntimeException(new IOException("unknown host: " + (e.getMessage() != null ? e.getMessage() : e.toString())));
                } catch (Throwable e) {
                    throw new RuntimeException(new IOException(e.getMessage() != null ? e.getMessage() : e.toString()));
                }
            }
        }
    }
}

package li.cil.oc.core.impl.server.machine.luac;

import com.google.common.base.Strings;
import com.google.common.io.PatternFilenameFilter;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.Channels;
import java.util.Random;
import java.util.regex.Pattern;
import li.cil.oc.core.Tags;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.ExtendedLuaState;
import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.repack.com.naef.jnlua.LuaStateFiveFour;
import li.cil.repack.com.naef.jnlua.LuaStateFiveThree;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LuaStateFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuaStateFactory.class);

    // Native Lua factories initialize one after another (5.2, 5.3, 5.4).
    // Clean stale extracted OC natives exactly once, before the first factory
    // extracts/loads anything for this JVM. Without the guard, a later factory
    // could delete a library that an earlier factory just extracted.
    private static boolean cleanedOldLibraries = false;

    private static synchronized void cleanupOldLibraries(File libDir) {
        if (cleanedOldLibraries) {
            return;
        }

        cleanedOldLibraries = true;

        if (!libDir.isDirectory()) {
            return;
        }

        File[] files = libDir.listFiles(new PatternFilenameFilter(
                "^" + Pattern.quote("OpenComputersMod-") + ".*\\.(dll|so|dylib)$"
        ));

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    try {
                        if (file.delete()) {
                            LOGGER.debug("Deleted stale native library '{}'.", file.getName());
                        } else if (file.exists()) {
                            // This is expected on Windows if another running JVM still has
                            // the native library loaded and therefore locked.
                            LOGGER.debug("Could not delete stale native library '{}'. It may still be in use.", file.getName());
                        }
                    } catch (Throwable t) {
                        LOGGER.debug("Could not delete stale native library '{}'.", file.getName(), t);
                    }
                }
            }
        }
    }

    private final String libraryName;

    private final String libraryFileName;
    private boolean haveNativeLibrary = false;
    private String currentLib = "";

    public LuaStateFactory() {
        String libExtension;
        if (SystemUtils.IS_OS_MAC) libExtension = ".dylib";
        else if (SystemUtils.IS_OS_WINDOWS) libExtension = ".dll";
        else libExtension = ".so";

        String platformName = getPlatformSystemName();

        libraryFileName = "libjnlua" + version() + "-" + platformName + libExtension;
        libraryName = libraryFileName;

        init();

        if (!haveNativeLibrary) {
            LOGGER.warn("Unsupported platform, you won't be able to host games with persistent computers.");
        }
    }

    private static String getPlatformSystemName() {
        if (!Strings.isNullOrEmpty(OCSettings.get().forceNativeLibPlatform)) {
            return OCSettings.get().forceNativeLibPlatform;
        }
        String systemName = getSystemName();
        String archName = getArchName();
        return systemName + "-" + archName;
    }

    private static @NotNull String getArchName() {
        String archName;
        if (Architecture.IS_OS_ARM64) archName = "aarch64";
        else if (Architecture.IS_OS_ARM) archName = "arm";
        else if (Architecture.IS_OS_X64) archName = "x86_64";
        else if (Architecture.IS_OS_X86) archName = "x86";
        else archName = "unknown";
        return archName;
    }

    private static @NotNull String getSystemName() {
        String systemName;
        if (SystemUtils.IS_OS_FREE_BSD) systemName = "freebsd";
        else if (SystemUtils.IS_OS_NET_BSD) systemName = "netbsd";
        else if (SystemUtils.IS_OS_OPEN_BSD) systemName = "openbsd";
        else if (SystemUtils.IS_OS_SOLARIS) systemName = "solaris";
        else if (SystemUtils.IS_OS_LINUX) systemName = "linux";
        else if (SystemUtils.IS_OS_MAC) systemName = "darwin";
        else if (SystemUtils.IS_OS_WINDOWS) systemName = "windows";
        else systemName = "unknown";
        return systemName;
    }

    public static boolean isAvailable() {
        boolean lua52 = Lua52.INSTANCE.haveLibrary();
        boolean lua53 = Lua53.INSTANCE.haveLibrary();
        boolean lua54 = Lua54.INSTANCE.haveLibrary();
        return lua52 || lua53 || lua54;
    }

    public static boolean luajRequested() {
        return OCSettings.get().forceLuaJ || OCSettings.get().registerLuaJArchitecture;
    }

    public static boolean includeLuaJ() {
        return !isAvailable() || luajRequested();
    }

    public static boolean include52() {
        return Lua52.INSTANCE.haveLibrary() && !OCSettings.get().forceLuaJ;
    }

    private static boolean isExistingFileMatching(java.net.URL libraryUrl, File tmpLibFile) {
        try {
            BufferedInputStream inCurrent = new BufferedInputStream(libraryUrl.openStream());
            BufferedInputStream inExisting = new BufferedInputStream(new FileInputStream(tmpLibFile));
            int inCurrentByte;
            int inExistingByte;
            do {
                inCurrentByte = inCurrent.read();
                inExistingByte = inExisting.read();
                if (inCurrentByte != inExistingByte) {
                    return false;
                }
            } while (inCurrentByte != -1);
            inCurrent.close();
            inExisting.close();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean include53() {
        return Lua53.INSTANCE.haveLibrary() && OCSettings.get().enableLua53 && !OCSettings.get().forceLuaJ;
    }

    public static boolean include54() {
        return Lua54.INSTANCE.haveLibrary() && OCSettings.get().enableLua54 && !OCSettings.get().forceLuaJ;
    }

    public static boolean default53() {
        return include53() && OCSettings.get().defaultLua53;
    }

    public static ItemStack setDefaultArch(ItemStack stack) {
        if (default53()) {
            Object driver = li.cil.oc.api.API.driver.driverFor(stack);
            if (driver instanceof li.cil.oc.api.driver.item.MutableProcessor) {
                ((li.cil.oc.api.driver.item.MutableProcessor) driver).setArchitecture(stack, NativeLua53Architecture.class);
            }
        }
        return stack;
    }

    public abstract String version();

    protected abstract LuaState create(Integer maxMemory);

    protected abstract void openLibs(LuaState state);

    public boolean haveLibrary() {
        return haveNativeLibrary;
    }

    private void init() {
        if (libraryName == null) {
            return;
        }

        if (SystemUtils.IS_OS_WINDOWS && !OCSettings.get().alwaysTryNative) {
            if (SystemUtils.IS_OS_WINDOWS_XP) {
                LOGGER.warn("Sorry, but Windows XP isn't supported. I'm afraid you'll have to use a newer Windows. I very much recommend upgrading your Windows, anyway, since Microsoft has stopped supporting Windows XP in April 2014.");
                return;
            }

            if (SystemUtils.IS_OS_WINDOWS_2003) {
                LOGGER.warn("Sorry, but Windows Server 2003 isn't supported. I'm afraid you'll have to use a newer Windows.");
                return;
            }
        }

        File tmpLibFile = null;
        if (!Strings.isNullOrEmpty(OCSettings.get().forceNativeLibPathFirst)) {
            File libraryTest = new File(OCSettings.get().forceNativeLibPathFirst, libraryFileName);
            if (libraryTest.canRead()) {
                tmpLibFile = libraryTest;
                currentLib = libraryTest.getAbsolutePath();
                LOGGER.info("Found forced-path filesystem library {}.", currentLib);
            } else {
                LOGGER.warn("forceNativeLibPathFirst is set, but {} was not found there. Falling back to checking the built-in libraries.", currentLib);
            }
        }

        if (currentLib.isEmpty()) {
            java.net.URL libraryUrl = LuaStateFactory.class.getResource("/assets/" + OCSettings.resourceDomain + "/lib/" + libraryFileName);
            if (libraryUrl == null) {
                var tccl = Thread.currentThread().getContextClassLoader();
                if (tccl != null) {
                    libraryUrl = tccl.getResource("assets/" + OCSettings.resourceDomain + "/lib/" + libraryFileName);
                }
                if (libraryUrl == null) {
                    libraryUrl = ClassLoader.getSystemClassLoader()
                            .getResource("assets/" + OCSettings.resourceDomain + "/lib/" + libraryFileName);
                }
            }
            if (libraryUrl == null) {
                LOGGER.warn("Native library with name '{}' not found.", libraryFileName);
                return;
            }

            String tmpLibName = "OpenComputersMod-" + Tags.VERSION + "-" + version() + "-" + libraryFileName;
            String tmpBasePath;
            if (OCSettings.get().nativeInTmpDir) {
                String path = System.getProperty("java.io.tmpdir");
                if (path == null) tmpBasePath = "";
                else if (path.endsWith("/") || path.endsWith("\\")) tmpBasePath = path;
                else tmpBasePath = path + "/";
            } else {
                File nativesDir = new File("opencomputers", "natives");
                //noinspection ResultOfMethodCallIgnored
                nativesDir.mkdirs();
                tmpBasePath = nativesDir.getAbsolutePath() + File.separator;
            }
            tmpLibFile = new File(tmpBasePath + tmpLibName);

            // Clean up stale OC native libraries once before this run extracts and
            // loads any of its bundled natives. Do not do this once per Lua version:
            // Lua 5.3/5.4 initialization must not delete the library Lua 5.2 just
            // extracted and loaded.
            if (!OCSettings.get().nativeInTmpDir) {
                cleanupOldLibraries(new File(tmpBasePath));
            }

            if (tmpLibFile.exists()) {
                if (!isExistingFileMatching(libraryUrl, tmpLibFile)) {
                    try {
                        if (!tmpLibFile.delete()) {
                            LOGGER.warn("Failed to delete old native library '{}'", tmpLibFile.getName());
                        }
                    } catch (Throwable ignored) {
                    }
                    if (tmpLibFile.exists()) {
                        LOGGER.warn("Could not update native library '{}'!", tmpLibFile.getName());
                    }
                }
            }

            try {
                File tmpFile = new File(tmpLibFile.getAbsolutePath() + ".tmp");
                try (java.nio.channels.ReadableByteChannel in = Channels.newChannel(libraryUrl.openStream())) {
                    try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                        try (java.nio.channels.FileChannel out = fos.getChannel()) {
                            out.transferFrom(in, 0, Long.MAX_VALUE);
                        }
                    }
                }
                try {
                    java.nio.file.Files.move(tmpFile.toPath(), tmpLibFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                    java.nio.file.Files.move(tmpFile.toPath(), tmpLibFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                tmpLibFile.deleteOnExit();
                if (!tmpLibFile.setReadable(true, true)) {
                    LOGGER.warn("Failed to set readable on: {}", tmpLibFile);
                }
                if (!tmpLibFile.setWritable(true, true)) {
                    LOGGER.warn("Failed to set writable on: {}", tmpLibFile);
                }
                if (!tmpLibFile.setExecutable(true, true)) {
                    LOGGER.warn("Failed to set executable on: {}", tmpLibFile);
                }
            } catch (Throwable ignored) {
            }

            currentLib = tmpLibFile.getAbsolutePath();
        }

        try {
            synchronized (LuaStateFactory.class) {
                System.load(currentLib);
                create(null).close();
            }
            if (tmpLibFile != null) {
                LOGGER.info("Found a compatible native library: '{}'.", tmpLibFile.getName());
            }
            haveNativeLibrary = true;
        } catch (Throwable t) {
            if (tmpLibFile != null) {
                if (OCSettings.get().logFullLibLoadErrors) {
                    LOGGER.warn("Could not load native library '{}'.", tmpLibFile.getName(), t);
                } else {
                    LOGGER.trace("Could not load native library '{}'.", tmpLibFile.getName());
                }
                if (!tmpLibFile.delete()) {
                    LOGGER.warn("Failed to delete native library '{}'", tmpLibFile.getName());
                }
            }
        }
    }

    public LuaState createState() {
        if (!haveNativeLibrary) return null;

        try {
            LuaState state;
            synchronized (LuaStateFactory.class) {
                System.load(currentLib);
                if (OCSettings.get().limitMemory) state = create(Integer.MAX_VALUE);
                else state = create(null);
            }
            try {
                openLibs(state);

                if (!OCSettings.get().disableLocaleChanging) {
                    state.openLib(LuaState.Library.OS);
                    state.getField(-1, "setlocale");
                    state.pushString("C");
                    state.call(1, 0);
                    state.pop(1);
                }

                state.newTable();
                state.setGlobal("os");
                state.pushNil();
                state.setGlobal("unpack");
                state.pushNil();
                state.setGlobal("loadstring");

                state.getGlobal("math");
                state.pushNil();
                state.setField(-2, "log10");
                state.pop(1);
                state.getGlobal("table");
                state.pushNil();
                state.setField(-2, "maxn");
                state.pop(1);
                state.pushNil();
                state.setGlobal("dofile");
                state.pushNil();
                state.setGlobal("loadfile");
                state.getGlobal("math");

                Random random = new Random();
                ExtendedLuaState.pushScalaFunction(state, l -> {
                    double r = random.nextDouble();
                    switch (l.getTop()) {
                        case 0:
                            l.pushNumber(r);
                            break;
                        case 1: {
                            double u = l.checkNumber(1);
                            l.checkArg(1, 1 <= u, "interval is empty");
                            l.pushNumber(Math.floor(r * u) + 1);
                            break;
                        }
                        case 2: {
                            double lv = l.checkNumber(1);
                            double u = l.checkNumber(2);
                            l.checkArg(2, lv <= u, "interval is empty");
                            l.pushNumber(Math.floor(r * (u - lv + 1)) + lv);
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("wrong number of arguments");
                    }
                    return 1;
                });
                state.setField(-2, "random");

                ExtendedLuaState.pushScalaFunction(state, l -> {
                    random.setSeed(l.checkInteger(1));
                    return 0;
                });
                state.setField(-2, "randomseed");

                state.pop(1);

                ExtendedLuaState.pushScalaFunction(state, l -> {
                    l.getGlobal("type");
                    if (!l.isFunction(-1)) {
                        l.pop(1);
                        l.pushString("global 'type' is not a function");
                        return 1;
                    }
                    l.pop(1);
                    l.getGlobal("pcall");
                    if (!l.isFunction(-1)) {
                        l.pop(1);
                        l.pushString("global 'pcall' is not a function");
                        return 1;
                    }
                    l.pop(1);
                    l.getGlobal("assert");
                    if (!l.isFunction(-1)) {
                        l.pop(1);
                        l.pushString("global 'assert' is not a function");
                        return 1;
                    }
                    l.pop(1);
                    l.pushBoolean(true);
                    return 1;
                });
                state.setGlobal("_OC_sandboxCheck");

                return state;
            } catch (Throwable t) {
                LOGGER.warn("Failed creating Lua state.", t);
                state.close();
            }
        } catch (UnsatisfiedLinkError e) {
            LOGGER.error("Failed loading the native libraries.");
        } catch (Throwable t) {
            LOGGER.warn("Failed creating Lua state.", t);
        }
        return null;
    }

    public static class Lua52 extends LuaStateFactory {
        public static final Lua52 INSTANCE = new Lua52();

        @Override
        public String version() {
            return "52";
        }

        @Override
        protected LuaState create(Integer maxMemory) {
            return maxMemory != null ? new LuaState(maxMemory) : new LuaState();
        }

        @Override
        protected void openLibs(LuaState state) {
            state.openLib(LuaState.Library.BASE);
            state.openLib(LuaState.Library.BIT32);
            state.openLib(LuaState.Library.COROUTINE);
            state.openLib(LuaState.Library.DEBUG);
            state.openLib(LuaState.Library.ERIS);
            state.openLib(LuaState.Library.MATH);
            state.openLib(LuaState.Library.STRING);
            state.openLib(LuaState.Library.TABLE);
            state.pop(8);
        }
    }

    public static class Lua53 extends LuaStateFactory {
        public static final Lua53 INSTANCE = new Lua53();

        @Override
        public String version() {
            return "53";
        }

        @Override
        protected LuaState create(Integer maxMemory) {
            return maxMemory != null ? new LuaStateFiveThree(maxMemory) : new LuaStateFiveThree();
        }

        @Override
        protected void openLibs(LuaState state) {
            state.openLib(LuaState.Library.BASE);
            state.openLib(LuaState.Library.COROUTINE);
            state.openLib(LuaState.Library.DEBUG);
            state.openLib(LuaState.Library.ERIS);
            state.openLib(LuaState.Library.MATH);
            state.openLib(LuaState.Library.STRING);
            state.openLib(LuaState.Library.TABLE);
            state.openLib(LuaState.Library.UTF8);
            state.pop(8);
        }
    }

    public static class Lua54 extends LuaStateFactory {
        public static final Lua54 INSTANCE = new Lua54();

        @Override
        public String version() {
            return "54";
        }

        @Override
        protected LuaState create(Integer maxMemory) {
            return maxMemory != null ? new LuaStateFiveFour(maxMemory) : new LuaStateFiveFour();
        }

        @Override
        protected void openLibs(LuaState state) {
            state.openLib(LuaState.Library.BASE);
            state.openLib(LuaState.Library.COROUTINE);
            state.openLib(LuaState.Library.DEBUG);
            state.openLib(LuaState.Library.ERIS);
            state.openLib(LuaState.Library.MATH);
            state.openLib(LuaState.Library.STRING);
            state.openLib(LuaState.Library.TABLE);
            state.openLib(LuaState.Library.UTF8);
            state.pop(8);
        }
    }

    public static class Architecture {
        public static final String OS_ARCH = getSystemProperty();

        public static final boolean IS_OS_ARM = isOSArchMatch("arm");
        public static final boolean IS_OS_ARM64 = isOSArchMatch("aarch64");
        public static final boolean IS_OS_X86 = isOSArchMatch("x86") || isOSArchMatch("i386");
        public static final boolean IS_OS_X64 = isOSArchMatch("x86_64") || isOSArchMatch("amd64");

        private static String getSystemProperty() {
            try {
                return System.getProperty("os.arch");
            } catch (SecurityException e) {
                return null;
            }
        }

        private static boolean isOSArchMatch(String archPrefix) {
            return OS_ARCH != null && OS_ARCH.startsWith(archPrefix);
        }
    }
}

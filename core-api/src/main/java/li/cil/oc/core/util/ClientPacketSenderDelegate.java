package li.cil.oc.core.util;


public abstract class ClientPacketSenderDelegate {
    private static ClientPacketSenderDelegate instance;

    public static void setInstance(ClientPacketSenderDelegate inst) {
        instance = inst;
    }

    public static ClientPacketSenderDelegate get() {
        return instance;
    }

    public abstract void sendTextBufferInit(String address) ;

    public abstract void sendKeyDown(String address, char character, int code) ;

    public abstract void sendKeyUp(String address, char character, int code) ;

    public abstract void sendClipboard(String address, String value);

    public abstract void sendDropFile(String address, String fileName, String fileContent);

    public abstract void sendMouseClick(String address, double x, double y, boolean drag, int button) ;

    public abstract void sendMouseUp(String address, double x, double y, int button) ;

    public abstract void sendMouseScroll(String address, double x, double y, int delta) ;

    public abstract void sendCopyToAnalyzer(String address, int line) ;
}

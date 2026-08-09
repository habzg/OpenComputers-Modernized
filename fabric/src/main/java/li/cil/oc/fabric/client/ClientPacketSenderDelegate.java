package li.cil.oc.fabric.client;

public class ClientPacketSenderDelegate extends li.cil.oc.core.util.ClientPacketSenderDelegate {
    @Override
    public void sendTextBufferInit(String address) {
        PacketSender.sendTextBufferInit(address);
    }

    @Override
    public void sendKeyDown(String address, char character, int code) {
        PacketSender.sendKeyDown(address, character, code);
    }

    @Override
    public void sendKeyUp(String address, char character, int code) {
        PacketSender.sendKeyUp(address, character, code);
    }

    @Override
    public void sendClipboard(String address, String value) {
        PacketSender.sendClipboard(address, value);
    }

    @Override
    public void sendDropFile(String address, String name, String data) {
        PacketSender.sendDropFile(address, name, data);
    }

    @Override
    public void sendMouseClick(String address, double x, double y, boolean drag, int button) {
        PacketSender.sendMouseClick(address, x, y, drag, button);
    }

    @Override
    public void sendMouseUp(String address, double x, double y, int button) {
        PacketSender.sendMouseUp(address, x, y, button);
    }

    @Override
    public void sendMouseScroll(String address, double x, double y, int delta) {
        PacketSender.sendMouseScroll(address, x, y, delta);
    }

    @Override
    public void sendCopyToAnalyzer(String address, int line) {
        PacketSender.sendCopyToAnalyzer(address, line);
    }
}

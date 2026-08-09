package li.cil.oc.neoforge.util;

public class ClientPacketSenderDelegate extends li.cil.oc.core.util.ClientPacketSenderDelegate {
    @Override
    public void sendTextBufferInit(String address) {
        li.cil.oc.neoforge.client.PacketSender.sendTextBufferInit(address);
    }

    @Override
    public void sendKeyDown(String address, char character, int code) {
        li.cil.oc.neoforge.client.PacketSender.sendKeyDown(address, character, code);
    }

    @Override
    public void sendKeyUp(String address, char character, int code) {
        li.cil.oc.neoforge.client.PacketSender.sendKeyUp(address, character, code);
    }

    @Override
    public void sendClipboard(String address, String value) {
        li.cil.oc.neoforge.client.PacketSender.sendClipboard(address, value);
    }

    @Override
    public void sendDropFile(String address, String fileName, String fileContent) {
        li.cil.oc.neoforge.client.PacketSender.sendDropFile(address, fileName, fileContent);
    }

    @Override
    public void sendMouseClick(String address, double x, double y, boolean drag, int button) {
        li.cil.oc.neoforge.client.PacketSender.sendMouseClick(address, x, y, drag, button);
    }

    @Override
    public void sendMouseUp(String address, double x, double y, int button) {
        li.cil.oc.neoforge.client.PacketSender.sendMouseUp(address, x, y, button);
    }

    @Override
    public void sendMouseScroll(String address, double x, double y, int delta) {
        li.cil.oc.neoforge.client.PacketSender.sendMouseScroll(address, x, y, delta);
    }

    @Override
    public void sendCopyToAnalyzer(String address, int line) {
        li.cil.oc.neoforge.client.PacketSender.sendCopyToAnalyzer(address, line);
    }
}

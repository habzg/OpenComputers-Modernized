package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import net.minecraft.world.entity.player.Player;


import java.util.HashMap;
import java.util.Map;

public class Keyboard extends li.cil.oc.api.prefab.ManagedEnvironment implements li.cil.oc.api.internal.Keyboard, DeviceInfo {
    public final EnvironmentHost host;
    public final Node node = Network.newNode(this, Visibility.Network)
            .withComponent("keyboard")
            .create();

    public final Map<Player, Map<Integer, Character>> pressedKeys = new HashMap<>();
    private final Map<String, String> deviceInfo;
    public UsabilityChecker usableOverride = null;

    public Keyboard(EnvironmentHost host) {
        this.host = host;
        deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Input, DeviceAttribute.Description, "Keyboard", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "Fancytyper MX-Stone");
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public void setUsableOverride(UsabilityChecker callback) {
        usableOverride = callback;
    }

    public void releasePressedKeys(Player player) {
        Map<Integer, Character> keys = pressedKeys.get(player);
        if (keys != null) {
            for (Map.Entry<Integer, Character> entry : keys.entrySet()) {
                if (Settings.get().inputUsername) {
                    signal(player, "key_up", (int) entry.getValue(), entry.getKey(), player.getScoreboardName());
                } else {
                    signal(player, "key_up", (int) entry.getValue(), entry.getKey());
                }
            }
            pressedKeys.remove(player);
        }
    }

    @Override
    public void onConnect(li.cil.oc.api.network.Node node) {
        super.onConnect(node);
        if (node == this.node()) {
            li.cil.oc.core.impl.util.EventHandlerDelegate.get().addKeyboard(this);
        }
    }

    @Override
    public void onMessage(Message message) {
        Object[] data = message.data();
        if (data.length >= 2) {
            if ("keyboard.keyDown".equals(message.name()) && data[0] instanceof Player p && data[1] instanceof Character && data[2] instanceof Integer) {
                if (isUsableByPlayer(p)) {
                    char ch = (Character) data[1];
                    int code = (Integer) data[2];
                    pressedKeys.computeIfAbsent(p, k -> new HashMap<>()).put(code, ch);
                    if (Settings.get().inputUsername) {
                        signal(p, "key_down", (int) ch, code, p.getScoreboardName());
                    } else {
                        signal(p, "key_down", (int) ch, code);
                    }
                }
            } else if ("keyboard.keyUp".equals(message.name()) && data[0] instanceof Player p && data[1] instanceof Character && data[2] instanceof Integer) {
                Map<Integer, Character> keys = pressedKeys.get(p);
                if (keys != null) {
                    int code = (Integer) data[2];
                    if (keys.containsKey(code)) {
                        char ch = (Character) data[1];
                        keys.remove(code);
                        if (Settings.get().inputUsername) {
                            signal(p, "key_up", (int) ch, code, p.getScoreboardName());
                        } else {
                            signal(p, "key_up", (int) ch, code);
                        }
                    }
                }
            } else if ("keyboard.clipboard".equals(message.name()) && data[0] instanceof Player p && data[1] instanceof String value) {
                if (isUsableByPlayer(p)) {
                    for (String line : value.split("(?<=\\n)")) {
                        if (Settings.get().inputUsername) {
                            signal(p, "clipboard", line, p.getScoreboardName());
                        } else {
                            signal(p, "clipboard", line);
                        }
                    }
                }
            }
        }
    }

    public boolean isUsableByPlayer(Player p) {
        if (usableOverride != null) return usableOverride.isUsableByPlayer(this, p);
        return p.distanceToSqr(host.xPosition(), host.yPosition(), host.zPosition()) <= 64;
    }

    protected void signal(Object... args) {
        node.sendToReachable("computer.checked_signal", args);
    }
}

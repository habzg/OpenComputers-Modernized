package li.cil.oc.neoforge.common.nanomachines;

import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.util.PlayerUtils;
import net.minecraft.world.entity.player.Player;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class Nanomachines implements li.cil.oc.api.detail.NanomachinesAPI {
    public final Set<BehaviorProvider> providers = new HashSet<>();
    public final Map<Player, ControllerImpl> serverControllers = new WeakHashMap<>();
    public final Map<Player, ControllerImpl> clientControllers = new WeakHashMap<>();

    public Map<Player, ControllerImpl> controllers(Player player) {
        return player.level().isClientSide ? clientControllers : serverControllers;
    }

    @Override
    public void addProvider(BehaviorProvider provider) {
        providers.add(provider);
    }

    @Override
    public Iterable<BehaviorProvider> getProviders() {
        return providers;
    }

    @Override
    public Controller getController(Player player) {
        if (hasController(player)) {
            return controllers(player).computeIfAbsent(player, k -> new ControllerImpl(player));
        }
        return null;
    }

    public boolean hasController(Player player) {
        return PlayerUtils.persistedData(player).getBoolean(Settings.namespace + "hasNanomachines");
    }

    @Override
    public Controller installController(Player player) {
        if (!hasController(player)) {
            PlayerUtils.persistedData(player).putBoolean(Settings.namespace + "hasNanomachines", true);
        }
        return getController(player);
    }

    @Override
    public void uninstallController(Player player) {
        Controller controller = getController(player);
        if (controller instanceof ControllerImpl impl) {
            impl.dispose();
            controllers(player).remove(player);
            PlayerUtils.persistedData(player).remove(Settings.namespace + "hasNanomachines");
            if (!player.level().isClientSide) {
                PacketSender.sendNanomachineConfiguration(player);
            }
        }
    }
}

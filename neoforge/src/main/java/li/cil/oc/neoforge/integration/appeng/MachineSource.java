package li.cil.oc.neoforge.integration.appeng;

import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public class MachineSource implements IActionSource {
    private final IActionHost via;

    public MachineSource(IActionHost via) {
        this.via = via;
    }

    @Override
    public Optional<Player> player() {
        return Optional.empty();
    }

    @Override
    public Optional<IActionHost> machine() {
        return Optional.of(via);
    }

    @Override
    public <T> Optional<T> context(Class<T> key) {
        return Optional.empty();
    }
}

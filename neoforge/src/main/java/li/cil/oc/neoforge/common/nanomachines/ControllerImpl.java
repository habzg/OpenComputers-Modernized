package li.cil.oc.neoforge.common.nanomachines;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.impl.util.PlayerUtils;
import li.cil.oc.neoforge.common.item.data.NanomachineData;
import li.cil.oc.neoforge.integration.util.DamageSourceWithRandomCause;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ControllerImpl implements Controller, WirelessEndpoint {
    public static final int FullSyncInterval = 20 * 60;
    public static final DamageSource OverloadDamage = new DamageSourceWithRandomCause("oc.nanomachinesOverload", 3);
    public final Player player;
    public final double CommandRange;
    public final NeuralNetwork configuration = new NeuralNetwork(this);
    public final Set<Behavior> activeBehaviors = new HashSet<>();
    public String previousDimension;
    public String uuid = UUID.randomUUID().toString();
    public int responsePort = 0;
    public int commandDelay = 0;
    public Runnable queuedCommand = null;
    public double storedEnergy = Settings.get().bufferNanomachines * 0.25;
    public boolean hadPower = true;
    public volatile boolean activeBehaviorsDirty = true;
    public boolean hasSentConfiguration = false;

    public ControllerImpl(Player player) {
        this.player = player;
        if (isServer()) li.cil.oc.api.Network.joinWirelessNetwork(this);
        this.previousDimension = player.level().dimension().location().toString();
        this.CommandRange = Settings.get().nanomachinesCommandRange * Settings.get().nanomachinesCommandRange;
    }

    @Override
    public Level level() {
        return player.level();
    }

    @Override
    public int x() {
        return BlockPosition.apply(player).x();
    }

    @Override
    public int y() {
        return BlockPosition.apply(player).y();
    }

    @Override
    public int z() {
        return BlockPosition.apply(player).z();
    }

    @Override
    public void receivePacket(Packet packet, WirelessEndpoint sender) {
        if (getLocalBuffer() > 0 && commandDelay < 1 && !player.isDeadOrDying()) {
            double dx = (sender.x() + 0.5) - player.getX();
            double dy = (sender.y() + 0.5) - player.getY();
            double dz = (sender.z() + 0.5) - player.getZ();
            double dSq = dx * dx + dy * dy + dz * dz;
            if (dSq <= CommandRange && packet.data().length > 0 && packet.data()[0] instanceof byte[] header &&
                    new String(header, StandardCharsets.UTF_8).equals("nanomachines")) {
                Object[] cmd = new Object[packet.data().length - 1];
                System.arraycopy(packet.data(), 1, cmd, 0, cmd.length);
                for (int i = 0; i < cmd.length; i++) {
                    if (cmd[i] instanceof byte[]) cmd[i] = new String((byte[]) cmd[i], StandardCharsets.UTF_8);
                }
                if (cmd.length > 0) {
                    String op = (String) cmd[0];
                    switch (op) {
                        case "setResponsePort" -> {
                            if (cmd.length > 1 && cmd[1] instanceof Number n) {
                                responsePort = Math.clamp(n.intValue(), 0, 0xFFFF);
                                respond(sender, "port", responsePort);
                            }
                        }
                        case "getPowerState" -> respond(sender, "power", getLocalBuffer(), getLocalBufferSize());
                        case "saveConfiguration" -> {
                            var nanomachinesItem = li.cil.oc.api.Items.get(Constants.ItemName.Nanomachines);
                            try {
                                int index = -1;
                                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                                    var stack = player.getInventory().getItem(i);
                                    if (li.cil.oc.api.Items.get(stack) == nanomachinesItem && new NanomachineData(stack).configuration.isEmpty()) {
                                        index = i;
                                        break;
                                    }
                                }
                                if (index >= 0) {
                                    var stack = player.getInventory().removeItem(index, 1);
                                    new NanomachineData(this).save(stack);
                                    player.getInventory().add(stack);
                                    InventoryUtils.spawnStackInWorld(BlockPosition.apply(player), stack, null, null);
                                    respond(sender, "saved", true);
                                } else respond(sender, "saved", false, "no nanomachines");
                            } catch (Throwable t) {
                                respond(sender, "saved", false, "error");
                            }
                        }
                        case "getHealth" -> respond(sender, "health", player.getHealth(), player.getMaxHealth());
                        case "getHunger" ->
                                respond(sender, "hunger", player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel());
                        case "getAge" -> respond(sender, "age", player instanceof net.minecraft.server.level.ServerPlayer sp ? (int) ((System.currentTimeMillis() - sp.getLastActionTime()) / 1000) : player.tickCount / 20);
                        case "getName" -> respond(sender, "name", player.getDisplayName().getString());
                        case "getExperience" -> respond(sender, "experience", player.experienceLevel);
                        case "getTotalInputCount" -> respond(sender, "totalInputCount", getTotalInputCount());
                        case "getSafeActiveInputs" -> respond(sender, "safeActiveInputs", getSafeActiveInputs());
                        case "getMaxActiveInputs" -> respond(sender, "maxActiveInputs", getMaxActiveInputs());
                        case "getInput" -> {
                            try {
                                int idx = ((Number) cmd[1]).intValue() - 1;
                                respond(sender, "input", idx + 1, getInput(idx));
                            } catch (Throwable t) {
                                respond(sender, "input", "error");
                            }
                        }
                        case "setInput" -> {
                            try {
                                int idx = ((Number) cmd[1]).intValue() - 1;
                                boolean val = (Boolean) cmd[2];
                                if (setInput(idx, val)) respond(sender, "input", idx + 1, getInput(idx));
                                else respond(sender, "input", "too many active inputs");
                            } catch (Throwable t) {
                                respond(sender, "input", "error");
                            }
                        }
                        case "getActiveEffects" -> {
                            synchronized (configuration) {
                                var names = java.util.stream.StreamSupport.stream(getActiveBehaviors().spliterator(), false)
                                        .map(Behavior::getNameHint)
                                        .filter(s -> s != null && !s.isEmpty())
                                        .map(s -> s.replace(',', '_').replace('"', '_'))
                                        .toArray(String[]::new);
                                respond(sender, "effects", "{" + String.join(",", names) + "}");
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public void respond(WirelessEndpoint endpoint, Object... data) {
        queuedCommand = () -> {
            if (responsePort > 0) {
                double cost = Settings.get().wirelessCostPerRange[Tier.Two] * CommandRange;
                double epsilon = 0.1;
                if (changeBuffer(-cost) > -epsilon) {
                    Object[] payload = new Object[data.length + 1];
                    payload[0] = "nanomachines";
                    System.arraycopy(data, 0, payload, 1, data.length);
                    Packet packet = li.cil.oc.api.Network.newPacket(uuid, null, responsePort, payload);
                    li.cil.oc.api.Network.sendWirelessPacket(this, CommandRange, packet);
                }
            }
        };
        commandDelay = (int) (Settings.get().nanomachinesCommandDelay * 20);
    }

    @SuppressWarnings("unused")
    @Override
    public Controller reconfigure() {
        if (isServer()) synchronized (configuration) {
            configuration.reconfigure();
            activeBehaviorsDirty = true;
            if (player instanceof ServerPlayer sp) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100));
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 150));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200));
                changeBuffer(-Settings.get().nanomachineReconfigureCost);
                hasSentConfiguration = false;
            }
        }
        return this;
    }

    public int getTotalInputCount() {
        synchronized (configuration) {
            return configuration.triggers.size();
        }
    }

    public int getSafeActiveInputs() {
        return Settings.get().nanomachinesSafeInputsActive;
    }

    public int getMaxActiveInputs() {
        return Settings.get().nanomachinesMaxInputsActive;
    }

    public boolean getInput(int index) {
        synchronized (configuration) {
            return configuration.triggers.get(index).isActive;
        }
    }

    public boolean setInput(int index, boolean value) {
        if (!isServer()) return false;
        synchronized (configuration) {
            if (!value || configuration.triggers.stream().filter(t -> t.isActive).count() < Settings.get().nanomachinesMaxInputsActive) {
                configuration.triggers.get(index).isActive = value;
                activeBehaviorsDirty = true;
                return true;
            }
            return false;
        }
    }

    @Override
    public Iterable<Behavior> getActiveBehaviors() {
        synchronized (configuration) {
            cleanActiveBehaviors(DisableReason.InputChanged);
            return activeBehaviors;
        }
    }

    @Override
    public int getInputCount(Behavior behavior) {
        synchronized (configuration) {
            var node = configuration.behaviorMap.get(behavior);
            if (node != null) return (int) node.inputs.stream().filter(NeuralNetwork.Neuron::isActive).count();
            return 0;
        }
    }

    @Override
    public double getLocalBuffer() {
        return storedEnergy;
    }

    @Override
    public double getLocalBufferSize() {
        return Settings.get().bufferNanomachines;
    }

    @Override
    public double changeBuffer(double delta) {
        if (isClient()) return delta;
        if (delta < 0 && (Settings.get().ignorePower || player.getAbilities().instabuild)) return 0.0;
        double newValue = storedEnergy + delta;
        storedEnergy = Math.clamp(newValue, 0, getLocalBufferSize());
        return newValue - storedEnergy;
    }

    public void update() {
        if (player.isDeadOrDying()) return;

        if (isServer()) {
            if (commandDelay > 0) {
                commandDelay--;
                if (commandDelay == 0 && queuedCommand != null) {
                    queuedCommand.run();
                    queuedCommand = null;
                }
            }

            String currentDim = player.level().dimension().location().toString();
            if (!currentDim.equals(previousDimension)) {
                li.cil.oc.api.Network.leaveWirelessNetwork(this, previousDimension);
                li.cil.oc.api.Network.joinWirelessNetwork(this);
                previousDimension = currentDim;
            } else {
                li.cil.oc.api.Network.updateWirelessNetwork(this);
            }
        }

        boolean hasPower = getLocalBuffer() > 0 || Settings.get().ignorePower;

        if (hasPower != hadPower) {
            if (!hasPower) {
                for (Behavior b : getActiveBehaviors()) b.onDisable(DisableReason.OutOfEnergy);
                hasPower = getLocalBuffer() > 0 || Settings.get().ignorePower;
            } else {
                for (Behavior b : getActiveBehaviors()) b.onEnable();
            }
        }

        if (hasPower) {
            for (Behavior b : getActiveBehaviors()) b.update();
            if (isServer()) {
                if (player.level().getGameTime() % Settings.get().tickFrequency == 0) {
                    long activeInputs = configuration.triggers.stream().filter(t -> t.isActive).count();
                    changeBuffer(-Settings.get().nanomachineCost * Settings.get().tickFrequency * (activeInputs + 0.5));
                    PacketSender.sendNanomachinePower(player);
                }

                long activeInputs = configuration.triggers.stream().filter(t -> t.isActive).count();
                int overload = (int) activeInputs - getSafeActiveInputs();
                if (!player.getAbilities().instabuild && overload > 0 && player.level().getGameTime() % 20 == 0) {
                    player.hurt(OverloadDamage, overload);
                }
            }

            if (isClient() && Settings.get().enableNanomachinePfx) {
                double energyRatio = getLocalBuffer() / (getLocalBufferSize() + 1);
                long activeInputs = configuration.triggers.stream().filter(t -> t.isActive).count();
                double triggerRatio = activeInputs / (configuration.triggers.size() + 1.0);
                double intensity = (energyRatio + triggerRatio) * 0.25;
                PlayerUtils.spawnParticleAround(player, "portal", intensity);
            }
        }

        if (isServer()) {
            if (hadPower != hasPower) PacketSender.sendNanomachinePower(player);
            if (!hasSentConfiguration || player.level().getGameTime() % FullSyncInterval == 0) {
                hasSentConfiguration = true;
                PacketSender.sendNanomachineConfiguration(player);
            }
        }

        hadPower = hasPower;
    }

    public void reset() {
        synchronized (configuration) {
            for (var trigger : configuration.triggers) {
                trigger.isActive = false;
                activeBehaviorsDirty = true;
            }
            cleanActiveBehaviors(DisableReason.Default);
        }
    }

    public void dispose() {
        reset();
        if (isServer()) li.cil.oc.api.Network.leaveWirelessNetwork(this);
    }

    public void debug() {
        if (isServer()) {
            configuration.debug();
            activeBehaviorsDirty = true;
        }
    }

    public void print() {
        if (isServer()) configuration.print(player);
    }

    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        synchronized (configuration) {
            nbt.putString("uuid", uuid);
            nbt.putInt("port", responsePort);
            nbt.putDouble("energy", storedEnergy);
            var cfgNbt = new CompoundTag();
            configuration.save(cfgNbt, provider);
            nbt.put("configuration", cfgNbt);
        }
    }

    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        synchronized (configuration) {
            uuid = nbt.getString("uuid");
            responsePort = nbt.getInt("port");
            storedEnergy = nbt.getDouble("energy");
            configuration.load(nbt.getCompound("configuration"), provider);
            activeBehaviorsDirty = true;
        }
    }

    private boolean isClient() {
        return player.level().isClientSide();
    }

    private boolean isServer() {
        return !isClient();
    }

    private void cleanActiveBehaviors(DisableReason reason) {
        if (activeBehaviorsDirty) {
            synchronized (configuration) {
                if (activeBehaviorsDirty) {
                    var newBehaviors = new HashSet<Behavior>();
                    for (var bn : configuration.behaviors) {
                        if (bn.isActive()) newBehaviors.add(bn.behavior);
                    }
                    var added = new HashSet<>(newBehaviors);
                    added.removeAll(activeBehaviors);
                    var removed = new HashSet<>(activeBehaviors);
                    removed.removeAll(newBehaviors);
                    activeBehaviors.clear();
                    activeBehaviors.addAll(newBehaviors);
                    activeBehaviorsDirty = false;
                    added.forEach(Behavior::onEnable);
                    removed.forEach(b -> b.onDisable(reason));
                    if (isServer()) PacketSender.sendNanomachineInputs(player);
                }
            }
        }
    }
}

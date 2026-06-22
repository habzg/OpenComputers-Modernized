package li.cil.oc.neoforge.common.nanomachines;

import li.cil.oc.api.Persistable;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.PacketSender;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@SuppressWarnings("SuspiciousMethodCalls")
public class NeuralNetwork implements Persistable {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeuralNetwork.class);
    public final List<TriggerNeuron> triggers = new ArrayList<>();
    public final List<ConnectorNeuron> connectors = new ArrayList<>();
    public final List<BehaviorNeuron> behaviors = new ArrayList<>();
    public final Map<Behavior, BehaviorNeuron> behaviorMap = new HashMap<>();
    public final ControllerImpl controller;

    public NeuralNetwork(ControllerImpl controller) {
        this.controller = controller;
    }

    public void reconfigure() {
        behaviors.clear();
        for (var provider : li.cil.oc.api.Nanomachines.getProviders()) {
            var behs = provider.createBehaviors(controller.player);
            if (behs != null) {
                for (var b : behs) {
                    if (b != null) behaviors.add(new BehaviorNeuron(provider, b));
                }
            }
        }

        int quota = (int) (behaviors.size() * Settings.get().nanomachineTriggerQuota);
        while (triggers.size() > quota) triggers.removeLast();
        for (var t : triggers) t.isActive = false;
        while (triggers.size() < quota) triggers.add(new TriggerNeuron());

        int connQuota = (int) (behaviors.size() * Settings.get().nanomachineConnectorQuota);
        while (connectors.size() > connQuota) connectors.removeLast();
        for (var c : connectors) c.inputs.clear();
        while (connectors.size() < connQuota) connectors.add(new ConnectorNeuron());

        var rng = new Random(controller.player.level().random.nextInt());

        var sourcePool = new ArrayList<Neuron>();
        for (int i = 0; i < Settings.get().nanomachineMaxOutputs; i++) sourcePool.addAll(triggers);
        connect(connectors, sourcePool, rng);
        for (int i = 0; i < Settings.get().nanomachineMaxOutputs; i++) sourcePool.addAll(connectors);
        connect(behaviors, sourcePool, rng);

        var deadConnectors = connectors.stream().filter(c -> c.inputs.isEmpty()).toList();
        connectors.removeAll(deadConnectors);
        for (var b : behaviors) b.inputs.removeAll(deadConnectors);

        var deadBehaviors = behaviors.stream().filter(b -> b.inputs.isEmpty()).toList();
        behaviors.removeAll(deadBehaviors);

        behaviorMap.clear();
        for (var n : behaviors) behaviorMap.put(n.behavior, n);
    }

    private <Sink extends ConnectorNeuron, Source extends Neuron> void connect(List<Sink> sinks, List<Source> sources, Random rng) {
        var sinkPool = new ArrayList<>(sinks);
        Collections.shuffle(sinkPool, rng);
        for (var sink : sinkPool) {
            if (sources.isEmpty()) break;
            Set<Source> blacklist = new HashSet<>();
            int count = rng.nextInt(Settings.get().nanomachineMaxInputs + 1);
            for (int n = 0; n < count && !sources.isEmpty(); n++) {
                int baseIndex = rng.nextInt(sources.size());
                int sourceIndex = -1;
                for (int i = 0; i < sources.size(); i++) {
                    var s = sources.get((i + baseIndex) % sources.size());
                    if (!blacklist.contains(s)) {
                        sourceIndex = (i + baseIndex) % sources.size();
                        break;
                    }
                }
                if (sourceIndex >= 0) {
                    var source = sources.remove(sourceIndex);
                    blacklist.add(source);
                    sink.inputs.add(source);
                }
            }
        }
    }

    public void debug() {
        behaviors.clear();
        for (var provider : li.cil.oc.api.Nanomachines.getProviders()) {
            var behs = provider.createBehaviors(controller.player);
            if (behs != null) {
                for (var b : behs) {
                    if (b != null) behaviors.add(new BehaviorNeuron(provider, b));
                }
            }
        }
        connectors.clear();
        triggers.clear();
        for (int i = 0; i < behaviors.size(); i++) {
            var behavior = behaviors.get(i);
            var trigger = new TriggerNeuron();
            triggers.add(trigger);
            behavior.inputs.add(trigger);
            var log = controller.player instanceof ServerPlayer sp ?
                    (java.util.function.Consumer<String>) s -> PacketSender.sendClientLog(s, sp) :
                    (java.util.function.Consumer<String>) LOGGER::info;
            log.accept(i + " -> " + behavior.behavior.getNameHint() + " (" + behavior.behavior.getClass() + ")");
        }
    }

    public void print(Player player) {
        var sb = new StringBuilder();
        for (var behavior : behaviors) {
            String name = behavior.behavior.getNameHint() != null ? behavior.behavior.getNameHint() : behavior.behavior.getClass().getSimpleName();
            if (behavior.isActive()) sb.append("§a");
            else sb.append("§c");
            sb.append(name);
            sb.append("§r <- (");
            boolean first = true;
            for (var input : behavior.inputs) {
                if (first) first = false;
                else sb.append(", ");
                if (input instanceof TriggerNeuron tn) {
                    int idx = triggers.indexOf(tn);
                    if (tn.isActive()) sb.append("§a").append(idx + 1).append("§r");
                    else sb.append("§c").append(idx + 1).append("§r");
                } else if (input instanceof ConnectorNeuron cn) {
                    sb.append("(");
                    boolean f2 = true;
                    for (var trig : cn.inputs) {
                        if (f2) f2 = false;
                        else sb.append(", ");
                        int idx = triggers.indexOf(trig);
                        if (trig.isActive()) sb.append("§a").append(idx + 1).append("§r");
                        else sb.append("§c").append(idx + 1).append("§r");
                    }
                    sb.append(")");
                }
            }
            sb.append(")");
            player.sendSystemMessage(Component.literal(sb.toString()));
            sb.setLength(0);
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        save(nbt, false);
    }

    public void save(CompoundTag nbt, boolean forItem) {
        var triggersList = new ListTag();
        for (var t : triggers) {
            var tnbt = new CompoundTag();
            tnbt.putBoolean("isActive", t.isActive && !forItem);
            triggersList.add(tnbt);
        }
        nbt.put("triggers", triggersList);

        var connectorsList = new ListTag();
        for (var c : connectors) {
            var cnbt = new CompoundTag();
            cnbt.putIntArray("triggerInputs", c.inputs.stream().mapToInt(triggers::indexOf).filter(i -> i >= 0).toArray());
            connectorsList.add(cnbt);
        }
        nbt.put("connectors", connectorsList);

        var behaviorsList = new ListTag();
        for (var b : behaviors) {
            var bnbt = new CompoundTag();
            bnbt.putIntArray("triggerInputs", b.inputs.stream().mapToInt(triggers::indexOf).filter(i -> i >= 0).toArray());
            bnbt.putIntArray("connectorInputs", b.inputs.stream().mapToInt(connectors::indexOf).filter(i -> i >= 0).toArray());
            bnbt.put("behavior", b.provider.writeToNBT(b.behavior));
            behaviorsList.add(bnbt);
        }
        nbt.put("behaviors", behaviorsList);
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider registry) {
        triggers.clear();
        var triggersList = nbt.getList("triggers", Tag.TAG_COMPOUND);
        for (int i = 0; i < triggersList.size(); i++) {
            var tnbt = triggersList.getCompound(i);
            var neuron = new TriggerNeuron();
            neuron.isActive = tnbt.getBoolean("isActive");
            triggers.add(neuron);
        }

        connectors.clear();
        var connectorsList = nbt.getList("connectors", Tag.TAG_COMPOUND);
        for (int i = 0; i < connectorsList.size(); i++) {
            var cnbt = connectorsList.getCompound(i);
            var neuron = new ConnectorNeuron();
            for (int idx : cnbt.getIntArray("triggerInputs")) neuron.inputs.add(triggers.get(idx));
            connectors.add(neuron);
        }

        behaviors.clear();
        var behaviorsList = nbt.getList("behaviors", Tag.TAG_COMPOUND);
        for (int i = 0; i < behaviorsList.size(); i++) {
            var bnbt = behaviorsList.getCompound(i);
            for (var regProvider : li.cil.oc.api.Nanomachines.getProviders()) {
                var behavior = regProvider.readFromNBT(controller.player, bnbt.getCompound("behavior"));
                if (behavior != null) {
                    var neuron = new BehaviorNeuron(regProvider, behavior);
                    for (int idx : bnbt.getIntArray("triggerInputs")) neuron.inputs.add(triggers.get(idx));
                    for (int idx : bnbt.getIntArray("connectorInputs")) neuron.inputs.add(connectors.get(idx));
                    behaviors.add(neuron);
                    break;
                }
            }
        }

        behaviorMap.clear();
        for (var n : behaviors) behaviorMap.put(n.behavior, n);
    }

    public interface Neuron {
        boolean isActive();
    }

    public static class TriggerNeuron implements Neuron {
        public boolean isActive = false;

        @Override
        public boolean isActive() {
            return isActive;
        }
    }

    public static class ConnectorNeuron implements Neuron {
        public final List<Neuron> inputs = new ArrayList<>();

        @Override
        public boolean isActive() {
            return inputs.stream().allMatch(Neuron::isActive);
        }
    }

    public static class BehaviorNeuron extends ConnectorNeuron {
        public final BehaviorProvider provider;
        public final Behavior behavior;

        public BehaviorNeuron(BehaviorProvider provider, Behavior behavior) {
            this.provider = provider;
            this.behavior = behavior;
        }

    }
}

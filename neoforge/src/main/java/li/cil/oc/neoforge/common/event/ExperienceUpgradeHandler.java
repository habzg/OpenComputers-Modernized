package li.cil.oc.neoforge.common.event;

import li.cil.oc.api.event.RobotAnalyzeEvent;
import li.cil.oc.api.event.RobotAttackEntityEvent;
import li.cil.oc.api.event.RobotBreakBlockEvent;
import li.cil.oc.api.event.RobotExhaustionEvent;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.event.RobotPlaceBlockEvent;
import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.server.component.UpgradeExperience;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;

public final class ExperienceUpgradeHandler {
    @SubscribeEvent
    public static void onRobotAnalyze(RobotAnalyzeEvent e) {
        double[] levelAndExp = getLevelAndExperience(e.agent);
        double experience = levelAndExp[1];
        if (experience != 0.0) {
            e.player.sendSystemMessage(Component.translatable("gui.opencomputers.analyzer.robotxp", String.format("%.2f", experience), String.valueOf((int) levelAndExp[0])));
        }
    }

    @SubscribeEvent
    public static void onRobotComputeDamageRate(RobotUsedToolEvent.ComputeDamageRate e) {
        e.setDamageRate(e.getDamageRate() * Math.max(0, 1 - getLevel(e.agent) * OCSettings.get().toolEfficiencyPerLevel));
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onRobotBreakBlockPre(RobotBreakBlockEvent.Pre e) {
        double boost = Math.max(0, 1 - getLevel(e.agent) * OCSettings.get().harvestSpeedBoostPerLevel);
        e.setBreakTime((long) (e.getBreakTime() * boost));
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onRobotAttackEntityPost(RobotAttackEntityEvent.Post e) {
        if (e.agent instanceof Robot robot) {
            if (!robot.equipmentInventory().getItem(0).isEmpty() && e.target instanceof net.minecraft.world.entity.LivingEntity le && le.isDeadOrDying()) {
                addExperience(robot, OCSettings.get().robotActionXp);
            }
        }
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onRobotBreakBlockPost(RobotBreakBlockEvent.Post e) {
        addExperience(e.agent, e.experience * OCSettings.get().robotOreXpRate + OCSettings.get().robotActionXp);
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onRobotPlaceBlockPost(RobotPlaceBlockEvent.Post e) {
        addExperience(e.agent, OCSettings.get().robotActionXp);
    }

    @SubscribeEvent
    public static void onRobotMovePost(RobotMoveEvent.Post e) {
        addExperience(e.agent, OCSettings.get().robotExhaustionXpRate * 0.01);
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onRobotExhaustion(RobotExhaustionEvent e) {
        addExperience(e.agent, OCSettings.get().robotExhaustionXpRate * e.exhaustion);
    }

    private static int getLevel(Agent agent) {
        int[] level = {0};
        foreachUpgrade(agent.machine().node(), upgrade -> level[0] += upgrade.level);
        return level[0];
    }

    private static double[] getLevelAndExperience(Agent agent) {
        int[] level = {0};
        double[] experience = {0.0};
        foreachUpgrade(agent.machine().node(), upgrade -> {
            level[0] += upgrade.level;
            experience[0] += upgrade.experience;
        });
        return new double[]{level[0], experience[0]};
    }

    private static void addExperience(Agent agent, double amount) {
        foreachUpgrade(agent.machine().node(), upgrade -> upgrade.addExperience(amount));
    }

    private static void foreachUpgrade(Node node, java.util.function.Consumer<UpgradeExperience> f) {
        if (node == null) return;
        for (Node n : node.reachableNodes()) {
            if (n.host() instanceof UpgradeExperience) {
                f.accept((UpgradeExperience) n.host());
            }
        }
    }
}

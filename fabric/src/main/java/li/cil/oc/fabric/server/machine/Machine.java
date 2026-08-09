package li.cil.oc.fabric.server.machine;

import java.util.function.BooleanSupplier;
import li.cil.oc.api.detail.MachineAPI;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.blockentity.traits.Computer;
import li.cil.oc.core.impl.server.machine.MachineBase;
import li.cil.oc.fabric.common.EventHandler;
import net.minecraft.world.level.block.entity.BlockEntity;

public class Machine extends MachineBase {
    private static BooleanSupplier gamePausedCheck = () -> false;

    public static void setGamePausedCheck(BooleanSupplier check) {
        gamePausedCheck = check;
    }

    public Machine(MachineHost host) {
        super(host);
    }

    @Override
    protected void platformScheduleClose() {
        EventHandler.scheduleClose(this);
    }

    @Override
    protected void platformUnscheduleClose() {
        EventHandler.unscheduleClose(this);
    }

    @Override
    protected void platformBeep(int frequency, int duration) {
        PacketSender.sendSound(host.level(), host.xPosition(), host.yPosition(), host.zPosition(), frequency, duration);
    }

    @Override
    protected void platformBeep(String pattern) {
        PacketSender.sendSound(host.level(), host.xPosition(), host.yPosition(), host.zPosition(), pattern);
    }

    @Override
    protected void platformSendComputerUserList(String[] list) {
        if (host instanceof Computer computer) {
            PacketSender.sendComputerUserList((BlockEntity) computer, list);
        }
    }

    @Override
    protected boolean platformIsGamePaused() {
        return gamePausedCheck.getAsBoolean();
    }

    public static class API implements MachineAPI {
        @Override
        public void add(Class<? extends Architecture> architecture) {
            MachineBase.add(architecture);
        }

        @Override
        public java.util.List<Class<? extends Architecture>> architectures() {
            return MachineBase.architectures();
        }

        @Override
        public li.cil.oc.api.machine.Machine create(MachineHost host) {
            return new Machine(host);
        }

        @Override
        public String getArchitectureName(Class<? extends Architecture> architecture) {
            return MachineBase.getArchitectureName(architecture);
        }
    }
}

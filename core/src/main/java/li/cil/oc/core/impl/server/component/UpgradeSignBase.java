package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.jetbrains.annotations.Nullable;


import java.util.Map;

public abstract class UpgradeSignBase extends li.cil.oc.api.prefab.ManagedEnvironment implements DeviceInfo {
    private final Map<String, String> deviceInfo = new java.util.HashMap<>() {{
        put(DeviceAttribute.Class, DeviceClass.Generic);
        put(DeviceAttribute.Description, "Sign upgrade");
        put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
        put(DeviceAttribute.Product, "Labelizer Deluxe");
    }};

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    public abstract EnvironmentHost host();

    protected Object[] getValue(@Nullable SignBlockEntity tileEntity) {
        if (tileEntity == null) {
            return ResultWrapper.result(null, "no sign");
        }
        StringBuilder sb = new StringBuilder();
        var text = tileEntity.getFrontText();
        for (int i = 0; i < 4; i++) {
            if (i > 0) sb.append("\n");
            String line = text.getMessage(i, false).getString();
            sb.append(line);
        }
        return ResultWrapper.result(sb.toString());
    }

    protected abstract Player getSignPlayer();

    protected abstract boolean checkSignBreak(Player player, BlockPos pos, net.minecraft.world.level.block.state.BlockState state);

    protected abstract boolean fireSignPreEvent(SignBlockEntity tileEntity, String[] lines);

    protected abstract void fireSignPostEvent(SignBlockEntity tileEntity, String[] lines);

    protected Object[] setValue(@Nullable SignBlockEntity tileEntity, String text) {
        if (tileEntity == null) {
            return ResultWrapper.result(null, "no sign");
        }
        Player player = getSignPlayer();
        String[] parts = text.split("\n", -1);
        String[] lines = new String[4];
        for (int i = 0; i < 4; i++) {
            if (i < parts.length && parts[i] != null) {
                lines[i] = parts[i].length() > 15 ? parts[i].substring(0, 15) : parts[i];
            } else {
                lines[i] = "";
            }
        }
        if (!canChangeSign(player, tileEntity, lines)) {
            return ResultWrapper.result(null, "not allowed");
        }
        var oldText = tileEntity.getFrontText();
        for (int i = 0; i < 4; i++) {
            oldText = oldText.setMessage(i, Component.literal(lines[i]));
        }
        tileEntity.setText(oldText, true);
        var pos = tileEntity.getBlockPos();
        host().level().sendBlockUpdated(pos, tileEntity.getBlockState(), tileEntity.getBlockState(), 3);
        fireSignPostEvent(tileEntity, lines);
        StringBuilder sb = new StringBuilder();
        var newText = tileEntity.getFrontText();
        for (int i = 0; i < 4; i++) {
            if (i > 0) sb.append("\n");
            String line = newText.getMessage(i, false).getString();
            sb.append(line);
        }
        return ResultWrapper.result(sb.toString());
    }

    private boolean canChangeSign(Player player, SignBlockEntity tileEntity, String[] lines) {
        var pos = tileEntity.getBlockPos();
        if (!host().level().mayInteract(player, pos)) {
            return false;
        }
        if (!checkSignBreak(player, pos, tileEntity.getBlockState())) {
            return false;
        }
        return fireSignPreEvent(tileEntity, lines);
    }

    protected @Nullable SignBlockEntity findSign(Direction side) {
        BlockPosition hostPos = BlockPosition.apply(host());
        var hostPosB = new BlockPos(hostPos.x(), hostPos.y(), hostPos.z());
        var te = host().level().getBlockEntity(hostPosB);
        if (te instanceof SignBlockEntity sign) {
            return sign;
        }
        BlockPosition offsetPos = hostPos.offset(side);
        var offsetPosB = new BlockPos(offsetPos.x(), offsetPos.y(), offsetPos.z());
        te = host().level().getBlockEntity(offsetPosB);
        if (te instanceof SignBlockEntity sign) {
            return sign;
        }
        return null;
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("tablet.use".equals(message.name()) && message.source().host() instanceof li.cil.oc.api.machine.Machine machine) {
            if (machine.host() instanceof li.cil.oc.api.internal.Tablet && message.data().length >= 7) {
                Object[] data = message.data();
                CompoundTag nbt = (CompoundTag) data[0];
                BlockPosition blockPos = (BlockPosition) data[3];
                var blockPosM = new BlockPos(blockPos.x(), blockPos.y(), blockPos.z());
                var te = host().level().getBlockEntity(blockPosM);
                if (te instanceof SignBlockEntity sign) {
                    StringBuilder sb = new StringBuilder();
                    var text = sign.getFrontText();
                    for (int i = 0; i < 4; i++) {
                        if (i > 0) sb.append("\n");
                        String line = text.getMessage(i, false).getString();
                        sb.append(line);
                    }
                    nbt.putString("signText", sb.toString());
                }
            }
        }
    }
}

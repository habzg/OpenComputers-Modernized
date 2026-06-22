package li.cil.oc.core.impl.common.tileentity;

import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.common.tileentity.traits.NotAnalyzable;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.tileentity.traits.Colored;
import li.cil.oc.core.impl.common.tileentity.traits.Environment;
import li.cil.oc.core.impl.common.tileentity.traits.TileEntity;
import li.cil.oc.core.impl.util.Color;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.ItemColorizer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class Cable extends TileEntity implements Environment, NotAnalyzable, Colored {

    public static BlockEntityType<Cable> TYPE;
    public final Node node = li.cil.oc.api.Network.newNode(this, Visibility.None).create();
    private int cableColor = Color.LightGray;

    public Cable(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public Level level() {
        return getLevel();
    }

    @Override
    public double xPosition() {
        return worldPosition.getX() + 0.5;
    }

    @Override
    public double yPosition() {
        return worldPosition.getY() + 0.5;
    }

    @Override
    public double zPosition() {
        return worldPosition.getZ() + 0.5;
    }

    @Override
    public void markChanged() {
        setChanged();
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level() != null && isServer()) {
            EventHandlerDelegate.get().scheduleServer(this);
        }
    }

    @Override
    public boolean isConnected() {
        return node.address() != null && node.network() != null;
    }

    @Override
    public void onConnect(Node node) {
    }

    @Override
    public void onDisconnect(Node node) {
    }

    @Override
    public void onMessage(li.cil.oc.api.network.Message message) {
    }

    @Override
    public Object result(Object... args) {
        return li.cil.oc.core.util.ResultWrapper.result(args);
    }

    @Override
    public void readFromNBTForServer(CompoundTag nbt) {
        super.readFromNBTForServer(nbt);
        if (nbt.contains(li.cil.oc.core.impl.Settings.namespace + "renderColor")) {
            cableColor = nbt.getInt(li.cil.oc.core.impl.Settings.namespace + "renderColor");
        }
        var provider = getEffectiveProvider();
        if (node.host() == this && provider != null) {
            node.load(nbt.getCompound(li.cil.oc.core.impl.Settings.namespace + "node"), provider);
        }
    }

    @Override
    public void writeToNBTForServer(CompoundTag nbt) {
        super.writeToNBTForServer(nbt);
        nbt.putInt(li.cil.oc.core.impl.Settings.namespace + "renderColor", cableColor);
        if (node.host() == this) {
            var tag = new net.minecraft.nbt.CompoundTag();
            node.save(tag, getEffectiveProvider());
            nbt.put(li.cil.oc.core.impl.Settings.namespace + "node", tag);
        }
    }

    @Override
    public void readFromNBTForClient(CompoundTag nbt) {
        cableColor = nbt.getInt("renderColor");
    }

    @Override
    public void writeToNBTForClient(CompoundTag nbt) {
        nbt.putInt("renderColor", cableColor);
    }

    @Override
    public Node[] onAnalyze(net.minecraft.world.entity.player.Player player, int side, float hitX, float hitY, float hitZ) {
        return null;
    }

    @Override
    public int getColor() {
        return color();
    }

    @Override
    public void setColor(int value) {
        color(value);
    }

    @Override
    public int color() {
        return cableColor;
    }

    @Override
    public void color(int value) {
        if (value != cableColor) {
            cableColor = value;
            onColorChanged();
        }
    }

    @Override
    public boolean consumesDye() {
        return true;
    }

    @Override
    public boolean controlsConnectivity() {
        return true;
    }

    @Override
    public void onColorChanged() {
        if (level() != null && isServer()) {
            PacketSender.sendColorChange(this, color());
            li.cil.oc.api.Network.joinOrCreateNetwork(this);
        }
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag nbt, net.minecraft.core.HolderLookup.@NotNull Provider provider) {
        super.loadAdditional(nbt, provider);
        if (isServer()) {
            readFromNBTForServer(nbt);
        }
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt, net.minecraft.core.HolderLookup.@NotNull Provider provider) {
        super.saveAdditional(nbt, provider);
        if (isServer()) writeToNBTForServer(nbt);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        dispose();
    }

    @Override
    public void onChunkUnloaded() {
        dispose();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (isServer()) {
            node.remove();
        }
    }

    public ItemStack createItemStack() {
        var stack = new ItemStack(getBlockState().getBlock().asItem());
        if (color() != Color.LightGray) ItemColorizer.setColor(stack, color());
        return stack;
    }

    public void fromItemStack(ItemStack stack) {
        if (ItemColorizer.hasColor(stack)) color(ItemColorizer.getColor(stack));
    }
}

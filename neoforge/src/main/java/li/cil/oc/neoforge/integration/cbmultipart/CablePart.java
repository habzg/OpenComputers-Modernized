package li.cil.oc.neoforge.integration.cbmultipart;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.multipart.api.MultipartType;
import codechicken.multipart.api.part.NormalOcclusionPart;
import codechicken.multipart.api.part.SlottedPart;
import codechicken.multipart.util.PartMap;
import codechicken.multipart.util.PartRayTraceResult;
import li.cil.oc.api.Items;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.Color;
import li.cil.oc.neoforge.common.block.Cable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class CablePart extends SimpleBlockPart
        implements NormalOcclusionPart, SlottedPart, Environment {

    private static final VoxelShape OCCLUSION_BOX = Shapes.box(0.375, 0.375, 0.375, 0.625, 0.625, 0.625);

    private final li.cil.oc.core.impl.common.tileentity.Cable original;
    public final Node node;
    private int color = Color.LightGray;

    public CablePart() {
        this(null);
    }

    public CablePart(li.cil.oc.core.impl.common.tileentity.Cable original) {
        this.original = original;
        node = Network.newNode(this, Visibility.None).create();
        if (original != null) color = original.color();
    }

    public int getColor() {
        return color;
    }

    public void setColor(int value) {
        if (value != color) {
            color = value;
            onColorChanged();
        }
    }

    private void onColorChanged() {
        if (hasLevel() && !level().isClientSide) {
            sendUpdate(this::writeDesc);
            li.cil.oc.api.Network.joinOrCreateNetwork(tile());
        }
    }

    @Override
    public @NotNull MultipartType<?> getType() {
        return MultipartRegistrations.CABLE_TYPE.get();
    }

    @Override
    public Cable simpleBlock() {
        return (Cable) Items.get(Constants.BlockName.Cable).block();
    }

    @Override
    public float getStrength(@NotNull Player player, @NotNull PartRayTraceResult hit) {
        return simpleBlock().defaultBlockState().getDestroyProgress(player, level(), pos());
    }

    @Override
    public @NotNull VoxelShape getOcclusionShape() {
        return OCCLUSION_BOX;
    }

    @Override
    public @NotNull VoxelShape getShape(net.minecraft.world.phys.shapes.@NotNull CollisionContext context) {
        if (!hasLevel()) {
            return OCCLUSION_BOX;
        }
        return simpleBlock().getShape(
                simpleBlock().defaultBlockState(),
                level(),
                pos(),
                context);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(net.minecraft.world.phys.shapes.@NotNull CollisionContext context) {
        return getShape(context);
    }

    @Override
    public int getSlotMask() {
        return 1 << PartMap.CENTER.i;
    }

    @Override
    public net.minecraft.world.@NotNull InteractionResult useWithoutItem(Player player, @NotNull PartRayTraceResult hit) {
        ItemStack held = player.getMainHandItem();
        if (Color.isDye(held)) {
            setColor(Color.dyeColor(held));
            tile().setChanged();
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    @Override
    public void invalidateConvertedTile() {
        super.invalidateConvertedTile();
        if (original != null) {
            original.node().neighbors().forEach(n -> n.connect(node));
        } else if (hasLevel() && !level().isClientSide) {
            for (Direction side : Direction.values()) {
                BlockPos neighborPos = pos().relative(side);
                if (level().hasChunk(neighborPos.getX() >> 4, neighborPos.getZ() >> 4)) {
                    BlockEntity neighborTile = level().getBlockEntity(neighborPos);
                    Node neighborNode = li.cil.oc.core.impl.server.network.Network.getNetworkNode(neighborTile, side.getOpposite());
                    if (neighborNode != null && neighborNode != node && neighborNode.network() != null) {
                        neighborNode.connect(node);
                    }
                }
            }
        }
    }

    @Override
    public void onPartChanged(codechicken.multipart.api.part.MultiPart part) {
        super.onPartChanged(part);
        if (hasLevel() && !level().isClientSide) {
            li.cil.oc.api.Network.joinOrCreateNetwork(tile());
        }
        if (hasLevel()) {
            tile().markRender();
        }
    }

    @Override
    public void onNeighborBlockChanged(net.minecraft.core.@NotNull BlockPos from) {
        super.onNeighborBlockChanged(from);
        if (hasLevel()) {
            tile().markRender();
        }
    }

    @Override
    public void onWorldJoin() {
        super.onWorldJoin();
        li.cil.oc.neoforge.common.EventHandler.scheduleServer(tile());
    }

    @Override
    public void onWorldSeparate() {
        super.onWorldSeparate();
        if (node != null) node.remove();
    }

    @Override
    public void save(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider registries) {
        super.save(nbt, registries);
        if (node != null) {
            var nodeNbt = new CompoundTag();
            node.save(nodeNbt, registries);
            nbt.put(Settings.namespace + "node", nodeNbt);
        }
        nbt.putInt(Settings.namespace + "renderColor", color);
    }

    @Override
    public void load(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider registries) {
        super.load(nbt, registries);
        if (node != null && nbt.contains(Settings.namespace + "node")) {
            node.load(nbt.getCompound(Settings.namespace + "node"), registries);
        }
        if (nbt.contains(Settings.namespace + "renderColor")) {
            color = nbt.getInt(Settings.namespace + "renderColor");
        }
    }

    @Override
    public void writeDesc(@NotNull MCDataOutput packet) {
        super.writeDesc(packet);
        packet.writeInt(color);
    }

    @Override
    public void readDesc(@NotNull MCDataInput packet) {
        super.readDesc(packet);
        color = packet.readInt();
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public void onConnect(Node node) {
    }

    @Override
    public void onDisconnect(Node node) {
    }

    @Override
    public void onMessage(Message message) {
    }
}

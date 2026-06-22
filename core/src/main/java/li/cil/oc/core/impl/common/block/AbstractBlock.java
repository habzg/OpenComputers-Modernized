package li.cil.oc.core.impl.common.block;

import li.cil.oc.core.common.block.traits.StateAware;
import li.cil.oc.core.impl.common.tileentity.traits.Colored;
import li.cil.oc.core.impl.common.tileentity.traits.Inventory;
import li.cil.oc.core.impl.common.tileentity.traits.Rotatable;
import li.cil.oc.core.impl.util.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;


import java.util.List;

public abstract class AbstractBlock extends Block implements EntityBlock {
    public AbstractBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 5f).sound(SoundType.METAL));
    }

    public AbstractBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public ItemStack createItemStack(int amount) {
        return new ItemStack(this, amount);
    }

    public ItemStack createItemStack() {
        return createItemStack(1);
    }

    public void setFacing(Level world, BlockPos pos, Direction value) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof Rotatable) {
            ((Rotatable) te).setFromFacing(value);
        }
    }

    public void setRotationFromEntityPitchAndYaw(Level world, BlockPos pos, Entity value) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof Rotatable rotatable) {
            rotatable.setFromEntityPitchAndYaw(value);
            Direction[] valid = rotatable.validFacings();
            boolean isValid = false;
            for (Direction d : valid) {
                if (d == rotatable.facing()) {
                    isValid = true;
                    break;
                }
            }
            if (!isValid && valid.length > 0) {
                rotatable.setFromFacing(valid[0]);
            }
        }
    }

    @SuppressWarnings("unused")
    public net.minecraft.world.item.Rarity rarity(ItemStack stack) {
        return net.minecraft.world.item.Rarity.COMMON;
    }

    public void addInformation(int metadata, ItemStack stack, Player player, List<Component> tooltip, boolean advanced) {
        tooltipHead(metadata, stack, player, tooltip, advanced);
        tooltipBody(metadata, stack, player, tooltip, advanced);
        tooltipTail(metadata, stack, player, tooltip, advanced);
    }

    protected void tooltipHead(int ignoredMetadata, ItemStack stack, Player ignoredPlayer, List<Component> tooltip, boolean ignoredAdvanced) {
    }

    protected void tooltipBody(int ignoredMetadata, ItemStack stack, Player ignoredPlayer, List<Component> tooltip, boolean ignoredAdvanced) {
    }

    public void tooltipTail(int metadata, ItemStack stack, Player player, List<Component> tooltip, boolean advanced) {
        if (advanced && this instanceof li.cil.oc.core.impl.common.block.traits.PowerAcceptor powerAcceptor) {
            tooltip.add(Component.translatable("tooltip.opencomputers.poweracceptor", (int) powerAcceptor.energyThroughput()));
        }
    }

    @Override
    public boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter reader, @NotNull BlockPos pos) {
        return true;
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return this instanceof StateAware;
    }

    @Override
    public int getLightEmission(@NotNull BlockState state, BlockGetter world, @NotNull BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.api.util.StateAware stateful) {
            var s = stateful.getCurrentState();
            if (s != null && s.contains(li.cil.oc.api.util.StateAware.State.IsWorking)) return 15;
            else if (s != null && s.contains(li.cil.oc.api.util.StateAware.State.CanWork)) return 10;
        }
        return super.getLightEmission(state, world, pos);
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, Level world, @NotNull BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.api.util.StateAware stateful) {
            var s = stateful.getCurrentState();
            if (s != null && s.contains(li.cil.oc.api.util.StateAware.State.IsWorking)) return 15;
            else if (s != null && s.contains(li.cil.oc.api.util.StateAware.State.CanWork)) return 10;
        }
        return 0;
    }

    protected void onDropInventory(@NotNull BlockState ignoredState, @NotNull Level level, @NotNull BlockPos pos) {
        if (!level.isClientSide) {
            BlockEntity te = level.getBlockEntity(pos);
            if (te instanceof Inventory) {
                ((Inventory) te).dropAllSlots();
            }
        }
    }

    @Override
    protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            onDropInventory(state, level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof Colored colored && Color.isDye(stack)) {
            if (!level.isClientSide) {
                colored.color(Color.dyeColor(stack));
                if (colored.consumesDye() && !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.sendBlockUpdated(pos, state, state, 3);
            }
            return ItemInteractionResult.SUCCESS;
        }
        Direction side = hit.getDirection();
        float hitX = (float) (hit.getLocation().x - pos.getX());
        float hitY = (float) (hit.getLocation().y - pos.getY());
        float hitZ = (float) (hit.getLocation().z - pos.getZ());
        if (onBlockActivated(level, pos, player, side, hitX, hitY, hitZ, hand)) {
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        Direction side = hit.getDirection();
        float hitX = (float) (hit.getLocation().x - pos.getX());
        float hitY = (float) (hit.getLocation().y - pos.getY());
        float hitZ = (float) (hit.getLocation().z - pos.getZ());
        if (onBlockActivated(level, pos, player, side, hitX, hitY, hitZ, InteractionHand.MAIN_HAND)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public boolean onBlockActivated(Level world, BlockPos pos, Player player, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        if (this instanceof li.cil.oc.core.impl.common.block.traits.GUI gui) {
            return gui.openGuiFor(world, pos, player, side, hitX, hitY, hitZ);
        }
        return false;
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
    }

    @Override
    public boolean onDestroyedByPlayer(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player, boolean willHarvest, @NotNull net.minecraft.world.level.material.FluidState fluid) {
        return super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return null;
    }
}

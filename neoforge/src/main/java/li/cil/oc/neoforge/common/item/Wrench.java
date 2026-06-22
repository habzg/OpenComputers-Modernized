package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.impl.common.tileentity.traits.Rotatable;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class Wrench extends DelegateItem implements li.cil.oc.api.internal.Wrench {

    public Wrench(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean doesSneakBypassUse(@NotNull ItemStack stack, LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        return true;
    }

    @Override
    public @NotNull InteractionResult onItemUseFirst(@NotNull ItemStack stack, @NotNull UseOnContext context) {
        var world = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!world.isClientSide && (!player.canInteractWithBlock(pos, 9.0) || player.isSpectator())) {
            return InteractionResult.PASS;
        }
        if (world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && world.isAreaLoaded(pos, 1)) {
            var state = world.getBlockState(pos);
            var face = context.getClickedFace();
            var rotation = switch (face) {
                case UP, DOWN, SOUTH -> Rotation.CLOCKWISE_90;
                case NORTH -> Rotation.COUNTERCLOCKWISE_90;
                case WEST -> Rotation.NONE;
                case EAST -> Rotation.CLOCKWISE_180;
            };
            var newState = state.rotate(world, pos, rotation);
            if (newState != state) {
                world.setBlock(pos, newState, 3);
                world.blockUpdated(pos, state.getBlock());
                player.swing(InteractionHand.MAIN_HAND);
                return !world.isClientSide ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof Rotatable rotatable) {
                var currentFacing = rotatable.facing();
                var newFacing = face.getAxis() == Direction.Axis.Y
                        ? Direction.from2DDataValue((currentFacing.get2DDataValue() + 3) & 3)
                        : currentFacing.getClockWise();
                rotatable.facing(newFacing);
                world.blockUpdated(pos, state.getBlock());
                player.swing(InteractionHand.MAIN_HAND);
                return !world.isClientSide ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean useWrenchOnBlock(Player player, Level world, int x, int y, int z, boolean simulate) {
        if (!simulate) {
            player.swing(InteractionHand.MAIN_HAND);
        }
        return true;
    }
}

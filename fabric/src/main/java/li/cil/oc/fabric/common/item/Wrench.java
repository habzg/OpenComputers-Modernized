package li.cil.oc.fabric.common.item;

import li.cil.oc.core.impl.common.blockentity.traits.Rotatable;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class Wrench extends DelegateItem {
    @SuppressWarnings("unused")
    public Wrench(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        var world = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!world.isClientSide && (!player.canInteractWithBlock(pos, 9.0) || player.isSpectator())) {
            return InteractionResult.PASS;
        }
        if (world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            var state = world.getBlockState(pos);
            var face = context.getClickedFace();
            var rotation = switch (face) {
                case UP, DOWN, SOUTH -> Rotation.CLOCKWISE_90;
                case NORTH -> Rotation.COUNTERCLOCKWISE_90;
                case WEST -> Rotation.NONE;
                case EAST -> Rotation.CLOCKWISE_180;
            };
            var newState = state.rotate(rotation);
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
}

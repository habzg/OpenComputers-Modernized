package li.cil.oc.core.impl.integration.vanilla;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlock;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("unused")
public final class DriverNoteBlock extends DriverSidedBlock {
    public DriverNoteBlock() {
        super(new ItemStack(Blocks.NOTE_BLOCK));
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment(world, pos);
    }

    public static final class Environment extends AbstractManagedEnvironment implements NamedBlock {
        private final Level world;
        private final BlockPos pos;

        public Environment(Level world, BlockPos pos) {
            this.world = world;
            this.pos = pos;
            setNode(Network.newNode(this, Visibility.Network).withComponent("note_block").create());
        }

        @Override
        public String preferredName() {
            return "note_block";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(direct = true, doc = "function():number -- Get the currently set pitch on this note block.")
        public Object[] getPitch(Context context, Arguments args) {
            return ResultWrapper.result(world.getBlockState(pos).getValue(NoteBlock.NOTE) + 1);
        }

        @Callback(doc = "function(value:number) -- Set the pitch for this note block. Must be in the interval [1, 25].")
        public Object[] setPitch(Context context, Arguments args) {
            int value = args.checkInteger(0);
            if (value < 1 || value > 25) {
                throw new IllegalArgumentException("invalid pitch");
            }
            BlockState state = world.getBlockState(pos);
            world.setBlock(pos, state.setValue(NoteBlock.NOTE, value - 1), 3);
            return ResultWrapper.result(true);
        }

        @Callback(doc = "function([pitch:number]):boolean -- Triggers the note block if possible. Allows setting the pitch to save a tick.")
        public Object[] trigger(Context context, Arguments args) {
            if (args.count() > 0 && args.checkAny(0) != null) {
                int value = args.checkInteger(0);
                if (value < 1 || value > 25) {
                    throw new IllegalArgumentException("invalid pitch");
                }
                BlockState state = world.getBlockState(pos);
                world.setBlock(pos, state.setValue(NoteBlock.NOTE, value - 1), 3);
            }
            BlockState state = world.getBlockState(pos);
            boolean canTrigger = world.getBlockState(pos.above()).isAir();
            world.blockEvent(pos, state.getBlock(), 0, state.getValue(NoteBlock.NOTE));
            return ResultWrapper.result(canTrigger);
        }
    }

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (stack != null && Block.byItem(stack.getItem()) == Blocks.NOTE_BLOCK) {
                return Environment.class;
            }
            return null;
        }
    }
}

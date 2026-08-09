package li.cil.oc.core.impl.common.block;

import java.util.List;
import li.cil.oc.api.Items;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.block.traits.CustomDrops;
import li.cil.oc.core.impl.common.item.data.PrintData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Print extends RedstoneAware implements CustomDrops<li.cil.oc.core.impl.common.blockentity.Print> {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public Print() {
        this(defaultProperties());
    }

    protected Print(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    private static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1f, 5f).sound(SoundType.METAL).noOcclusion().dynamicShape().lightLevel(state -> state.hasProperty(AbstractBlock.LIGHT_LEVEL) ? state.getValue(AbstractBlock.LIGHT_LEVEL) : 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void tooltipBody(int metadata, ItemStack stack, Player player, List<Component> tooltip, boolean advanced) {
        var data = new PrintData(stack);
        if (data.tooltip != null) {
            for (var line : data.tooltip.split("\n")) {
                tooltip.add(Component.literal(line));
            }
        }
    }

    @Override
    public void tooltipTail(int metadata, ItemStack stack, Player player, List<Component> tooltip, boolean advanced) {
        super.tooltipTail(metadata, stack, player, tooltip, advanced);
        var data = new PrintData(stack);
        if (data.isBeaconBase || Items.get(stack) == Items.get(Constants.BlockName.BeaconBasePrint)) {
            tooltip.add(Component.translatable("tooltip.opencomputers.print.beaconbase"));
        }
        if (data.emitRedstone()) {
            tooltip.add(Component.translatable("tooltip.opencomputers.print.redstonelevel", data.redstoneLevel));
        }
        if (data.lightLevel > 0) {
            tooltip.add(Component.translatable("tooltip.opencomputers.print.lightvalue", data.lightLevel));
        }
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.Print print) {
            return print.state ? print.shapeOn : print.shapeOff;
        }
        return super.getShape(state, world, pos, context);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.Print print) {
            return print.state ? print.shapeOn : print.shapeOff;
        }
        return super.getCollisionShape(state, world, pos, context);
    }

    @Override
    public @NotNull VoxelShape getVisualShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.Print print) {
            return print.state ? print.shapeOn : print.shapeOff;
        }
        return super.getVisualShape(state, world, pos, context);
    }

    @Override
    public int getLightBlock(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos) {
        if (OCSettings.get().printsHaveOpacity && world.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.Print print) {
            return Math.round(print.data.opacity() * 4);
        }
        return super.getLightBlock(state, world, pos);
    }

    @Override
    public boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter reader, @NotNull BlockPos pos) {
        if (OCSettings.get().printsHaveOpacity && reader.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.Print print) {
            return print.data.opacity() <= 0;
        }
        return super.propagatesSkylightDown(state, reader, pos);
    }

    @Override
    public boolean useShapeForLightOcclusion(@NotNull BlockState state) {
        return true;
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state) {
        if (level.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.Print print) {
            return print.data.createItemStack();
        }
        return super.getCloneItemStack(level, pos, state);
    }

    @Override
    public boolean onBlockActivated(Level world, BlockPos pos, Player player, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.Print print) {
            return print.activate();
        }
        return super.onBlockActivated(world, pos, player, side, hitX, hitY, hitZ, hand);
    }

    @Override
    public Class<li.cil.oc.core.impl.common.blockentity.Print> getBlockClass() {
        return li.cil.oc.core.impl.common.blockentity.Print.class;
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(state.getBlock())) return;
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.scheduleTick(pos, this, 0);
        }
    }

    @Override
    protected void tick(@NotNull BlockState state, ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (level.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.Print print) {
            print.syncFromBlockState(state);
            print.updateBounds();
            if (print.state) {
                print.toggleState();
                level.scheduleTick(pos, this, 20);
            }
        }
    }

    @Override
    public void doCustomInit(li.cil.oc.core.impl.common.blockentity.Print blockEntity, LivingEntity player, ItemStack stack) {
        var level = blockEntity.getLevel();
        if (level == null) return;
        blockEntity.data.load(stack, level.registryAccess());
        blockEntity.syncFromBlockState(blockEntity.getBlockState());
        blockEntity.updateBounds();
        blockEntity.updateRedstone();
        var state = blockEntity.getBlockState();
        if (state.hasProperty(AbstractBlock.LIGHT_LEVEL) && state.getValue(AbstractBlock.LIGHT_LEVEL) != blockEntity.data.lightLevel) {
            level.setBlock(blockEntity.getBlockPos(), state.setValue(AbstractBlock.LIGHT_LEVEL, blockEntity.data.lightLevel), 3);
        }
        blockEntity.setChanged();
    }

    @Override
    public void doCustomDrops(li.cil.oc.core.impl.common.blockentity.Print blockEntity, Player player, boolean willHarvest) {
        if (!player.getAbilities().instabuild) {
            var level = blockEntity.getLevel();
            if (level != null) {
                Block.popResource(level, blockEntity.getBlockPos(), blockEntity.data.createItemStack());
            }
        }
    }
}

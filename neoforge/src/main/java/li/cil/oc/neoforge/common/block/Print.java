package li.cil.oc.neoforge.common.block;

import li.cil.oc.api.Items;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.block.traits.CustomDrops;
import li.cil.oc.core.impl.common.item.data.PrintData;
import li.cil.oc.neoforge.common.tileentity.PrintNeoForge;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Print extends RedstoneAware implements CustomDrops<li.cil.oc.core.impl.common.tileentity.Print> {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public Print() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1f, 5f).sound(SoundType.METAL).noOcclusion().dynamicShape());
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
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
        if (world.getBlockEntity(pos) instanceof PrintNeoForge print) {
            return print.state ? print.shapeOn : print.shapeOff;
        }
        return super.getShape(state, world, pos, context);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        if (world.getBlockEntity(pos) instanceof PrintNeoForge print) {
            return print.state ? print.shapeOn : print.shapeOff;
        }
        return super.getCollisionShape(state, world, pos, context);
    }

    @Override
    public @NotNull VoxelShape getVisualShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        if (world.getBlockEntity(pos) instanceof PrintNeoForge print) {
            return print.state ? print.shapeOn : print.shapeOff;
        }
        return super.getVisualShape(state, world, pos, context);
    }

    @Override
    public int getLightEmission(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.tileentity.Print print) {
            return print.data.lightLevel;
        }
        return super.getLightEmission(state, world, pos);
    }

    @Override
    public int getLightBlock(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos) {
        if (Settings.get().printsHaveOpacity && world.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.tileentity.Print print) {
            return Math.round(print.data.opacity() * 4);
        }
        return super.getLightBlock(state, world, pos);
    }

    @Override
    public boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter reader, @NotNull BlockPos pos) {
        if (Settings.get().printsHaveOpacity && reader.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.tileentity.Print print) {
            return print.data.opacity() <= 0;
        }
        return super.propagatesSkylightDown(state, reader, pos);
    }

    @Override
    public boolean useShapeForLightOcclusion(@NotNull BlockState state) {
        return true;
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockState state, @NotNull HitResult hitResult, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        if (level.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.tileentity.Print print) {
            return print.data.createItemStack();
        }
        return super.getCloneItemStack(state, hitResult, level, pos, player);
    }

    @Override
    public boolean onBlockActivated(Level world, BlockPos pos, Player player, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.tileentity.Print print) {
            return print.activate();
        }
        return super.onBlockActivated(world, pos, player, side, hitX, hitY, hitZ, hand);
    }


    @Override
    public Class<li.cil.oc.core.impl.common.tileentity.Print> getTileClass() {
        return li.cil.oc.core.impl.common.tileentity.Print.class;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new li.cil.oc.neoforge.common.tileentity.PrintNeoForge(pos, state);
    }

    @Override
    protected void tick(@NotNull BlockState state, ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (level.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.tileentity.Print print) {
            if (print.state) print.toggleState();
            if (print.state) level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    public void doCustomInit(li.cil.oc.core.impl.common.tileentity.Print tileEntity, LivingEntity player, ItemStack stack) {
        var level = tileEntity.getLevel();
        if (level == null) return;
        tileEntity.data.load(stack, level.registryAccess());
        tileEntity.syncFromBlockState(tileEntity.getBlockState());
        tileEntity.updateBounds();
        tileEntity.updateRedstone();
        tileEntity.setChanged();
    }

    @Override
    public void doCustomDrops(li.cil.oc.core.impl.common.tileentity.Print tileEntity, Player player, boolean willHarvest) {
        if (!player.getAbilities().instabuild) {
            var level = tileEntity.getLevel();
            if (level != null) {
                Block.popResource(level, tileEntity.getBlockPos(), tileEntity.data.createItemStack());
            }
        }
    }

    @Override
    public void onBlockStateChange(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState oldState, @NotNull BlockState newState) {
        super.onBlockStateChange(level, pos, oldState, newState);
        var te = level.getBlockEntity(pos);
        if (te instanceof li.cil.oc.core.impl.common.tileentity.Print print) {
            print.syncFromBlockState(newState);
            print.updateBounds();
        }
    }
}

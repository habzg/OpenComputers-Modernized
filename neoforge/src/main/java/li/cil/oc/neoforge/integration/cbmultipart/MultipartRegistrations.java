package li.cil.oc.neoforge.integration.cbmultipart;

import codechicken.multipart.api.MultipartType;
import codechicken.multipart.api.PartConverter;
import codechicken.multipart.api.SimpleMultipartType;
import codechicken.multipart.api.part.MultiPart;
import codechicken.multipart.util.MultipartPlaceContext;
import java.util.Collection;
import java.util.Collections;
import li.cil.oc.api.Items;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.blockentity.Cable;
import li.cil.oc.core.impl.common.blockentity.Print;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class MultipartRegistrations {
    private static final DeferredRegister<MultipartType<?>> TYPES =
            DeferredRegister.create(MultipartType.MULTIPART_TYPES, OCSettings.resourceDomain);

    private static final DeferredRegister<PartConverter> CONVERTERS =
            DeferredRegister.create(PartConverter.PART_CONVERTERS, OCSettings.resourceDomain);

    public static final DeferredHolder<MultipartType<?>, MultipartType<CablePart>> CABLE_TYPE =
            TYPES.register("cable", () -> new SimpleMultipartType<>(client -> new CablePart()));

    public static final DeferredHolder<MultipartType<?>, MultipartType<PrintPart>> PRINT_TYPE =
            TYPES.register("print", () -> new SimpleMultipartType<>(client -> new PrintPart()));

    public static final DeferredHolder<PartConverter, PartConverter> OC_CONVERTER =
            CONVERTERS.register("oc", OCConverter::new);

    private MultipartRegistrations() {
    }

    public static void init(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        CONVERTERS.register(modEventBus);
    }

    private static final class OCConverter extends PartConverter {
        @Override
        public @NotNull ConversionResult<Collection<MultiPart>> convert(
                LevelAccessor world, @NotNull BlockPos pos, @NotNull BlockState state) {
            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof Cable cable) {
                return ConversionResult.success(Collections.singleton(new CablePart(cable)));
            }
            if (te instanceof Print print) {
                return ConversionResult.success(Collections.singleton(new PrintPart(print)));
            }
            return super.convert(world, pos, state);
        }

        @Override
        public @NotNull ConversionResult<MultiPart> convert(MultipartPlaceContext context) {
            ItemStack stack = context.getItemInHand();
            var cableInfo = Items.get(Constants.BlockName.Cable);
            var printInfo = Items.get(Constants.BlockName.Print);
            if (cableInfo != null && cableInfo.item() != null && stack.getItem() == cableInfo.item()) {
                return ConversionResult.success(new CablePart());
            }
            if (printInfo != null && printInfo.item() != null && stack.getItem() == printInfo.item()) {
                PrintPart part = new PrintPart();
                part.data.load(stack);
                part.facing = facingFromPlayer(context);
                return ConversionResult.success(part);
            }
            return super.convert(context);
        }

        private static Direction facingFromPlayer(MultipartPlaceContext context) {
            var player = context.getPlayer();
            if (player == null) return Direction.SOUTH;
            int yaw = Math.round(player.getYRot() / 360f * 4f) & 3;
            Direction[] yaw2dir = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
            return yaw2dir[yaw].getOpposite();
        }
    }
}

package li.cil.oc.neoforge.integration.enderstorage;

import codechicken.enderstorage.api.Frequency;
import codechicken.enderstorage.tile.TileEnderTank;
import codechicken.enderstorage.tile.TileFrequencyOwner;
import codechicken.lib.colour.EnumColour;
import java.util.HashMap;
import java.util.Map;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.core.impl.integration.ManagedBlockEntityEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

@SuppressWarnings("unused")
public final class DriverFrequencyOwner extends DriverSidedBlockEntity {
    @Override
    public Class<?> getBlockEntityClass() {
        return TileFrequencyOwner.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        return new Environment((TileFrequencyOwner) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<TileFrequencyOwner> implements NamedBlock {
        public Environment(final TileFrequencyOwner blockEntity) {
            super(blockEntity, blockEntity instanceof TileEnderTank ? "ender_tank" : "ender_chest");
        }

        @Override
        public String preferredName() {
            return getBlockEntity() instanceof TileEnderTank ? "ender_tank" : "ender_chest";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():table -- Get the currently set frequency. {left, middle, right}")
        public Object[] getFrequency(final Context context, final Arguments args) {
            Frequency frequency = getBlockEntity().getFrequency();
            return new Object[]{new int[]{
                    frequency.left().ordinal(),
                    frequency.middle().ordinal(),
                    frequency.right().ordinal()
            }};
        }

        @Callback(doc = "function(left:number, middle:number, right:number) -- Set the frequency. Range 0-15 (inclusive).")
        @SuppressWarnings("SameReturnValue")
        public Object[] setFrequency(final Context context, final Arguments args) {
            final int left;
            final int middle;
            final int right;
            if (args.count() == 1) {
                final int freq = args.checkInteger(0);
                if ((freq & 0xFFF) != freq) {
                    throw new IllegalArgumentException("invalid frequency");
                }
                left = (freq >> 8) & 0xF;
                middle = (freq >> 4) & 0xF;
                right = freq & 0xF;
            } else {
                left = args.checkInteger(0);
                middle = args.checkInteger(1);
                right = args.checkInteger(2);
                if ((left & 0xF) != left || (middle & 0xF) != middle || (right & 0xF) != right) {
                    throw new IllegalArgumentException("invalid frequency");
                }
            }
            getBlockEntity().setFreq(getBlockEntity().getFrequency()
                    .withLeft(EnumColour.fromWoolMeta(left))
                    .withMiddle(EnumColour.fromWoolMeta(middle))
                    .withRight(EnumColour.fromWoolMeta(right)));
            return null;
        }

        @Callback(doc = "function():string -- Get the name of the owner, which is usually a player's name or 'global'.")
        public Object[] getOwner(final Context context, final Arguments args) {
            Frequency frequency = getBlockEntity().getFrequency();
            if (frequency.ownerName().isPresent()) {
                return new Object[]{frequency.ownerName().get().getString()};
            }
            return new Object[]{"global"};
        }

        @Callback(doc = "function():table -- Get the currently set frequency as a table of color names.")
        public Object[] getFrequencyColors(final Context context, final Arguments args) {
            Frequency frequency = getBlockEntity().getFrequency();
            return new Object[]{frequency.toArray()};
        }

        @Callback(doc = "function():table -- Get a table with the mapping of colors (as Minecraft names) to Frequency numbers. NB: Frequencies are zero based!")
        public Object[] getColors(final Context context, final Arguments args) {
            EnumColour[] colors = EnumColour.values();
            Map<Integer, EnumColour> map = new HashMap<>();
            for (int i = 0; i < colors.length; i++) {
                map.put(i, colors[i]);
            }
            return new Object[]{map};
        }
    }
}

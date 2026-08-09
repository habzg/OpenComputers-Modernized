package li.cil.oc.core.impl.common.item.data;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import li.cil.oc.api.Items;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.ReflectionUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public class PrintData extends ItemData {
    private static final int stepping = 4;
    private static final float step = stepping / 16f;
    private static final float invMaxVolume = 1f / (stepping * stepping * stepping);
    private static final Set<Method> inkProviders = new LinkedHashSet<>();
    private static final int materialPerItem = OCSettings.get().printMaterialValue;
    public final Set<Shape> stateOff = new LinkedHashSet<>();
    public final Set<Shape> stateOn = new LinkedHashSet<>();
    public String label;
    public String tooltip;
    public boolean isButtonMode = false;
    public int redstoneLevel = 0;
    public boolean pressurePlate = false;
    public boolean isBeaconBase = false;
    public int lightLevel = 0;
    public boolean noclipOff = false;
    public boolean noclipOn = false;
    private float opacityCache = 0f;
    private boolean opacityDirty = true;

    public static List<Shape> getRenderShapes(ItemStack stack, boolean extendedTooltips) {
        var data = new PrintData(stack);
        var shapes = data.hasActiveState() && extendedTooltips ? data.stateOn : data.stateOff;
        List<Shape> result = new ArrayList<>(shapes.size());
        for (var shape : shapes) {
            if (shape.texture() != null && !shape.texture().isEmpty()) {
                result.add(shape);
            }
        }
        return result;
    }

    public PrintData() {
        super(Constants.BlockName.Print);
    }

    public PrintData(ItemStack stack) {
        this();
        load(stack);
    }

    @Override
    public void load(ItemStack stack, HolderLookup.Provider provider) {
        var info = Items.get(stack);
        if (info != null) {
            itemName = info.name();
        }
        super.load(stack, provider);
    }

    @Override
    public void load(ItemStack stack) {
        var info = Items.get(stack);
        if (info != null) {
            itemName = info.name();
        }
        super.load(stack);
    }

    public static void addInkProvider(Method provider) {
        inkProviders.add(provider);
    }

    public static float computeApproximateOpacity(Set<Shape> shapes) {
        float volume = 1f;
        if (!shapes.isEmpty()) {
            for (int x = 0; x < 16 / stepping; x++) {
                for (int y = 0; y < 16 / stepping; y++) {
                    for (int z = 0; z < 16 / stepping; z++) {
                        AABB bounds = new AABB(
                                x * step, y * step, z * step,
                                (x + 1) * step, (y + 1) * step, (z + 1) * step);
                        boolean intersects = false;
                        for (var shape : shapes) {
                            if (shape.bounds.intersects(bounds)) {
                                intersects = true;
                                break;
                            }
                        }
                        if (!intersects) {
                            volume -= invMaxVolume;
                        }
                    }
                }
            }
        }
        return volume;
    }

    public static int[] computeCosts(PrintData data) {
        int totalVolume = 0;
        int totalSurface = 0;
        for (var shape : data.stateOn) {
            totalVolume += volume(shape.bounds);
            totalSurface += surface(shape.bounds);
        }
        for (var shape : data.stateOff) {
            totalVolume += volume(shape.bounds);
            totalSurface += surface(shape.bounds);
        }
        double multiplier = (data.noclipOff || data.noclipOn) ? OCSettings.get().noclipMultiplier : 1;

        if (totalVolume > 0) {
            int baseMaterialRequired = Math.max(totalVolume / 2, 1);
            int materialRequired = (data.redstoneLevel > 0 && data.redstoneLevel < 15)
                    ? baseMaterialRequired + OCSettings.get().printCustomRedstone
                    : baseMaterialRequired;
            int inkRequired = Math.max(totalSurface / 6, 1);
            return new int[]{(int) (materialRequired * multiplier), inkRequired};
        }
        return null;
    }

    private static int volume(AABB aabb) {
        double dx = aabb.maxX - aabb.minX;
        double dy = aabb.maxY - aabb.minY;
        double dz = aabb.maxZ - aabb.minZ;
        return (int) (dx * dy * dz * 4096);
    }

    private static int surface(AABB aabb) {
        double dx = aabb.maxX - aabb.minX;
        double dy = aabb.maxY - aabb.minY;
        double dz = aabb.maxZ - aabb.minZ;
        return (int) ((dx * dy + dy * dz + dz * dx) * 2 * 4096);
    }

    public static int materialValue(ItemStack stack) {
        if (Items.get(stack) == Items.get(Constants.ItemName.Chamelium))
            return materialPerItem;
        else if (Items.get(stack) == Items.get(Constants.BlockName.Print) || Items.get(stack) == Items.get(Constants.BlockName.BeaconBasePrint)) {
            var data = new PrintData(stack);
            var costs = computeCosts(data);
            if (costs != null) {
                return (int) (costs[0] * OCSettings.get().printRecycleRate);
            }
        }
        return 0;
    }

    public static int inkValue(ItemStack stack) {
        for (var provider : inkProviders) {
            int value = (Integer) ReflectionUtil.tryInvokeStatic(provider, 0, stack);
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }

    public static Shape nbtToShape(CompoundTag nbt) {
        AABB aabb;
        if (nbt.contains("minX")) {
            float minX = nbt.getByte("minX") / 16f;
            float minY = nbt.getByte("minY") / 16f;
            float minZ = nbt.getByte("minZ") / 16f;
            float maxX = nbt.getByte("maxX") / 16f;
            float maxY = nbt.getByte("maxY") / 16f;
            float maxZ = nbt.getByte("maxZ") / 16f;
            aabb = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        } else {
            byte[] bounds = nbt.getByteArray("bounds");
            if (bounds.length < 6) bounds = Arrays.copyOf(bounds, 6);
            aabb = new AABB(
                    bounds[0] / 16f, bounds[1] / 16f, bounds[2] / 16f,
                    bounds[3] / 16f, bounds[4] / 16f, bounds[5] / 16f);
        }
        String texture = nbt.getString("texture");
        Integer tint = nbt.contains("tint") ? nbt.getInt("tint") : null;
        return new Shape(aabb, texture, tint);
    }

    public static CompoundTag shapeToNBT(Shape shape) {
        var nbt = new CompoundTag();
        nbt.putByteArray("bounds", new byte[]{
                (byte) Math.round(shape.bounds.minX * 16),
                (byte) Math.round(shape.bounds.minY * 16),
                (byte) Math.round(shape.bounds.minZ * 16),
                (byte) Math.round(shape.bounds.maxX * 16),
                (byte) Math.round(shape.bounds.maxY * 16),
                (byte) Math.round(shape.bounds.maxZ * 16)
        });
        nbt.putString("texture", shape.texture);
        if (shape.tint != null) {
            nbt.putInt("tint", shape.tint);
        }
        return nbt;
    }

    public boolean hasActiveState() {
        return !stateOn.isEmpty();
    }

    public boolean emitRedstone() {
        return redstoneLevel > 0;
    }

    public boolean emitRedstone(boolean state) {
        return state ? emitRedstoneWhenOn() : emitRedstoneWhenOff();
    }

    public boolean emitRedstoneWhenOff() {
        return emitRedstone() && !hasActiveState();
    }

    public boolean emitRedstoneWhenOn() {
        return emitRedstone() && hasActiveState();
    }

    public float opacity() {
        if (opacityDirty) {
            opacityDirty = false;
            opacityCache = Math.min(computeApproximateOpacity(stateOn), computeApproximateOpacity(stateOff));
        }
        return opacityCache;
    }

    @Override
    public void load(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        if (nbt.contains("label")) label = nbt.getString("label");
        else label = null;
        if (nbt.contains("tooltip")) tooltip = nbt.getString("tooltip");
        else tooltip = null;
        isButtonMode = nbt.getBoolean("isButtonMode");
        redstoneLevel = Math.clamp(nbt.getInt("redstoneLevel"), 0, 15);
        if (nbt.getBoolean("emitRedstone")) redstoneLevel = 15;
        pressurePlate = nbt.getBoolean("pressurePlate");
        stateOff.clear();
        var offList = nbt.getList("stateOff", Tag.TAG_COMPOUND);
        for (int i = 0; i < offList.size(); i++) {
            stateOff.add(nbtToShape(offList.getCompound(i)));
        }
        stateOn.clear();
        var onList = nbt.getList("stateOn", Tag.TAG_COMPOUND);
        for (int i = 0; i < onList.size(); i++) {
            stateOn.add(nbtToShape(onList.getCompound(i)));
        }
        isBeaconBase = nbt.getBoolean("isBeaconBase");
        lightLevel = Math.clamp(nbt.getByte("lightLevel") & 0xFF, 0, 15);
        noclipOff = nbt.getBoolean("noclipOff");
        noclipOn = nbt.getBoolean("noclipOn");
        opacityDirty = true;
    }

    @Override
    public void save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        if (label != null) nbt.putString("label", label);
        if (tooltip != null) nbt.putString("tooltip", tooltip);
        nbt.putBoolean("isButtonMode", isButtonMode);
        nbt.putInt("redstoneLevel", redstoneLevel);
        nbt.putBoolean("pressurePlate", pressurePlate);
        setNewShapeSet(nbt, "stateOff", stateOff);
        setNewShapeSet(nbt, "stateOn", stateOn);
        nbt.putBoolean("isBeaconBase", isBeaconBase);
        nbt.putByte("lightLevel", (byte) lightLevel);
        nbt.putBoolean("noclipOff", noclipOff);
        nbt.putBoolean("noclipOn", noclipOn);
    }

    private void setNewShapeSet(CompoundTag nbt, String name, Set<Shape> shapes) {
        List<Shape> sorted = new ArrayList<>(shapes);
        sorted.sort(this::compareShape);
        ListTag list = new ListTag();
        for (var shape : sorted) {
            list.add(shapeToNBT(shape));
        }
        nbt.put(name, list);
    }

    private int compareShape(Shape a, Shape b) {
        if (a.bounds.minX != b.bounds.minX) return Double.compare(b.bounds.minX, a.bounds.minX);
        if (a.bounds.minY != b.bounds.minY) return Double.compare(b.bounds.minY, a.bounds.minY);
        if (a.bounds.minZ != b.bounds.minZ) return Double.compare(b.bounds.minZ, a.bounds.minZ);
        if (a.bounds.maxX != b.bounds.maxX) return Double.compare(b.bounds.maxX, a.bounds.maxX);
        if (a.bounds.maxY != b.bounds.maxY) return Double.compare(b.bounds.maxY, a.bounds.maxY);
        if (a.bounds.maxZ != b.bounds.maxZ) return Double.compare(b.bounds.maxZ, a.bounds.maxZ);
        if (!java.util.Objects.equals(a.tint, b.tint)) {
            if (a.tint == null) return 1;
            if (b.tint == null) return -1;
            return Integer.compare(b.tint, a.tint);
        }
        if (a.texture != null && b.texture != null) return b.texture.compareTo(a.texture);
        return 0;
    }

    public record Shape(AABB bounds, String texture, Integer tint) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Shape shape = (Shape) o;
            return bounds.equals(shape.bounds) &&
                    Objects.equals(texture, shape.texture) &&
                    Objects.equals(tint, shape.tint);
        }

    }
}

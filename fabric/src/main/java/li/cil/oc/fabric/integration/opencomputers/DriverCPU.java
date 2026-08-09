package li.cil.oc.fabric.integration.opencomputers;

import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.integration.opencomputers.Item;
import li.cil.oc.fabric.OpenComputers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

@SuppressWarnings("unused")
public class DriverCPU extends Item implements li.cil.oc.api.driver.item.MutableProcessor, li.cil.oc.api.driver.item.CallBudget {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                li.cil.oc.api.Items.get(Constants.ItemName.CPUTier1),
                li.cil.oc.api.Items.get(Constants.ItemName.CPUTier2),
                li.cil.oc.api.Items.get(Constants.ItemName.CPUTier3));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, li.cil.oc.api.network.EnvironmentHost host) {
        return new li.cil.oc.core.impl.server.component.CPU(tier(stack));
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.CPU;
    }

    @Override
    public int tier(ItemStack stack) {
        return cpuTier(stack);
    }

    public int cpuTier(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof li.cil.oc.core.impl.common.item.CPU cpu) {
            return cpu.cpuTier();
        }
        return Tier.One;
    }

    public int supportedComponents(ItemStack stack) {
        return OCSettings.get().cpuComponentSupport[cpuTier(stack)];
    }

    public java.util.List<Class<? extends li.cil.oc.api.machine.Architecture>> allArchitectures() {
        return new java.util.ArrayList<>(li.cil.oc.api.Machine.architectures());
    }

    public static Class<? extends li.cil.oc.api.machine.Architecture> getArchitecture(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && !cd.isEmpty()) {
            CompoundTag tag = cd.copyTag();
            String archClass = tag.getString(OCSettings.namespace + "archClass");
            if (!archClass.isEmpty()) {
                try {
                    return Class.forName(archClass).asSubclass(li.cil.oc.api.machine.Architecture.class);
                } catch (ClassNotFoundException | ClassCastException t) {
                    OpenComputers.log().warn("Failed getting class for CPU architecture. Resetting CPU to use the default.", t);
                    tag.remove(OCSettings.namespace + "archClass");
                    tag.remove(OCSettings.namespace + "archName");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                }
            }
        }
        if (!li.cil.oc.api.Machine.architectures().isEmpty()) {
            return li.cil.oc.api.Machine.architectures().iterator().next();
        }
        return null;
    }

    @Override
    public Class<? extends li.cil.oc.api.machine.Architecture> architecture(ItemStack stack) {
        return getArchitecture(stack);
    }

    @Override
    public double getCallBudget(ItemStack stack) {
        return OCSettings.get().callBudgets[Math.clamp(tier(stack), Tier.One, Tier.Three)];
    }

    @Override
    public void setArchitecture(ItemStack stack, Class<? extends li.cil.oc.api.machine.Architecture> architecture) {
        if (!worksWith(stack)) throw new IllegalArgumentException("Unsupported processor type.");
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag;
        if (cd == null || cd.isEmpty()) {
            tag = new CompoundTag();
        } else {
            tag = cd.copyTag();
        }
        tag.putString(OCSettings.namespace + "archClass", architecture.getName());
        tag.putString(OCSettings.namespace + "archName", li.cil.oc.api.Machine.getArchitectureName(architecture));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}

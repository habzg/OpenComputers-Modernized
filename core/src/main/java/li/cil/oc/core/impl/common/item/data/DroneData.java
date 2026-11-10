package li.cil.oc.core.impl.common.item.data;

import com.google.common.base.Strings;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.item.data.NameProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class DroneData extends MicrocontrollerData {
  public String name = "";

  public DroneData() {
    super(Constants.ItemName.Drone);
  }

  public DroneData(ItemStack stack) {
    this();
    load(stack);
  }

  @Override
  public void load(CompoundTag nbt, HolderLookup.Provider provider) {
    super.load(nbt, provider);
    if (nbt.contains("display") && nbt.getCompound("display").contains("Name")) {
      name = nbt.getCompound("display").getString("Name");
    }
    if (Strings.isNullOrEmpty(name)) {
      name = NameProvider.randomName();
    }
  }

  @Override
  public void load(ItemStack stack, HolderLookup.Provider provider) {
    super.load(stack, provider);
    var customName = stack.get(DataComponents.CUSTOM_NAME);
    if (customName != null && !customName.getString().equals(name)) {
      name = customName.getString();
      var tag = stack.get(DataComponents.CUSTOM_DATA);
      var nbt = tag != null && !tag.isEmpty() ? tag.copyTag() : new CompoundTag();
      writeName(nbt);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }
  }

  @Override
  public void save(ItemStack stack, HolderLookup.Provider provider) {
    super.save(stack, provider);
    if (!Strings.isNullOrEmpty(name)) {
      stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
    }
  }

  private void writeName(CompoundTag nbt) {
    if (!Strings.isNullOrEmpty(name)) {
      if (!nbt.contains("display")) {
        nbt.put("display", new CompoundTag());
      }
      nbt.getCompound("display").putString("Name", name);
    }
  }

  @Override
  public void save(CompoundTag nbt, HolderLookup.Provider provider) {
    super.save(nbt, provider);
    writeName(nbt);
  }
}

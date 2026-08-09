package li.cil.oc.core.impl.common.item.data;

import com.google.common.base.Strings;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.item.data.NameProvider;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RobotData extends ItemData {
    private static final Logger LOGGER = LoggerFactory.getLogger(RobotData.class);
    private static final String[] names;

    static {
        String[] loadedNames;
        try {
            InputStream is = RobotData.class.getResourceAsStream("/assets/" + OCSettings.resourceDomain + "/robot.names");
            if (is != null) {
                List<String> nameList = new ArrayList<>();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.replaceAll("#.*", "").trim();
                    if (!line.isEmpty()) {
                        nameList.add(line);
                    }
                }
                reader.close();
                loadedNames = nameList.toArray(new String[0]);
            } else {
                loadedNames = new String[0];
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed loading robot name list.", t);
            loadedNames = new String[0];
        }
        names = loadedNames;
        NameProvider.setRandomNameSupplier(RobotData::randomName);
    }

    public String name = "";
    public int totalEnergy = 0;
    public int robotEnergy = 0;
    public int tier = 0;
    public List<ItemStack> components = new ArrayList<>();
    public List<ItemStack> containers = new ArrayList<>();
    public int lightColor = 0xF23030;

    public RobotData() {
        super(Constants.BlockName.Robot);
    }

    public RobotData(ItemStack stack) {
        this();
        load(stack);
    }

    public static String randomName() {
        return names.length > 0 ? names[(int) (Math.random() * names.length)] : "Robot";
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        if (nbt.contains("display") && nbt.getCompound("display").contains("Name")) {
            name = nbt.getCompound("display").getString("Name");
        }
        if (Strings.isNullOrEmpty(name)) {
            name = randomName();
        }
        totalEnergy = nbt.getInt(OCSettings.namespace + "storedEnergy");
        robotEnergy = nbt.getInt(OCSettings.namespace + "robotEnergy");
        tier = nbt.getInt(OCSettings.namespace + "tier");
        components.clear();
        var componentList = nbt.getList(OCSettings.namespace + "components", Tag.TAG_COMPOUND);
        for (int i = 0; i < componentList.size(); i++) {
            components.add(ItemStack.parseOptional(provider, componentList.getCompound(i)));
        }
        containers.clear();
        var containerList = nbt.getList(OCSettings.namespace + "containers", Tag.TAG_COMPOUND);
        for (int i = 0; i < containerList.size(); i++) {
            containers.add(ItemStack.parseOptional(provider, containerList.getCompound(i)));
        }
        if (nbt.contains(OCSettings.namespace + "lightColor")) {
            lightColor = nbt.getInt(OCSettings.namespace + "lightColor");
        }
    }

    @Override
    public void load(ItemStack stack, HolderLookup.Provider provider) {
        super.load(stack, provider);
        var customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            name = customName.getString();
        }
    }

    @Override
    public void save(ItemStack stack, HolderLookup.Provider provider) {
        super.save(stack, provider);
        stack.set(DataComponents.RARITY, li.cil.oc.core.impl.util.Rarity.byTier(tier));
        if (!Strings.isNullOrEmpty(name)) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        if (!Strings.isNullOrEmpty(name)) {
            if (!nbt.contains("display")) {
                nbt.put("display", new CompoundTag());
            }
            nbt.getCompound("display").putString("Name", name);
        }
        nbt.putInt(OCSettings.namespace + "storedEnergy", totalEnergy);
        nbt.putInt(OCSettings.namespace + "robotEnergy", robotEnergy);
        nbt.putInt(OCSettings.namespace + "tier", tier);
        ListTag compList = new ListTag();
        for (var stack : components) {
            if (stack != null && !stack.isEmpty()) {
                compList.add(stack.save(provider, new CompoundTag()));
            }
        }
        nbt.put(OCSettings.namespace + "components", compList);
        ListTag contList = new ListTag();
        for (var stack : containers) {
            if (stack != null && !stack.isEmpty()) {
                contList.add(stack.save(provider, new CompoundTag()));
            }
        }
        nbt.put(OCSettings.namespace + "containers", contList);
        nbt.putInt(OCSettings.namespace + "lightColor", lightColor);
    }

    public ItemStack copyItemStack() {
        var stack = createItemStack();
        var newInfo = new RobotData(stack);
        for (var cs : newInfo.components) {
            if (cs != null && !cs.isEmpty()) {
                var customData = cs.get(DataComponents.CUSTOM_DATA);
                if (customData != null && !customData.isEmpty()) {
                    var nbt = customData.copyTag();
                    if (nbt.contains(OCSettings.namespace + "data", Tag.TAG_COMPOUND)) {
                        nbt.getCompound(OCSettings.namespace + "data").remove("node");
                    }
                    cs.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(nbt));
                }
            }
        }
        newInfo.totalEnergy = 0;
        newInfo.robotEnergy = 50000;
        newInfo.save(stack);
        return stack;
    }
}

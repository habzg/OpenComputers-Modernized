package li.cil.oc.core.impl.common.item.data;

import com.google.common.base.Strings;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.DriverScreenHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RobotData extends ItemData {
    private static final Logger LOGGER = LoggerFactory.getLogger(RobotData.class);
    private static final String[] names;

    static {
        String[] loadedNames;
        try {
            InputStream is = RobotData.class.getResourceAsStream("/assets/" + Settings.resourceDomain + "/robot.names");
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
        totalEnergy = nbt.getInt(Settings.namespace + "storedEnergy");
        robotEnergy = nbt.getInt(Settings.namespace + "robotEnergy");
        tier = nbt.getInt(Settings.namespace + "tier");
        components.clear();
        var componentList = nbt.getList(Settings.namespace + "components", Tag.TAG_COMPOUND);
        for (int i = 0; i < componentList.size(); i++) {
            components.add(ItemStack.parseOptional(provider, componentList.getCompound(i)));
        }
        containers.clear();
        var containerList = nbt.getList(Settings.namespace + "containers", Tag.TAG_COMPOUND);
        for (int i = 0; i < containerList.size(); i++) {
            containers.add(ItemStack.parseOptional(provider, containerList.getCompound(i)));
        }
        if (nbt.contains(Settings.namespace + "lightColor")) {
            lightColor = nbt.getInt(Settings.namespace + "lightColor");
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
        nbt.putInt(Settings.namespace + "storedEnergy", totalEnergy);
        nbt.putInt(Settings.namespace + "robotEnergy", robotEnergy);
        nbt.putInt(Settings.namespace + "tier", tier);
        ListTag compList = new ListTag();
        for (var stack : components) {
            if (stack != null && !stack.isEmpty()) {
                compList.add(stack.save(provider, new CompoundTag()));
            }
        }
        nbt.put(Settings.namespace + "components", compList);
        ListTag contList = new ListTag();
        for (var stack : containers) {
            if (stack != null && !stack.isEmpty()) {
                contList.add(stack.save(provider, new CompoundTag()));
            }
        }
        nbt.put(Settings.namespace + "containers", contList);
        nbt.putInt(Settings.namespace + "lightColor", lightColor);
    }

    public ItemStack copyItemStack() {
        var stack = createItemStack();
        var newInfo = new RobotData(stack);
        for (var cs : newInfo.components) {
            var driver = li.cil.oc.api.API.driver.driverFor(cs);
            if (DriverScreenHelper.get() != null && DriverScreenHelper.get().isDriverScreen(driver)) {
                DriverScreenHelper.get().clearDataTag(driver, cs);
            }
        }
        newInfo.totalEnergy = 0;
        newInfo.robotEnergy = 50000;
        newInfo.save(stack);
        return stack;
    }
}

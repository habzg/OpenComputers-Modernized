package li.cil.oc.fabric.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Manual;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.LootManager;
import li.cil.oc.core.server.machine.CallbackWrapper;
import li.cil.oc.core.server.machine.Callbacks;
import li.cil.oc.fabric.client.gui.Database;
import li.cil.oc.fabric.client.gui.Relay;
import li.cil.oc.fabric.integration.util.JEI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@EmiEntrypoint
@SuppressWarnings("unused")
public class OCEMIPlugin implements EmiPlugin {
    public static EmiRecipeCategory CALLBACKS;
    public static EmiRecipeCategory MANUAL_CAT;

    private static final ResourceLocation TABLET_TEXTURE = ResourceLocation.fromNamespaceAndPath("opencomputers", "textures/item/tablet_on.png");
    private static final ResourceLocation MANUAL_TEXTURE = ResourceLocation.fromNamespaceAndPath("opencomputers", "textures/item/manual.png");

    private static final Pattern DocPattern = Pattern.compile("(?s)^function(\\(.*?\\).*?) -- (.*)$");
    private static final Pattern VexPattern = Pattern.compile("(?s)^function(\\(.*?\\).*?); (.*)$");

    @Override
    public void register(EmiRegistry registry) {
        CALLBACKS = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath("opencomputers", "callbacks"),
            (guiGraphics, x, y, delta) -> {
                int frame = (int) (System.currentTimeMillis() / 500) % 2;
                guiGraphics.blit(TABLET_TEXTURE, x, y, 0, frame * 16, 16, 16, 16, 32);
            });
        MANUAL_CAT = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath("opencomputers", "manual"),
            (guiGraphics, x, y, delta) ->
                guiGraphics.blit(MANUAL_TEXTURE, x, y, 0, 0, 16, 16, 16, 16));
        registry.addCategory(CALLBACKS);
        registry.addCategory(MANUAL_CAT);

        registerComparisons(registry);
        hideItems(registry);
        registerCallbackRecipes(registry);
        registerManualRecipes(registry);
        registerLootDiskCyclingRecipes(registry);

        registry.addDragDropHandler(Database.class, new DatabaseDragHandler());
        registry.addExclusionArea(Relay.class, (screen, consumer) -> consumer.accept(new Bounds(
            screen.windowX() + screen.getXSize(),
            screen.windowY() + 10,
            23, 26)));
    }

    private void registerComparisons(EmiRegistry registry) {
        var mc = li.cil.oc.api.Items.get(Constants.BlockName.Microcontroller);
        if (mc != null && mc.item() != null) {
            registry.setDefaultComparison(EmiStack.of(mc.item()), Comparison.compareComponents());
        }
        var robot = li.cil.oc.api.Items.get(Constants.BlockName.Robot);
        if (robot != null && robot.item() != null) {
            registry.setDefaultComparison(EmiStack.of(robot.item()), Comparison.compareComponents());
        }
        var eeprom = li.cil.oc.api.Items.get(Constants.ItemName.EEPROM);
        if (eeprom != null && eeprom.item() != null) {
            registry.setDefaultComparison(EmiStack.of(eeprom.item()), Comparison.compareData(stack -> {
                var customData = stack.getItemStack().get(DataComponents.CUSTOM_DATA);
                if (customData != null && !customData.isEmpty()) {
                    var tag = customData.copyTag();
                    if (tag.contains(OCSettings.namespace + "data")) {
                        var data = tag.getCompound(OCSettings.namespace + "data");
                        if (data.contains(OCSettings.namespace + "eeprom") && data.getByteArray(OCSettings.namespace + "eeprom").length > 0) {
                            return 1;
                        }
                    }
                }
                return 0;
            }));
        }
        var floppy = li.cil.oc.api.Items.get(Constants.ItemName.Floppy);
        if (floppy != null && floppy.item() != null) {
            registry.setDefaultComparison(EmiStack.of(floppy.item()), Comparison.compareData(stack -> {
                var customData = stack.getItemStack().get(DataComponents.CUSTOM_DATA);
                if (customData != null && !customData.isEmpty()) {
                    var tag = customData.copyTag();
                    if (tag.contains(OCSettings.namespace + "lootFactory")) {
                        return tag.getString(OCSettings.namespace + "lootFactory");
                    }
                }
                return "";
            }));
        }
        var hoverBoots = li.cil.oc.api.Items.get(Constants.ItemName.HoverBoots);
        if (hoverBoots != null && hoverBoots.item() != null) {
            registry.setDefaultComparison(EmiStack.of(hoverBoots.item()), Comparison.compareData(stack -> {
                var customData = stack.getItemStack().get(DataComponents.CUSTOM_DATA);
                if (customData != null && !customData.isEmpty()) {
                    var tag = customData.copyTag();
                    double charge = tag.getDouble(OCSettings.namespace + "charge");
                    if (charge > 0) {
                        return 1;
                    }
                }
                return 0;
            }));
        }
    }

    private void hideItems(EmiRegistry registry) {
        var hiddenItems = new HashSet<net.minecraft.world.item.Item>();
        for (var supplier : JEI.getHiddenItems()) {
            var stack = supplier.get();
            if (!stack.isEmpty()) {
                hiddenItems.add(stack.getItem());
            }
        }
        registry.removeEmiStacks(stack -> {
            var itemStack = stack.getItemStack();
            return !itemStack.isEmpty() && hiddenItems.contains(itemStack.getItem());
        });
    }

    private void registerCallbackRecipes(EmiRegistry registry) {
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            var id = entry.getKey().location();
            if (!id.getNamespace().equals("opencomputers")) continue;
            var stack = entry.getValue().getDefaultInstance();
            if (stack.isEmpty()) continue;
            var docs = getCallbackDocs(stack);
            if (docs.isEmpty()) continue;
            var recipes = buildCallbackRecipes(stack, docs);
            for (var r : recipes) {
                registry.addRecipe(r);
            }
        }
    }

    private void registerManualRecipes(EmiRegistry registry) {
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            var id = entry.getKey().location();
            if (!id.getNamespace().equals("opencomputers")) continue;
            var stack = entry.getValue().getDefaultInstance();
            if (stack.isEmpty()) continue;
            String path = Manual.pathFor(stack);
            if (path != null) {
                registry.addRecipe(new ManualRecipe(stack, path));
            }
        }
    }

    private void registerLootDiskCyclingRecipes(EmiRegistry registry) {
        var disks = LootManager.disksForCycling();
        if (disks.isEmpty()) return;
        registry.removeRecipes(ResourceLocation.fromNamespaceAndPath("opencomputers", "loot_disk_cycling"));
        var wrench = EmiStack.of(li.cil.oc.api.Items.get(Constants.ItemName.Wrench).createItemStack(1));
        var diskStacks = disks.stream().map(EmiStack::of).toList();
        var diskIngredient = EmiIngredient.of(diskStacks);

        registry.addRecipe(new LootDiskCyclingEmiRecipe(
            ResourceLocation.fromNamespaceAndPath("opencomputers", "/loot_disk_cycling"),
            List.of(diskIngredient, wrench), diskStacks,
            diskIngredient, wrench, diskIngredient));
    }

    static List<String> getCallbackDocs(ItemStack stack) {
        List<String> result = new ArrayList<>();
        var seen = new HashSet<String>();
        List<Class<?>> environments = new ArrayList<>();
        try {
            environments.addAll(Driver.environmentsFor(stack));
        } catch (Throwable ignored) {
        }
        for (Class<?> env : environments) {
            Map<String, CallbackWrapper> callbacks = Callbacks.fromClass(env);
            for (Map.Entry<String, CallbackWrapper> entry : callbacks.entrySet()) {
                String name = entry.getKey();
                if (!seen.add(name)) continue;
                CallbackWrapper cw = entry.getValue();
                String doc = cw.annotation().doc();
                if (doc == null || doc.isEmpty()) {
                    result.add(name);
                } else {
                    String signature = name;
                    String documentation;
                    Matcher m = DocPattern.matcher(doc);
                    if (m.matches()) {
                        signature = name + m.group(1);
                        documentation = m.group(2);
                    } else {
                        m = VexPattern.matcher(doc);
                        if (m.matches()) {
                            signature = name + m.group(1);
                            documentation = m.group(2);
                        } else {
                            documentation = doc;
                        }
                    }
                    List<String> wrapSig = wrap(signature, 152);
                    StringBuilder sb = new StringBuilder();
                    for (String s : wrapSig) {
                        if (!sb.isEmpty()) sb.append("\n");
                        sb.append(s);
                    }
                    sb.append("\n");
                    for (String s : wrap(documentation, 144)) {
                        sb.append("  ").append(s).append("\n");
                    }
                    result.add(sb.toString().trim());
                }
            }
        }
        return result;
    }

    static List<CallbackRecipe> buildCallbackRecipes(ItemStack stack, List<String> docs) {
        List<CallbackRecipe> recipes = new ArrayList<>();
        if (docs.isEmpty()) return recipes;
        var pages = new ArrayList<String>();
        var current = new StringBuilder();
        for (String doc : docs) {
            int lines = doc.split("\n").length;
            int currentLines = current.isEmpty() ? 0 : current.toString().split("\n").length + 1;
            if (currentLines + lines > 12) {
                if (!current.isEmpty()) {
                    pages.add(current.toString());
                    current = new StringBuilder();
                }
                String[] docLines = doc.split("\n");
                for (int i = 0; i < docLines.length; i += 12) {
                    int end = Math.min(i + 12, docLines.length);
                    StringBuilder page = new StringBuilder();
                    for (int j = i; j < end; j++) {
                        if (!page.isEmpty()) page.append("\n");
                        page.append(docLines[j]);
                    }
                    pages.add(page.toString());
                }
            } else {
                if (!current.isEmpty()) current.append("\n\n");
                current.append(doc);
            }
        }
        if (!current.isEmpty()) pages.add(current.toString());
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        int pageIndex = 0;
        for (String page : pages) {
            var id = ResourceLocation.fromNamespaceAndPath("opencomputers",
                "/callbacks/" + itemId.getPath() + "/" + pageIndex);
            recipes.add(new CallbackRecipe(id, stack, page));
            pageIndex++;
        }
        return recipes;
    }

    static List<String> wrap(String line, int width) {
        var font = Minecraft.getInstance().font;
        List<String> lines = new ArrayList<>();
        if (font.width(line) <= width) {
            lines.add(line);
            return lines;
        }
        StringBuilder current = new StringBuilder();
        for (String word : line.split(" ")) {
            if (font.width(word) > width) {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder();
                }
                String remaining = word;
                while (!remaining.isEmpty()) {
                    String part = font.plainSubstrByWidth(remaining, width);
                    lines.add(part);
                    remaining = remaining.substring(part.length());
                }
            } else if (!current.isEmpty() && font.width(current + " " + word) > width) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                if (!current.isEmpty()) current.append(" ");
                current.append(word);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    public static class CallbackRecipe extends BasicEmiRecipe {
        private final String pageText;

        public CallbackRecipe(ResourceLocation id, ItemStack stack, String pageText) {
            super(CALLBACKS, id, 160, 125);
            this.pageText = pageText;
            this.inputs.add(EmiStack.of(stack));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addDrawable(0, 0, 160, 125, (GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) -> {
                var font = Minecraft.getInstance().font;
                int maxWidth = 152;
                int y = 4;
                for (String line : pageText.split("\n")) {
                    if (y >= 121) break;
                    if (font.width(line) > maxWidth) {
                        for (String w : wrap(line, maxWidth)) {
                            if (y >= 121) break;
                            guiGraphics.drawString(font, w, 4, y, 0x333333, false);
                            y += font.lineHeight;
                        }
                    } else {
                        guiGraphics.drawString(font, line, 4, y, 0x333333, false);
                        y += font.lineHeight;
                    }
                }
            });
        }
    }

    public static class ManualRecipe extends BasicEmiRecipe {
        private final String manualPath;

        public ManualRecipe(ItemStack stack, String path) {
            super(MANUAL_CAT, ResourceLocation.fromNamespaceAndPath("opencomputers",
                "/manual/" + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()), 160, 40);
            this.manualPath = path;
            this.inputs.add(EmiStack.of(stack));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            Component label = Component.translatable("jei.manual.opencomputers.open");
            int w = 100, h = 20;
            int x = (160 - w) / 2;
            int y = 10;
            widgets.add(new ManualButtonWidget(x, y, w, h, label, manualPath));
        }
    }

    public static class LootDiskCyclingEmiRecipe extends BasicEmiRecipe {
        private final EmiIngredient displayInput;
        private final EmiStack displayWrench;
        private final EmiIngredient displayOutput;

        public LootDiskCyclingEmiRecipe(ResourceLocation id,
                List<EmiIngredient> lookupInputs, List<EmiStack> lookupOutputs,
                EmiIngredient displayInput, EmiStack displayWrench, EmiIngredient displayOutput) {
            super(VanillaEmiRecipeCategories.CRAFTING, id, 118, 54);
            this.inputs.addAll(lookupInputs);
            this.outputs.addAll(lookupOutputs);
            this.displayInput = displayInput;
            this.displayWrench = displayWrench;
            this.displayOutput = displayOutput;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addTexture(EmiTexture.EMPTY_ARROW, 60, 18);
            widgets.addTexture(EmiTexture.SHAPELESS, 97, 0);
            widgets.addSlot(displayInput, 0, 0);
            widgets.addSlot(displayWrench, 18, 0);
            for (int i = 2; i < 9; i++) {
                widgets.addSlot(i % 3 * 18, i / 3 * 18);
            }
            widgets.addSlot(displayOutput, 92, 14).large(true).recipeContext(this);
        }
    }

    private static class ManualButtonWidget extends Widget {
        private static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("widget/button");
        private static final ResourceLocation BUTTON_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/button_highlighted");
        private final int x, y, w, h;
        private final Component label;
        private final String manualPath;

        ManualButtonWidget(int x, int y, int w, int h, Component label, String path) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.label = label;
            this.manualPath = path;
        }

        @Override
        public Bounds getBounds() {
            return new Bounds(x, y, w, h);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
            guiGraphics.blitSprite(hovered ? BUTTON_HIGHLIGHTED : BUTTON, x, y, w, h);
            var font = Minecraft.getInstance().font;
            guiGraphics.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, 0xFFFFFF, false);
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button) {
            if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
                var mc = Minecraft.getInstance();
                mc.setScreen(null);
                Manual.openFor(mc.player);
                Manual.navigate(manualPath);
                return true;
            }
            return false;
        }
    }
}

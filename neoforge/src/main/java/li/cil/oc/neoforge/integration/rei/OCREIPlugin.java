package li.cil.oc.neoforge.integration.rei;

import dev.architectury.event.EventResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Manual;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.LootManager;
import li.cil.oc.core.impl.integration.util.Wrench;
import li.cil.oc.core.server.machine.CallbackWrapper;
import li.cil.oc.core.server.machine.Callbacks;
import li.cil.oc.neoforge.client.gui.Relay;
import li.cil.oc.neoforge.integration.util.JEI;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.display.DynamicDisplayGenerator;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.comparison.EntryComparator;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomShapelessDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@REIPluginClient
@SuppressWarnings("unused")
public class OCREIPlugin implements REIClientPlugin {
    public static final CategoryIdentifier<CallbackDisplay> CALLBACKS =
            CategoryIdentifier.of("opencomputers", "callbacks");
    public static final CategoryIdentifier<ManualDisplay> MANUAL =
            CategoryIdentifier.of("opencomputers", "manual");

    private static final Pattern DocPattern = Pattern.compile("(?s)^function(\\(.*?\\).*?) -- (.*)$");
    private static final Pattern VexPattern = Pattern.compile("(?s)^function(\\(.*?\\).*?); (.*)$");

    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        var mc = li.cil.oc.api.Items.get(Constants.BlockName.Microcontroller);
        if (mc != null && mc.item() != null) {
            registry.register(EntryComparator.itemComponents(), mc.item());
        }
        var robot = li.cil.oc.api.Items.get(Constants.BlockName.Robot);
        if (robot != null && robot.item() != null) {
            registry.register(EntryComparator.itemComponents(), robot.item());
        }

        var eeprom = li.cil.oc.api.Items.get(Constants.ItemName.EEPROM);
        if (eeprom != null && eeprom.item() != null) {
            registry.register((context, stack) -> {
                var customData = stack.get(DataComponents.CUSTOM_DATA);
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
            }, eeprom.item());
        }

        var floppy = li.cil.oc.api.Items.get(Constants.ItemName.Floppy);
        if (floppy != null && floppy.item() != null) {
            registry.register((context, stack) -> {
                var customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData != null && !customData.isEmpty()) {
                    var tag = customData.copyTag();
                    if (tag.contains(OCSettings.namespace + "lootFactory")) {
                        return tag.getString(OCSettings.namespace + "lootFactory").hashCode();
                    }
                }
                return 0;
            }, floppy.item());
        }

        var hoverBoots = li.cil.oc.api.Items.get(Constants.ItemName.HoverBoots);
        if (hoverBoots != null && hoverBoots.item() != null) {
            registry.register((context, stack) -> {
                var customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData != null && !customData.isEmpty()) {
                    var tag = customData.copyTag();
                    double charge = tag.getDouble(OCSettings.namespace + "charge");
                    if (charge > 0) {
                        return 1;
                    }
                }
                return 0;
            }, hoverBoots.item());
        }
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new CallbackCategory());
        registry.add(new ManualCategory());
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        registry.registerGlobalDisplayGenerator(new OCDisplayGenerator());
        registry.registerVisibilityPredicate((category, display) -> {
            var location = display.getDisplayLocation();
            if (location.isPresent() && location.get().equals(ResourceLocation.fromNamespaceAndPath("opencomputers", "loot_disk_cycling"))) {
                return EventResult.interruptFalse();
            }
            return EventResult.pass();
        });
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerDraggableStackVisitor(new DatabaseDragHandler<>());
    }

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(Relay.class, screen -> List.of(new Rectangle(
                screen.windowX() + screen.getXSize(),
                screen.windowY() + 10,
                23, 26
        )));
    }

    @Override
    public void registerTransferHandlers(me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry registry) {
        registry.register(new me.shedaniel.rei.api.client.registry.transfer.TransferHandler() {
            @Override
            public double getPriority() {
                return 1000;
            }

            @Override
            public ApplicabilityResult checkApplicable(Context context) {
                if (!(context.getDisplay() instanceof LootDiskCyclingDisplay)) {
                    return ApplicabilityResult.createNotApplicable();
                }
                var menu = context.getMenu();
                if (!(menu instanceof net.minecraft.world.inventory.CraftingMenu)
                        && !(menu instanceof net.minecraft.world.inventory.InventoryMenu)) {
                    return ApplicabilityResult.createNotApplicable();
                }
                return ApplicabilityResult.createApplicable();
            }

            @Override
            public Result handle(Context context) {
                var menu = context.getMenu();
                if (menu == null) return Result.createNotApplicable();
                var mc = context.getMinecraft();
                var player = mc.player;
                if (player == null) return Result.createNotApplicable();

                int craftStart, craftEnd, invStart;
                if (menu instanceof net.minecraft.world.inventory.CraftingMenu) {
                    craftStart = 1;
                    craftEnd = 10;
                    invStart = 10;
                } else if (menu instanceof net.minecraft.world.inventory.InventoryMenu) {
                    craftStart = 1;
                    craftEnd = 5;
                    invStart = 9;
                } else {
                    return Result.createNotApplicable();
                }

                if (!menu.getCarried().isEmpty()) {
                    return Result.createFailed(Component.literal("Clear cursor first"))
                            .blocksFurtherHandling();
                }

                int diskSlot = -1, wrenchSlot = -1;
                for (int i = invStart; i < menu.slots.size(); i++) {
                    var stack = menu.getSlot(i).getItem();
                    if (diskSlot == -1 && LootManager.isLootDisk(stack)) diskSlot = i;
                    if (wrenchSlot == -1 && Wrench.isWrench(stack)) wrenchSlot = i;
                    if (diskSlot != -1 && wrenchSlot != -1) break;
                }

                if (diskSlot == -1 || wrenchSlot == -1) {
                    var sb = new StringBuilder();
                    if (diskSlot == -1) sb.append("Missing loot disk");
                    if (diskSlot == -1 && wrenchSlot == -1) sb.append(" and ");
                    if (wrenchSlot == -1) sb.append("Missing wrench");
                    return Result.createFailed(Component.literal(sb.toString()))
                            .blocksFurtherHandling();
                }

                if (!context.isActuallyCrafting()) {
                    return Result.createSuccessful();
                }

                var containerScreen = context.getContainerScreen();
                if (containerScreen != null) {
                    mc.setScreen(containerScreen);
                }

                var gameMode = mc.gameMode;
                int containerId = menu.containerId;

                if(gameMode == null) {
                  return Result.createFailed(Component.literal("Failed to query gamemode!"))
                    .blocksFurtherHandling();
                }

                for (int i = craftStart; i < craftEnd; i++) {
                    if (!menu.getSlot(i).getItem().isEmpty()) {
                        gameMode.handleInventoryMouseClick(containerId, i, 0,
                                net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
                    }
                }

                diskSlot = -1;
                wrenchSlot = -1;
                for (int i = invStart; i < menu.slots.size(); i++) {
                    var stack = menu.getSlot(i).getItem();
                    if (diskSlot == -1 && LootManager.isLootDisk(stack)) diskSlot = i;
                    if (wrenchSlot == -1 && Wrench.isWrench(stack)) wrenchSlot = i;
                    if (diskSlot != -1 && wrenchSlot != -1) break;
                }
                if (diskSlot == -1 || wrenchSlot == -1) {
                    return Result.createFailed(Component.literal("Items lost during transfer"))
                            .blocksFurtherHandling();
                }

                gameMode.handleInventoryMouseClick(containerId, diskSlot, 0,
                        net.minecraft.world.inventory.ClickType.PICKUP, player);
                gameMode.handleInventoryMouseClick(containerId, craftStart, 0,
                        net.minecraft.world.inventory.ClickType.PICKUP, player);
                gameMode.handleInventoryMouseClick(containerId, wrenchSlot, 0,
                        net.minecraft.world.inventory.ClickType.PICKUP, player);
                gameMode.handleInventoryMouseClick(containerId, craftStart + 1, 0,
                        net.minecraft.world.inventory.ClickType.PICKUP, player);

                return Result.createSuccessful();
            }
        });
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        var hiddenItems = new HashSet<net.minecraft.world.item.Item>();
        for (var supplier : JEI.getHiddenItems()) {
            var stack = supplier.get();
            if (!stack.isEmpty()) {
                hiddenItems.add(stack.getItem());
            }
        }
        registry.removeEntryIf((EntryStack<?> entry) -> {
            if (entry.getType() == VanillaEntryTypes.ITEM) {
                ItemStack stack = entry.castValue();
                return hiddenItems.contains(stack.getItem());
            }
            return false;
        });
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
                    List<String> wrapSig = wrap(signature, 160);
                    StringBuilder sb = new StringBuilder();
                    for (String s : wrapSig) {
                        if (!sb.isEmpty()) sb.append("\n");
                        sb.append(s);
                    }
                    sb.append("\n");
                    for (String s : wrap(documentation, 152)) {
                        sb.append("  ").append(s).append("\n");
                    }
                    result.add(sb.toString().trim());
                }
            }
        }
        return result;
    }

    static List<CallbackDisplay> buildCallbackDisplays(ItemStack stack, List<String> docs) {
        List<CallbackDisplay> displays = new ArrayList<>();
        if (docs.isEmpty()) return displays;
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
        for (String page : pages) {
            displays.add(new CallbackDisplay(stack, page));
        }
        return displays;
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

    public static class CallbackDisplay implements Display {
        private final EntryIngredient input;
        private final String pageText;

        public CallbackDisplay(ItemStack stack, String pageText) {
            this.input = EntryIngredients.of(stack);
            this.pageText = pageText;
        }

        public String page() {
            return pageText;
        }

        @Override
        public List<EntryIngredient> getInputEntries() {
            return List.of(input);
        }

        @Override
        public List<EntryIngredient> getOutputEntries() {
            return List.of();
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return CALLBACKS;
        }
    }

    public static class ManualDisplay implements Display {
        private final EntryIngredient input;
        private final String path;

        public ManualDisplay(ItemStack stack, String path) {
            this.input = EntryIngredients.of(stack);
            this.path = path;
        }

        public String path() {
            return path;
        }

        @Override
        public List<EntryIngredient> getInputEntries() {
            return List.of(input);
        }

        @Override
        public List<EntryIngredient> getOutputEntries() {
            return List.of();
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return MANUAL;
        }
    }

    private static class LootDiskCyclingDisplay extends DefaultCustomShapelessDisplay {
        public LootDiskCyclingDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs) {
            super(null, inputs, outputs);
        }
    }

    private static class OCDisplayGenerator implements DynamicDisplayGenerator<Display> {
        @Override
        public Optional<List<Display>> getUsageFor(EntryStack<?> entry) {
            if (entry.getType() != VanillaEntryTypes.ITEM) {
                return Optional.empty();
            }
            ItemStack stack = entry.castValue();
            List<Display> displays = new ArrayList<>();
            var docs = getCallbackDocs(stack);
            if (!docs.isEmpty()) {
                displays.addAll(buildCallbackDisplays(stack, docs));
            }
            String path = Manual.pathFor(stack);
            if (path != null) {
                displays.add(new ManualDisplay(stack, path));
            }
            var disks = LootManager.disksForCycling();
            if (!disks.isEmpty() && (Wrench.isWrench(stack) || LootManager.isLootDisk(stack))) {
                var wrench = li.cil.oc.api.Items.get(Constants.ItemName.Wrench).createItemStack(1);
                List<EntryIngredient> inputs = List.of(
                        EntryIngredients.ofItemStacks(disks),
                        EntryIngredients.of(wrench)
                );
                List<EntryIngredient> outputs = List.of(EntryIngredients.ofItemStacks(disks));
                displays.add(new LootDiskCyclingDisplay(inputs, outputs));
            }
            return displays.isEmpty() ? Optional.empty() : Optional.of(displays);
        }

        @Override
        public Optional<List<Display>> getRecipeFor(EntryStack<?> entry) {
            if (entry.getType() != VanillaEntryTypes.ITEM) {
                return Optional.empty();
            }
            ItemStack stack = entry.castValue();
            if (!LootManager.isLootDisk(stack)) {
                return Optional.empty();
            }
            var disks = LootManager.disksForCycling();
            if (disks.isEmpty()) {
                return Optional.empty();
            }
            var wrench = li.cil.oc.api.Items.get(Constants.ItemName.Wrench).createItemStack(1);
            List<EntryIngredient> inputs = List.of(
                    EntryIngredients.ofItemStacks(disks),
                    EntryIngredients.of(wrench)
            );
            List<EntryIngredient> outputs = List.of(EntryIngredients.ofItemStacks(disks));
            return Optional.of(List.of(new LootDiskCyclingDisplay(inputs, outputs)));
        }
    }

    public static class CallbackCategory implements DisplayCategory<CallbackDisplay> {
        private final Renderer icon;

        public CallbackCategory() {
            this.icon = new Renderer() {
                private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("opencomputers", "textures/item/tablet_on.png");

                @Override
                public void render(GuiGraphics graphics, Rectangle bounds, int mouseX, int mouseY, float delta) {
                    int textureIndex = (int) (System.currentTimeMillis() / 500) % 2;
                    graphics.blit(TEXTURE, bounds.x, bounds.y, 0, textureIndex * 16, 16, 16, 16, 32);
                }
            };
        }

        @Override
        public CategoryIdentifier<? extends CallbackDisplay> getCategoryIdentifier() {
            return CALLBACKS;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.category.opencomputers.callbacks");
        }

        @Override
        public Renderer getIcon() {
            return icon;
        }

        @Override
        public int getDisplayHeight() {
            return 125;
        }

        @Override
        public int getDisplayWidth(CallbackDisplay display) {
            return 176;
        }

        @Override
        public List<Widget> setupDisplay(CallbackDisplay display, Rectangle bounds) {
            List<Widget> widgets = new ArrayList<>();
            widgets.add(Widgets.createRecipeBase(bounds));
            var font = Minecraft.getInstance().font;
            String page = display.page();
            if (page != null && !page.isEmpty()) {
                int maxWidth = bounds.width - 8;
                int y = bounds.y + 4;
                String[] lines = page.split("\n");
                for (String line : lines) {
                    if (y >= bounds.y + bounds.height - 4) break;
                    if (font.width(line) > maxWidth) {
                        for (String w : wrap(line, maxWidth)) {
                            if (y >= bounds.y + bounds.height - 4) break;
                            final int ly = y;
                            final String lw = w;
                            widgets.add(Widgets.createDrawableWidget((g, mx, my, d) ->
                                    g.drawString(font, lw, bounds.x + 4, ly, 0x333333, false)));
                            y += font.lineHeight;
                        }
                    } else {
                        final int ly = y;
                        final String lw = line;
                        widgets.add(Widgets.createDrawableWidget((g, mx, my, d) ->
                                g.drawString(font, lw, bounds.x + 4, ly, 0x333333, false)));
                        y += font.lineHeight;
                    }
                }
            }
            return widgets;
        }
    }

    public static class ManualCategory implements DisplayCategory<ManualDisplay> {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("opencomputers", "textures/item/manual.png");
        private final Renderer icon;

        public ManualCategory() {
            this.icon = (graphics, bounds, mouseX, mouseY, delta) -> graphics.blit(TEXTURE, bounds.x, bounds.y, 0, 0, 16, 16, 16, 16);
        }

        @Override
        public CategoryIdentifier<? extends ManualDisplay> getCategoryIdentifier() {
            return MANUAL;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.category.opencomputers.manual");
        }

        @Override
        public Renderer getIcon() {
            return icon;
        }

        @Override
        public int getDisplayHeight() {
            return 40;
        }

        @Override
        public int getDisplayWidth(ManualDisplay display) {
            return 176;
        }

        @Override
        public List<Widget> setupDisplay(ManualDisplay display, Rectangle bounds) {
            List<Widget> widgets = new ArrayList<>();
            widgets.add(Widgets.createRecipeBase(bounds));
            Component label = Component.translatable("jei.manual.opencomputers.open");
            int w = 100, h = 20;
            int x = bounds.x + (bounds.width - w) / 2;
            int y = bounds.y + 10;
            var button = Widgets.createButton(new Rectangle(x, y, w, h), label);
            final String path = display.path();
            button.onClick(btn -> {
                var mc = Minecraft.getInstance();
                mc.setScreen(null);
                Manual.openFor(mc.player);
                Manual.navigate(path);
            });
            widgets.add(button);
            return widgets;
        }
    }
}

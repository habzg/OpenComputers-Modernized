package li.cil.oc.fabric.integration.jei;

import com.mojang.blaze3d.platform.InputConstants;
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
import li.cil.oc.core.impl.common.recipe.LootDiskCyclingRecipe;
import li.cil.oc.core.server.machine.CallbackWrapper;
import li.cil.oc.core.server.machine.Callbacks;
import li.cil.oc.fabric.client.gui.Database;
import li.cil.oc.fabric.client.gui.Relay;
import li.cil.oc.fabric.integration.util.JEI;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@JeiPlugin
@SuppressWarnings("unused")
public class OCJEIPlugin implements IModPlugin {
    public static final RecipeType<CallbackRecipe> CALLBACKS =
            new RecipeType<>(ResourceLocation.fromNamespaceAndPath("opencomputers", "callbacks"), CallbackRecipe.class);
    public static final RecipeType<ManualRecipe> MANUAL =
            new RecipeType<>(ResourceLocation.fromNamespaceAndPath("opencomputers", "manual"), ManualRecipe.class);

    private static final Pattern DocPattern = Pattern.compile("(?s)^function(\\(.*?\\).*?) -- (.*)$");
    private static final Pattern VexPattern = Pattern.compile("(?s)^function(\\(.*?\\).*?); (.*)$");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("opencomputers", "jei");
    }

    @Override
    public void registerItemSubtypes(@NotNull ISubtypeRegistration registration) {
        ISubtypeInterpreter<ItemStack> nbtInterpreter = new ISubtypeInterpreter<>() {
            @Override
            public @NotNull Object getSubtypeData(ItemStack stack, @NotNull UidContext context) {
                return stack.getComponents();
            }

            @Override
            public @NotNull String getLegacyStringSubtypeInfo(@NotNull ItemStack stack, @NotNull UidContext context) {
                return "";
            }
        };
        var mc = li.cil.oc.api.Items.get(Constants.BlockName.Microcontroller);
        if (mc != null && mc.item() != null) {
            registration.registerSubtypeInterpreter(mc.item(), nbtInterpreter);
        }
        var robot = li.cil.oc.api.Items.get(Constants.BlockName.Robot);
        if (robot != null && robot.item() != null) {
            registration.registerSubtypeInterpreter(robot.item(), nbtInterpreter);
        }

        var eeprom = li.cil.oc.api.Items.get(Constants.ItemName.EEPROM);
        if (eeprom != null && eeprom.item() != null) {
            registration.registerSubtypeInterpreter(eeprom.item(), new ISubtypeInterpreter<>() {
                @Override
                public @NotNull Object getSubtypeData(@NotNull ItemStack stack, @NotNull UidContext context) {
                    var customData = stack.get(DataComponents.CUSTOM_DATA);
                    if (customData != null && !customData.isEmpty()) {
                        var tag = customData.copyTag();
                        if (tag.contains(OCSettings.namespace + "data")) {
                            var data = tag.getCompound(OCSettings.namespace + "data");
                            if (data.contains(OCSettings.namespace + "eeprom") && data.getByteArray(OCSettings.namespace + "eeprom").length > 0) {
                                return "programmed";
                            }
                        }
                    }
                    return "blank";
                }

                @Override
                public @NotNull String getLegacyStringSubtypeInfo(@NotNull ItemStack stack, @NotNull UidContext context) {
                    return "";
                }
            });
        }
        var floppy = li.cil.oc.api.Items.get(Constants.ItemName.Floppy);
        if (floppy != null && floppy.item() != null) {
            registration.registerSubtypeInterpreter(floppy.item(), new ISubtypeInterpreter<>() {
                @Override
                public @Nullable Object getSubtypeData(@NotNull ItemStack stack, @NotNull UidContext context) {
                    var customData = stack.get(DataComponents.CUSTOM_DATA);
                    if (customData != null && !customData.isEmpty() && customData.copyTag().contains(OCSettings.namespace + "lootFactory")) {
                        var tag = new CompoundTag();
                        tag.putString(OCSettings.namespace + "lootFactory", customData.copyTag().getString(OCSettings.namespace + "lootFactory"));
                        return tag;
                    }
                    return null;
                }

                @Override
                public @NotNull String getLegacyStringSubtypeInfo(@NotNull ItemStack stack, @NotNull UidContext context) {
                    return "";
                }
            });
        }
        var hoverBoots = li.cil.oc.api.Items.get(Constants.ItemName.HoverBoots);
        if (hoverBoots != null && hoverBoots.item() != null) {
            registration.registerSubtypeInterpreter(hoverBoots.item(), new ISubtypeInterpreter<>() {
                @Override
                public @NotNull Object getSubtypeData(@NotNull ItemStack stack, @NotNull UidContext context) {
                    var customData = stack.get(DataComponents.CUSTOM_DATA);
                    if (customData != null && !customData.isEmpty()) {
                        var tag = customData.copyTag();
                        double charge = tag.getDouble(OCSettings.namespace + "charge");
                        if (charge > 0) {
                            return "charged";
                        }
                    }
                    return "uncharged";
                }

                @Override
                public @NotNull String getLegacyStringSubtypeInfo(@NotNull ItemStack stack, @NotNull UidContext context) {
                    return "";
                }
            });
        }
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new ManualCategory(guiHelper));
        registration.addRecipeCategories(new CallbackCategory(guiHelper));
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addRecipeManagerPlugin(new OCRecipeManagerPlugin());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(Database.class, new DatabaseGhostIngredientHandler());
        registration.addGuiContainerHandler(Relay.class, new IGuiContainerHandler<>() {
            @Override
            public @NotNull List<Rect2i> getGuiExtraAreas(@NotNull Relay containerScreen) {
                return List.of(new Rect2i(
                        containerScreen.windowX() + containerScreen.getXSize(),
                        containerScreen.windowY() + 10,
                        23, 26
                ));
            }
        });
    }

    @Override
    public void registerVanillaCategoryExtensions(@NotNull IVanillaCategoryExtensionRegistration registration) {
        if (!LootManager.disksForCycling().isEmpty()) {
            registration.getCraftingCategory().addExtension(LootDiskCyclingRecipe.class, new LootDiskCyclingExtension());
        }
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime runtime) {
        for (var supplier : JEI.getHiddenItems()) {
            var stack = supplier.get();
            if (!stack.isEmpty()) {
                runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, List.of(stack));
            }
        }
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
        for (String page : pages) {
            recipes.add(new CallbackRecipe(stack, page));
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

    @SuppressWarnings("unused")
    public record CallbackRecipe(ItemStack stack, String page) {
    }

    @SuppressWarnings("unused")
    public record ManualRecipe(ItemStack stack, String path) {
    }

    private static class OCRecipeManagerPlugin implements mezz.jei.api.recipe.advanced.IRecipeManagerPlugin {
        @Override
        public <V> @NotNull List<RecipeType<?>> getRecipeTypes(IFocus<V> focus) {
            if (focus.getRole() == RecipeIngredientRole.INPUT && focus.getTypedValue().getType() == VanillaTypes.ITEM_STACK) {
                ItemStack stack = (ItemStack) focus.getTypedValue().getIngredient();
                List<RecipeType<?>> types = new ArrayList<>();
                if (!getCallbackDocs(stack).isEmpty()) types.add(CALLBACKS);
                if (Manual.pathFor(stack) != null) types.add(MANUAL);
                return types;
            }
            return List.of();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T, V> @NotNull List<T> getRecipes(@NotNull IRecipeCategory<T> recipeCategory, IFocus<V> focus) {
            if (focus.getRole() == RecipeIngredientRole.INPUT && focus.getTypedValue().getType() == VanillaTypes.ITEM_STACK) {
                ItemStack stack = (ItemStack) focus.getTypedValue().getIngredient();
                if (recipeCategory.getRecipeType() == CALLBACKS) {
                    List<String> docs = getCallbackDocs(stack);
                    if (!docs.isEmpty()) {
                        return (List<T>) buildCallbackRecipes(stack, docs);
                    }
                } else if (recipeCategory.getRecipeType() == MANUAL) {
                    String path = Manual.pathFor(stack);
                    if (path != null) {
                        return List.of((T) new ManualRecipe(stack, path));
                    }
                }
            }
            return List.of();
        }

        @Override
        public <T> @NotNull List<T> getRecipes(@NotNull IRecipeCategory<T> recipeCategory) {
            return List.of();
        }
    }

    private static class LootDiskCyclingExtension implements ICraftingCategoryExtension<LootDiskCyclingRecipe> {
        @Override
        public boolean isHandled(@NotNull RecipeHolder<LootDiskCyclingRecipe> recipeHolder) {
            return !LootManager.disksForCycling().isEmpty();
        }

        @Override
        public void setRecipe(@NotNull RecipeHolder<LootDiskCyclingRecipe> recipeHolder, @NotNull IRecipeLayoutBuilder builder, @NotNull ICraftingGridHelper craftingGridHelper, @NotNull IFocusGroup focuses) {
            var disks = LootManager.disksForCycling();
            if (disks.isEmpty()) return;
            var wrench = li.cil.oc.api.Items.get(Constants.ItemName.Wrench).createItemStack(1);
            List<List<ItemStack>> inputs = List.of(new ArrayList<>(disks), List.of(wrench));
            craftingGridHelper.createAndSetInputs(builder, inputs, 0, 0);
            craftingGridHelper.createAndSetOutputs(builder, new ArrayList<>(disks));
        }
    }

    public static class CallbackCategory implements IRecipeCategory<CallbackRecipe> {
        private final IDrawable icon;

        public CallbackCategory(IGuiHelper guiHelper) {
            var timer = guiHelper.createTickTimer(20, 1, false);
            this.icon = new IDrawable() {
                private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("opencomputers", "textures/item/tablet_on.png");

                @Override
                public int getWidth() {
                    return 16;
                }

                @Override
                public int getHeight() {
                    return 16;
                }

                @Override
                public void draw(@NotNull GuiGraphics guiGraphics, int xOffset, int yOffset) {
                    int frame = timer.getValue();
                    guiGraphics.blit(TEXTURE, xOffset, yOffset, 0, frame * 16, 16, 16, 16, 32);
                }
            };
        }

        @Override
        public @NotNull RecipeType<CallbackRecipe> getRecipeType() {
            return CALLBACKS;
        }

        @Override
        public @NotNull Component getTitle() {
            return Component.translatable("jei.category.opencomputers.callbacks");
        }

        @Override
        public int getWidth() {
            return 176;
        }

        @Override
        public int getHeight() {
            return 125;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull CallbackRecipe recipe, @NotNull IFocusGroup focuses) {
        }

        @Override
        public void draw(CallbackRecipe recipe, @NotNull IRecipeSlotsView view, @NotNull GuiGraphics gui, double mouseX, double mouseY) {
            var font = Minecraft.getInstance().font;
            if (recipe.page() != null && !recipe.page().isEmpty()) {
                int maxWidth = getWidth() - 8;
                int y = 4;
                String[] lines = recipe.page().split("\n");
                for (String line : lines) {
                    if (y >= getHeight() - 4) break;
                    if (font.width(line) > maxWidth) {
                        var wrapped = wrap(line, maxWidth);
                        for (String w : wrapped) {
                            if (y >= getHeight() - 4) break;
                            gui.drawString(font, w, 4, y, 0x333333, false);
                            y += font.lineHeight;
                        }
                    } else {
                        gui.drawString(font, line, 4, y, 0x333333, false);
                        y += font.lineHeight;
                    }
                }
            }
        }
    }

    public static class ManualCategory implements IRecipeCategory<ManualRecipe> {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("opencomputers", "textures/item/manual.png");
        private final IDrawable icon;

        public ManualCategory(IGuiHelper ignoredGuiHelper) {
            this.icon = new IDrawable() {
                @Override
                public int getWidth() {
                    return 16;
                }

                @Override
                public int getHeight() {
                    return 16;
                }

                @Override
                public void draw(@NotNull GuiGraphics guiGraphics, int xOffset, int yOffset) {
                    guiGraphics.blit(TEXTURE, xOffset, yOffset, 0, 0, 16, 16, 16, 16);
                }
            };
        }

        @Override
        public @NotNull RecipeType<ManualRecipe> getRecipeType() {
            return MANUAL;
        }

        @Override
        public @NotNull Component getTitle() {
            return Component.translatable("jei.category.opencomputers.manual");
        }

        @Override
        public int getWidth() {
            return 176;
        }

        @Override
        public int getHeight() {
            return 40;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull ManualRecipe recipe, @NotNull IFocusGroup focuses) {
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, ManualRecipe recipe, @NotNull IFocusGroup focuses) {
            Component label = Component.translatable("jei.manual.opencomputers.open");
            int x = (176 - 100) / 2, y = 10, w = 100, h = 20;
            builder.addWidget(new ManualOpenButton(x, y, w, h, label, recipe.path()));
            builder.addInputHandler(new ManualOpenInput(x, y, w, h, recipe.path()));
        }
    }

    @SuppressWarnings("unused")
    private record ManualOpenButton(int x, int y, int w, int h, Component label, String path) implements IRecipeWidget {
        private static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("widget/button");
        private static final ResourceLocation BUTTON_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/button_highlighted");

        @Override
        public @NotNull ScreenPosition getPosition() {
            return new ScreenPosition(x, y);
        }

        @Override
        public void drawWidget(GuiGraphics guiGraphics, double mouseX, double mouseY) {
            boolean hovered = mouseX >= 0 && mouseX < w && mouseY >= 0 && mouseY < h;
            guiGraphics.blitSprite(hovered ? BUTTON_HIGHLIGHTED : BUTTON, 0, 0, w, h);
            var font = Minecraft.getInstance().font;
            guiGraphics.drawString(font, label, (w - font.width(label)) / 2, (h - 8) / 2, 0xFFFFFF, false);
        }
    }

    private record ManualOpenInput(int x, int y, int w, int h, String path) implements IJeiInputHandler {
        @Override
        public @NotNull ScreenRectangle getArea() {
            return new ScreenRectangle(x, y, w, h);
        }

        @Override
        public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
            if (input.getKey().getType() != InputConstants.Type.MOUSE) return false;
            boolean hovered = mouseX >= 0 && mouseX < w && mouseY >= 0 && mouseY < h;
            if (!input.isSimulate() && hovered) {
                var mc = Minecraft.getInstance();
                mc.setScreen(null);
                Manual.openFor(mc.player);
                Manual.navigate(path);
                return true;
            }
            return input.isSimulate() && hovered;
        }
    }
}

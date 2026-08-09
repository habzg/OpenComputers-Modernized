package li.cil.oc.fabric.common.init;

import li.cil.oc.core.impl.common.recipe.ColorizeRecipe;
import li.cil.oc.core.impl.common.recipe.ColorizeRecipeSerializer;
import li.cil.oc.core.impl.common.recipe.DecolorizeRecipe;
import li.cil.oc.core.impl.common.recipe.DecolorizeRecipeSerializer;
import li.cil.oc.core.impl.common.recipe.ExtendedShapedRecipe;
import li.cil.oc.core.impl.common.recipe.ExtendedShapelessOreRecipe;
import li.cil.oc.core.impl.common.recipe.LootDiskCyclingRecipe;
import li.cil.oc.core.impl.common.recipe.LuaBiosRecipe;
import li.cil.oc.core.impl.common.recipe.OPPMRecipe;
import li.cil.oc.core.impl.common.recipe.OpenOSRecipe;
import li.cil.oc.fabric.OpenComputers;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class Recipes {
    private Recipes() {
    }

    public static void init() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "luabios"),
                LuaBiosRecipe.LuaBiosRecipeSerializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "loot_disk_cycling"),
                LootDiskCyclingRecipe.Serializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "openos"),
                OpenOSRecipe.OpenOSRecipeSerializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "oppm"),
                OPPMRecipe.OPPMRecipeSerializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "colorize"),
                ColorizeRecipeSerializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "decolorize"),
                DecolorizeRecipeSerializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "shapeless"),
                ExtendedShapelessOreRecipe.Serializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "shaped"),
                ExtendedShapedRecipe.Serializer.INSTANCE);

        ColorizeRecipe.setSerializer(ColorizeRecipeSerializer.INSTANCE);
        DecolorizeRecipe.setSerializer(DecolorizeRecipeSerializer.INSTANCE);
    }
}

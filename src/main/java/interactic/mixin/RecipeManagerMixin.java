package interactic.mixin;

import interactic.InteracticInit;
import net.minecraft.recipe.PreparedRecipes;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(ServerRecipeManager.class)
public class RecipeManagerMixin {

    @Shadow private PreparedRecipes preparedRecipes;

    @Inject(
        method = "apply(Lnet/minecraft/recipe/PreparedRecipes;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V",
        at = @At("TAIL")
    )
    private void conditionallyRemoveFilterRecipe(PreparedRecipes prepared, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci) {
        if (InteracticInit.getConfig().itemFilterEnabled()) return;

        RegistryKey<Recipe<?>> filterKey = RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(InteracticInit.MOD_ID, "item_filter"));
        List<RecipeEntry<?>> filtered = this.preparedRecipes.recipes().stream()
                .filter(entry -> !entry.id().equals(filterKey))
                .collect(Collectors.toList());
        this.preparedRecipes = PreparedRecipes.of(filtered);
    }
}

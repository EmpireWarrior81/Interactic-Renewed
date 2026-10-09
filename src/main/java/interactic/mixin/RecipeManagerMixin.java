package interactic.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import interactic.InteracticInit;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    // Format verified against a real vanilla recipe (data/minecraft/recipe/compass.json)
    // extracted from the 26.1 client jar: "key" entries are now plain ingredient-id
    // strings (no longer {"item": "..."}), and a "category" field is now present.
    private static final String FILTER_RECIPE = """
            {
                "type": "minecraft:crafting_shaped",
                "category": "misc",
                "pattern": [
                    " c ",
                    "cEc",
                    " c "
                ],
                "key": {
                    "c": "minecraft:copper_ingot",
                    "E": "minecraft:ender_pearl"
                },
                "result": {
                    "id": "interactic:item_filter"
                }
            }
            """;

    @Shadow
    @Final
    private HolderLookup.Provider registries;

    @Inject(method = "prepare", at = @At("RETURN"), cancellable = true)
    public void injectFilterRecipe(ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfoReturnable<RecipeMap> cir) {
        if (!InteracticInit.getConfig().itemFilterEnabled()) return;

        JsonObject json = new Gson().fromJson(FILTER_RECIPE, JsonObject.class);
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(InteracticInit.MOD_ID, "item_filter"));
        Recipe<?> recipe = Recipe.CODEC.parse(this.registries.createSerializationContext(JsonOps.INSTANCE), json).getOrThrow(JsonParseException::new);
        RecipeHolder<?> holder = new RecipeHolder<>(key, recipe);

        List<RecipeHolder<?>> combined = new ArrayList<>(cir.getReturnValue().values());
        combined.add(holder);
        cir.setReturnValue(RecipeMap.create(combined));
    }
}

package interactic.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import interactic.InteracticInit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    private static final String FILTER_RECIPE = """
            {
                "type": "minecraft:crafting_shaped",
                "pattern": [
                    " c ",
                    "cEc",
                    " c "
                ],
                "key": {
                    "c": {
                        "item": "minecraft:copper_ingot"
                    },
                    "E": {
                        "item": "minecraft:ender_pearl"
                    }
                },
                "result": {
                    "id": "interactic:item_filter",
                    "count": 1
                }
            }
            """;

    @Inject(method = "apply", at = @At("HEAD"))
    public void injectFilterRecipe(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        if (InteracticInit.getConfig().itemFilterEnabled()) {
            map.put(ResourceLocation.fromNamespaceAndPath(InteracticInit.MOD_ID, "item_filter"), new Gson().fromJson(FILTER_RECIPE, JsonObject.class));
        }
    }
}

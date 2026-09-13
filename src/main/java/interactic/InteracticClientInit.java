package interactic;

import interactic.network.PickupPayload;
import interactic.network.SetFilterModePayload;
import interactic.util.InteracticRenderState;
import io.wispforest.owo.config.ui.ConfigScreenProviders;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Util;

public class InteracticClientInit implements ClientModInitializer {

    public static final KeyMapping PICKUP_ITEM = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.interactic.pickup_item",
            InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC));

    @Override
    public void onInitializeClient() {
        // TODO: ModelPredicateProviderRegistry was removed in 1.21.4. The item_filter
        // enabled/disabled texture swap needs to be reimplemented via the new item-model
        // definition system (assets/interactic/items/*.json using a "minecraft:select"
        // component-driven model instead of assets/interactic/models/item/item_filter*.json).

        if (InteracticInit.getConfig().itemFilterEnabled()) {
            MenuScreens.register(InteracticInit.ITEM_FILTER_SCREEN_HANDLER, ItemFilterScreen::new);

            ClientPlayNetworking.registerGlobalReceiver(SetFilterModePayload.TYPE, (payload, context) -> {
                context.client().execute(() -> {
                    if (!(context.client().screen instanceof ItemFilterScreen screen)) return;
                    screen.blockMode = payload.mode();
                });
            });
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (PICKUP_ITEM.consumeClick()) {
                ClientPlayNetworking.send(new PickupPayload());
                client.player.swing(InteractionHand.MAIN_HAND);
            }
        });

        ConfigScreenProviders.register("interactic", InteracticConfigScreen::new);

        LevelRenderEvents.START_MAIN.register(context -> {
            long now = Util.getMillis();
            if (InteracticRenderState.lastFrameMs != 0) {
                InteracticRenderState.frameDuration = Math.min((now - InteracticRenderState.lastFrameMs) / 50.0f, 0.5f);
            }
            InteracticRenderState.lastFrameMs = now;
        });
    }
}

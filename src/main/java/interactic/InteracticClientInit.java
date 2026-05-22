package interactic;

import interactic.network.PickupPayload;
import interactic.network.SetFilterModePayload;
import interactic.util.InteracticRenderState;
import io.wispforest.owo.config.ui.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class InteracticClientInit implements ClientModInitializer {

    public static final KeyBinding PICKUP_ITEM = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.interactic.pickup_item",
            InputUtil.UNKNOWN_KEY.getCode(), "key.categories.misc"));

    @Override
    public void onInitializeClient() {
        ModelPredicateProviderRegistry.register(Identifier.of("interactic", "enabled"), (stack, world, entity, seed) -> {
            var data = stack.get(DataComponentTypes.CUSTOM_DATA);
            return data != null && data.copyNbt().getBoolean("Enabled") ? 1 : 0;
        });

        if (InteracticInit.getConfig().itemFilterEnabled()) {
            HandledScreens.register(InteracticInit.ITEM_FILTER_SCREEN_HANDLER, ItemFilterScreen::new);

            ClientPlayNetworking.registerGlobalReceiver(SetFilterModePayload.ID, (payload, context) -> {
                context.client().execute(() -> {
                    if (!(context.client().currentScreen instanceof ItemFilterScreen screen)) return;
                    screen.blockMode = payload.mode();
                });
            });
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (PICKUP_ITEM.wasPressed()) {
                ClientPlayNetworking.send(new PickupPayload());
                client.player.swingHand(Hand.MAIN_HAND);
            }
        });

        ConfigScreen.registerProvider("interactic", InteracticConfigScreen::new);

        WorldRenderEvents.START.register(context -> {
            long now = Util.getMeasuringTimeMs();
            if (InteracticRenderState.lastFrameMs != 0) {
                InteracticRenderState.frameDuration = Math.min((now - InteracticRenderState.lastFrameMs) / 50.0f, 0.5f);
            }
            InteracticRenderState.lastFrameMs = now;
        });
    }
}

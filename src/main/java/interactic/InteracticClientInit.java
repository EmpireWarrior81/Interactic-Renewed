package interactic;

import interactic.network.DropWithPowerPayload;
import interactic.network.PickupPayload;
import interactic.network.SetFilterModePayload;
import interactic.util.InteracticRenderState;
import io.wispforest.owo.config.ui.ConfigScreenProviders;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class InteracticClientInit implements ClientModInitializer {

    public static final KeyBinding PICKUP_ITEM = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.interactic.pickup_item",
            InputUtil.UNKNOWN_KEY.getCode(), "key.categories.misc"));

    // Throw power state — tracked here via isPressed() every tick, separate from handleInputEvents.
    public static float dropPower = 0.9f;
    public static boolean dropKeyHeld = false;
    // Set by MinecraftClientMixin.handleDropPower when the redirect fires, so quick taps still drop.
    public static boolean dropKeyPressed = false;

    @Override
    public void onInitializeClient() {
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

            if (!InteracticInit.getConfig().itemThrowing() || client.player == null) {
                dropPower = 0.9f;
                dropKeyHeld = false;
                dropKeyPressed = false;
                return;
            }

            boolean keyHeld = client.options.dropKey.isPressed() && !Screen.hasShiftDown();

            if (keyHeld) {
                dropPower += 0.075f;
                if (dropPower > 5f) dropPower = 5f;
                if (dropPower >= 1.5f)
                    client.player.sendMessage(Text.of("Power: " + BigDecimal.valueOf(Math.max(dropPower, 1)).setScale(1, RoundingMode.HALF_UP)), true);
                dropKeyHeld = true;
                dropKeyPressed = false;
            } else if (dropKeyHeld) {
                // Key was held and just released — execute the throw.
                boolean dropAll = Screen.hasControlDown();
                if (dropPower >= 1.5f) {
                    ClientPlayNetworking.send(new DropWithPowerPayload(dropPower, dropAll));
                    if (InteracticInit.getConfig().swingArm()) client.player.swingHand(Hand.MAIN_HAND);
                } else if (client.player.dropSelectedItem(dropAll)) {
                    if (InteracticInit.getConfig().swingArm()) client.player.swingHand(Hand.MAIN_HAND);
                }
                dropPower = 0.9f;
                dropKeyHeld = false;
                dropKeyPressed = false;
            } else if (dropKeyPressed) {
                // Quick tap — the redirect fired but the key released before the tick listener ran.
                if (client.player.dropSelectedItem(Screen.hasControlDown()))
                    if (InteracticInit.getConfig().swingArm()) client.player.swingHand(Hand.MAIN_HAND);
                dropPower = 0.9f;
                dropKeyPressed = false;
            } else {
                dropPower = 0.9f;
            }
        });

        ConfigScreenProviders.register("interactic", InteracticConfigScreen::new);

        WorldRenderEvents.START.register(context -> {
            long now = Util.getMeasuringTimeMs();
            if (InteracticRenderState.lastFrameMs != 0) {
                InteracticRenderState.frameDuration = Math.min((now - InteracticRenderState.lastFrameMs) / 50.0f, 0.5f);
            }
            InteracticRenderState.lastFrameMs = now;
        });
    }
}

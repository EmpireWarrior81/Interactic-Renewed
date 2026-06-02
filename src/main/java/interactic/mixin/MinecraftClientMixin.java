package interactic.mixin;

import interactic.InteracticClientInit;
import interactic.InteracticInit;
import interactic.network.PickupPayload;
import interactic.util.Helpers;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow @Nullable public Entity cameraEntity;
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;
    @Shadow @Nullable public ClientPlayerEntity player;

    @Inject(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isRiding()Z", shift = At.Shift.AFTER), cancellable = true)
    private void tryPickupItem(CallbackInfo ci) {
        if (!InteracticInit.getConfig().rightClickPickup()) return;
        if (KeyBindingHelper.getBoundKeyOf(InteracticClientInit.PICKUP_ITEM) != InputUtil.UNKNOWN_KEY) return;

        if (Helpers.raycastItem(cameraEntity, this.player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE)) == null) return;
        ClientPlayNetworking.send(new PickupPayload());
        this.player.swingHand(Hand.MAIN_HAND);
        ci.cancel();
    }

    @Redirect(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;dropSelectedItem(Z)Z"))
    private boolean handleDropPower(ClientPlayerEntity clientPlayerEntity, boolean dropEntireStack) {
        if (!InteracticInit.getConfig().itemThrowing()) return clientPlayerEntity.dropSelectedItem(dropEntireStack);

        if (!Screen.hasShiftDown()) {
            // Suppress vanilla drop. Power tracking and throw are handled in InteracticClientInit
            // via END_CLIENT_TICK using isPressed(), since wasPressed() doesn't repeat in 1.21.4.
            InteracticClientInit.dropKeyPressed = true;
            return false;
        } else {
            return clientPlayerEntity.dropSelectedItem(dropEntireStack);
        }
    }

    @Redirect(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V"))
    private void dontSwingArms(ClientPlayerEntity player, Hand hand) {
        if (!InteracticInit.getConfig().swingArm()) return;
        player.swingHand(hand);
    }
}

package interactic.mixin;

import interactic.InteracticClientInit;
import interactic.InteracticInit;
import interactic.network.DropWithPowerPayload;
import interactic.network.PickupPayload;
import interactic.util.Helpers;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {

    @Unique
    private float dropPower = 0.9f;

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    @Final
    public Options options;

    @Shadow
    public abstract boolean hasShiftDown();

    @Shadow
    public abstract boolean hasControlDown();

    @Shadow
    private int rightClickDelay;

    // Note: vanilla's startUseItem() no longer has an isRiding()/isPassenger() gate to
    // hook after (it now gates on isHandsBusy() instead, which is not equivalent -
    // covers boat-steering input, not general riding). Injecting at HEAD instead, which
    // preserves the intent (try custom pickup before vanilla item-use proceeds).
    //
    // handleKeybinds() calls startUseItem() every tick the use key is held down, gated on
    // rightClickDelay == 0 (see the "isDown() && rightClickDelay == 0" repeat-fire check).
    // Vanilla's own startUseItem() sets rightClickDelay = 4 as its very first action - but
    // since we cancel at HEAD, that line never runs on a successful pickup, so the cooldown
    // never engages and the repeat-fire check keeps calling startUseItem() every subsequent
    // tick the button stays held. Once the item is actually gone (after server round-trip
    // confirms the pickup), the very next repeat-tick - still with the button held down from
    // the original click - falls through to vanilla's normal item-use/block-placement logic
    // instead, which is what caused pickups to sometimes place a block right after. Setting
    // the same cooldown vanilla would have applied closes that gap.
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void tryPickupItem(CallbackInfo ci) {
        if (!InteracticInit.getConfig().rightClickPickup()) return;
        if (KeyMappingHelper.getBoundKeyOf(InteracticClientInit.PICKUP_ITEM) != InputConstants.UNKNOWN) return;

        if (Helpers.raycastItem(((Minecraft) (Object) this).getCameraEntity(), this.player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE)) == null) return;
        ClientPlayNetworking.send(new PickupPayload());
        this.player.swing(InteractionHand.MAIN_HAND);
        this.rightClickDelay = 4;
        ci.cancel();
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;drop(Z)Z"))
    private boolean handleDropPower(LocalPlayer clientPlayerEntity, boolean dropEntireStack) {
        if (!InteracticInit.getConfig().itemThrowing()) return clientPlayerEntity.drop(dropEntireStack);

        if (!this.hasShiftDown()) {
            dropPower += 0.075;
            if (dropPower > 5) dropPower = 5;
            if (dropPower >= 1.5)
                ((Minecraft) (Object) this).gui.setOverlayMessage(Component.literal("Power: " + BigDecimal.valueOf(Math.max(dropPower, 1)).setScale(1, RoundingMode.HALF_UP)), false);
            return false;
        } else {
            return clientPlayerEntity.drop(dropEntireStack);
        }
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"))
    private void dontSwingArms(LocalPlayer player, InteractionHand hand) {
        if (!InteracticInit.getConfig().swingArm()) return;
        player.swing(hand);
    }

    @Inject(method = "handleKeybinds", at = @At("RETURN"))
    private void afterDrop(CallbackInfo ci) {
        if (!InteracticInit.getConfig().itemThrowing()) return;

        if (dropPower > 0.9f && !options.keyDrop.isDown()) {
            final var dropAll = this.hasControlDown();

            if (dropPower >= 1.5) {
                ClientPlayNetworking.send(new DropWithPowerPayload(dropPower, dropAll));

                if (!this.player.getInventory().removeFromSelected(dropAll).isEmpty()) {
                    if (InteracticInit.getConfig().swingArm()) this.player.swing(InteractionHand.MAIN_HAND);
                }
            } else if (this.player.drop(dropAll)) {
                if (InteracticInit.getConfig().swingArm()) this.player.swing(InteractionHand.MAIN_HAND);
            }

            dropPower = 0.9f;
        }
    }
}

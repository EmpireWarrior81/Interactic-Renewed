package interactic.mixin;

import interactic.InteracticClientInit;
import interactic.InteracticInit;
import interactic.network.DropWithPowerPayload;
import interactic.network.PickupPayload;
import interactic.util.Helpers;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
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

@Mixin(net.minecraft.client.Minecraft.class)
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
    public abstract Entity getCameraEntity();

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z"), cancellable = true)
    private void tryPickupItem(CallbackInfo ci) {
        if (!InteracticInit.getConfig().rightClickPickup()) return;
        if (!InteracticClientInit.PICKUP_ITEM.isUnbound()) return;

        if (Helpers.raycastItem(getCameraEntity(), this.player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE)) == null) return;
        PacketDistributor.sendToServer(new PickupPayload());
        this.player.swing(InteractionHand.MAIN_HAND);
        ci.cancel();
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;drop(Z)Z"))
    private boolean handleDropPower(LocalPlayer clientPlayerEntity, boolean dropEntireStack) {
        if (!InteracticInit.getConfig().itemThrowing()) return clientPlayerEntity.drop(dropEntireStack);

        if (!Screen.hasShiftDown()) {
            dropPower += 0.075;
            if (dropPower > 5) dropPower = 5;
            if (dropPower >= 1.5)
                clientPlayerEntity.displayClientMessage(Component.literal("Power: " + BigDecimal.valueOf(Math.max(dropPower, 1)).setScale(1, RoundingMode.HALF_UP)), true);
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
            final var dropAll = Screen.hasControlDown();

            if (dropPower >= 1.5) {
                PacketDistributor.sendToServer(new DropWithPowerPayload(dropPower, dropAll));

                if (!this.player.getInventory().removeItem(this.player.getInventory().selected, dropAll && !this.player.getInventory().getSelected().isEmpty() ? this.player.getInventory().getSelected().getCount() : 1).isEmpty()) {
                    if (InteracticInit.getConfig().swingArm()) this.player.swing(InteractionHand.MAIN_HAND);
                }
            } else if (this.player.drop(dropAll)) {
                if (InteracticInit.getConfig().swingArm()) this.player.swing(InteractionHand.MAIN_HAND);
            }

            dropPower = 0.9f;
        }
    }
}

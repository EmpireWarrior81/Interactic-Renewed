package interactic.mixin;

import interactic.InteracticInit;
import interactic.util.InteracticItemExtensions;
import interactic.util.InteracticPlayerExtension;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Player.class)
public class PlayerEntityMixin implements InteracticPlayerExtension {

    @Unique
    private float dropPower = 1;

    @Override
    public void setDropPower(float power) {
        this.dropPower = power;
    }

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;setDeltaMovement(DDD)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    private void applyDropPower(ItemStack droppedItem, boolean dropAround, boolean includeThrowerName, CallbackInfoReturnable<ItemEntity> cir, double d0, ItemEntity itementity) {
        if (!InteracticInit.getConfig().itemThrowing()) return;

        if (this.dropPower > 1) {
            var velocity = ((Player) (Object) this).getViewVector(0f).scale(this.dropPower * .35f);
            itementity.setDeltaMovement(velocity);
            itementity.hasImpulse = true;

            itementity.setPos(itementity.getX(), ((Player) (Object) this).getEyeY(), itementity.getZ());

            if (includeThrowerName) {
                ((InteracticItemExtensions) itementity).markThrown();
                if (this.dropPower >= 5) ((InteracticItemExtensions) itementity).markFullPower();
            }

            this.dropPower = 1;
        }
    }
}

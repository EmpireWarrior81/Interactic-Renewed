package interactic.mixin;

import interactic.InteracticInit;
import interactic.util.InteracticItemExtensions;
import interactic.util.InteracticPlayerExtension;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Note: the drop-item logic moved off Player entirely - the public 2-arg
// Player.drop(ItemStack, boolean) just delegates to LivingEntity's private
// createItemStackToDrop(ItemStack, boolean randomly, boolean thrownFromHand), which is
// where the ItemEntity is actually built and its throw velocity set. Since that method
// is private, it can only be targeted by mixing into LivingEntity directly (a private
// method isn't visible to a Player-only mixin). Rather than local-capturing the many
// branch-local floats (pow/sinX/cosX/sinY/cosY/dir/pow2) that exist at the old
// setVelocity-equivalent call site - fragile, and exactly the kind of thing that needs
// real bytecode verification - this instead captures the method's return value
// (the ItemEntity itself) via CallbackInfoReturnable and mutates it directly, which
// needs no local capture at all and is more robust to future decompiler drift.
@Mixin(LivingEntity.class)
public abstract class PlayerEntityMixin implements InteracticPlayerExtension {

    @Unique
    private float dropPower = 1;

    @Override
    public void setDropPower(float power) {
        this.dropPower = power;
    }

    @Inject(method = "createItemStackToDrop", at = @At("RETURN"))
    private void applyDropPower(ItemStack itemStack, boolean randomly, boolean thrownFromHand, CallbackInfoReturnable<ItemEntity> cir) {
        if (!InteracticInit.getConfig().itemThrowing()) return;

        var item = cir.getReturnValue();
        if (item == null) return;

        if (this.dropPower > 1) {
            var self = (LivingEntity) (Object) this;
            var velocity = self.getViewVector(0f).scale(this.dropPower * .35f);
            item.setDeltaMovement(velocity);
            item.hurtMarked = true;

            item.setPos(item.getX(), self.getEyeY(), item.getZ());

            if (thrownFromHand) {
                ((InteracticItemExtensions) item).markThrown();
                if (this.dropPower >= 5) ((InteracticItemExtensions) item).markFullPower();
            }

            this.dropPower = 1;
        }
    }
}

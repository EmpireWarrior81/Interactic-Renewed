package interactic.mixin;

import interactic.InteracticInit;
import interactic.util.Helpers;
import interactic.util.InteracticItemExtensions;
import interactic.util.ItemDamageSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements InteracticItemExtensions {

    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    private int itemAge;

    @Shadow
    @Nullable
    public abstract Entity getOwner();

    @Unique
    private float rotation = -1;

    @Unique
    private boolean wasThrown;

    @Unique
    private boolean wasFullPower;

    private ItemEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public float getRotation() {
        return rotation;
    }

    @Override
    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    @Override
    public void markThrown() {
        this.wasThrown = true;
    }

    @Override
    public void markFullPower() {
        this.wasFullPower = true;
    }

    @Inject(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;getItem()Lnet/minecraft/world/item/ItemStack;", ordinal = 0), cancellable = true)
    private void controlPickup(Player player, CallbackInfo ci) {
        if (Helpers.canPlayerPickUpItem(player, (ItemEntity) (Object) this)) return;
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void dealThrowingDamage(CallbackInfo ci) {
        if (!InteracticInit.getConfig().itemsActAsProjectiles()) return;
        if (itemAge < 2) return;

        var level = this.level();
        if (level.isClientSide()) return;
        var serverLevel = (ServerLevel) level;

        if (this.onGround()) this.wasThrown = false;
        if (!this.wasThrown) return;

        var component = this.getItem().get(DataComponents.ATTRIBUTE_MODIFIERS);
        boolean hasDamageModifiers = component != null && component.modifiers().stream()
                .anyMatch(e -> e.attribute().equals(Attributes.ATTACK_DAMAGE));

        if (!(this.wasFullPower || hasDamageModifiers)) return;

        final double damage = hasDamageModifiers
                ? component.modifiers().stream()
                        .filter(e -> e.attribute().equals(Attributes.ATTACK_DAMAGE)
                                && e.modifier().operation() == AttributeModifier.Operation.ADD_VALUE)
                        .mapToDouble(e -> e.modifier().amount()).sum()
                : 2;

        final var entities = level.getEntities(EntityTypeTest.forClass(LivingEntity.class), this.getBoundingBox().inflate(0.15), e -> !e.isSpectator());
        if (entities.isEmpty()) return;

        final var target = entities.getFirst();
        final var damageSource = new ItemDamageSource((ItemEntity) (Object) this, this.getOwner());

        if (target.hurtTime != 0 || target.isInvulnerableTo(serverLevel, damageSource)) return;

        target.hurtServer(serverLevel, damageSource, (float) damage);

        var stack = this.getItem();
        if (stack.isDamageableItem()) {
            stack.setDamageValue(stack.getDamageValue() + 1);
            if (stack.getDamageValue() >= stack.getMaxDamage()) this.remove(Entity.RemovalReason.DISCARDED);
        }
    }

    @Override
    public float getPickRadius() {
        return .2f;
    }
}

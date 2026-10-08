package interactic.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import org.jetbrains.annotations.Nullable;

public class ItemDamageSource extends DamageSource {

    public ItemDamageSource(ItemEntity projectile, @Nullable Entity attacker) {
        super(projectile.level().damageSources().thrown(projectile, attacker).typeHolder(), projectile, attacker);
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity entity) {
        Component attackerName = this.getEntity() == null ? this.getDirectEntity().getDisplayName() : this.getEntity().getDisplayName();
        ItemStack itemStack = ((ItemEntity) this.getDirectEntity()).getItem();
        String key = "death.attack.thrown_item";
        if (itemStack.getItem() instanceof SwordItem) key = key + ".sword";
        if (itemStack.getItem() instanceof AxeItem) key = key + ".axe";
        if (itemStack.getItem() instanceof PickaxeItem) key = key + ".pickaxe";
        if (itemStack.getItem() instanceof ShovelItem) key = key + ".shovel";
        if (itemStack.getItem() instanceof HoeItem) key = key + ".hoe";
        return Component.translatable(key, entity.getDisplayName(), attackerName, itemStack.getDisplayName());
    }
}

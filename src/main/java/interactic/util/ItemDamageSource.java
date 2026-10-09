package interactic.util;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public class ItemDamageSource extends DamageSource {

    public ItemDamageSource(ItemEntity projectile, @Nullable Entity attacker) {
        super(projectile.level().damageSources().thrown(projectile, attacker).typeHolder(), projectile, attacker);
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity entity) {
        Component attackerName = this.getEntity() == null ? this.getDirectEntity().getDisplayName() : this.getEntity().getDisplayName();
        ItemStack itemStack = ((ItemEntity) this.getDirectEntity()).getItem();
        String key = "death.attack.thrown_item";
        if (itemStack.is(ItemTags.SWORDS)) key = key + ".sword";
        if (itemStack.is(ItemTags.AXES)) key = key + ".axe";
        if (itemStack.is(ItemTags.PICKAXES)) key = key + ".pickaxe";
        if (itemStack.is(ItemTags.SHOVELS)) key = key + ".shovel";
        if (itemStack.is(ItemTags.HOES)) key = key + ".hoe";
        return Component.translatable(key, entity.getDisplayName(), attackerName, itemStack.getDisplayName());
    }
}

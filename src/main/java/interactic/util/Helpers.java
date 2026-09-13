package interactic.util;

import interactic.InteracticInit;
import interactic.ItemFilterItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.stream.StreamSupport;

public class Helpers {

    public static ItemEntity raycastItem(Entity camera, double reach) {
        Vec3 normalizedFacing = camera.getViewVector(1.0F);
        Vec3 denormalizedFacing = camera.getEyePosition(0).add(normalizedFacing.x * reach, normalizedFacing.y * reach, normalizedFacing.z * reach);

        final EntityHitResult result = ProjectileUtil.getEntityHitResult(camera, camera.getEyePosition(0), denormalizedFacing,
                camera.getBoundingBox().expandTowards(normalizedFacing.scale(reach)).inflate(1), entity -> entity instanceof ItemEntity, reach * reach);

        if (result != null) {
            var distance = camera.position().distanceTo(result.getLocation()) - .3;
            if (camera.pick(distance, 1f, false) instanceof BlockHitResult blockResult) {
                if (!camera.level().getBlockState(blockResult.getBlockPos()).getCollisionShape(camera.level(), blockResult.getBlockPos()).isEmpty()) {
                    return null;
                }
            }
        }

        if (result == null) return null;
        return result.getEntity() instanceof ItemEntity itemEntity ? itemEntity : null;
    }

    public static boolean canPlayerPickUpItem(Player player, ItemEntity item) {
        if (player.isShiftKeyDown()) return true;

        if (!InteracticInit.getConfig().autoPickup()) {
            return item.entityTags().contains("interactic.ignore_auto_pickup_rule");
        }
        if (!InteracticInit.getConfig().itemFilterEnabled()) return true;

        var filterOptional = StreamSupport.stream(player.getInventory().spliterator(), false).filter(itemStack -> itemStack.is(InteracticInit.getItemFilter())).findFirst();
        if (filterOptional.isEmpty()) return true;

        final ItemStack filterStack = filterOptional.get();
        var filterData = filterStack.get(DataComponents.CUSTOM_DATA);
        if (filterData == null) return true;

        var filterNbt = filterData.copyTag();
        if (!filterNbt.getBooleanOr("Enabled", false)) return true;

        return filterNbt.getBooleanOr("BlockMode", false) != ItemFilterItem.getItemsInFilter(filterStack).contains(item.getItem().getItem());
    }

}

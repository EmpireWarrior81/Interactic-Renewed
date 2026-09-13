package interactic.util;

import com.mojang.logging.LogUtils;
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
import org.slf4j.Logger;

import java.util.stream.StreamSupport;
import java.util.concurrent.atomic.AtomicInteger;

public class Helpers {

    private static final Logger INTERACTIC_LOGGER = LogUtils.getLogger();
    private static final AtomicInteger INTERACTIC_DEBUG_LOG_COUNT = new AtomicInteger(0);

    public static ItemEntity raycastItem(Entity camera, double reach) {
        return raycastItem(camera, reach, "unspecified");
    }

    public static ItemEntity raycastItem(Entity camera, double reach, String source) {
        boolean debug = INTERACTIC_DEBUG_LOG_COUNT.get() < 300;

        Vec3 normalizedFacing = camera.getViewVector(1.0F);
        Vec3 eyePos = camera.getEyePosition(0);
        Vec3 denormalizedFacing = eyePos.add(normalizedFacing.x * reach, normalizedFacing.y * reach, normalizedFacing.z * reach);
        var searchBox = camera.getBoundingBox().expandTowards(normalizedFacing.scale(reach)).inflate(1);

        final EntityHitResult result = ProjectileUtil.getEntityHitResult(camera, eyePos, denormalizedFacing,
                searchBox, entity -> entity instanceof ItemEntity, reach * reach);

        if (debug) {
            INTERACTIC_DEBUG_LOG_COUNT.incrementAndGet();
            INTERACTIC_LOGGER.info(
                "[interactic-raycast-debug] source={} camera={} cameraClass={} reach={} eyePos={} to={} box={} primaryResult={}",
                source, camera, camera.getClass().getSimpleName(), reach, eyePos, denormalizedFacing, searchBox, result
            );
        }

        if (result != null) {
            var distance = camera.position().distanceTo(result.getLocation()) - .3;
            if (camera.pick(distance, 1f, false) instanceof BlockHitResult blockResult) {
                if (!camera.level().getBlockState(blockResult.getBlockPos()).getCollisionShape(camera.level(), blockResult.getBlockPos()).isEmpty()) {
                    if (debug) INTERACTIC_LOGGER.info("[interactic-raycast-debug] source={} rejected: block occlusion at {}", source, blockResult.getBlockPos());
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

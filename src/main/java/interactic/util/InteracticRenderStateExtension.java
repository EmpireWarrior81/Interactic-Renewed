package interactic.util;

import net.minecraft.world.entity.item.ItemEntity;

public interface InteracticRenderStateExtension {
    ItemEntity getInteracticEntity();

    void setInteracticEntity(ItemEntity entity);
}

package interactic.mixin;

import interactic.util.InteracticRenderStateExtension;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

// Carries the source ItemEntity from extraction (ItemEntityRendererMixin#extractRenderState,
// which has the live entity) through to submission (ItemEntityRendererMixin#submit, which
// only receives this render-state object) - the "extraction/submission" split introduced by
// the modern rendering pipeline means the entity itself is no longer available at draw time
// unless something carries it forward.
@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements InteracticRenderStateExtension {

    @Unique
    private ItemEntity interactic$entity;

    @Override
    public ItemEntity getInteracticEntity() {
        return interactic$entity;
    }

    @Override
    public void setInteracticEntity(ItemEntity entity) {
        this.interactic$entity = entity;
    }
}

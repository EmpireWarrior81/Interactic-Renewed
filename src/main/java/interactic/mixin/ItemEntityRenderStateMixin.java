package interactic.mixin;

import interactic.util.InteracticRenderStateExtensions;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements InteracticRenderStateExtensions {

    @Unique private float interactic_rotation = -1f;
    @Unique private ItemStack interactic_stack = ItemStack.EMPTY;
    @Unique private float interactic_yaw = 0f;
    @Unique private boolean interactic_treatAsDepthModel = false;
    @Unique private double interactic_blockHeight = 0.0;

    @Override public float interactic_getRotation() { return interactic_rotation; }
    @Override public void interactic_setRotation(float r) { this.interactic_rotation = r; }
    @Override public ItemStack interactic_getStack() { return interactic_stack; }
    @Override public void interactic_setStack(ItemStack s) { this.interactic_stack = s; }
    @Override public float interactic_getYaw() { return interactic_yaw; }
    @Override public void interactic_setYaw(float y) { this.interactic_yaw = y; }
    @Override public boolean interactic_isTreatAsDepthModel() { return interactic_treatAsDepthModel; }
    @Override public void interactic_setTreatAsDepthModel(boolean v) { this.interactic_treatAsDepthModel = v; }
    @Override public double interactic_getBlockHeight() { return interactic_blockHeight; }
    @Override public void interactic_setBlockHeight(double h) { this.interactic_blockHeight = h; }
}

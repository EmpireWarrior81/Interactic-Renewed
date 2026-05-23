package interactic.util;

import net.minecraft.item.ItemStack;

public interface InteracticRenderStateExtensions {
    float interactic_getRotation();
    void interactic_setRotation(float rotation);
    ItemStack interactic_getStack();
    void interactic_setStack(ItemStack stack);
    float interactic_getYaw();
    void interactic_setYaw(float yaw);
    boolean interactic_isTreatAsDepthModel();
    void interactic_setTreatAsDepthModel(boolean value);
    double interactic_getBlockHeight();
    void interactic_setBlockHeight(double height);
}

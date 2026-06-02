package interactic.mixin;

import interactic.InteracticInit;
import interactic.util.InteracticItemExtensions;
import interactic.util.InteracticRenderState;
import interactic.util.InteracticRenderStateExtensions;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin extends EntityRenderer<ItemEntity, ItemEntityRenderState> {

    @Unique private static final float TWO_PI = (float) (Math.PI * 2);
    @Unique private static final float HALF_PI = (float) (Math.PI * 0.5);
    @Unique private static final float THREE_HALF_PI = (float) (Math.PI * 1.5);

    @Shadow @Final private Random random;

    private ItemEntityRendererMixin(EntityRendererFactory.Context context) {
        super(context);
    }

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void captureEntityData(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        InteracticRenderStateExtensions ext = (InteracticRenderStateExtensions) state;

        ItemStack stack = entity.getStack();
        ext.interactic_setStack(stack);
        ext.interactic_setYaw(entity.getYaw());

        boolean treatAsDepthModel = stack.getItem() instanceof BlockItem;
        ext.interactic_setTreatAsDepthModel(treatAsDepthModel);

        double blockHeight = 0;
        if (treatAsDepthModel) {
            try {
                BlockState bs = ((BlockItem) stack.getItem()).getBlock().getDefaultState();
                var shape = bs.getOutlineShape(entity.getWorld(), entity.getBlockPos(), ShapeContext.absent());
                if (!shape.isEmpty()) blockHeight = shape.getMax(Direction.Axis.Y);
            } catch (Exception ignored) {}
        }
        ext.interactic_setBlockHeight(blockHeight);

        if (!InteracticInit.getConfig().fancyItemRendering()) return;

        // Read rotation from the entity so it persists across frames — render states are ephemeral.
        InteracticItemExtensions rotator = (InteracticItemExtensions) entity;
        float rotation = rotator.getRotation();

        int seed = stack.isEmpty() ? 187 : Registries.ITEM.getRawId(stack.getItem()) * entity.getId();
        this.random.setSeed(seed);

        if (rotation == -1f) {
            rotation = (this.random.nextInt(20) - 10) * 0.15f;
        }

        if (!entity.isOnGround()) {
            double velY = entity.getVelocity().y;
            float speedMult = entity.isSubmergedInWater() ? 0.25f : 1f;
            rotation += (float) (MathHelper.clamp(velY * 0.25, 0.075, 0.3) * speedMult
                    * (InteracticRenderState.frameDuration * 5)
                    * InteracticInit.getItemRotationSpeedMultiplier());
            if (rotation >= TWO_PI) rotation -= TWO_PI;
        } else {
            if (rotation != 0 && rotation != (float) Math.PI) {
                if (rotation > Math.PI) {
                    rotation += rotation > THREE_HALF_PI ? tickDelta * 0.5f : -(tickDelta * 0.5f);
                } else {
                    if (rotation > HALF_PI) {
                        rotation += tickDelta * 0.5f;
                        if (rotation > Math.PI) rotation = (float) Math.PI;
                    } else {
                        rotation -= tickDelta * 0.5f;
                    }
                }
                if (rotation < 0) rotation = 0;
                if (rotation > TWO_PI) rotation = 0;
            }
        }

        // Write back to the entity (persistence) and to the render state (for the render method).
        rotator.setRotation(rotation);
        ext.interactic_setRotation(rotation);
    }

    @Inject(at = @At("HEAD"),
            method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            cancellable = true)
    private void render(ItemEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!InteracticInit.getConfig().fancyItemRendering()) return;

        InteracticRenderStateExtensions ext = (InteracticRenderStateExtensions) state;
        ItemStack itemStack = ext.interactic_getStack();
        float angle = ext.interactic_getRotation();
        boolean treatAsDepthModel = ext.interactic_isTreatAsDepthModel();
        double blockHeight = ext.interactic_getBlockHeight();

        int seed = itemStack.isEmpty() ? 187 : Registries.ITEM.getRawId(itemStack.getItem()) * state.seed;
        this.random.setSeed(seed);

        final int renderCount = state.renderedAmount;

        // scaleZ drives positioning math; 0.5 matches the standard block ground scale, 0.25 matches items.
        final float scaleZ = treatAsDepthModel ? 0.5f : 0.25f;

        final double distanceToCenter = (0.5 - blockHeight + blockHeight / 2.0) * 0.25;
        final boolean isFlatBlock = treatAsDepthModel && blockHeight <= 0.75;

        // Shields and tridents are 3D models: +HALF_PI flips them face-down, -HALF_PI shows the
        // front face upward. Flat sprites need +HALF_PI to lie horizontal on the ground.
        final boolean invertRotation = itemStack.isOf(Items.SHIELD) || itemStack.isOf(Items.TRIDENT);

        matrices.push();

        matrices.translate(0, 0.125f, 0);

        if (treatAsDepthModel) matrices.translate(0, distanceToCenter, 0);

        float groundDistance = treatAsDepthModel ? (float) distanceToCenter : (float) (0.125 - 0.0625 * scaleZ);
        if (!treatAsDepthModel) groundDistance -= (renderCount - 1) * 0.05f * scaleZ;
        matrices.translate(0, -groundDistance, 0);

        matrices.translate(0, (random.nextDouble() - 0.5) * 0.005, 0);
        if (treatAsDepthModel && !isFlatBlock) matrices.translate(0, -0.1, 0);

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ext.interactic_getYaw()));

        float rotationOffset = isFlatBlock ? 0 : (invertRotation ? -HALF_PI : HALF_PI);
        matrices.multiply(RotationAxis.POSITIVE_X.rotation(angle + rotationOffset));

        if (treatAsDepthModel) matrices.translate(0, -distanceToCenter, 0);

        if (treatAsDepthModel && !isFlatBlock && !InteracticInit.getConfig().blocksLayFlat()) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.random.nextFloat() * 45));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.random.nextFloat() * 45));
        }

        if (treatAsDepthModel) matrices.translate(0, distanceToCenter, 0);

        matrices.translate(0, 0, ((0.09375f - (renderCount * 0.1f)) * 0.5f) * scaleZ);

        for (int i = 0; i < renderCount; i++) {
            matrices.push();

            if (i > 0) {
                if (treatAsDepthModel) {
                    float rx = (this.random.nextFloat() * 2f - 1f) * 0.1f;
                    float ry = (this.random.nextFloat() * 2f - 1f) * 0.1f;
                    float rz = (this.random.nextFloat() * 2f - 1f) * 0.1f;
                    matrices.translate(rx, ry, rz);
                } else {
                    matrices.translate(0, 0.125f, 0);
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((this.random.nextFloat() - 0.5f)));
                    matrices.translate(0, -0.125f, 0);
                }
            }

            state.itemRenderState.render(matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);

            matrices.pop();

            if (!treatAsDepthModel) {
                matrices.translate(0, 0, 0.1f * scaleZ);
            }
        }

        matrices.pop();

        this.shadowRadius = 0f;
        super.render(state, matrices, vertexConsumers, light);
        ci.cancel();
    }
}

package interactic.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import interactic.InteracticInit;
import interactic.util.InteracticItemExtensions;
import interactic.util.InteracticRenderStateExtension;
import interactic.util.InteracticRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// NOTE: this mixin was the highest-effort/highest-risk item in the 26.1 port, per its own
// plan (see reference/ or project notes). The old render(ItemEntity, float, float,
// MatrixStack, VertexConsumerProvider, int) method this mixin used to cancel-and-replace
// no longer exists at all - vanilla's rendering pipeline now splits "extraction" (reading
// live entity/world state, once per frame, into a plain state object) from "submission"
// (the actual PoseStack/draw calls, using only that state object - no live entity access).
// This port:
//   - stashes the live ItemEntity on the ItemEntityRenderState during extraction (see
//     ItemEntityRenderStateMixin / InteracticRenderStateExtension), so submit() can still
//     read entity.getYRot()/onGround()/getDeltaMovement()/isUnderWater() etc. the same way
//     the original code did.
//   - uses the already-resolved ItemStackRenderState (state.item) instead of manually
//     fetching a BakedModel; per-copy drawing goes through state.item.submit(...) instead of
//     ItemRenderer.renderItem(...).
//   - drops the old manual re-application of the model's ground-transform scale/rotation via
//     the PoseStack, since the resolved ItemStackRenderState is extracted with
//     ItemDisplayContext.GROUND already and appears to bake that transform in internally;
//     also drops the old BuiltinModelItemRenderer/isBuiltin() special-casing (the 1.21.1-era
//     trident/shield-invisible-on-ground workaround) since that API no longer exists and the
//     rewritten renderer may not have the same bug.
//   - approximates the old "is this a chunky block-like model" (treatAsDepthModel/
//     isFlatBlock) check using the resolved model's bounding box instead of querying the
//     block's outline shape directly, mirroring the threshold vanilla's own submit() uses
//     for its analogous flat-vs-3D-jitter item layout decision.
// This has NOT been visually verified against a running client - budget time to compare it
// side by side with the 1.21.1 branch's rendering before considering this port complete.
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin extends EntityRenderer<ItemEntity, ItemEntityRenderState> {

    @Unique private static final float TWO_PI = (float) (Math.PI * 2);
    @Unique private static final float HALF_PI = (float) (Math.PI * 0.5);
    @Unique private static final float THREE_HALF_PI = (float) (Math.PI * 1.5);
    @Unique private static final float DEPTH_THRESHOLD = 0.0625F;

    @Shadow
    @Final
    private RandomSource random;

    private ItemEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(at = @At("RETURN"), method = "<init>")
    private void onConstructor(EntityRendererProvider.Context context, CallbackInfo ci) {
        this.shadowRadius = 0;
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void stashEntity(ItemEntity entity, ItemEntityRenderState state, float partialTicks, CallbackInfo ci) {
        ((InteracticRenderStateExtension) state).setInteracticEntity(entity);
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void submit(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo callback) {
        if (!InteracticInit.getConfig().fancyItemRendering()) return;
        if (state.item.isEmpty()) return;

        ItemEntity entity = ((InteracticRenderStateExtension) state).getInteracticEntity();
        if (entity == null) return;

        // Custom seed distinct from vanilla's (item+damage only) so item entities of the
        // same type/stack don't all render identically.
        int seed = state.seed * (entity.getId() + 1);
        this.random.setSeed(seed);

        poseStack.pushPose();

        final int renderCount = state.count;
        InteracticItemExtensions rotator = (InteracticItemExtensions) entity;

        final AABB boundingBox = state.item.getModelBoundingBox();
        final boolean treatAsDepthModel = boundingBox.getZsize() > DEPTH_THRESHOLD;
        final float scaleZ = 1f;

        // Note: getModelBoundingBox() is measured AFTER the model's GROUND-context transform
        // is already applied (unlike the old block-outline-shape query this used to mirror),
        // so a hand-tuned block-height-based Y reposition on top of that double-applies an
        // offset - that was causing chunky/block items to render in the wrong place (and
        // sometimes get culled entirely) while flat items, which skip this branch, rendered
        // fine.
        //
        // Lifting by just -boundingBox.minY (the model's lowest point when unrotated) still
        // clips intermittently: the block spins around the X axis while falling and settling
        // (see the angle/rotation-snap logic below), and a lift computed for the *unrotated*
        // orientation only guarantees the bottom face clears the ground in that one
        // orientation - mid-spin, a different point of the model becomes the lowest one and
        // can dip below ground again. Instead, lift the model's *center* by the radius of its
        // Y/Z bounding circle - the maximum distance any point can be from the center after
        // an X-axis rotation, regardless of angle - which guarantees no point ever goes below
        // Y=0 at any rotation, not just the resting one.
        final boolean isFlatBlock = treatAsDepthModel && boundingBox.getYsize() <= 0.75;

        // Translate so that everything happens in the middle of the item hitbox
        poseStack.translate(0, 0.125f, 0);

        if (treatAsDepthModel) {
            double centerY = (boundingBox.minY + boundingBox.maxY) / 2.0;
            double halfY = boundingBox.getYsize() / 2.0;
            double halfZ = boundingBox.getZsize() / 2.0;
            double safeRadius = Math.sqrt(halfY * halfY + halfZ * halfZ);
            poseStack.translate(0, safeRadius - centerY, 0);
        }

        // Calculate ground distance from the amount of items rendered
        float groundDistance = (float) (0.125 - 0.0625 * scaleZ);
        groundDistance -= (renderCount - 1) * 0.05f * scaleZ;
        poseStack.translate(0, -groundDistance, 0);

        // Translate randomly to avoid Z-Fighting
        poseStack.translate(0, (random.nextDouble() - 0.5) * 0.005, 0);

        // Rotate the item by its yaw to get some randomness for the spinning axis
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));

        // Calculate rotation based on velocity or get the one the item had before it hit the ground
        if (rotator.getRotation() == -1) rotator.setRotation((random.nextInt(20) - 10) * 0.15f);
        float angle = entity.onGround() ? rotator.getRotation() : (float) (rotator.getRotation() + ((Mth.clamp(entity.getDeltaMovement().y * 0.25, 0.075, 0.3))) * (entity.isUnderWater() ? 0.25f : 1) * (InteracticRenderState.frameDuration * 5) * InteracticInit.getItemRotationSpeedMultiplier());

        // Make sure the angle never exceeds two pi
        if (angle >= TWO_PI) angle -= TWO_PI;

        // Clusterfuck our way back to either 0 or 180 degrees
        if (entity.onGround() && !(angle == 0 || angle == (float) Math.PI)) {
            if (angle > Math.PI) {
                if (angle > THREE_HALF_PI) angle += 0.5f;
                else {
                    angle -= 0.5f;
                }
            } else {
                if (angle > HALF_PI) {
                    angle += 0.5f;
                    if (angle > Math.PI) angle = (float) Math.PI;
                } else angle -= 0.5f;
            }

            if (angle < 0) angle = 0;
            if (angle > TWO_PI) angle = 0;
        }

        // Spin the item and store the value inside it should it hit the ground next tick
        poseStack.mulPose(Axis.XP.rotation(angle + (isFlatBlock ? 0 : HALF_PI)));
        rotator.setRotation(angle);

        // If the block is chonky, rotate it randomly
        if (treatAsDepthModel && !isFlatBlock && !InteracticInit.getConfig().blocksLayFlat()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(this.random.nextFloat() * 45));
            poseStack.mulPose(Axis.ZP.rotationDegrees(this.random.nextFloat() * 45));
        }

        // Translate so that the origin gets moved back for stacks with multiple items rendered
        poseStack.translate(0, 0, ((0.09375 - (renderCount * 0.1)) * 0.5) * scaleZ);

        float x;
        float y;

        for (int i = 0; i < renderCount; ++i) {

            // Only apply random transformation to the current item
            poseStack.pushPose();

            // Only apply transformations to items from the second one onward
            if (i > 0) {

                // Decide whether to use random rotation or positioning based on whether the
                // item has depth, which most of the time means that it's a block
                if (treatAsDepthModel) {
                    x = (this.random.nextFloat() * 2f - 1f) * .1f;
                    y = (this.random.nextFloat() * 2f - 1f) * .1f;
                    float z = (this.random.nextFloat() * 2f - 1f) * .1f;
                    poseStack.translate(x, y, z);
                } else {
                    poseStack.translate(0, 0.125f, 0.0D);
                    poseStack.mulPose(Axis.ZP.rotationDegrees((this.random.nextFloat() - 0.5f)));
                    poseStack.translate(0, -0.125f, 0.0D);
                }
            }

            state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);

            poseStack.popPose();

            // Translate normal items to create visual layering
            if (!treatAsDepthModel) {
                poseStack.translate(0, 0, 0.1F * scaleZ);
            }
        }

        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
        callback.cancel();
    }
}

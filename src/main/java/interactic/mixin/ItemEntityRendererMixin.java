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
        // is already applied (unlike the old block-outline-shape query this used to mirror).
        //
        // Diagnostic logging showed the real cause of the previous two attempts still
        // clipping: the model's own local origin (Y=0/Z=0, the point our rotation actually
        // pivots around, since we rotate BEFORE calling submit()) is NOT at the bounding
        // box's center - e.g. for minecraft:grass_block, bbox Y ran from 0.0625 to 0.3125,
        // meaning local Y=0 sits 0.0625 *below* the model entirely, not in the middle of it.
        //
        // isFlatBlock's old threshold (Ysize <= 0.75) was tuned for the old block-outline-shape
        // measurement, on a 0-1 "fraction of a full block" scale. getModelBoundingBox() uses a
        // different, much smaller scale entirely - a full cube like grass_block measured
        // Ysize=0.25 there, well under 0.75, so every block was misclassified as "flat" and
        // never got the chonky random-tumble treatment. Compare against the model's own
        // horizontal footprint instead of an absolute number, so it's independent of whatever
        // scale GROUND context happens to use: short relative to its footprint (a slab, a
        // carpet) reads as flat, roughly as tall as it is wide (a full block) reads as chonky.
        final boolean isFlatBlock = treatAsDepthModel
                && boundingBox.getYsize() <= Math.max(boundingBox.getXsize(), boundingBox.getZsize()) * 0.75;

        // Calculate rotation based on velocity or get the one the item had before it hit the ground.
        // Computed up front (rather than after positioning, like the original code did) so the
        // exact rotation angle is known before we compute how much to lift depth models by -
        // see below.
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
        rotator.setRotation(angle);
        final float rotationRad = angle + (isFlatBlock ? 0 : HALF_PI);

        // Translate so that everything happens in the middle of the item hitbox. Flat items
        // compensate for this afterward via their own groundDistance subtraction below; depth
        // models don't (they use the exact-lift calculation instead, which is already relative
        // to the model's own correctly-anchored bounding box), so applying it there left a
        // small uncompensated surplus on top of the lift - visible as a slight float even after
        // the lift itself became exact. Only applied for flat items now.
        if (!treatAsDepthModel) poseStack.translate(0, 0.125f, 0);

        if (treatAsDepthModel) {
            // A worst-case (any-angle) lift made blocks float, since it stays constant even at
            // the common flat/resting angle where much less lift is actually needed. Since we
            // now know the exact rotation angle being applied this frame, compute the exact
            // lift for that angle instead: evaluate where each of the model's 4 Y/Z corners
            // ends up after rotating by rotationRad around the X axis, and lift only enough to
            // clear the lowest one. (The rotation-matrix sign convention doesn't matter here -
            // evaluating both minZ and maxZ covers both possibilities either way.)
            double cos = Math.cos(rotationRad);
            double sin = Math.sin(rotationRad);
            double c1 = boundingBox.minY * cos - boundingBox.minZ * sin;
            double c2 = boundingBox.minY * cos - boundingBox.maxZ * sin;
            double c3 = boundingBox.maxY * cos - boundingBox.minZ * sin;
            double c4 = boundingBox.maxY * cos - boundingBox.maxZ * sin;
            double lowestY = Math.min(Math.min(c1, c2), Math.min(c3, c4));
            double lift = lowestY < 0 ? -lowestY : 0;
            poseStack.translate(0, lift, 0);
        } else {
            // Calculate ground distance from the amount of items rendered (flat items only)
            float groundDistance = (float) (0.125 - 0.0625 * scaleZ);
            groundDistance -= (renderCount - 1) * 0.05f * scaleZ;
            poseStack.translate(0, -groundDistance, 0);
        }

        // Translate randomly to avoid Z-Fighting
        poseStack.translate(0, (random.nextDouble() - 0.5) * 0.005, 0);

        // Rotate the item by its yaw to get some randomness for the spinning axis
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));

        // Spin the item (already computed above)
        poseStack.mulPose(Axis.XP.rotation(rotationRad));

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

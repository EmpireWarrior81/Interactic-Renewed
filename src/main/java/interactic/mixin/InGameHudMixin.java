package interactic.mixin;

import interactic.InteracticInit;
import interactic.util.Helpers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "renderCrosshair", at = @At("TAIL"))
    private void renderItemTooltip(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!InteracticInit.getConfig().renderItemTooltips()) return;

        final var client = MinecraftClient.getInstance();
        final var item = Helpers.raycastItem(client.getCameraEntity(), 5);

        if (item == null || client.world == null) return;

        List<Text> tooltip = InteracticInit.getConfig().renderFullTooltip()
                ? item.getStack().getTooltip(Item.TooltipContext.create(client.world), client.player, TooltipType.BASIC)
                : Collections.singletonList(item.getStack().getName());

        final int screenWidth = client.getWindow().getScaledWidth();
        final int screenHeight = client.getWindow().getScaledHeight();

        for (int i = 0, tooltipSize = tooltip.size(); i < tooltipSize; i++) {
            final var text = tooltip.get(i);
            context.drawText(client.textRenderer, text, screenWidth / 2 - client.textRenderer.getWidth(text) / 2, screenHeight / 2 + 15 + i * 10, 0xFFFFFF, true);
        }
    }
}

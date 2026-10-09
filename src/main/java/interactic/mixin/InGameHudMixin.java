package interactic.mixin;

import interactic.InteracticInit;
import interactic.util.Helpers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

@Mixin(Hud.class)
public class InGameHudMixin {

    @Inject(method = "extractCrosshair", at = @At("TAIL"))
    private void renderItemTooltip(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!InteracticInit.getConfig().renderItemTooltips()) return;

        final var client = Minecraft.getInstance();
        final var item = Helpers.raycastItem(client.getCameraEntity(), 5);

        if (item == null || client.level == null) return;

        List<Component> tooltip = InteracticInit.getConfig().renderFullTooltip()
                ? item.getItem().getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL)
                : Collections.singletonList(item.getItem().getHoverName());

        final int screenWidth = client.getWindow().getGuiScaledWidth();
        final int screenHeight = client.getWindow().getGuiScaledHeight();

        for (int i = 0, tooltipSize = tooltip.size(); i < tooltipSize; i++) {
            final var text = tooltip.get(i);
            // Note: GuiGraphicsExtractor.text() silently no-ops if the color's alpha byte is 0
            // (unlike the old DrawContext API, which treated missing alpha as fully opaque) -
            // 0xFFFFFF has alpha=0x00, so the text was being computed correctly every frame
            // and then discarded right at the final draw call. Needs an explicit alpha byte.
            graphics.text(client.font, text, screenWidth / 2 - client.font.width(text) / 2, screenHeight / 2 + 15 + i * 10, 0xFFFFFFFF, true);
        }
    }
}

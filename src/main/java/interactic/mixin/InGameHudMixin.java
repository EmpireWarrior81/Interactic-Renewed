package interactic.mixin;

import com.mojang.logging.LogUtils;
import interactic.InteracticInit;
import interactic.util.Helpers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(Gui.class)
public class InGameHudMixin {

    private static final Logger INTERACTIC_LOGGER = LogUtils.getLogger();
    private static final AtomicInteger INTERACTIC_DEBUG_LOG_COUNT = new AtomicInteger(0);

    @Inject(method = "extractCrosshair", at = @At("TAIL"))
    private void renderItemTooltip(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        boolean debug = InteracticInit.getConfig().renderItemTooltips() && INTERACTIC_DEBUG_LOG_COUNT.get() < 200;

        if (!InteracticInit.getConfig().renderItemTooltips()) return;

        final var client = Minecraft.getInstance();
        final var item = Helpers.raycastItem(client.getCameraEntity(), 5, "tooltip");

        if (debug) {
            INTERACTIC_DEBUG_LOG_COUNT.incrementAndGet();
            INTERACTIC_LOGGER.info("[interactic-tooltip-debug] extractCrosshair TAIL reached, item={}, level={}", item, client.level);
        }

        if (item == null || client.level == null) return;

        List<Component> tooltip = InteracticInit.getConfig().renderFullTooltip()
                ? item.getItem().getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL)
                : Collections.singletonList(item.getItem().getHoverName());

        final int screenWidth = client.getWindow().getGuiScaledWidth();
        final int screenHeight = client.getWindow().getGuiScaledHeight();

        if (debug) {
            INTERACTIC_LOGGER.info("[interactic-tooltip-debug] tooltip lines={}, screenW={}, screenH={}, font={}", tooltip, screenWidth, screenHeight, client.font);
        }

        for (int i = 0, tooltipSize = tooltip.size(); i < tooltipSize; i++) {
            final var text = tooltip.get(i);
            graphics.text(client.font, text, screenWidth / 2 - client.font.width(text) / 2, screenHeight / 2 + 15 + i * 10, 0xFFFFFF, true);
        }
    }
}

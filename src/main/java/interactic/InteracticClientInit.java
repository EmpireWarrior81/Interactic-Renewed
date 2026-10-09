package interactic;

import com.mojang.blaze3d.platform.InputConstants;
import empire.ewlib.config.ui.ConfigScreenProviders;
import interactic.network.PickupPayload;
import interactic.network.SetFilterModePayload;
import interactic.util.InteracticRenderState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = InteracticInit.MOD_ID, dist = Dist.CLIENT)
public class InteracticClientInit {

    public static final KeyMapping PICKUP_ITEM = new KeyMapping("key.interactic.pickup_item",
            InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC);

    public InteracticClientInit(IEventBus modEventBus) {
        modEventBus.addListener((RegisterKeyMappingsEvent event) -> event.register(PICKUP_ITEM));

        if (InteracticInit.getConfig().itemFilterEnabled()) {
            modEventBus.addListener((RegisterMenuScreensEvent event) -> event.register(InteracticInit.getItemFilterMenu(), ItemFilterScreen::new));
        }

        modEventBus.addListener((RegisterClientPayloadHandlersEvent event) -> event.register(SetFilterModePayload.TYPE, (payload, context) -> context.enqueueWork(() -> {
            if (!(Minecraft.getInstance().gui.screen() instanceof ItemFilterScreen screen)) return;
            screen.blockMode = payload.mode();
        })));

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            var client = Minecraft.getInstance();
            while (PICKUP_ITEM.consumeClick()) {
                ClientPacketDistributor.sendToServer(new PickupPayload());
                client.player.swing(InteractionHand.MAIN_HAND);
            }
        });

        ConfigScreenProviders.register("interactic", InteracticConfigScreen::new);

        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Pre event) -> {
            long now = Util.getMillis();
            if (InteracticRenderState.lastFrameMs != 0) {
                InteracticRenderState.frameDuration = Math.min((now - InteracticRenderState.lastFrameMs) / 50.0f, 0.5f);
            }
            InteracticRenderState.lastFrameMs = now;
        });
    }
}

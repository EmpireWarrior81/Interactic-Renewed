package interactic;

import com.mojang.blaze3d.platform.InputConstants;
import interactic.network.PickupPayload;
import interactic.network.SetFilterModePayload;
import interactic.util.InteracticRenderState;
import empire.ewlib.config.ui.ConfigScreen;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(value = InteracticInit.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = InteracticInit.MOD_ID, value = Dist.CLIENT)
public class InteracticClientInit {

    public static final KeyMapping PICKUP_ITEM = new KeyMapping("key.interactic.pickup_item",
            InputConstants.UNKNOWN.getValue(), "key.categories.misc");

    public InteracticClientInit() {
        ItemProperties.registerGeneric(ResourceLocation.fromNamespaceAndPath("interactic", "enabled"), (stack, level, entity, seed) -> {
            var data = stack.get(DataComponents.CUSTOM_DATA);
            return data != null && data.copyTag().getBoolean("Enabled") ? 1 : 0;
        });

        ConfigScreen.registerProvider("interactic", InteracticConfigScreen::new);

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            var client = Minecraft.getInstance();
            while (PICKUP_ITEM.consumeClick()) {
                PacketDistributor.sendToServer(new PickupPayload());
                client.player.swing(InteractionHand.MAIN_HAND);
            }
        });

        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Pre event) -> {
            long now = Util.getMillis();
            if (InteracticRenderState.lastFrameMs != 0) {
                InteracticRenderState.frameDuration = Math.min((now - InteracticRenderState.lastFrameMs) / 50.0f, 0.5f);
            }
            InteracticRenderState.lastFrameMs = now;
        });
    }

    @SubscribeEvent
    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PICKUP_ITEM);
    }

    @SubscribeEvent
    static void registerMenuScreens(RegisterMenuScreensEvent event) {
        if (!InteracticInit.getConfig().itemFilterEnabled()) return;
        event.register(InteracticInit.ITEM_FILTER_MENU.get(), ItemFilterScreen::new);
    }

    /**
     * Client side of {@link SetFilterModePayload}. The payload is registered in {@link InteracticInit},
     * since a dedicated server also has to know it in order to send it.
     */
    static void handleSetFilterMode(SetFilterModePayload payload) {
        if (!(Minecraft.getInstance().screen instanceof ItemFilterScreen screen)) return;
        screen.blockMode = payload.mode();
    }
}

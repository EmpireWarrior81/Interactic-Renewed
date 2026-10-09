package interactic;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import interactic.network.DropWithPowerPayload;
import interactic.network.FilterModeRequestPayload;
import interactic.network.PickupPayload;
import interactic.network.SetFilterModePayload;
import interactic.util.Helpers;
import interactic.util.InteracticConfig;
import interactic.util.InteracticPlayerExtension;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

@Mod(InteracticInit.MOD_ID)
public class InteracticInit {

    public static final String MOD_ID = "interactic";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MOD_ID);

    private static final InteracticConfig CONFIG = InteracticConfig.createAndLoad();

    public static final DeferredItem<ItemFilterItem> ITEM_FILTER = CONFIG.itemFilterEnabled()
            ? ITEMS.register("item_filter", ItemFilterItem::new)
            : null;

    public static final DeferredHolder<MenuType<?>, MenuType<ItemFilterScreenHandler>> ITEM_FILTER_MENU = MENU_TYPES.register("item_filter",
            () -> new MenuType<>(ItemFilterScreenHandler::new, FeatureFlags.DEFAULT_FLAGS));

    private static float itemRotationSpeedMultiplier = 1f;

    public InteracticInit(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::addCreative);

        NeoForge.EVENT_BUS.addListener(this::onItemPickupPre);

        if (ModList.get().isLoaded("iris")) itemRotationSpeedMultiplier = 0.5f;

        CONFIG.subscribeToClientOnlyMode(clientOnlyMode -> {
            if (!clientOnlyMode) return;
            CONFIG.itemsActAsProjectiles(false);
            CONFIG.itemThrowing(false);
            CONFIG.itemFilterEnabled(false);
            CONFIG.autoPickup(true);
            CONFIG.rightClickPickup(false);
        });

        enforceInClientOnlyMode(CONFIG::subscribeToItemsActAsProjectiles, CONFIG::itemsActAsProjectiles, false);
        enforceInClientOnlyMode(CONFIG::subscribeToItemThrowing, CONFIG::itemThrowing, false);
        enforceInClientOnlyMode(CONFIG::subscribeToItemFilterEnabled, CONFIG::itemFilterEnabled, false);
        enforceInClientOnlyMode(CONFIG::subscribeToAutoPickup, CONFIG::autoPickup, true);
        enforceInClientOnlyMode(CONFIG::subscribeToRightClickPickup, CONFIG::rightClickPickup, false);
    }

    private static void enforceInClientOnlyMode(Consumer<Consumer<Boolean>> eventSource, Consumer<Boolean> setter, boolean defaultValue) {
        eventSource.accept(value -> {
            if (!CONFIG.clientOnlyMode()) return;
            if (value != defaultValue) setter.accept(defaultValue);
        });
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        // Payload types are always registered so the negotiated protocol stays stable; the
        // config flags below only gate the resulting *behavior*, mirroring the Fabric receiver gating.
        registrar.playToServer(PickupPayload.TYPE, PickupPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (!CONFIG.rightClickPickup()) return;
            var player = (ServerPlayer) context.player();
            final var item = Helpers.raycastItem(player.getCamera(), 6);
            if (item == null) return;

            if (player.getInventory().add(item.getItem().copy())) {
                player.take(item, item.getItem().getCount());
                item.discard();
            }
        }));

        registrar.playToServer(DropWithPowerPayload.TYPE, DropWithPowerPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (!CONFIG.itemThrowing()) return;
            var player = (ServerPlayer) context.player();
            ((InteracticPlayerExtension) player).setDropPower(payload.power());
            dropSelected(player, payload.dropAll());
        }));

        registrar.playToServer(FilterModeRequestPayload.TYPE, FilterModeRequestPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (!CONFIG.itemFilterEnabled()) return;
            var player = (ServerPlayer) context.player();
            if (!(player.containerMenu instanceof ItemFilterScreenHandler filterHandler)) return;
            filterHandler.setFilterMode(payload.mode());
        }));

        // Registered here instead of in the client-only class, so dedicated servers know the
        // payload too (they send it). The handler only ever runs on the client.
        registrar.playToClient(SetFilterModePayload.TYPE, SetFilterModePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> InteracticClientInit.handleSetFilterMode(payload)));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (ITEM_FILTER != null && event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ITEM_FILTER);
        }
    }

    private void onItemPickupPre(ItemEntityPickupEvent.Pre event) {
        if (!Helpers.canPlayerPickUpItem(event.getPlayer(), event.getItemEntity())) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    private static void dropSelected(Player player, boolean dropAll) {
        player.drop(player.getInventory().removeItem(player.getInventory().selected,
                dropAll && !player.getInventory().getSelected().isEmpty() ? player.getInventory().getSelected().getCount() : 1), false, true);
    }

    public static InteracticConfig getConfig() {
        return CONFIG;
    }

    public static Item getItemFilter() {
        return ITEM_FILTER == null ? null : ITEM_FILTER.get();
    }

    public static float getItemRotationSpeedMultiplier() {
        return itemRotationSpeedMultiplier;
    }
}

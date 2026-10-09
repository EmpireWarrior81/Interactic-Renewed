package interactic;

import interactic.network.DropWithPowerPayload;
import interactic.network.FilterModeRequestPayload;
import interactic.network.PickupPayload;
import interactic.network.SetFilterModePayload;
import interactic.util.Helpers;
import interactic.util.InteracticConfig;
import interactic.util.InteracticPlayerExtension;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

@Mod(InteracticInit.MOD_ID)
public class InteracticInit {

    public static final String MOD_ID = "interactic";

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MOD_ID);

    private static final InteracticConfig CONFIG = InteracticConfig.createAndLoad();
    private static float itemRotationSpeedMultiplier = 1f;

    private static final DeferredItem<ItemFilterItem> ITEM_FILTER = CONFIG.itemFilterEnabled()
            ? ITEMS.register("item_filter", id -> new ItemFilterItem(ResourceKey.create(Registries.ITEM, id)))
            : null;

    private static final DeferredHolder<MenuType<?>, MenuType<ItemFilterScreenHandler>> ITEM_FILTER_MENU = MENU_TYPES.register("item_filter",
            () -> new MenuType<>(ItemFilterScreenHandler::new, FeatureFlags.VANILLA_SET));

    public InteracticInit(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::addToCreativeTab);

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

        if (ModList.get().isLoaded("iris")) itemRotationSpeedMultiplier = 0.5f;
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        // Payload types are always registered so the negotiated protocol stays stable;
        // the config flags only gate the behavior, mirroring the Fabric receiver gating.
        registrar.playToServer(PickupPayload.TYPE, PickupPayload.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (!CONFIG.rightClickPickup()) return;
            var player = (ServerPlayer) context.player();
            final var item = Helpers.raycastItem(player.getCamera(), 6);
            if (item == null) return;

            if (player.getInventory().add(item.getItem().copy())) {
                player.take(item, item.getItem().getCount());
                item.remove(Entity.RemovalReason.DISCARDED);
            }
        }));

        registrar.playToServer(DropWithPowerPayload.TYPE, DropWithPowerPayload.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (!CONFIG.itemThrowing()) return;
            var player = context.player();
            ((InteracticPlayerExtension) player).setDropPower(payload.power());
            dropSelected(player, payload.dropAll());
        }));

        registrar.playToServer(FilterModeRequestPayload.TYPE, FilterModeRequestPayload.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (!CONFIG.itemFilterEnabled()) return;
            if (!(context.player().containerMenu instanceof ItemFilterScreenHandler filterHandler)) return;
            filterHandler.setFilterMode(payload.mode());
        }));

        // the client handler is registered in InteracticClientInit
        registrar.playToClient(SetFilterModePayload.TYPE, SetFilterModePayload.CODEC);
    }

    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (ITEM_FILTER != null && event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ITEM_FILTER);
        }
    }

    public static Item getItemFilter() {
        return ITEM_FILTER == null ? null : ITEM_FILTER.get();
    }

    public static MenuType<ItemFilterScreenHandler> getItemFilterMenu() {
        return ITEM_FILTER_MENU.get();
    }

    private static void enforceInClientOnlyMode(Consumer<Consumer<Boolean>> eventSource, Consumer<Boolean> setter, boolean defaultValue) {
        eventSource.accept(value -> {
            if (!CONFIG.clientOnlyMode()) return;
            if (value != defaultValue) setter.accept(defaultValue);
        });
    }

    private static void dropSelected(Player player, boolean dropAll) {
        player.drop(player.getInventory().removeFromSelected(dropAll), true, Prediction.PREDICTED);
    }

    public static float getItemRotationSpeedMultiplier() {
        return itemRotationSpeedMultiplier;
    }

    public static InteracticConfig getConfig() {
        return CONFIG;
    }
}

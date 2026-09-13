package interactic;

import interactic.network.DropWithPowerPayload;
import interactic.network.FilterModeRequestPayload;
import interactic.network.PickupPayload;
import interactic.network.SetFilterModePayload;
import interactic.util.Helpers;
import interactic.util.InteracticConfig;
import interactic.util.InteracticPlayerExtension;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;

import java.util.function.Consumer;

public class InteracticInit implements ModInitializer {

    public static final String MOD_ID = "interactic";

    private static Item ITEM_FILTER = null;

    private static final InteracticConfig CONFIG = InteracticConfig.createAndLoad();
    private static float itemRotationSpeedMultiplier = 1f;

    public static final MenuType<ItemFilterScreenHandler> ITEM_FILTER_SCREEN_HANDLER =
            Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(MOD_ID, "item_filter"), new MenuType<>(ItemFilterScreenHandler::new, FeatureFlags.VANILLA_SET));

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(PickupPayload.TYPE, PickupPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DropWithPowerPayload.TYPE, DropWithPowerPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FilterModeRequestPayload.TYPE, FilterModeRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SetFilterModePayload.TYPE, SetFilterModePayload.CODEC);

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

        if (FabricLoader.getInstance().isModLoaded("iris")) itemRotationSpeedMultiplier = 0.5f;

        if (CONFIG.itemFilterEnabled()) {
            ResourceKey<Item> itemFilterKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "item_filter"));
            ITEM_FILTER = Registry.register(BuiltInRegistries.ITEM, itemFilterKey, new ItemFilterItem(itemFilterKey));

            ServerPlayNetworking.registerGlobalReceiver(FilterModeRequestPayload.TYPE, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    if (!(player.containerMenu instanceof ItemFilterScreenHandler filterHandler)) return;
                    filterHandler.setFilterMode(payload.mode());
                });
            });
        }

        if (CONFIG.rightClickPickup()) {
            ServerPlayNetworking.registerGlobalReceiver(PickupPayload.TYPE, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    final var item = Helpers.raycastItem(player.getCamera(), 6);
                    if (item == null) return;

                    if (player.getInventory().add(item.getItem().copy())) {
                        player.take(item, item.getItem().getCount());
                        item.remove(Entity.RemovalReason.DISCARDED);
                    }
                });
            });
        }

        if (CONFIG.itemThrowing()) {
            ServerPlayNetworking.registerGlobalReceiver(DropWithPowerPayload.TYPE, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    ((InteracticPlayerExtension) player).setDropPower(payload.power());
                    dropSelected(player, payload.dropAll());
                });
            });
        }
    }

    public static Item getItemFilter() {
        return ITEM_FILTER;
    }

    private static void enforceInClientOnlyMode(Consumer<Consumer<Boolean>> eventSource, Consumer<Boolean> setter, boolean defaultValue) {
        eventSource.accept(value -> {
            if (!CONFIG.clientOnlyMode()) return;
            if (value != defaultValue) setter.accept(defaultValue);
        });
    }

    private void dropSelected(Player player, boolean dropAll) {
        player.drop(player.getInventory().removeFromSelected(dropAll), true);
    }

    public static float getItemRotationSpeedMultiplier() {
        return itemRotationSpeedMultiplier;
    }

    public static InteracticConfig getConfig() {
        return CONFIG;
    }
}

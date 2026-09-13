package interactic;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class ItemFilterItem extends Item {

    static {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
            var filter = InteracticInit.getItemFilter();
            if (filter != null) output.accept(filter);
        });
    }

    public ItemFilterItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        final var playerStack = user.getItemInHand(hand);
        if (user.isShiftKeyDown()) {
            var nbt = playerStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            var enabled = nbt.getBooleanOr("Enabled", false);
            nbt.putBoolean("Enabled", !enabled);
            playerStack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        } else {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            final var inv = new FilterInventory(playerStack);
            final var factory = new MenuProvider() {
                @Override
                public @Nullable AbstractContainerMenu createMenu(int syncId, Inventory playerInv, Player player) {
                    return new ItemFilterScreenHandler(syncId, playerInv, inv);
                }

                @Override
                public Component getDisplayName() {
                    return getName(playerStack);
                }
            };
            user.openMenu(factory);

            var handler = (ItemFilterScreenHandler) user.containerMenu;
            handler.setFilterMode(inv.getFilterMode());
        }
        return InteractionResult.SUCCESS;
    }

    public static List<Item> getItemsInFilter(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return List.of();
        final var invTag = data.copyTag().getListOrEmpty("Items");

        return invTag.stream()
                .map(s -> BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(((CompoundTag) s).getStringOr("id", ""))).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    public static class FilterInventory implements Container {

        public final ItemStack filter;
        private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

        public FilterInventory(ItemStack filter) {
            this.filter = filter;
            readItems();
        }

        private void readItems() {
            var data = filter.get(DataComponents.CUSTOM_DATA);
            if (data == null) return;
            var invTag = data.copyTag().getListOrEmpty("Items");
            for (int i = 0; i < invTag.size(); i++) {
                var compound = (CompoundTag) invTag.get(i);
                int slot = compound.getByteOr("Slot", (byte) 0) & 0xFF;
                if (slot < items.size()) {
                    var id = Identifier.tryParse(compound.getStringOr("id", ""));
                    if (id != null) {
                        BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> items.set(slot, new ItemStack(item)));
                    }
                }
            }
        }

        public void setFilterMode(boolean mode) {
            var nbt = filter.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            nbt.putBoolean("BlockMode", mode);
            filter.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }

        public boolean getFilterMode() {
            var data = filter.get(DataComponents.CUSTOM_DATA);
            return data != null && data.copyTag().getBooleanOr("BlockMode", false);
        }

        @Override
        public int getContainerSize() {
            return 9;
        }

        @Override
        public boolean isEmpty() {
            return items.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return items.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            var result = items.get(slot).copy();
            if (amount >= result.getCount()) {
                items.set(slot, ItemStack.EMPTY);
            } else {
                result.setCount(amount);
                items.get(slot).shrink(amount);
            }
            if (!result.isEmpty()) setChanged();
            return result;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            var stack = items.get(slot).copy();
            items.set(slot, ItemStack.EMPTY);
            setChanged();
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            items.set(slot, stack);
        }

        @Override
        public void setChanged() {
            var nbt = filter.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            var itemsTag = new ListTag();
            for (int i = 0; i < items.size(); i++) {
                var stack = items.get(i);
                if (!stack.isEmpty()) {
                    var compound = new CompoundTag();
                    compound.putByte("Slot", (byte) i);
                    compound.putString("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                    itemsTag.add(compound);
                }
            }
            nbt.put("Items", itemsTag);
            filter.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }

        @Override
        public boolean stillValid(Player player) {
            return player.getInventory().contains(filter);
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < items.size(); i++) {
                items.set(i, ItemStack.EMPTY);
            }
        }
    }
}

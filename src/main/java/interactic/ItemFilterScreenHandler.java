package interactic;

import interactic.network.SetFilterModePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ItemFilterScreenHandler extends AbstractContainerMenu {

    public static final int SLOT_COUNT = 9;
    private final Container inventory;
    private final Player player;

    public ItemFilterScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(SLOT_COUNT));
    }

    public ItemFilterScreenHandler(int syncId, Inventory playerInventory, Container inventory) {
        super(InteracticInit.ITEM_FILTER_SCREEN_HANDLER, syncId);
        this.inventory = inventory;
        checkContainerSize(inventory, SLOT_COUNT);

        this.player = playerInventory.player;
        inventory.startOpen(player);

        int m;
        for (m = 0; m < SLOT_COUNT; ++m) {
            this.addSlot(new GhostSlot(inventory, m, 8 + m * 18, 20));
        }

        for (m = 0; m < 3; ++m) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + m * 9 + 9, 8 + l * 18, m * 18 + 60));
            }
        }

        for (m = 0; m < 9; ++m) {
            this.addSlot(new Slot(playerInventory, m, 8 + m * 18, 118));
        }
    }

    public void setFilterMode(boolean mode) {
        if (!(inventory instanceof ItemFilterItem.FilterInventory filterInventory)) return;
        filterInventory.setFilterMode(mode);
        ServerPlayNetworking.send((ServerPlayer) player, new SetFilterModePayload(mode));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (index < this.inventory.getContainerSize()) {
                if (!this.moveItemStackTo(itemStack2, this.inventory.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 0, this.inventory.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.inventory.stopOpen(player);
    }

    private static class GhostSlot extends Slot {

        public GhostSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            this.set(new ItemStack(stack.getItem()));
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            this.set(ItemStack.EMPTY);
            return false;
        }
    }
}

package interactic;

import interactic.network.FilterModeRequestPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class ItemFilterScreen extends AbstractContainerScreen<ItemFilterScreenHandler> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(InteracticInit.MOD_ID, "textures/gui/item_filter.png");

    public boolean blockMode = true;

    private Button blockButton = null;
    private Button allowButton = null;

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        this.blockButton = this.addRenderableWidget(Button.builder(Component.literal("Block"), button -> sendModeRequest(true))
                .bounds(i + 43, j + 42, 60, 12)
                .build());

        this.allowButton = this.addRenderableWidget(Button.builder(Component.literal("Allow"), button -> sendModeRequest(false))
                .bounds(i + 108, j + 42, 60, 12)
                .build());
    }

    private static void sendModeRequest(boolean mode) {
        PacketDistributor.sendToServer(new FilterModeRequestPayload(mode));
    }

    public ItemFilterScreen(ItemFilterScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageHeight = 142;
        this.inventoryLabelY = 69420;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        context.drawString(this.font, "Mode", this.leftPos + 8, this.topPos + 44, 0x404040, false);

        this.renderTooltip(context, mouseX, mouseY);

        this.blockButton.active = !this.blockMode;
        this.allowButton.active = this.blockMode;
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        context.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (!blockMode) {
            context.blit(TEXTURE, this.leftPos + 7, this.topPos + 19, 0, 142, 162, 18);
        }
    }
}

package interactic;

import interactic.network.FilterModeRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ItemFilterScreen extends AbstractContainerScreen<ItemFilterScreenHandler> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(InteracticInit.MOD_ID, "textures/gui/item_filter.png");

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
        ClientPlayNetworking.send(new FilterModeRequestPayload(mode));
    }

    public ItemFilterScreen(ItemFilterScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 142);
        this.inventoryLabelY = 69420;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        graphics.text(this.font, "Mode", this.leftPos + 8, this.topPos + 44, 0x404040, false);

        this.blockButton.active = !this.blockMode;
        this.allowButton.active = this.blockMode;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        if (!blockMode) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 7, this.topPos + 19, 0, 142, 162, 18, 256, 256);
        }
    }
}

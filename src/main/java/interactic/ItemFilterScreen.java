package interactic;

import interactic.network.FilterModeRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ItemFilterScreen extends HandledScreen<ItemFilterScreenHandler> {

    private static final Identifier TEXTURE = Identifier.of(InteracticInit.MOD_ID, "textures/gui/item_filter.png");

    public boolean blockMode = true;

    private ButtonWidget blockButton = null;
    private ButtonWidget allowButton = null;

    @Override
    protected void init() {
        super.init();
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;

        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;

        this.addDrawableChild(this.blockButton = ButtonWidget.builder(Text.of("Block"), button -> sendModeRequest(true))
                .dimensions(i + 43, j + 42, 60, 12)
                .build());

        this.addDrawableChild(this.allowButton = ButtonWidget.builder(Text.of("Allow"), button -> sendModeRequest(false))
                .dimensions(i + 108, j + 42, 60, 12)
                .build());
    }

    private static void sendModeRequest(boolean mode) {
        ClientPlayNetworking.send(new FilterModeRequestPayload(mode));
    }

    public ItemFilterScreen(ItemFilterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 142;
        this.playerInventoryTitleY = 69420;
    }

    @SuppressWarnings({"ConstantConditions"})
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        context.drawText(this.client.textRenderer, "Mode", this.x + 8, this.y + 44, 0x404040, false);

        this.drawMouseoverTooltip(context, mouseX, mouseY);

        this.blockButton.active = !this.blockMode;
        this.allowButton.active = this.blockMode;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);

        if (!blockMode) {
            context.drawTexture(TEXTURE, this.x + 7, this.y + 19, 0, 142, 162, 18);
        }
    }
}

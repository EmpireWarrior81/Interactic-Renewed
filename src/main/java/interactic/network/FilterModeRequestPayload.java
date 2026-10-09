package interactic.network;

import interactic.InteracticInit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FilterModeRequestPayload(boolean mode) implements CustomPacketPayload {
    public static final Type<FilterModeRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(InteracticInit.MOD_ID, "filter_mode_request"));
    public static final StreamCodec<FriendlyByteBuf, FilterModeRequestPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FilterModeRequestPayload::mode,
            FilterModeRequestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

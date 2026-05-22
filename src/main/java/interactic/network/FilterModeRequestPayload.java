package interactic.network;

import interactic.InteracticInit;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FilterModeRequestPayload(boolean mode) implements CustomPayload {
    public static final Id<FilterModeRequestPayload> ID = new Id<>(Identifier.of(InteracticInit.MOD_ID, "filter_mode_request"));
    public static final PacketCodec<PacketByteBuf, FilterModeRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, FilterModeRequestPayload::mode,
            FilterModeRequestPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

package interactic.network;

import interactic.InteracticInit;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SetFilterModePayload(boolean mode) implements CustomPayload {
    public static final Id<SetFilterModePayload> ID = new Id<>(Identifier.of(InteracticInit.MOD_ID, "set_filter_mode"));
    public static final PacketCodec<PacketByteBuf, SetFilterModePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, SetFilterModePayload::mode,
            SetFilterModePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

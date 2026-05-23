package interactic.network;

import interactic.InteracticInit;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DropWithPowerPayload(float power, boolean dropAll) implements CustomPayload {
    public static final Id<DropWithPowerPayload> ID = new Id<>(Identifier.of(InteracticInit.MOD_ID, "drop_with_power"));
    public static final PacketCodec<PacketByteBuf, DropWithPowerPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.FLOAT, DropWithPowerPayload::power,
            PacketCodecs.BOOLEAN, DropWithPowerPayload::dropAll,
            DropWithPowerPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

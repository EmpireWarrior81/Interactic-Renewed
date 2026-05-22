package interactic.network;

import interactic.InteracticInit;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PickupPayload() implements CustomPayload {
    public static final Id<PickupPayload> ID = new Id<>(Identifier.of(InteracticInit.MOD_ID, "pickup"));
    public static final PacketCodec<PacketByteBuf, PickupPayload> CODEC = PacketCodec.unit(new PickupPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

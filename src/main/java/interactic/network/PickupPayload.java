package interactic.network;

import interactic.InteracticInit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PickupPayload() implements CustomPacketPayload {
    public static final Type<PickupPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(InteracticInit.MOD_ID, "pickup"));
    public static final StreamCodec<FriendlyByteBuf, PickupPayload> CODEC = StreamCodec.unit(new PickupPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

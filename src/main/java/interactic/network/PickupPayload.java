package interactic.network;

import interactic.InteracticInit;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PickupPayload() implements CustomPacketPayload {
    public static final Type<PickupPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(InteracticInit.MOD_ID, "pickup"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PickupPayload> STREAM_CODEC = StreamCodec.unit(new PickupPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

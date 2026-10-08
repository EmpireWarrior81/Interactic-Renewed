package interactic.network;

import interactic.InteracticInit;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DropWithPowerPayload(float power, boolean dropAll) implements CustomPacketPayload {
    public static final Type<DropWithPowerPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(InteracticInit.MOD_ID, "drop_with_power"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DropWithPowerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, DropWithPowerPayload::power,
            ByteBufCodecs.BOOL, DropWithPowerPayload::dropAll,
            DropWithPowerPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

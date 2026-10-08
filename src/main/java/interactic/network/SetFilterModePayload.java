package interactic.network;

import interactic.InteracticInit;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetFilterModePayload(boolean mode) implements CustomPacketPayload {
    public static final Type<SetFilterModePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(InteracticInit.MOD_ID, "set_filter_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetFilterModePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SetFilterModePayload::mode,
            SetFilterModePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

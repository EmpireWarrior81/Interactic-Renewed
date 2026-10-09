package interactic.network;

import interactic.InteracticInit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DropWithPowerPayload(float power, boolean dropAll) implements CustomPacketPayload {
    public static final Type<DropWithPowerPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(InteracticInit.MOD_ID, "drop_with_power"));
    public static final StreamCodec<FriendlyByteBuf, DropWithPowerPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, DropWithPowerPayload::power,
            ByteBufCodecs.BOOL, DropWithPowerPayload::dropAll,
            DropWithPowerPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

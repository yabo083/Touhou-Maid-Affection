package net.minecraft.network.codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;

public final class ByteBufCodecs {
    public static final StreamCodec<ByteBuf, Integer> INT = StreamCodec.of(ByteBuf::writeInt, ByteBuf::readInt);
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of(ByteBufCodecs::writeVarInt, ByteBufCodecs::readVarInt);
    public static final StreamCodec<ByteBuf, Long> VAR_LONG = StreamCodec.of(ByteBufCodecs::writeVarLong, ByteBufCodecs::readVarLong);
    public static final StreamCodec<ByteBuf, Boolean> BOOL = StreamCodec.of(ByteBuf::writeBoolean, ByteBuf::readBoolean);
    public static final StreamCodec<ByteBuf, Double> DOUBLE = StreamCodec.of(ByteBuf::writeDouble, ByteBuf::readDouble);
    public static final StreamCodec<ByteBuf, Float> FLOAT = StreamCodec.of(ByteBuf::writeFloat, ByteBuf::readFloat);
    public static final StreamCodec<ByteBuf, String> STRING_UTF8 = StreamCodec.of(ByteBufCodecs::writeUtf, ByteBufCodecs::readUtf);
    public static final StreamCodec<ByteBuf, byte[]> BYTE_ARRAY = StreamCodec.of(ByteBufCodecs::writeByteArray, ByteBufCodecs::readByteArray);
    public static final StreamCodec<ByteBuf, UUID> UUID = StreamCodec.of(ByteBufCodecs::writeUuid, ByteBufCodecs::readUuid);

    private ByteBufCodecs() {
    }

    public static <C extends Collection<T>, T> StreamCodec<ByteBuf, C> collection(
            Supplier<C> supplier,
            StreamCodec<ByteBuf, T> elementCodec
    ) {
        return StreamCodec.of(
                (buf, collection) -> {
                    writeVarInt(buf, collection.size());
                    for (T value : collection) {
                        elementCodec.encode(buf, value);
                    }
                },
                buf -> {
                    int size = readVarInt(buf);
                    C values = supplier.get();
                    for (int i = 0; i < size; i++) {
                        values.add(elementCodec.decode(buf));
                    }
                    return values;
                }
        );
    }

    private static void writeUtf(ByteBuf buf, String value) {
        asFriendly(buf).writeUtf(value == null ? "" : value);
    }

    private static String readUtf(ByteBuf buf) {
        return asFriendly(buf).readUtf();
    }

    private static void writeVarInt(ByteBuf buf, int value) {
        asFriendly(buf).writeVarInt(value);
    }

    private static int readVarInt(ByteBuf buf) {
        return asFriendly(buf).readVarInt();
    }

    private static void writeVarLong(ByteBuf buf, long value) {
        asFriendly(buf).writeVarLong(value);
    }

    private static long readVarLong(ByteBuf buf) {
        return asFriendly(buf).readVarLong();
    }

    private static void writeByteArray(ByteBuf buf, byte[] bytes) {
        asFriendly(buf).writeByteArray(bytes == null ? new byte[0] : bytes);
    }

    private static byte[] readByteArray(ByteBuf buf) {
        return asFriendly(buf).readByteArray();
    }

    private static void writeUuid(ByteBuf buf, UUID uuid) {
        asFriendly(buf).writeUUID(uuid);
    }

    private static UUID readUuid(ByteBuf buf) {
        return asFriendly(buf).readUUID();
    }

    private static FriendlyByteBuf asFriendly(ByteBuf buf) {
        if (buf instanceof FriendlyByteBuf friendlyByteBuf) {
            return friendlyByteBuf;
        }
        return new FriendlyByteBuf(buf);
    }
}

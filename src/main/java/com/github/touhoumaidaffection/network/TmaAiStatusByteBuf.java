package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.bond.settings.TmaAiStatusWire;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;

/**
 * Adapts netty {@link ByteBuf} to the transport-agnostic {@link TmaAiStatusWire} codec so the status
 * request/status/clear payloads share one implementation.
 */
public final class TmaAiStatusByteBuf {
    private TmaAiStatusByteBuf() {
    }

    public static TmaAiStatusWire.Sink sink(ByteBuf buf) {
        return new ByteBufSink(buf);
    }

    public static TmaAiStatusWire.Source source(ByteBuf buf) {
        return new ByteBufSource(buf);
    }

    private static final class ByteBufSink implements TmaAiStatusWire.Sink {
        private final ByteBuf buf;

        private ByteBufSink(ByteBuf buf) {
            this.buf = buf;
        }

        @Override
        public void writeInt(int value) {
            ByteBufCodecs.VAR_INT.encode(buf, value);
        }

        @Override
        public void writeLong(long value) {
            ByteBufCodecs.VAR_LONG.encode(buf, value);
        }

        @Override
        public void writeBoolean(boolean value) {
            ByteBufCodecs.BOOL.encode(buf, value);
        }

        @Override
        public void writeString(String value) {
            ByteBufCodecs.STRING_UTF8.encode(buf, value);
        }
    }

    private static final class ByteBufSource implements TmaAiStatusWire.Source {
        private final ByteBuf buf;

        private ByteBufSource(ByteBuf buf) {
            this.buf = buf;
        }

        @Override
        public int readInt() {
            return ByteBufCodecs.VAR_INT.decode(buf);
        }

        @Override
        public long readLong() {
            return ByteBufCodecs.VAR_LONG.decode(buf);
        }

        @Override
        public boolean readBoolean() {
            return ByteBufCodecs.BOOL.decode(buf);
        }

        @Override
        public String readString() {
            return ByteBufCodecs.STRING_UTF8.decode(buf);
        }
    }
}
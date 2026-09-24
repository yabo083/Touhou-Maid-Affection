package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.bond.settings.TmaSettingsWire;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;

/**
 * Adapts netty {@link ByteBuf} to the transport-agnostic {@link TmaSettingsWire} codec so both
 * settings payloads share one entry-list implementation.
 */
public final class TmaSettingsByteBuf {
    private TmaSettingsByteBuf() {
    }

    public static TmaSettingsWire.Sink sink(ByteBuf buf) {
        return new ByteBufSink(buf);
    }

    public static TmaSettingsWire.Source source(ByteBuf buf) {
        return new ByteBufSource(buf);
    }

    private static final class ByteBufSink implements TmaSettingsWire.Sink {
        private final ByteBuf buf;

        private ByteBufSink(ByteBuf buf) {
            this.buf = buf;
        }

        @Override
        public void writeInt(int value) {
            ByteBufCodecs.VAR_INT.encode(buf, value);
        }

        @Override
        public void writeString(String value) {
            ByteBufCodecs.STRING_UTF8.encode(buf, value);
        }
    }

    private static final class ByteBufSource implements TmaSettingsWire.Source {
        private final ByteBuf buf;

        private ByteBufSource(ByteBuf buf) {
            this.buf = buf;
        }

        @Override
        public int readInt() {
            return ByteBufCodecs.VAR_INT.decode(buf);
        }

        @Override
        public String readString() {
            return ByteBufCodecs.STRING_UTF8.decode(buf);
        }
    }
}
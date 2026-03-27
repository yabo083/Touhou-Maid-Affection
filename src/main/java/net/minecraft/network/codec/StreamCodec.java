package net.minecraft.network.codec;

import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface StreamCodec<B, T> {
    void encode(B buffer, T value);

    T decode(B buffer);

    static <B, T> StreamCodec<B, T> of(BiConsumer<B, T> encoder, Function<B, T> decoder) {
        return new StreamCodec<>() {
            @Override
            public void encode(B buffer, T value) {
                encoder.accept(buffer, value);
            }

            @Override
            public T decode(B buffer) {
                return decoder.apply(buffer);
            }
        };
    }

    static <B, T1, R> StreamCodec<B, R> composite(
            StreamCodec<B, T1> codec1,
            Function<R, T1> getter1,
            Function<T1, R> constructor
    ) {
        return of(
                (buf, value) -> codec1.encode(buf, getter1.apply(value)),
                buf -> constructor.apply(codec1.decode(buf))
        );
    }

    static <B, T1, T2, R> StreamCodec<B, R> composite(
            StreamCodec<B, T1> codec1,
            Function<R, T1> getter1,
            StreamCodec<B, T2> codec2,
            Function<R, T2> getter2,
            BiFunction<T1, T2, R> constructor
    ) {
        return of(
                (buf, value) -> {
                    codec1.encode(buf, getter1.apply(value));
                    codec2.encode(buf, getter2.apply(value));
                },
                buf -> constructor.apply(codec1.decode(buf), codec2.decode(buf))
        );
    }

    static <B, T1, T2, T3, R> StreamCodec<B, R> composite(
            StreamCodec<B, T1> codec1,
            Function<R, T1> getter1,
            StreamCodec<B, T2> codec2,
            Function<R, T2> getter2,
            StreamCodec<B, T3> codec3,
            Function<R, T3> getter3,
            TriFunction<T1, T2, T3, R> constructor
    ) {
        return of(
                (buf, value) -> {
                    codec1.encode(buf, getter1.apply(value));
                    codec2.encode(buf, getter2.apply(value));
                    codec3.encode(buf, getter3.apply(value));
                },
                buf -> constructor.apply(codec1.decode(buf), codec2.decode(buf), codec3.decode(buf))
        );
    }

    static <B, T1, T2, T3, T4, R> StreamCodec<B, R> composite(
            StreamCodec<B, T1> codec1,
            Function<R, T1> getter1,
            StreamCodec<B, T2> codec2,
            Function<R, T2> getter2,
            StreamCodec<B, T3> codec3,
            Function<R, T3> getter3,
            StreamCodec<B, T4> codec4,
            Function<R, T4> getter4,
            QuadFunction<T1, T2, T3, T4, R> constructor
    ) {
        return of(
                (buf, value) -> {
                    codec1.encode(buf, getter1.apply(value));
                    codec2.encode(buf, getter2.apply(value));
                    codec3.encode(buf, getter3.apply(value));
                    codec4.encode(buf, getter4.apply(value));
                },
                buf -> constructor.apply(codec1.decode(buf), codec2.decode(buf), codec3.decode(buf), codec4.decode(buf))
        );
    }

    static <B, T1, T2, T3, T4, T5, R> StreamCodec<B, R> composite(
            StreamCodec<B, T1> codec1,
            Function<R, T1> getter1,
            StreamCodec<B, T2> codec2,
            Function<R, T2> getter2,
            StreamCodec<B, T3> codec3,
            Function<R, T3> getter3,
            StreamCodec<B, T4> codec4,
            Function<R, T4> getter4,
            StreamCodec<B, T5> codec5,
            Function<R, T5> getter5,
            PentaFunction<T1, T2, T3, T4, T5, R> constructor
    ) {
        return of(
                (buf, value) -> {
                    codec1.encode(buf, getter1.apply(value));
                    codec2.encode(buf, getter2.apply(value));
                    codec3.encode(buf, getter3.apply(value));
                    codec4.encode(buf, getter4.apply(value));
                    codec5.encode(buf, getter5.apply(value));
                },
                buf -> constructor.apply(codec1.decode(buf), codec2.decode(buf), codec3.decode(buf), codec4.decode(buf), codec5.decode(buf))
        );
    }

    static <B, T1, T2, T3, T4, T5, T6, R> StreamCodec<B, R> composite(
            StreamCodec<B, T1> codec1,
            Function<R, T1> getter1,
            StreamCodec<B, T2> codec2,
            Function<R, T2> getter2,
            StreamCodec<B, T3> codec3,
            Function<R, T3> getter3,
            StreamCodec<B, T4> codec4,
            Function<R, T4> getter4,
            StreamCodec<B, T5> codec5,
            Function<R, T5> getter5,
            StreamCodec<B, T6> codec6,
            Function<R, T6> getter6,
            HexaFunction<T1, T2, T3, T4, T5, T6, R> constructor
    ) {
        return of(
                (buf, value) -> {
                    codec1.encode(buf, getter1.apply(value));
                    codec2.encode(buf, getter2.apply(value));
                    codec3.encode(buf, getter3.apply(value));
                    codec4.encode(buf, getter4.apply(value));
                    codec5.encode(buf, getter5.apply(value));
                    codec6.encode(buf, getter6.apply(value));
                },
                buf -> constructor.apply(
                        codec1.decode(buf),
                        codec2.decode(buf),
                        codec3.decode(buf),
                        codec4.decode(buf),
                        codec5.decode(buf),
                        codec6.decode(buf)
                )
        );
    }

    @FunctionalInterface
    interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    @FunctionalInterface
    interface QuadFunction<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }

    @FunctionalInterface
    interface PentaFunction<A, B, C, D, E, R> {
        R apply(A a, B b, C c, D d, E e);
    }

    @FunctionalInterface
    interface HexaFunction<A, B, C, D, E, F, R> {
        R apply(A a, B b, C c, D d, E e, F f);
    }
}

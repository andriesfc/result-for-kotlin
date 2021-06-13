package io.github.andriesfc.kotlin.result.interop;

import org.jetbrains.annotations.NotNull;

public interface ThrowableProducer<T> {
    @NotNull T produce() throws Throwable;
}

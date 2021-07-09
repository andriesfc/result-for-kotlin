package resultk.interop;

import org.jetbrains.annotations.NotNull;

/**
 * A functional interface which modelling a producer function which throws any exception. It is up the
 * caller to deal with any possible exception being thrown.
 *
 * @param <T> The type parameter of instance this function should return.
 *
 */
@FunctionalInterface
public interface ThrowingProducer<T> {
    /**
     * Produces a <em>non nullable</em> instance of <code>T</code>
     * @return A non nullable instance
     * @throws Throwable If any exception within the function implementation throws an exception.
     */
    @NotNull T produce() throws Throwable;
}

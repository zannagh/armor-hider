package de.zannagh.armorhider.client.compat;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * A reusable resolver for a fixed chain of no-argument methods (e.g. {@code inventory().getSlotType().getGroup()})
 * on objects whose type is not on the compile classpath (optional-dependency mod APIs).
 * <p>
 * The {@link Method} chain is resolved <b>once per concrete receiver class</b> and cached in a
 * {@link ClassValue}, so the render hot path only pays a cheap {@link Method#invoke} — never a fresh
 * {@code getMethod} lookup per frame. {@code ClassValue} handles the concurrency and its keys are the
 * classes themselves, so the cache is naturally bounded by the (tiny) number of accessory-slot types.
 */
public final class ReflectiveChain {

    private static final Method[] UNRESOLVED = new Method[0];

    private final String[] methodNames;

    private final ClassValue<Method[]> resolved = new ClassValue<>() {
        @Override
        protected Method[] computeValue(Class<?> type) {
            Method[] chain = new Method[methodNames.length];
            Class<?> current = type;
            try {
                for (int i = 0; i < methodNames.length; i++) {
                    Method method = current.getMethod(methodNames[i]);
                    method.setAccessible(true);
                    chain[i] = method;
                    current = method.getReturnType();
                }
                return chain;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return UNRESOLVED;
            }
        }
    };

    public ReflectiveChain(String... methodNames) {
        this.methodNames = methodNames;
    }

    /**
     * Walk the cached method chain against {@code target} and return the final value as a String,
     * or {@code null} if the target is null, the chain could not be resolved, an intermediate value
     * is null, or the terminal value is not a String.
     */
    @Nullable
    public String resolve(@Nullable Object target) {
        if (target == null) {
            return null;
        }
        Method[] chain = resolved.get(target.getClass());
        if (chain.length == 0) {
            return null;
        }
        Object current = target;
        try {
            for (Method method : chain) {
                if (current == null) {
                    return null;
                }
                current = method.invoke(current);
            }
            return current instanceof String result ? result : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}

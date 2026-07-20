package de.zannagh.armorhider.api.compat;

/**
 * A compat initializer with a target flag and an initialization method.
 */
public interface CompatInitializer {

    /**
     * The target compat flag of the initializer.
     * @return The {@link CompatFlags} target of the initializer.
     */
    CompatFlags targetFlag();

    /**
     * Initializes the compat.<br/>
     * <remarks>
     *     This method should not throw exceptions.
     * </remarks>
     * @return The {@link CompatInitializationResult} of the initialization.
     */
    CompatInitializationResult init();
}

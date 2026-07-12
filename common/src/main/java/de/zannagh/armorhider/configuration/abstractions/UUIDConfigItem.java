package de.zannagh.armorhider.configuration.abstractions;

import java.util.UUID;

/**
 * A UUID-based configuration item that extends {@link ConfigurationItemBase} with the type parameter {@code UUID}.
 * This abstract class provides a foundation for storing and managing configuration items
 * where the value is represented as a UUID.<br/><br/>
 *
 * Subclasses are required to implement the {@code getDefaultValue} method to specify the default
 * UUID value used when no value is assigned or when a null value is set.<br/><br/>
 *
 * Constructors:
 * - {@link UUIDConfigItem#UUIDConfigItem(UUID)}: Initializes the configuration item with
 *   a specific UUID value.
 * - {@link UUIDConfigItem#UUIDConfigItem()}: Initializes the configuration item with a default
 *   UUID value, as defined by the {@code getDefaultValue} method.
 */
public abstract class UUIDConfigItem extends ConfigurationItemBase<UUID> {

    public UUIDConfigItem(UUID defaultValue) {
        super(defaultValue);
    }

    public UUIDConfigItem() {
        super();
    }
}

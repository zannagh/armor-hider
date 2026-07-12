package de.zannagh.armorhider.configuration.abstractions;

import java.util.HashMap;

/**
 * A configuration item that encapsulates a {@link HashMap} with string keys and generic values.
 * This class extends {@link ConfigurationItemBase} to provide default handling
 * and initialization for {@link HashMap}-based configuration data structures.
 *
 * @param <T> The type of values mapped within the {@link HashMap}.
 */
public class HashMapConfigItem<T> extends ConfigurationItemBase<HashMap<String, T>>{
    public HashMapConfigItem(HashMap<String, T> currentValue) {
        super(currentValue);
    }

    public HashMapConfigItem() {
        super();
    }

    @Override
    public HashMap<String, T> getDefaultValue() {
        return new HashMap<>();
    }
}

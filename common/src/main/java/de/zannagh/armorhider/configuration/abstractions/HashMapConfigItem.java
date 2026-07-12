package de.zannagh.armorhider.configuration.abstractions;

import de.zannagh.armorhider.net.packets.PlayerConfig;

import java.util.HashMap;

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

package de.zannagh.armorhider.configuration.items.implementations;

import de.zannagh.armorhider.configuration.items.BooleanConfigItem;

public class CombatDetection extends BooleanConfigItem  {

    public CombatDetection(boolean currentValue) {
        super(currentValue);
    }
    
    public CombatDetection() {
        super();
    }
    
    @Override
    public Boolean getDefaultValue() {
        return true;
    }
}

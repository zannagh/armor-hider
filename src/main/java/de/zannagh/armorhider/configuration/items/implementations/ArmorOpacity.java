package de.zannagh.armorhider.configuration.items.implementations;

import de.zannagh.armorhider.configuration.items.DoubleConfigurationItem;

public class ArmorOpacity extends DoubleConfigurationItem {

    public static final double TRANSPARENCY_STEP = 0.05;

    public static final double DEFAULT_OPACITY = 1.0;
    
    public ArmorOpacity() {
        super();
    }
    
    public ArmorOpacity(double opacity) {
        super(opacity);
    }
    
    @Override
    public Double getDefaultValue() {
        return DEFAULT_OPACITY;
    }
}

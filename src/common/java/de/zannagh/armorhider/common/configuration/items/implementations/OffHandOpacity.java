package de.zannagh.armorhider.common.configuration.items.implementations;

import de.zannagh.armorhider.common.configuration.items.DoubleConfigurationItem;

public class OffHandOpacity extends DoubleConfigurationItem {
    public static final double TRANSPARENCY_STEP = 0.05;

    public static final double DEFAULT_OPACITY = 1;

    public OffHandOpacity() {
        super();
    }

    public OffHandOpacity(double opacity) {
        super(opacity);
    }

    @Override
    public Double getDefaultValue() {
        return DEFAULT_OPACITY;
    }
}

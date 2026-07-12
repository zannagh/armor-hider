package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.DoubleConfigurationItem;

/**
 * A {@link DoubleConfigurationItem} that determines the opacity of the armor.
 *
 * @since 0.1.0
 */
public class ArmorOpacity extends DoubleConfigurationItem {

    /**
     * The step size for the opacity value.
     */
    public static final double TRANSPARENCY_STEP = 0.05;

    /**
     * The default opacity value.
     */
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

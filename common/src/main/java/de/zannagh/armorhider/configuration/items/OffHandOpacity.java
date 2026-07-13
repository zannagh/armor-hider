package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.DoubleConfigurationItem;

import static de.zannagh.armorhider.configuration.items.ArmorOpacity.DEFAULT_OPACITY;

/**
 * A client-side {@link DoubleConfigurationItem} that allows the client to set the opacity of the off-hand armor.
 *
 * @since 0.7.8-pre.1
 */
public class OffHandOpacity extends DoubleConfigurationItem {

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

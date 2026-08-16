package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.DoubleConfigurationItem;
import de.zannagh.armorhider.configuration.abstractions.IntConfigurationItem;

/**
 * A {@link DoubleConfigurationItem} that determines the dithering phases (for rotation to have dithered textures picked up by TAA) for partial opacities when iris is used on 26.2.
 *
 * @since 0.12.16, schema 15
 */
public class IrisDitherPhases extends IntConfigurationItem {

    /**
     * The default opacity value.
     */
    public static final Integer DEFAULT_PHASES = 32;

    public IrisDitherPhases() {
        super();
    }

    public IrisDitherPhases(int scale) {
        super(scale);
    }

    @Override
    public Integer getDefaultValue() {
        return DEFAULT_PHASES;
    }

    @Override
    protected Integer getMinValue() {
        return 1;
    }

    @Override
    protected Integer getMaxValue() {
        return 256;
    }
}

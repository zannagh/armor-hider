package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.DoubleConfigurationItem;
import de.zannagh.armorhider.configuration.abstractions.IntConfigurationItem;

/**
 * A {@link DoubleConfigurationItem} that determines the dithering resolution cap for partial opacities when iris is used on 26.2.
 *
 * @since 0.12.16, schema 15
 */
public class IrisDitherResCap extends IntConfigurationItem {

    /**
     * The default opacity value.
     */
    public static final Integer DEFAULT_SCALE = 8192;

    public IrisDitherResCap() {
        super();
    }

    public IrisDitherResCap(int scale) {
        super(scale);
    }

    @Override
    public Integer getDefaultValue() {
        return DEFAULT_SCALE;
    }

    @Override
    protected Integer getMinValue() {
        return 256;
    }

    @Override
    protected Integer getMaxValue() {
        return 65536;
    }
}

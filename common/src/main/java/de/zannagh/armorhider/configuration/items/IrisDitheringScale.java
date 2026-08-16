package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.IntConfigurationItem;

/**
 * A {@link IntConfigurationItem} that determines the dithering scale for partial opacities when iris is used on 26.2.
 *
 * @since 0.12.16, schema 15
 */
public class IrisDitheringScale extends IntConfigurationItem {

    /**
     * The default opacity value.
     */
    public static final Integer DEFAULT_SCALE = 16;

    public IrisDitheringScale() {
        super();
    }

    public IrisDitheringScale(int scale) {
        super(scale);
    }

    @Override
    public Integer getDefaultValue() {
        return DEFAULT_SCALE;
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

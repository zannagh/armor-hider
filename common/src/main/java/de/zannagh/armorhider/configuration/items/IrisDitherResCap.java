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
     * The default resolution cap (longest side, in pixels) for a generated dither texture. Lowered from 8192
     * to keep each cached texture small: at 8192 a single entry could be 8192*8192*4 = 256 MB, which under an
     * HD armor pack made the dither cache exhaust native memory (issue #357). At 2048 an entry is at most
     * 2048*2048*4 = 16 MB. Users on very high-res packs can still raise it up to the max.
     */
    public static final Integer DEFAULT_SCALE = 2048;

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

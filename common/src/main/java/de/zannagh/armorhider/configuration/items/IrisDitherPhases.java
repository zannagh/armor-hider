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
     * The default number of temporal dither phases. Lowered from 32 to halve the number of distinct
     * per-phase textures generated (and cached) for each faded armor piece, which shrinks the dither cache's
     * working set and reduces LRU thrash on busy scenes (issue #357 follow-up). TAA still has plenty of
     * phases to average, so the fade stays smooth; users can raise it again for finer temporal blending.
     */
    public static final Integer DEFAULT_PHASES = 16;

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

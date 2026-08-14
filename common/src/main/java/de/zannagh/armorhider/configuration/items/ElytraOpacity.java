package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.DoubleConfigurationItem;
import de.zannagh.armorhider.net.packets.PlayerConfig;

/**
 * A {@link DoubleConfigurationItem} that determines the opacity of the elytra, decoupled from the
 * chestplate slider. Replaces the legacy {@code opacityAffectingElytra} toggle (see
 * {@link #fromLegacyConfig}).
 *
 * @since 0.12.14, schema 14
 */
public class ElytraOpacity extends DoubleConfigurationItem {

    /**
     * The step size for the opacity value.
     */
    public static final double TRANSPARENCY_STEP = 0.05;

    /**
     * The default opacity value.
     */
    public static final double DEFAULT_OPACITY = 1.0;

    public ElytraOpacity() {
        super();
    }

    public ElytraOpacity(double opacity) {
        super(opacity);
    }

    @Override
    public Double getDefaultValue() {
        return DEFAULT_OPACITY;
    }

    @Override
    protected double getMinValue() {
        return 0.0;
    }

    @Override
    protected double getMaxValue() {
        return 1.0;
    }

    public static ElytraOpacity fromLegacyConfig(PlayerConfig config) {
        if (config.opacityAffectingElytra.getValue()) {
            return new ElytraOpacity(config.chestOpacity.getValue());
        }
        return new ElytraOpacity(DEFAULT_OPACITY);
    }
}

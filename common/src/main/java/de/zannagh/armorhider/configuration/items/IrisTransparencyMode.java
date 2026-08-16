package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.IrisPartialTransparencyMode;
import de.zannagh.armorhider.configuration.abstractions.ConfigurationItemBase;
import de.zannagh.armorhider.configuration.abstractions.IntConfigurationItem;

/**
 * A {@link IntConfigurationItem} that determines the dithering scale for partial opacities when iris is used on 26.2.
 *
 * @since 0.12.16, schema 15
 */
public class IrisTransparencyMode extends ConfigurationItemBase<IrisPartialTransparencyMode> {

    public IrisTransparencyMode() {
        super();
    }

    public IrisTransparencyMode(IrisPartialTransparencyMode mode) {
        super(mode);
    }

    @Override
    public IrisPartialTransparencyMode getDefaultValue() {
        return IrisPartialTransparencyMode.TEMPORAL_DITHERING;
    }
}

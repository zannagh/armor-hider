package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.EmfHiddenModelMode;
import de.zannagh.armorhider.configuration.abstractions.ConfigurationItemBase;

/**
 * How the custom EMF (Fresh Animations) player model is treated while the body armor is hidden.
 * Defaults to {@link EmfHiddenModelMode#KEEP} - the custom model is left alone. Only meaningful when
 * Entity Model Features is present; the GUI hides the control otherwise.
 *
 * @since 0.12.x
 */
public class HiddenModelBehaviour extends ConfigurationItemBase<EmfHiddenModelMode> {

    public HiddenModelBehaviour(EmfHiddenModelMode currentValue) {
        super(currentValue);
    }

    public HiddenModelBehaviour() {
        super();
    }

    @Override
    public EmfHiddenModelMode getDefaultValue() {
        return EmfHiddenModelMode.KEEP;
    }
}

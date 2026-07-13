package de.zannagh.armorhider.configuration.items;

import de.zannagh.armorhider.configuration.abstractions.BooleanConfigItem;

/**
 * Whether to enable glint on an armor piece. Also see {@link BooleanConfigItem}.
 *
 * @since 0.8.9
 */
public class EnableGlint extends BooleanConfigItem {
    public EnableGlint(boolean currentValue) {
        super(currentValue);
    }

    public EnableGlint() {
        super();
    }

    @Override
    public Boolean getDefaultValue() {
        return true;
    }
}

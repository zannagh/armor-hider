package de.zannagh.armorhider.client.scopes;

import org.jetbrains.annotations.Nullable;

public interface IdentityStateCarrier extends IdentityCarrier {
    void armorHider$attachIdentityCarrier(@Nullable IdentityCarrier carrier);
}

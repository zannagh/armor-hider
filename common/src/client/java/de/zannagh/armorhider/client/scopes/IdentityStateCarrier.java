package de.zannagh.armorhider.client.scopes;

import org.jetbrains.annotations.Nullable;

public interface IdentityStateCarrier extends IdentityCarrier {
    void attachCarrier(@Nullable IdentityCarrier carrier);
}

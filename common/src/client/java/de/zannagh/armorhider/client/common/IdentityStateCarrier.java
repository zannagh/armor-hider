package de.zannagh.armorhider.client.common;

import org.jetbrains.annotations.Nullable;

public interface IdentityStateCarrier extends IdentityCarrier {
    void ah$attachCarrier(@Nullable IdentityCarrier carrier);

    @Nullable IdentityCarrier ah$getCarrier();
}

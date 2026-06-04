package de.zannagh.armorhider.client.api.configuration;

import de.zannagh.armorhider.client.scopes.IdentityCarrier;

public record ScopeHandover
        (
                IdentityCarrier carrier,
                SlotModification modification
        ){
}

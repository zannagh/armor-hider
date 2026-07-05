package de.zannagh.armorhider.client.suppressions;

import de.zannagh.armorhider.client.api.AhRenderer;
import de.zannagh.armorhider.client.common.IdentityCarrier;
import de.zannagh.armorhider.client.common.RenderScope;

public interface ConditionalSuppressor {
    boolean shouldSuppress(RenderScope scope, IdentityCarrier carrier);

    boolean shouldSuppress(RenderScope scope, AhRenderer renderer);
}

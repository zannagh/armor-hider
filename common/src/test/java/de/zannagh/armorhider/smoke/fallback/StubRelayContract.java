package de.zannagh.armorhider.smoke.fallback;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.zannagh.eunomia.common.ApiVersion;

import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * The parts of the relay's wire contract that are pure decisions - which URIs are packet endpoints, which API
 * versions are served, and which HTTP status an inbound envelope earns - split out of {@link StubEunomiaRelay}
 * so the server plumbing there stays readable. Everything here mirrors a specific place in eunomia's C# server;
 * each rule names it, because these are the details that silently drift when the relay moves on and the stub
 * does not (which is exactly what happened between 0.3.0 and 0.3.2).
 */
final class StubRelayContract {

    /**
     * The {@code major.minor} REST segment this build's eunomia client speaks, read from eunomia's OWN
     * {@link ApiVersion} rather than spelled out here. That constant is the single source of truth on both
     * sides of the wire: {@code RelayEndpoints.api(...)} builds {@code /api/v<CURRENT>/...} from it and
     * {@code RelayEndpoints.handshake(...)} puts the same value in the socket's {@code v=} query. Deriving it
     * means a 0.4 bump moves the stub's routes with the client instead of leaving them 404ing.
     */
    static final String VERSION_SEGMENT = ApiVersion.CURRENT;

    /** The versioned packet route prefix - {@code [Route("api/v{version:apiVersion}/[controller]")]}. */
    static final String VERSIONED_PACKETS = "/api/v" + VERSION_SEGMENT + "/packets/";

    /**
     * The unversioned prefix the C# {@code PacketsController} still carries as a second {@code [Route]} for
     * pre-versioning clients (Java 0.3.0 and earlier). Served alongside, not instead of, the versioned one -
     * the E2E's raw puts use it deliberately, so dropping it here would only hide a real capability.
     */
    static final String LEGACY_PACKETS = "/api/packets/";

    /** {@code PacketsController.MaxBodyBytes} - a declared Content-Length above this is 413, body unread. */
    static final long MAX_BODY_BYTES = 1024 * 1024;

    /** {@code StorageLimits.MaxIdentifierLength} - the varchar(512) width scope/channel/key/name persist into. */
    static final int MAX_IDENTIFIER_LENGTH = 512;

    /** RFC 6455 policy violation; the socket-side twin of the 403 block ({@code RelayProtocol.POLICY_VIOLATION_CLOSE}). */
    static final int POLICY_VIOLATION_CLOSE = 1008;

    /**
     * {@code WebSocketMiddleware.UnsupportedVersionCloseCode}. Deliberately an application code (4000-4999) and
     * not {@link #POLICY_VIOLATION_CLOSE}, because the client treats the two as different terminal outcomes:
     * 1008 is "an operator blocked this scope", 4001 is "this relay does not speak your version".
     */
    static final int UNSUPPORTED_VERSION_CLOSE = 4001;

    /** Sentinel from {@link #rejection}: every gate passed, the envelope may be stored and relayed. */
    static final int ACCEPTED = 0;

    private StubRelayContract() {
    }

    /**
     * The packet route a URI addresses ({@code "keyed"} / {@code "plain"}), or {@code null} when the URI is not
     * a packet endpoint at all. Both the versioned and the legacy prefix resolve to the same route, because on
     * the real server they are two {@code [Route]} attributes on one controller action.
     */
    static String packetsRoute(String uri) {
        String tail = null;
        if (uri.startsWith(VERSIONED_PACKETS)) {
            tail = uri.substring(VERSIONED_PACKETS.length());
        } else if (uri.startsWith(LEGACY_PACKETS)) {
            tail = uri.substring(LEGACY_PACKETS.length());
        }
        return "keyed".equals(tail) || "plain".equals(tail) ? tail : null;
    }

    /**
     * Whether the socket handshake's {@code v=} names a version this relay serves, mirroring
     * {@code EunomiaApiVersions.TryResolve}: absent or empty is a pre-versioning client and is accepted (it is
     * served the oldest supported version), anything else must match one of the served versions. The stub
     * serves exactly one, so "supported" is "equal to {@link #VERSION_SEGMENT}".
     */
    static boolean isSupportedVersion(String raw) {
        return raw == null || raw.isBlank() || VERSION_SEGMENT.equals(raw);
    }

    /** {@code StorageLimits.IsWithinLimit} - null is fine (the column is nullable), over-long is not. */
    static boolean withinLimit(String value) {
        return value == null || value.length() <= MAX_IDENTIFIER_LENGTH;
    }

    /** A JSON string field, or {@code null} for absent/JSON-null - the shape Gson gives the C# binder. */
    static String asString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    /**
     * The status a {@code PUT} of {@code envelope} earns, or {@link #ACCEPTED}. This is
     * {@code PacketsController.ValidateRequest} plus the keyed action's null-key check, <em>in the server's
     * order</em> - the order is itself part of the contract, because it decides which status a request that
     * trips several gates actually gets back (an over-long identifier on a blocked scope is a 403, not a 400).
     *
     * <p>The ladder's first rung, the {@link #MAX_BODY_BYTES} 413, is applied by the caller instead: it is
     * decided on the declared Content-Length before the body is bound, so it cannot live behind a parsed
     * envelope. Everything from the 403 down is here.</p>
     *
     * <p>Note what is NOT gated: the envelope's {@code name} (added in 0.3.2 to carry the human-readable server
     * label) is neither required nor length-checked on this path - only the socket handshake's {@code name} is,
     * because only that one is persisted. An envelope carrying it, or omitting it, is equally valid.</p>
     *
     * @param scopeBlocked {@code IServerBlockService.IsBlocked}.
     * @param sessionLive  {@code ConnectionManager.IsConnected}, called with (scope, canonical sender uuid).
     */
    static int rejection(JsonObject envelope,
                         boolean keyed,
                         Predicate<String> scopeBlocked,
                         BiPredicate<String, String> sessionLive) {
        String scope = asString(envelope, "scope");
        String channel = asString(envelope, "channel");
        String key = asString(envelope, "key");

        // Terminal, and evaluated before anything else that survives binding: a blocked scope is refused
        // without touching the store.
        if (scopeBlocked.test(scope)) {
            return 403;
        }
        if (!withinLimit(scope) || !withinLimit(channel) || !withinLimit(key)) {
            return 400;
        }
        // Guid.TryParse, not a null check: a sender that is not a UUID is a 400 even with a live socket.
        String sender = canonicalUuid(asString(envelope, "sender"));
        if (sender == null) {
            return 400;
        }
        // The session gate that stops REST spoofing - a put is only honoured for an identity already on a socket.
        if (!sessionLive.test(scope, sender)) {
            return 409;
        }
        // Last, and only on /keyed: the C# controller checks this after ValidateRequest returned null, so a
        // keyless keyed put from an unconnected identity is a 409 rather than a 400.
        if (keyed && key == null) {
            return 400;
        }
        return ACCEPTED;
    }

    /**
     * The lower-case canonical form of a UUID, or {@code null} when the value is not one. Canonicalizing
     * mirrors the server comparing parsed {@code Guid}s rather than the raw strings, so a differently-cased
     * sender still matches its socket.
     */
    static String canonicalUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

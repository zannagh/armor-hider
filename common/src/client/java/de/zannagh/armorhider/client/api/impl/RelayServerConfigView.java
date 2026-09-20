package de.zannagh.armorhider.client.api.impl;

import de.zannagh.armorhider.net.packets.AhReplicatedPlayerConfig;
import de.zannagh.armorhider.net.packets.PlayerConfig;
import de.zannagh.armorhider.server.ServerConfiguration;
import de.zannagh.eunomia.keyed.KeyPath;

import java.util.Map;

/**
 * A materialised {@link ServerConfiguration} view over the relay mirror's keyed store, together with the exact
 * store contents it was built from.
 * <p>
 * {@code AhPlayerConfigApiImpl.currentServerConfig()} rebuilt this view on <b>every</b> call, and on the
 * relay-fallback path {@code resolveConfig(...)} calls it for every remote player render lookup - i.e. per
 * remote player per frame. Each rebuild costs a {@code Map.copyOf} of the store, a fresh
 * {@code ServerConfiguration} (two {@code HashMap}s plus a defaulted {@code ServerWideSettings} graph) and two
 * map insertions per synced player, so with a large cloud-synced store cloud sync became a render-loop cost.
 * This class makes it once-per-change instead of once-per-lookup.
 * <p>
 * <b>Why the store contents are the invalidation signal.</b> eunomia's {@code KeyedStore} exposes no version
 * counter and no change listener, and the two write paths into the client mirror do not pass through anything
 * this mod can hook: the live per-entry relay goes through the single {@code CLIENT_HANDLERS} entry that
 * {@code ReplicatedClientStore.enableClient()} owns (taking it over would mean reimplementing eunomia's apply
 * step), and the join snapshot is applied through the package-private {@code StoreSyncClient} binding, which
 * is not reachable at all. So the store has to be polled - but deliberately NOT by its size or by the identity
 * of the {@code snapshot()} copy (which is freshly allocated on every call and so never matches): the check is
 * an <b>entry-by-entry reference comparison</b> of the previous store contents against the current ones.
 * <p>
 * That is exact rather than heuristic, because both write paths install newly deserialized value instances:
 * the live path is {@code store.put(value.keyPath(), value)} with the just-decoded payload, and the snapshot
 * path is {@code replaceAll(...)} of a map of just-decoded payloads (a {@code clear()} followed by fresh
 * puts). A value can therefore never change while its instance stays the same, and an added, removed or
 * re-keyed entry changes the key set, which the size plus per-key lookup covers. A changed value can only make
 * the comparison fail, never silently pass.
 * <p>
 * In-place mutation of an already-stored config needs no invalidation at all: the view holds the store's own
 * {@code PlayerConfig} instances (see {@code AhReplicatedPlayerConfig.toPlayerConfig()}, which returns the
 * carried config rather than a copy), so such a change is visible through the cached view exactly as it was
 * through a freshly built one - the memoisation does not add a staleness window that did not exist before. The
 * one thing a cached view would not pick up is an in-place rename of a stored config, because
 * {@code ServerConfiguration}'s by-name index is built at insertion time; nothing on the client renames a
 * mirrored remote config (a renamed player arrives as a new relayed value), and a relay-sourced rename
 * replaces the instance and is caught.
 */
final class RelayServerConfigView {

    private final Map<KeyPath, AhReplicatedPlayerConfig> source;

    private final ServerConfiguration view;

    private RelayServerConfigView(Map<KeyPath, AhReplicatedPlayerConfig> source, ServerConfiguration view) {
        this.source = source;
        this.view = view;
    }

    /**
     * Materialises the view for {@code snapshot}, which must be the immutable copy returned by
     * {@code KeyedStore.snapshot()} - it is retained as the comparison baseline.
     */
    static RelayServerConfigView of(Map<KeyPath, AhReplicatedPlayerConfig> snapshot) {
        ServerConfiguration built = new ServerConfiguration();
        for (AhReplicatedPlayerConfig entry : snapshot.values()) {
            PlayerConfig config = entry.toPlayerConfig();
            if (config != null) {
                built.put(config);
            }
        }
        return new RelayServerConfigView(snapshot, built);
    }

    /** Whether {@code candidate} holds exactly the same entries, by key and by value reference, as this view. */
    boolean isBuiltFrom(Map<KeyPath, AhReplicatedPlayerConfig> candidate) {
        if (candidate.size() != source.size()) {
            return false;
        }
        for (Map.Entry<KeyPath, AhReplicatedPlayerConfig> entry : source.entrySet()) {
            if (candidate.get(entry.getKey()) != entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    ServerConfiguration view() {
        return view;
    }
}

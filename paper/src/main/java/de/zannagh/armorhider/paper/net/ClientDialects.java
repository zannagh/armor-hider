package de.zannagh.armorhider.paper.net;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers which channel namespace ("dialect") each connected client actually speaks.
 *
 * <p><strong>Why this exists.</strong> {@link ChannelSubscriber} force-subscribes every connection
 * to <em>both</em> aliases of every payload, because the client never sends
 * {@code minecraft:register} and Paper would otherwise drop clientbound sends silently. That is
 * deliberate and must stay: the server genuinely does not know the client's dialect at join time.
 * The side effect is that {@code getListeningPluginChannels()} always contains both aliases, so it
 * can no longer be used to pick a namespace - sending every listening alias means sending each
 * payload twice.</p>
 *
 * <p>The client, however, always talks back to us on the alias it speaks. Recording the namespace
 * of inbound traffic therefore identifies the dialect for free, and lets {@link PacketSender} drop
 * the redundant copy. This is purely a bandwidth optimisation, not a correctness requirement:
 * a client with no recorded dialect keeps receiving both aliases and ignores the one it has no
 * codec for.</p>
 */
public final class ClientDialects {

    private final ConcurrentHashMap<UUID, String> namespaces = new ConcurrentHashMap<>();

    /**
     * Records the namespace of {@code channel} as the dialect spoken by {@code player}.
     *
     * <p>Called for every inbound plugin message, including ones whose body fails to decode - the
     * channel name is evidence of the client's dialect regardless of what it carried. Channels
     * outside {@link Channels#DIALECT_BEARING_C2S} are ignored: only the settings family changed
     * namespace across versions, so only it identifies an era. A combat-log packet looks like the
     * current namespace on <em>every</em> client and would otherwise mislabel a legacy one.</p>
     *
     * @param player  the sending player's UUID
     * @param channel a fully qualified channel name, {@code namespace:path}
     */
    public void remember(UUID player, String channel) {
        if (player == null || channel == null) {
            return;
        }
        if (!Channels.DIALECT_BEARING_C2S.contains(channel)) {
            return;
        }
        int separator = channel.indexOf(':');
        if (separator <= 0) {
            return;
        }
        namespaces.put(player, channel.substring(0, separator));
    }

    /** Returns the namespace {@code player} was last seen speaking, if any. */
    public Optional<String> preferred(UUID player) {
        return player == null
                ? Optional.empty()
                : Optional.ofNullable(namespaces.get(player));
    }

    /** Drops the recorded dialect for {@code player}. Must run on quit so nothing leaks. */
    public void forget(UUID player) {
        if (player == null) {
            return;
        }
        namespaces.remove(player);
    }

    /**
     * Narrows {@code channels} to the aliases in {@code player}'s known dialect.
     *
     * <p>Returns {@code channels} unchanged when no dialect has been observed yet, and also when
     * the recorded namespace matches none of them - never sending is worse than sending twice.</p>
     */
    public List<String> select(UUID player, List<String> channels) {
        String namespace = player == null ? null : namespaces.get(player);
        if (namespace == null) {
            return channels;
        }
        String prefix = namespace + ":";
        List<String> matching = channels.stream()
                .filter(channel -> channel.startsWith(prefix))
                .toList();
        return matching.isEmpty() ? channels : matching;
    }
}

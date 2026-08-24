package de.zannagh.armorhider.paper.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

/**
 * Reproduces the name Armor Hider identifies a player by, so the plugin keys shared-rule state the
 * same way the mod's own server half does.
 *
 * <p>The mod resolves names through {@code PlayerNameUtil}, which reads
 * {@code ServerPlayer#getDisplayName()}. In vanilla that is
 * {@code PlayerTeam.formatNameForTeam(team, profileName)} - the player's <b>main-scoreboard</b> team
 * prefix and suffix wrapped around their profile name - flattened with {@code Component#getString()},
 * which keeps the prefix/suffix <em>text</em> and drops their styling. A receiving client computes
 * exactly the same string for the same entity, because team data is synced, which is what makes the
 * name usable as a shared key at all.</p>
 *
 * <p>Notably <b>not</b> {@code Player#getDisplayName()}: that is Bukkit's <em>chat</em> display name.
 * It defaults to the profile name and only changes when a plugin calls {@code setDisplayName}, which
 * has no effect on the name any client renders above the player. Using it would introduce a mismatch
 * on exactly the servers this is meant to fix.</p>
 *
 * <p>Uses the legacy Bukkit scoreboard API rather than Paper's Adventure accessors, keeping to the
 * "stable Bukkit API only" rule that lets one plugin jar load on 1.20.1 through 26.x - hence the
 * section-sign stripping, which is how a legacy-serialised prefix reduces to the same plain text the
 * client's {@code getString()} produces.</p>
 *
 * <p><b>Limit.</b> A plugin that gives each viewer their own scoreboard can decorate one player
 * differently per viewer, and no single relayed name can satisfy all of them. The main scoreboard is
 * both what vanilla uses and what such a server falls back to, so this matches wherever a single
 * answer exists at all.</p>
 */
public final class DisplayNames {

    /** Legacy colour/format codes: a section sign plus one code character. */
    private static final String LEGACY_CODE = "(?i)§[0-9A-FK-OR]";

    private DisplayNames() {
    }

    /**
     * @return the name Armor Hider identifies {@code player} by. Falls back to the profile name
     *         whenever the scoreboard is unavailable (no world loaded yet) or the player is on no team,
     *         which is also the undecorated case every vanilla server is in.
     */
    // Team#getPrefix()/getSuffix() are deprecated in favour of Paper's Adventure accessors. The legacy
    // pair is deliberate: it is plain Bukkit API available across the whole 1.20.1-26.x range this one
    // jar has to load on, and its legacy-serialised output reduces to the client's plain text with the
    // strip below. Switching would pull Adventure into a plugin that currently uses none.
    @SuppressWarnings("deprecation")
    public static String of(Player player) {
        String name = player.getName();
        try {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            if (manager == null) {
                return name;
            }
            Scoreboard main = manager.getMainScoreboard();
            Team team = main.getEntryTeam(name);
            return team == null ? name : decorate(name, team.getPrefix(), team.getSuffix());
        } catch (RuntimeException e) {
            // Never let a scoreboard quirk break the relay: an undecorated name is still the right
            // answer on every server that does not decorate, which is the overwhelming majority.
            return name;
        }
    }

    /**
     * The pure half: wraps {@code name} in a team's prefix and suffix the way the client flattens the
     * same components, with legacy formatting codes removed because they are styling on the client and
     * never appear in {@code Component#getString()}.
     */
    public static String decorate(String name, String prefix, String suffix) {
        return strip(prefix) + name + strip(suffix);
    }

    private static String strip(String legacyText) {
        return legacyText == null ? "" : legacyText.replaceAll(LEGACY_CODE, "");
    }
}

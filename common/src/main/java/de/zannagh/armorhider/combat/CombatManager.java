package de.zannagh.armorhider.combat;

import com.mojang.datafixers.util.Pair;
import de.zannagh.armorhider.api.ArmorHiderApi;
import de.zannagh.armorhider.api.combat.ArmorHiderCombatEvent;
import de.zannagh.armorhider.api.combat.ArmorHiderCombatEventConsumer;
import de.zannagh.armorhider.api.combat.ArmorHiderCombatManagementApi;
import de.zannagh.armorhider.common.InjectorFactory;

import java.util.*;

/**
 * A default implementation of {@link ArmorHiderCombatManagementApi}.
 */
public class CombatManager extends InjectorFactory<ArmorHiderCombatEvent, ArmorHiderCombatEventConsumer> implements ArmorHiderCombatManagementApi  {

    private final ArrayList<Pair<ArmorHiderCombatManagementApi, Integer>> customManagers = new ArrayList<>();

    public CombatManager() {
        super(new CombatEventConsumerImpl());
    }

    // Static delegates for backwards compatibility with existing call sites.
    public static void logCombat(String playerDisplayName) {
        ArmorHiderApi.getInstance().getCombatManagement().registerCombatEvent(new DefaultCombatEvent(playerDisplayName, System.currentTimeMillis()));
    }

    public static void logCombat(String playerDisplayName, long timestamp) {
        ArmorHiderApi.getInstance().getCombatManagement().registerCombatEvent(new DefaultCombatEvent(playerDisplayName, timestamp));
    }

    public static boolean isPlayerInCombat(String playerDisplayName) {
        return ArmorHiderApi.getInstance().getCombatManagement().isInCombat(playerDisplayName);
    }

    public static double transformTransparencyBasedOnCombat(String playerDisplayName, double transparency) {
        return ArmorHiderApi.getInstance().getCombatManagement().getCombatFade(playerDisplayName, transparency);
    }

    public void registerCombatEvent(String playerDisplayName) {
        if (!customManagers.isEmpty()) {
            customManagers.get(0).getFirst().registerCombatEvent(new DefaultCombatEvent(playerDisplayName, System.currentTimeMillis()));
            return;
        }
        if (!playerDisplayName.isEmpty()) {
            registerCombatEvent(new DefaultCombatEvent(playerDisplayName, System.currentTimeMillis()));
        }
    }

    public void registerCombatEvent(String playerDisplayName, long timestamp) {
        if (!customManagers.isEmpty()) {
            customManagers.get(0).getFirst().registerCombatEvent(new DefaultCombatEvent(playerDisplayName, timestamp));
            return;
        }
        if (!playerDisplayName.isEmpty()) {
            registerCombatEvent(new DefaultCombatEvent(playerDisplayName, timestamp));
        }
    }

    @Override
    public void overrideDefaultBehavior(ArmorHiderCombatManagementApi customManagement, int priority) {
        customManagers.add(Pair.of(customManagement, priority));
        customManagers.sort(Comparator.comparingInt(Pair::getSecond));
    }

    @Override
    public void registerCombatEventConsumer(ArmorHiderCombatEventConsumer consumer) {
        addHandler(consumer);
    }

    @Override
    public void registerCombatEvent(ArmorHiderCombatEvent event) {
        if (!customManagers.isEmpty()) {
            customManagers.get(0).getFirst().registerCombatEvent(event);
            return;
        }
        handle(event);
    }

    public boolean isInCombat(String playerDisplayName) {
        return !playerDisplayName.isEmpty() && anyHandler(handler -> handler.isPlayerConsideredInCombat(playerDisplayName));
    }

    public double getCombatFade(String playerDisplayName, double originalTransparency) {
        if (!customManagers.isEmpty()) {
            return customManagers.get(0).getFirst().getCombatFade(playerDisplayName, originalTransparency);
        }
        return findHandler(handler -> handler.isPlayerConsideredInCombat(playerDisplayName))
                .map(handler -> handler.getFadeFor(playerDisplayName, originalTransparency))
                .orElse(originalTransparency);
    }
}

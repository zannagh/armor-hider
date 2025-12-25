package de.zannagh.armorhider.resources;

import java.util.*;

public class ServerConfig {
    public Map<UUID, PlayerConfig> playerConfigs = new HashMap<>();
    public boolean enableCombatDetection = true;
}

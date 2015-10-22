package com.github.games647.healthname.config;

import ninja.leaping.configurate.objectmapping.Setting;

public class Config {

    @Setting(comment = "Show health bar above mobs")
    private boolean enabledMob = true;
    
    @Setting(comment = "Show the health of a player besides his nametag")
    private boolean nametagHealth = false;
    
    @Setting(comment = "Show the health of a player under his nametag  (only the health number)")
    private boolean belowNameHealth = false;
    
    @Setting(comment = "What character should be used to display the health")
    private char displayChar = '|';

    public boolean isEnabledMob() {
        return enabledMob;
    }

    public boolean isNametagHealth() {
        return nametagHealth;
    }

    public boolean isBelowNameHealth() {
        return belowNameHealth;
    }

    public char getDisplayChar() {
        return displayChar;
    }
}

package com.github.games647.healthname.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class Config {

    @Setting(comment = "Show health bar above mobs")
    private boolean enabledMob = true;

    @Setting(comment = "Show the health of a player besides his nametag")
    private boolean nametagHealth;

    @Setting(comment = "Show the health of a player under his nametag  (only the health number)")
    private boolean belowNameHealth = true;

    @Setting(comment = "What character should be used to display the health")
    private String displayChar = "|";

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
        return displayChar.charAt(0);
    }
}

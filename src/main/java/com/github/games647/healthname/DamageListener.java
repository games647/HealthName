package com.github.games647.healthname;

import com.github.games647.healthname.config.Config;

import java.util.Optional;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.Team;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlots;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;

public class DamageListener {

    private final HealthName plugin;

    public DamageListener(HealthName plugin) {
        this.plugin = plugin;
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join joinEvent) {
        Player player = joinEvent.getTargetEntity();
        //everyone should have a global scoreboard to see the health from others
        if (plugin.getConfig().isNametagHealth() || plugin.getConfig().isBelowNameHealth()) {
            //don't override the old scoreboard if the feature isn't needed
            player.setScoreboard(plugin.getGlobalScoreboard());
        }
    }

    @Listener
    public void onQuit(ClientConnectionEvent.Disconnect disconnectEvent) {
        String playerName = disconnectEvent.getTargetEntity().getName();
        //Clean up scoreboard in order to prevent to big ones
        plugin.getGlobalScoreboard().removeScores(Text.of(playerName));
        Optional<Team> optionalTeam = plugin.getGlobalScoreboard().getTeam(playerName);
        if (optionalTeam.isPresent()) {
            Team team = optionalTeam.get();
            team.unregister();
        }
    }

    @Listener
    public void onEntityDamage(DamageEntityEvent damageEntityEvent) {
        Entity targetEntity = damageEntityEvent.getTargetEntity();
        Optional<Double> optionalHealth = targetEntity.get(Keys.HEALTH);
        Optional<Double> optionalMaxHealth = targetEntity.get(Keys.MAX_HEALTH);
        if (optionalHealth.isPresent() && optionalMaxHealth.isPresent()) {
            double maxHealth = optionalMaxHealth.get();
            double currentHealth = optionalHealth.get() - damageEntityEvent.getFinalDamage();

            Text healthMessage = generateHealthMessage(currentHealth, maxHealth);
            if (targetEntity.getType() == EntityTypes.PLAYER) {
                Player targetPlayer = (Player) targetEntity;
                String playerName = targetPlayer.getName();

                Scoreboard playerScoreboard = targetPlayer.getScoreboard();
                if (plugin.getGlobalScoreboard().equals(playerScoreboard)) {
                    //does the player have still a global scoreboard -> we don't want to overrid eothers
                    setBelownameHealth(playerScoreboard, currentHealth, playerName);

                    setNametagHealth(playerScoreboard, playerName, healthMessage);
                }
            } else if (plugin.getConfig().isEnabledMob() && targetEntity.supports(Keys.DISPLAY_NAME)) {
                //mobs have only support for this
                targetEntity.offer(Keys.DISPLAY_NAME, healthMessage);
            }
        }
    }

    private void setBelownameHealth(Scoreboard playerScoreboard, double currentHealth, String playerName) {
        if (plugin.getConfig().isBelowNameHealth()) {
            Optional<Objective> optionalObjective = playerScoreboard.getObjective(DisplaySlots.BELOW_NAME);
            if (optionalObjective.isPresent()) {
                //make sure we are targeting our own objective on the correct display slot
                Objective objective = optionalObjective.get();
                if (objective.getName().equals(plugin.getContainer().getId())) {
                    //we don't want to override other scoreboards
                    int displayedHealth = (int) Math.ceil(currentHealth);
                    objective.getOrCreateScore(Text.of(playerName)).setScore(displayedHealth);
                }
            }
        }
    }

    private void setNametagHealth(Scoreboard playerScoreboard, String playerName, Text healthMessage) {
        if (plugin.getConfig().isNametagHealth()) {
            //player nametag prefix and suffix
            Optional<Team> optionalTeam = playerScoreboard.getTeam(playerName);
            if (!optionalTeam.isPresent()) {
                playerScoreboard.registerTeam(plugin.getGame().getRegistry().createBuilder(Team.Builder.class)
                        .name(playerName)
                        .build());
            }

            Team team = playerScoreboard.getTeam(playerName).get();
            team.addMember(Text.of(playerName));
            team.setPrefix(healthMessage);
            team.setSuffix(healthMessage);
        }
    }

    private Text generateHealthMessage(double currentHealth, double maxHealth) {
        double percent = currentHealth * 100 / maxHealth;
        //10-steps -> 1=10% 2=20% 3=30%
        int steps = (int) Math.ceil(percent / 10);

        Config config = plugin.getConfig();
        char displayChar = config.getDisplayChar();

        TextColor highlightColor = getHealthColor(steps);

        Text.Builder textBuilder = Text.builder();
        for (int i = 0; i < percent / 10; i++) {
            textBuilder.append(Text
                    .builder(displayChar)
                    .color(highlightColor)
                    .build());
        }

        textBuilder.append(Text.builder().color(TextColors.RESET).build());

        return textBuilder.build();
    }

    private TextColor getHealthColor(int steps) {
        TextColor highlightColor;
        switch (steps) {
            case 1:
            case 2:
                highlightColor = TextColors.DARK_RED;
                break;
            case 3:
            case 4:
                highlightColor = TextColors.RED;
                break;
            case 5:
                highlightColor = TextColors.GOLD;
                break;
            case 6:
                highlightColor = TextColors.YELLOW;
                break;
            case 7:
            case 8:
                highlightColor = TextColors.GREEN;
                break;
            case 9:
            case 10:
            default:
                highlightColor = TextColors.DARK_GREEN;
                break;
        }

        return highlightColor;
    }
}

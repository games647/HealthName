package com.github.games647.healthname;

import com.github.games647.healthname.config.Config;

import java.util.Optional;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.Team;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlots;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;

public class DamageListener {

    private final HealthName plugin;
    private final Scoreboard globalScoreboard;

    public DamageListener(HealthName plugin) {
        this.plugin = plugin;
        this.globalScoreboard = plugin.getGame()
                .getRegistry().createBuilder(Scoreboard.Builder.class)
                .build();

        if (plugin.getConfig().isBelowNameHealth()) {
            //since this is a global objective we need to create this just once
            globalScoreboard.addObjective(plugin.getGame().getRegistry().createBuilder(Objective.Builder.class)
                    .name(plugin.getContainer().getId())
                    .displayName(Texts.of(TextColors.DARK_RED, "Health"))
                    .build(), DisplaySlots.BELOW_NAME);
        }
    }

    @Listener(ignoreCancelled = true)
    public void onJoin(ClientConnectionEvent.Join joinEvent) {
        Player player = joinEvent.getTargetEntity();
        //everyone should have a global scoreboard to see the health from others
        if (plugin.getConfig().isNametagHealth() || plugin.getConfig().isBelowNameHealth()) {
            //don't override the old scoreboard if the feature isn't needed
            player.setScoreboard(globalScoreboard);
        }
    }

    @Listener(ignoreCancelled = true)
    public void onQuit(ClientConnectionEvent.Disconnect disconnectEvent) {
        String playerName = disconnectEvent.getTargetEntity().getName();
        //Clean up scoreboard in order to prevent to big ones
        globalScoreboard.removeScores(Texts.of(playerName));
        Optional<Team> optionalTeam = globalScoreboard.getTeam(playerName);
        if (optionalTeam.isPresent()) {
            globalScoreboard.removeTeam(optionalTeam.get());
        }
    }

    @Listener(ignoreCancelled = true)
    public void onEntityDamage(DamageEntityEvent damageEntityEvent) {
        plugin.getLogger().debug("DAMAGE EVENT");

        Entity targetEntity = damageEntityEvent.getTargetEntity();
        Optional<Double> optionalHealth = targetEntity.get(Keys.HEALTH);
        Optional<Double> optionalMaxHealth = targetEntity.get(Keys.MAX_HEALTH);
        if (optionalHealth.isPresent() && optionalMaxHealth.isPresent()) {
            double currentHealth = optionalHealth.get();
            double maxHealth = optionalMaxHealth.get();

            Text healthMessage = generateHealthMessage(currentHealth, maxHealth);
            if (plugin.getConfig().isEnabledMob() && targetEntity.supports(Keys.DISPLAY_NAME)) {
                //mobs have only support for this
                targetEntity.offer(Keys.DISPLAY_NAME, healthMessage);
            } else if (targetEntity instanceof Player) {
                Player targetPlayer = (Player) targetEntity;
                String playerName = targetPlayer.getName();

                Scoreboard playerScoreboard = targetPlayer.getScoreboard();
                if (globalScoreboard.equals(playerScoreboard)) {
                    //does the player have still a global scoreboard -> we don't want to overrid eothers
                    setBelownameHealth(playerScoreboard, currentHealth, playerName);

                    setNametagHealth(playerScoreboard, playerName, healthMessage);
                }
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
                    objective.getScore(Texts.of(playerName)).setScore(displayedHealth);
                }
            }
        }
    }

    private void setNametagHealth(Scoreboard playerScoreboard, String playerName, Text healthMessage) {
        if (plugin.getConfig().isNametagHealth()) {
            //player nametag prefix and suffix
            Optional<Team> optionalTeam = playerScoreboard.getTeam(playerName);
            if (!optionalTeam.isPresent()) {
                playerScoreboard.addTeam(plugin.getGame().getRegistry().createBuilder(Team.Builder.class)
                        .name(playerName)
                        .color(TextColors.DARK_RED)
                        .build());
            }

            Team team = optionalTeam.get();
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

        TextBuilder textBuilder = Texts.builder();
        textBuilder.color(highlightColor);
        for (int i = 0; i < percent; i++) {
            textBuilder.append(Texts.of(displayChar));
        }

        textBuilder.append(Texts.of(TextColors.RESET));

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

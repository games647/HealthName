package com.github.games647.healthname;

import com.github.games647.healthname.config.Settings;
import com.google.inject.Inject;

import java.util.Optional;

import org.spongepowered.api.Server;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.HealEntityEvent;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.Team;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlots;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.text.LiteralText;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Text.Builder;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;

public class DamageListener {

    private final Settings settings;
    private final Server server;

    @Inject
    DamageListener(Settings settings, Server server) {
        this.settings = settings;
        this.server = server;
    }

    @Listener
    public void onHeal(HealEntityEvent healEvent) {
        Entity targetEntity = healEvent.getTargetEntity();
        Optional<Double> optionalHealth = targetEntity.get(Keys.HEALTH);
        Optional<Double> optionalMaxHealth = targetEntity.get(Keys.MAX_HEALTH);
        if (optionalHealth.isPresent() && optionalMaxHealth.isPresent()) {
            double newHealth = optionalHealth.get() + healEvent.getFinalHealAmount();
            updateHealth(optionalMaxHealth.get(), newHealth, targetEntity);
        }
    }

    @Listener
    public void onEntityDamage(DamageEntityEvent damageEntityEvent) {
        Entity targetEntity = damageEntityEvent.getTargetEntity();
        Optional<Double> optionalHealth = targetEntity.get(Keys.HEALTH);
        Optional<Double> optionalMaxHealth = targetEntity.get(Keys.MAX_HEALTH);
        if (optionalHealth.isPresent() && optionalMaxHealth.isPresent()) {
            double newHealth = optionalHealth.get() - damageEntityEvent.getFinalDamage();
            updateHealth(optionalMaxHealth.get(), newHealth, targetEntity);
        }
    }

    @Listener
    public void onEntityDeath(DestructEntityEvent.Death deathEntityEvent) {
        Living targetEntity = deathEntityEvent.getTargetEntity();

        if (!deathEntityEvent.isMessageCancelled()) {
            //clear entity properties
            if (targetEntity.getType() == EntityTypes.PLAYER) {
                Player targetPlayer = (Player) targetEntity;
                Scoreboard playerScoreboard = targetPlayer.getScoreboard();

                String playerName = targetPlayer.getName();
                playerScoreboard.getTeam(playerName).ifPresent(Team::unregister);
            } else if (targetEntity.supports(Keys.DISPLAY_NAME)) {
                targetEntity.remove(Keys.DISPLAY_NAME);
            }

            //clean message
            Text oldMessage = deathEntityEvent.getMessage();
            Builder newMessageBuilder = Text.builder(oldMessage, "");
            for (Text child : oldMessage.getChildren()) {
                if (child instanceof LiteralText) {
                    String content = ((LiteralText) child).getContent();
                    String newContent = content.replace(String.valueOf(settings.getConfig().getDisplayChar()), "");
                    newMessageBuilder.append(Text.builder(child, newContent).build());
                    continue;
                }
                
                newMessageBuilder.append(child);
            }

            deathEntityEvent.setMessage(newMessageBuilder.build());
        }
    }

    private void updateHealth(double maxHealth, double newHealth, Entity targetEntity) {
        Text healthMessage = generateHealthMessage(newHealth, maxHealth);
        if (targetEntity.getType() == EntityTypes.PLAYER) {
            Player targetPlayer = (Player) targetEntity;
            String playerName = targetPlayer.getName();

            Scoreboard playerScoreboard = targetPlayer.getScoreboard();
            if (playerScoreboard.equals(server.getServerScoreboard().orElse(null))) {
                //does the player have still a global scoreboard -> we don't want to overrid eothers
                setBelownameHealth(playerScoreboard, newHealth, playerName);

                setNametagHealth(playerScoreboard, playerName, healthMessage);
            }
        } else if (settings.getConfig().isEnabledMob() && targetEntity.supports(Keys.DISPLAY_NAME)) {
            //mobs have only support for this
            targetEntity.offer(Keys.DISPLAY_NAME, healthMessage);
        }
    }

    private void setBelownameHealth(Scoreboard playerScoreboard, double currentHealth, String playerName) {
        if (settings.getConfig().isBelowNameHealth()) {
            Optional<Objective> optionalObjective = playerScoreboard.getObjective(DisplaySlots.BELOW_NAME);
            optionalObjective.ifPresent(objective -> {
                //make sure we are targeting our own objective on the correct display slot
                if (objective.getName().equals(PomData.ARTIFACT_ID)) {
                    //we don't want to override other scoreboards
                    int displayedHealth = (int) Math.ceil(currentHealth);
                    objective.getOrCreateScore(Text.of(playerName)).setScore(displayedHealth);
                }
            });
        }
    }

    private void setNametagHealth(Scoreboard playerScoreboard, String playerName, Text healthMessage) {
        if (settings.getConfig().isNametagHealth()) {
            //player nametag prefix and suffix
            Team team;

            Optional<Team> optionalTeam = playerScoreboard.getTeam(playerName);
            if (optionalTeam.isPresent()) {
                team = optionalTeam.get();
            } else {
                team = Team.builder().name(playerName).build();
                playerScoreboard.registerTeam(team);
            }

            team.addMember(Text.of(playerName));
            team.setPrefix(healthMessage);
            team.setSuffix(healthMessage);
        }
    }

    private Text generateHealthMessage(double currentHealth, double maxHealth) {
        double percent = currentHealth * 100 / maxHealth;
        //10-steps -> 1=10% 2=20% 3=30%
        int steps = (int) Math.ceil(percent / 10);

        char displayChar = settings.getConfig().getDisplayChar();

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

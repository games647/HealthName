package com.github.games647.healthname;

import com.github.games647.healthname.config.Config;
import com.github.games647.healthname.config.Settings;
import com.google.inject.Inject;

import java.io.File;

import me.flibio.updatifier.Updatifier;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.critieria.Criteria;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlots;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.scoreboard.objective.displaymode.ObjectiveDisplayModes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

@Updatifier(repoOwner = "games647", repoName = "HealthName", version = "0.2.2")
@Plugin(id = "com.github.games647.healthname", name = "HealthName", version = "0.2.2"
        , url = "https://github.com/games647/HealthName"
        , description = "A Sponge minecraft server plugin for displaying the health above an entity.")
public class HealthName {

    private final PluginContainer pluginContainer;
    private final Logger logger;
    private final Game game;

    private Scoreboard globalScoreboard;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private File defaultConfigFile;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;

    private Settings configuration;

    @Inject
    public HealthName(Logger logger, PluginContainer pluginContainer, Game game) {
        this.logger = logger;
        this.pluginContainer = pluginContainer;
        this.game = game;
    }

    @Listener //During this state, the plugin gets ready for initialization. Logger and config
    public void onPreInit(GamePreInitializationEvent preInitEvent) {
        logger.info("Loading {} v{}", pluginContainer.getName(), pluginContainer.getVersion());

        configuration = new Settings(configManager, defaultConfigFile, this);
        configuration.load();
    }

    @Listener //During this state, the plugin should finish any work needed in order to be functional. Commands register + events
    public void onInit(GameInitializationEvent initEvent) {
        //register events
        game.getEventManager().registerListeners(this, new DamageListener(this));
        game.getEventManager().registerListeners(this, new ConnectionListener(this));
    }

    @Listener
    public void onGameStarted(GameStartedServerEvent gameStartedServerEvent) {
        //Scoreboards are loaded when the world is loaded so load it here
        if (getConfig().isNametagHealth()) {
            Objective objective = globalScoreboard.getObjective(pluginContainer.getUnqualifiedId()).orElse(null);
            if (objective != null) {
                //clear all old values
                globalScoreboard.removeObjective(objective);
            }

            objective = Objective.builder()
                    .name(pluginContainer.getUnqualifiedId())
                    .displayName(Text.of(TextColors.DARK_RED, "Health"))
                    .criterion(Criteria.DUMMY)
                    .objectiveDisplayMode(ObjectiveDisplayModes.INTEGER)
                    .build();
            globalScoreboard.addObjective(objective);
            globalScoreboard.updateDisplaySlot(objective, DisplaySlots.BELOW_NAME);
        }
    }

    public Scoreboard getGlobalScoreboard() {
        return globalScoreboard;
    }

    public Settings getConfigManager() {
        return configuration;
    }

    public Config getConfig() {
        return configuration.getConfiguration();
    }

    public PluginContainer getContainer() {
        return pluginContainer;
    }

    public Logger getLogger() {
        return logger;
    }

    public Game getGame() {
        return game;
    }
}

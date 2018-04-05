package com.github.games647.healthname;

import com.github.games647.healthname.config.Settings;
import com.google.inject.Inject;
import com.google.inject.Injector;

import java.util.Optional;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.critieria.Criteria;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlots;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.scoreboard.objective.displaymode.ObjectiveDisplayModes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

@Plugin(id = PomData.ARTIFACT_ID, name = PomData.NAME, version = PomData.VERSION,
        url = PomData.URL, description = PomData.DESCRIPTION)
public class HealthName {

    private final Logger logger;
    private final Injector injector;
    private final Settings configuration;

    @Inject
    public HealthName(Logger logger, Injector injector, Settings settings) {
        this.logger = logger;
        this.injector = injector;
        this.configuration = settings;
    }

    @Listener //During this state, the plugin gets ready for initialization. Logger and config
    public void onPreInit(GamePreInitializationEvent preInitEvent) {
        configuration.load();
    }

    @Listener
    //During this state, the plugin should finish any work needed in order to be functional. Commands register + events
    public void onInit(GameInitializationEvent initEvent) {
        //register events
        Sponge.getEventManager().registerListeners(this, injector.getInstance(DamageListener.class));
        Sponge.getEventManager().registerListeners(this, injector.getInstance(ConnectionListener.class));
    }

    @Listener
    //Scoreboards are loaded when the world is loaded so load it here
    public void onGameStarted(GameStartedServerEvent gameStartedServerEvent) {
        Optional<Scoreboard> serverScoreboard = Sponge.getServer().getServerScoreboard();
        if (serverScoreboard.isPresent()) {
            Scoreboard globalScoreboard = serverScoreboard.get();
            if (configuration.getConfig().isNametagHealth()) {
                globalScoreboard.getObjective(PomData.ARTIFACT_ID).ifPresent(globalScoreboard::removeObjective);

                Objective objective = Objective.builder()
                        .name(PomData.ARTIFACT_ID)
                        .displayName(Text.of(TextColors.DARK_RED, "Health"))
                        .criterion(Criteria.DUMMY)
                        .objectiveDisplayMode(ObjectiveDisplayModes.INTEGER)
                        .build();
                globalScoreboard.addObjective(objective);
                globalScoreboard.updateDisplaySlot(objective, DisplaySlots.BELOW_NAME);
            }
        } else {
            logger.warn("Global scoreboard couldn't be loaded");
        }
    }

    @Listener
    public void onGameStarted(GameStoppingServerEvent gameStoppingServerEvent) {
        Sponge.getServer().getServerScoreboard()
                .ifPresent(scoreboard -> scoreboard.getObjective(PomData.ARTIFACT_ID)
                        .ifPresent(scoreboard::removeObjective));
    }
}

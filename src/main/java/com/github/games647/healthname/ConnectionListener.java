package com.github.games647.healthname;

import com.github.games647.healthname.config.Settings;
import com.google.inject.Inject;

import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.Team;
import org.spongepowered.api.text.Text;

public class ConnectionListener {

    private final Settings settings;

    @Inject
    ConnectionListener(Settings settings) {
        this.settings = settings;
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join joinEvent) {
        Player player = joinEvent.getTargetEntity();
        //everyone should have a global scoreboard to see the health from others
        if (settings.getConfig().isNametagHealth() || settings.getConfig().isBelowNameHealth()) {
            //don't override the old scoreboard if the feature isn't needed
            Sponge.getServer().getServerScoreboard().ifPresent(player::setScoreboard);
        }
    }

    @Listener
    public void onQuit(ClientConnectionEvent.Disconnect disconnectEvent) {
        String playerName = disconnectEvent.getTargetEntity().getName();
        //Clean up scoreboard in order to prevent to big ones
        Optional<Scoreboard> serverScoreboard = Sponge.getServer().getServerScoreboard();
        if (serverScoreboard.isPresent()) {
            Scoreboard globalScoreboard = serverScoreboard.get();
            globalScoreboard.removeScores(Text.of(playerName));
            globalScoreboard.getTeam(playerName).ifPresent(Team::unregister);
        }
    }
}

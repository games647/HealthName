package com.github.games647.healthname;

import com.github.games647.healthname.HealthName;
import java.util.Optional;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scoreboard.Team;
import org.spongepowered.api.text.Text;

public class ConnectionListener {

    private final HealthName plugin;

    public ConnectionListener(HealthName plugin) {
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
}

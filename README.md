# HealhName

### Description

A Sponge minecraft server plugin for displaying the health above an entity.
This plugins makes it possible to display player or mob health above their character.

Attention: The plugin doesn't work yet. The DamageEvent from the SpongeAPI isn't implemented yet in the Sponge implementation.

Tested against:

	Sponge: sponge-1.8-1521-2.1-DEV-748

	Minecraft (Client): 1.8.8

### Config
    # Show health bar above mobs
    enabledMob=totp
    # Show the health of a player besides his nametag
    nametagHealth=true
    # Show the health of a player under his nametag (only the health number)
    belowNameHealth=true
    # What character should be used to display the health
    displayChar='|'

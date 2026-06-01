# Hoarder
[spigot page (download jar here)](NOT_UPLOADED_YET)

[support discord](https://discord.gg/QySnx7Vpuh)

---

Bukkit plugin that picks a random item each day, and players have to feed this item to the hoarder for money, the players that feed the most to the hoarder are rewarded with random prizes at the end of the event.

## Features
- Data is stored in SQLite database
- Highly configurable via yaml
    - Edit GUI layouts [example](src/main/resources/gui/mainmenu.yml)
    - Configurable via config options in [config.yml](src/main/resources/config.yml)
    - Edit chat messages with [messages.yml](src/main/resources/messages.yml)
- GUI
- Admin commands allowing you to manage the plugin & reload the yaml files
- Leaderboard
- Command system with brigadier

## Screenshots

![img.png](docs/mainmenu.png)

## Dependencies
- PlaceholderAPI (Not required, but the plugin does support placeholderAPI placeholders in the messages.yml & gui yml files if installed)

## Setup

1. Drag the plugin .jar file to your servers' `plugins/` directory
2. (Optional) If you would like to have the custom GUI background pngs, then you must merge the `resourcepack` folder into your servers resource pack)

## Ideas

- Make the furnace take an item at an interval and be shared between clan members, so it's more like a real furnace slowly burning away
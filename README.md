# Nebulon

A client-side Fabric mod for **Hypixel SkyBlock** with slayer helpers and quality-of-life features, all configured in one settings menu (`/nebulon`).

> **Early alpha.** Nebulon is far from finished; expect bugs and changes.
>
> **Made with AI:** Nebulon was developed with the help of AI (Anthropic's Claude).

## Requirements

| Component | Version |
|---|---|
| Minecraft | 26.2 |
| Fabric Loader | 0.19.5 or newer |
| Fabric API | 0.161.0+26.2 or newer |
| Java | 25 |

Client-side only: Nebulon does not need to be on the server and sends no packets of its own.

## Features

**Slayers**
- **Inferno Demonlord**: helpers for the Blaze slayer boss (hide the fire, attunement colours, fire pillar alerts).

**QOL**
- **Darkmode Mist**: a dark variant of the Mist in the Dwarven Mines (purely visual, works with and without Sodium).
- **Inventory QOL**: remove the recipe book and offhand slot, a custom dark inventory style, hide potion effects, **Inventory Display** (your inventory on the HUD) and **Slot Binding** (bind inventory slots to hotbar slots).
- **Enchant Parsing**: colours enchants by tier and shows the enchants still missing on weapons, tools and armour, with a live preview.
- **Auction Input**: a text box instead of the auction price sign, with an undercut mode.
- **Item Prices**: lowest BIN and bazaar prices in tooltips (from Coflnet).
- **Item Tweaks**, **Scrollable Tooltips**, **HUD Tweaks** (smaller fire, no damage tilt, no nausea), **Command Keybinds** and **Anti Mouse Reset**.

**Render**
- **Performance Display**, **Particles**, **Fullbright**, **Rarity** and **Held Item**.

**Settings**
- **Menu Customisation**, **Gui Editor** (move and resize displays) and **Protect Item**.

## Building

Requires JDK 25.

```
gradlew build
```

The mod jar is written to `build/libs/`.

## Credits

Nebulon builds on the work of these projects:

- [NoammAddons](https://github.com/Noamm9/NoammAddons) (CC0-1.0)
- [NotEnoughUpdates-REPO](https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO) (MIT, see `src/main/resources/assets/nebulon/skyblock/LICENSE-NotEnoughUpdates-REPO.txt`)
- [SkyHanni-REPO](https://github.com/hannibal002/SkyHanni-REPO) (MIT, see `src/main/resources/assets/nebulon/skyblock/LICENSE-SkyHanni-REPO.txt`)
- [Roboto](https://github.com/googlefonts/roboto-classic) (SIL Open Font License 1.1, see `src/main/resources/assets/nebulon/font/roboto_ofl.txt`)
- [Coflnet](https://sky.coflnet.com) and [lb.tricked.dev](https://lb.tricked.dev) (price data)
- [Fabric API](https://github.com/FabricMC/fabric) (Apache-2.0)

## License

Nebulon is licensed under the [MIT License](LICENSE). Third-party files keep their own licenses (see above).

## Disclaimer

Nebulon is not affiliated with Hypixel or Mojang. Use mods on Hypixel at your own risk and follow the [Hypixel rules](https://hypixel.net/rules).

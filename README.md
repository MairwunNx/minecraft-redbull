# Red Bull

I’m a fan of Red Bull—the best energy drinks in the world. They give me energy and a good mood for the whole day, so I dedicated this plugin to them.

<p align="center">
  <a href="https://github.com/MairwunNx/minecraft-redbull/releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/MairwunNx/minecraft-redbull?label=release"></a>
  <a href="https://modrinth.com/plugin/minecraft-redbull"><img alt="Modrinth" src="https://img.shields.io/modrinth/dt/minecraft-redbull?logo=modrinth&label=Modrinth&color=00AF5C"></a>
  <a href="https://hangar.papermc.io/MairwunNx/RedBull"><img alt="Hangar" src="https://img.shields.io/hangar/dt/RedBull?label=Hangar&color=1E88E5"></a>
  <img alt="Paper 26.3" src="https://img.shields.io/badge/Paper-26.3-F7CF0D">
  <img alt="Java 25" src="https://img.shields.io/badge/Java-25-E76F00">
  <a href="https://github.com/MairwunNx/minecraft-redbull/actions/workflows/build.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/MairwunNx/minecraft-redbull/build.yml?branch=master&label=build"></a>
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/github/license/MairwunNx/minecraft-redbull"></a>
</p>

🥫 **Red Bull** brings 26 real cans into survival on Paper servers. You brew taurine from bull bile, fill cans at a crafting table, hunt the rarest editions in chests, and fly on their wiiings. Four cans too fast, and your heart gives out.

[Русская версия](https://github.com/MairwunNx/minecraft-redbull/blob/master/README.RU.md)

<p align="center"><img alt="Every Red Bull can and ingredient in a chest" src="https://raw.githubusercontent.com/MairwunNx/minecraft-redbull/master/media/cans.png" width="704"></p>

[![Play on Moonlands: pure vanilla, hard mobs, AI, claims. minecraft.mairwunnx.com](https://raw.githubusercontent.com/MairwunNx/minecraft-redbull/master/media/og-adv-image.png)](https://minecraft.mairwunnx.com)

Red Bull runs on Moonlands, my hard survival server with friends. Join at `minecraft.mairwunnx.com`, kill a few cows, and earn your first can.

## What you get

- 🥫 **26 real cans:** Original, Sugarfree, Zero, and the Editions from Red to Spring, each with its own buffs, a flavour, and a slogan.
- 🧪 **A chemistry chain:** cows drop bull bile, a brewing stand turns it into crude taurine, a furnace evaporates that into taurine, and a crafting table fills an empty can.
- 🗺️ **Cans to find:** most Editions also turn up in chests of their biomes, and four legends turn up nowhere else.
- 🪽 **Real wiiings:** a can with wings launches you, and the empty can in your hand glides like an elytra for a few seconds.
- 💓 **A price for it:** caffeine builds up. Two cans make you jittery, three bring a crash, and the fourth within two minutes kills you.
- 📖 **A collection:** `/redbull` shows every can you have held and question marks for the rest.
- 🌍 **Every player reads their own language:** English, Russian, German, Spanish, and Portuguese come in the resource pack, which the plugin sends on join.

## How to make a can

1. **Bull bile.** An adult cow or mooshroom you kill drops it with a 5% chance, 2% more per level of Looting.
2. **Crude taurine.** Brew bull bile into water bottles: one bile, three bottles.
3. **Taurine.** Smelt crude taurine in a furnace.
4. **An empty can.** Four iron nuggets and a copper nugget make two.
5. **The can.** Fill it at a crafting table:

   ```
   taurine   dye         taurine
   flavour   empty can   flavour
   sugar     water       sugar
   ```

   Sugarfree cans leave the sugar out. Every recipe sits in your recipe book.

## The cans

Every can carries the same caffeine, as the real ones do; they differ in buffs and wings. A can is harder to make than a potion, so its buffs last longer: 8 minutes for the crafted ones, 12 for the Editions you also find in chests, and 16 for the legends with a yellow name like a bottle o’ enchanting. Healing lasts 16 seconds at most and Strength II 3 minutes, or nobody could die. A can with sugar also feeds you a little. Chests hold a can with a 15% chance unless the table says otherwise.

| Can | Flavour and dye, or where to find it | Buffs | Wings |
|---|---|---|---|
| Energy Drink | sugar, blue dye | Speed II and Haste I for 8 min | 8 s |
| Sugarfree | honeycomb, light blue dye | Speed I for 8 min | 6 s |
| Zero | taurine, light blue dye | Speed II and Haste I for 8 min | 8 s |
| Red Edition | melon slice, red dye | Strength I for 8 min | 8 s |
| Red Edition Sugarfree | melon slice, red dye | Strength I for 8 min | 6 s |
| Pink Edition Sugarfree | sweet berries, pink dye | Regeneration I for 90 s | 6 s |
| Green Edition | cactus flower, green dye; swamps and lush caves | Luck and Speed I for 12 min | 8 s |
| Cherry Edition | pink petals, red dye; cherry groves | Luck and Speed II for 12 min | 12 s |
| Ice Edition | packed ice, light blue dye; ice spikes and frozen oceans | Resistance I for 12 min | 10 s |
| Winter Edition, Fuji Apple & Ginger | apple, orange dye; snowy taigas and groves | Absorption II for 12 min | 8 s |
| Winter Edition, Pistachio & Forest Fruits | sweet berries, lime dye; snowy plains and beaches | Regeneration II for 45 s | 8 s |
| Yellow Edition | sunflower, yellow dye; deserts | Speed I and Jump Boost I for 12 min | 10 s |
| Tropical Edition | cocoa beans, yellow dye; jungles | Jump Boost II for 12 min | 12 s |
| Amber Edition | sweet berries, orange dye; savannas | Haste II for 12 min | 8 s |
| Amber Edition Sugarfree | sweet berries, orange dye; savannas | Haste I for 12 min | 6 s |
| Peach Edition | pink tulip, pink dye; plains | Health Boost I for 12 min | 8 s |
| Lilac Edition Sugarfree | lilac, magenta dye; forests | Absorption I for 12 min | 6 s |
| Purple Edition | chorus fruit, purple dye; dark forests and pale gardens | Invisibility and Speed I for 12 min | 8 s |
| Sea Blue Edition | sea pickle, cyan dye; oceans | Dolphin’s Grace and Conduit Power for 12 min | — |
| Coconut Edition | milk bucket, white dye; beaches | Saturation, Speed I for 12 min | 8 s |
| Coconut Edition Sugarfree | milk bucket, white dye; beaches | Water Breathing for 12 min | 6 s |
| Blue Edition | blue orchid, blue dye; the deep dark | Night Vision and Speed I for 12 min | 8 s |
| Ruby Edition | only in Nether chests, 8% | Fire Resistance for 16 min, Strength II for 3 min | — |
| Summer Edition | only in badlands chests, 5% | Speed III for 16 min | 15 s |
| Glacier Edition | only in chests on frozen and jagged peaks and snowy slopes, 5% | Slow Falling and Resistance I for 16 min | 15 s |
| Spring Edition | only in cherry grove chests, 3% | Slow Falling for 16 min | 30 s |

## Your collection

Type `/redbull` to open your collection: every can you have ever held, crafted, found, or been given, and an empty can with question marks for the rest. The title counts how many of the 26 you have. Admins see every can and ingredient there and take one with a click, or a stack with Shift.

## Wiiings

A can with wings throws you upward as you finish it and leaves a winged can in your hand. Jump while in the air, and you glide as with an elytra for as many seconds as the can gives. The winged can works only in your main hand, and switching away drops you. After a minute, or once its flight runs out, it crumples.

## Crumpled cans

Every can you finish leaves a crumpled can in its own colours. They stack to 64, and a furnace melts each back into two iron nuggets, twice as fast in a blast furnace. Iron comes back; for a new can you need only a copper nugget more.

## Caffeine

Each can adds 30 caffeine, and caffeine wears off by one point every six seconds you play.

| Caffeine | What happens |
|---|---|
| 0–49 | Nothing. |
| 50–74 | Jitters: you get hungry faster and hear your heart now and then. |
| 75–100 | The crash: when a can’s buffs end, three minutes at most, Slowness II and Weakness I follow for a minute. Your heart pounds. |
| Over 100 | Your heart gives out. No totem saves you. |

A bucket of milk takes 20 caffeine away. Death takes all of it. Bull bile and crude taurine are drinkable too, but you won’t like them, and both within a minute put you in bull mode.

## Get started

1. Download the jar from [Modrinth](https://modrinth.com/plugin/minecraft-redbull), [Hangar](https://hangar.papermc.io/MairwunNx/RedBull), or [GitHub](https://github.com/MairwunNx/minecraft-redbull/releases/latest/download/redbull.jar) and put it in `plugins/` of a Paper 26.3 server on Java 25.
2. Restart the server. Players get the resource pack when they join; without it the cans show the missing texture.
3. Type `/redbull`, take a can, and drink it.

`config.yml` turns the pack, the overdose, and the wings on or off, points the pack to your own host, and scales the bile and chest chances. `/redbull give <item> [player] [amount]` and `/redbull caffeine [player]` need `redbull.admin`.

## Credits

- The can art grew from a fan pixel-art Red Bull can shared on Reddit; I don’t know its author.
- Taurine’s texture is Mekanism’s salt, © Aidan C. Brady and contributors, under the [MIT license](LICENSES/Mekanism.txt).
- Red Bull and “Red Bull gives you wings” are trademarks of Red Bull GmbH. This is a fan project, not made or endorsed by Red Bull.

## Related projects

- **[Moonlands](https://minecraft.mairwunnx.com)**—the hard-survival server where all of these run.
- **[WorldAPI](https://github.com/MairwunNx/minecraft-worldapi)**—REST and WebSocket API to a Paper server's world, players, and stats.
- **[Minecraft MCP](https://github.com/MairwunNx/minecraft-mcp)**—lets an AI client read and run a server through WorldAPI.
- **[Allay](https://github.com/allay-ai/minecraft)**—an AI helper in the game: ask with `/ask`, right in the chat.
- **[Allay Server](https://github.com/allay-ai/server)**—the backend behind Allay.
- **[Mo’Joins](https://github.com/MairwunNx/mojoins)**—join and quit messages in each player's language.
- **[Mo’Afks](https://github.com/MairwunNx/moafks)**—a safe AFK mode that doesn't block the night.
- **[Mo’Bosses](https://github.com/MairwunNx/mobosses)**—monsters that spawn as bosses with classes, abilities, and loot.
- **[minecraft-exporter](https://github.com/MairwunNx/minecraft-exporter)**—Prometheus metrics from WorldAPI.
- **[minecraft-grafana](https://github.com/MairwunNx/minecraft-grafana)**—the Grafana dashboard for WorldAPI.

> 🫡 Made by Pavel Erokhin (Павел Ерохин), aka mairwunnx.

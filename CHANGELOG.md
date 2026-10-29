# Changelog

The “Release” workflow takes the notes for a version from its section here, unless you type them into its `changelog` field. The same notes go to GitHub, Modrinth, and Hangar.

## 1.5.0

- 🪽 **The winged can shows what it has left on Paper:** its durability bar is all its wings now. It drains over the can’s flight time plus a minute while you stand or climb, and over its flight time alone while you glide, so the bar never sits full while a hidden timer runs out. A chime and the action bar warn you shortly before it crumples, and its tooltip says when it will without a flight.
- 🛬 **A softer landing:** when the wings run out mid-glide, 6 seconds of Slow Falling set you down instead of 5, on every platform.

## 1.4.3

- 📦 **Built on the newest platforms:** Paper API 26.3 build 223, NeoForge 26.3.0.99, Fabric Loader 0.20.1, and Fabric API 0.165.0, with Gradle 9.8.2 and kotlinx-serialization 1.12.0. Nothing changes in the game.

## 1.4.2

- 🧪 **Bull bile goes into the brewing stand on Paper:** the ingredient slot never took it, by click, shift-click, number key, or hopper, so no crude taurine, taurine, or can could be made. A brewing stand takes an ingredient by its item alone, the player’s client too, and bile was a potion underneath, like the bottles. It’s an ominous bottle underneath now, still drawn and drunk as before, so one bile brews three water bottles into crude taurine on a vanilla client. Bile from older versions turns into the new one when you join, open a container, or pick it up. Fabric and NeoForge took it all along.
- 🎁 **Cans turn up in chests a third less often**, on every platform: an Edition in 10% of its biome’s chests (15% before), Ruby in 5% (8%), Summer and Glacier in 3.5% (5%), and Spring in 2% (3%). `loot.chests` in the settings still scales them all.

## 1.4.1

- 🏷️ **Allay names the ingredients in your language:** Allay’s AI got Red Bull’s items only by their ids and translated them itself, so in Russian taurine came out as «таврин». Red Bull now hands it the names of bull bile, crude taurine, taurine, and the empty can, and each can’s flavour, in the language Allay talks to the player in (Allay 6.17 or newer; older Allay gets the game’s language). The Paper jar also carries every language, so Allay’s own item search finds the names there too.

## 1.4.0

- 🤖 **Allay knows the recipes:** with [Allay](https://github.com/allay-ai/minecraft) 6.12 or newer on the server, its AI asks Red Bull itself how a can is made and where it turns up: the crafting grid of each can, the bile, brewing, and furnace on the way to taurine, a legend’s chests, a trader’s price. It works on Paper, Fabric, and NeoForge alike, and Red Bull runs as before without Allay.
- 🧰 **One core for both mods:** Fabric and NeoForge now run the same code for the cans, caffeine, wings, loot, the collection, and `/redbull`, so every fix reaches both at once. For players nothing changes: the same items, recipes, loot, advancements, settings, and jar names.

## 1.3.0

- 🧳 **A wandering trader brings cans:** one or two different ones besides its own wares, 6 emeralds for a crafted can and 10 for an Edition, three times each. It never sells a legend.
- 🎣 **Cans among fishing junk:** about one junk catch in ten is an empty can or a crumpled can of any flavour.
- 🪶 **Feathers trail behind you** as you glide on a can’s wings, for everyone nearby to see.
- 🏆 **Six advancements** in a Red Bull tab of their own: your first can, Gives You Wings, Sugar Pit, On the Edge (90 caffeine or more, and alive), Legend (any of the four), and Collector (all 26). A collection from before counts the moment you join. On Paper they come in a data pack inside the jar and read in your language with the resource pack, in English without it.
- 📄 **The Hangar page** follows the store page with every release now.

All of it works the same on Paper, Fabric, and NeoForge.

## 1.2.1

- 🥫 **An empty can is one can now:** four iron nuggets and a copper nugget make one, not two. A crumpled can still smelts into two iron nuggets, so two of them and a copper nugget make a new one.

## 1.2.0

- 🧩 **Red Bull is a mod now, for NeoForge and Fabric 26.3:** the same 26 cans, recipes, chests, caffeine, wings, and `/redbull` collection as the plugin, in `redbull-neoforge-1.2.0.jar` and `redbull-fabric-1.2.0.jar`. Install it on the client and the server. The NeoForge mod needs nothing else, since Kotlin ships inside; the Fabric mod needs Fabric API and Fabric Language Kotlin.
- 🥫 **Real items, no resource pack:** in the mods every can, crumpled can, and ingredient is an item of its own, with its texture, name, and five languages inside (Mexico and Portugal read Spain’s and Brazil’s texts). `/give` hands any of them out by id, like `redbull:original`, and they sit in their own Red Bull tab of the creative inventory.
- ☕ **Caffeine is an effect in the mods:** Caffeine I while calm, II in the jitters, III in the crash, and its timer says when the caffeine wears off, so you see when the next can is safe. Milk takes 20 caffeine and every effect, then Caffeine shows what’s left; `/effect clear` takes it off until the next heartbeat.
- 🪽 **Wiiings is an effect in the mods:** a can with wings still throws you upward, then gives Wiiings for as many seconds as the can has. Jump in the air, and you glide as with an elytra, wearing nothing out, not even the elytra you have on; sculk still hears you fly. The can crumples like any other, so the mods have no winged can. When the wings run out, or milk takes them mid-flight, “Your wings are gone.” Taken on the ground, they go quietly.
- 🕊️ **Longer wings everywhere:** 16 seconds from a crafted can, 32 from an Edition you also find in chests, and 64 from a legend. On Paper the winged can lives that long plus a minute.
- 🍯 **Sugar or sugarfree is a real choice now,** everywhere: a sugarfree can takes two honeycombs instead of the sugar, feeds nothing, and never crashes. A can with sugar feeds a little but sinks you into a sugar pit when its buffs end: Hunger I for 30 seconds and Slowness I for 15, announced in gold; only it brings the crash when caffeine runs too high.
- 🦇 **Zero is the strong one without sugar:** the Energy Drink’s full buffs and 42 seconds of wings, flavoured with phantom membranes instead of two extra taurine. The plain Sugarfree is flavoured with sugar cane.
- 🛬 **A soft landing everywhere:** when the wings run out mid-glide, 5 seconds of Slow Falling set you down. On Paper that’s the winged can’s last flight.
- 😵 **Caffeine keeps you awake:** with any caffeine in you, the bed says “The caffeine won’t let you sleep.” Everywhere.
- 📖 **`/redbull` in the mods too:** the collection for every player, which survives death; operators take any item from the same menu, and use `/redbull give` and `/redbull caffeine`.
- 🗺️ **Cans in every chest of their biomes in the mods,** the game’s, another mod’s, or a data pack’s, as on Paper.
- 💀 **An overdose kills through a totem in the mods too,** and chat says the heart “gave out after one Red Bull too many,” even when it stops in the middle of a fight.
- 📦 **The Paper jar is `redbull-paper-1.2.0.jar` now,** beside the mods’ jars; `redbull.jar` still fetches the newest one from GitHub. On Modrinth the versions read `1.2.0+paper`, `1.2.0+fabric`, and `1.2.0+neoforge`.
- ⚙️ **`config/redbull.toml` in the mods:** the overdose, the wings, and the loot multipliers, as in Paper’s `config.yml` without the pack. Fabric writes the defaults when the file is missing and reads it on start and on `/reload`; NeoForge reads it again whenever you save it.

## 1.1.1

- 🛒 **On Modrinth and Hangar.** The first release there, with everything 1.1.0 brought: the `/redbull` collection, buffs of 8, 12, and 16 minutes, crumpled cans, and the legends’ yellow names.

## 1.1.0

- 📖 **`/redbull` opens a collection** for every player: each can they have ever held, crafted, found, or been given, and an empty can with question marks for the rest. The title counts how many of the 26 they have. A can joins the collection the moment it reaches the player.
- 🧰 **Admins take anything from the same menu:** every can and ingredient, one with a click or a stack with Shift.
- ⏳ **Buffs last far longer than potions:** 8 minutes for the crafted cans, 12 for the Editions also found in chests, and 16 for the legends. Healing lasts 16 seconds at most and Strength II 3 minutes. Wings stay as they were.
- 🥫 **Every drink leaves a crumpled can** in its flavour’s colours, instead of an empty one. Crumpled cans stack to 64 and smelt into two iron nuggets, faster in a blast furnace. A winged can crumples too once its flight is over.
- ✨ **The legends have a yellow name** like a bottle o’ enchanting: Ruby, Summer, Glacier, and Spring. The other cans are named in white.
- 💓 **The crash is a minute long** and comes when a can’s buffs end, three minutes after the drink at most.

## 1.0.0

- 🥫 **26 Red Bull cans** for Paper 26.3: Energy Drink, Sugarfree, Zero, and 23 Editions, each with its own texture, buffs, flavour, and slogan.
- 🧪 **A way to make them:** cows drop bull bile, a brewing stand turns it into crude taurine, a furnace evaporates that into taurine, and a crafting table fills an empty can. Every recipe sits in the recipe book.
- 🗺️ **Cans in chests** by the biome the chest stands in; Ruby, Summer, Glacier, and Spring turn up nowhere else.
- 🪽 **Wiiings:** a can with wings launches the player and leaves a winged can that glides from the main hand.
- 💓 **Caffeine:** jitters, a crash, and death by overdose above 100. Milk helps a little.
- 🌍 **Names and texts** in English, Russian, German, Spanish, and Portuguese, in a resource pack the plugin sends on join.
- `/redbull give` and `/redbull caffeine` for admins; `config.yml` switches the pack, the overdose, and the wings, and scales the loot.

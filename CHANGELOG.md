# Changelog

The “Release” workflow takes the notes for a version from its section here, unless you type them into its `changelog` field. The same notes go to GitHub, Modrinth, and Hangar.

## 1.2.0

- 🧩 **Red Bull is a mod now, for NeoForge and Fabric 26.3:** the same 26 cans, recipes, chests, caffeine, wings, and `/redbull` collection as the plugin, in `redbull-neoforge-1.2.0.jar` and `redbull-fabric-1.2.0.jar`. Install it on the client and the server. The NeoForge mod needs nothing else, since Kotlin ships inside; the Fabric mod needs Fabric API and Fabric Language Kotlin.
- 🥫 **Real items, no resource pack:** in the mods every can, crumpled can, and ingredient is an item of its own, with its texture, name, and five languages inside (Mexico and Portugal read Spain’s and Brazil’s texts). `/give` hands any of them out by id, like `redbull:original`, and they sit in their own Red Bull tab of the creative inventory.
- ☕ **Caffeine is an effect in the mods:** Caffeine I while calm, II in the jitters, III in the crash, and its timer says when the caffeine wears off, so you see when the next can is safe. Milk takes 20 caffeine and every effect, then Caffeine shows what’s left; `/effect clear` takes it off until the next heartbeat.
- 🪽 **Wiiings is an effect in the mods:** a can with wings still throws you upward, then gives Wiiings for as many seconds as the can has. Jump in the air, and you glide as with an elytra, wearing nothing out, not even the elytra you have on; sculk still hears you fly. The can crumples like any other, so the mods have no winged can. When the wings run out, or milk takes them mid-flight, “Your wings are gone.” Taken on the ground, they go quietly.
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

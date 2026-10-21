# Changelog

The “Release” workflow takes the notes for a version from its section here, unless you type them into its `changelog` field. The same notes go to GitHub, Modrinth, and Hangar.

## 1.2.0

- 🧩 **Red Bull for NeoForge 26.3:** the same 26 cans, recipes, chests, caffeine, wings, and `/redbull` collection as a mod, `redbull-neoforge-1.2.0.jar`. The items are real items with their textures and names built in, so players need no resource pack, and Kotlin ships inside the jar.
- 🥤 **A caffeine meter** on NeoForge: a row of ten small cans above the hunger bar while there’s caffeine in you, trembling in the jitters and shaking in the crash.
- ⚙️ NeoForge’s settings live in `config/redbull.toml`: the overdose, the wings, and the loot multipliers, as in Paper’s `config.yml`.

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

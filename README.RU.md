# Red Bull

Я фанат Red Bull — лучших энергетиков в мире. Они дают мне силы и настроение на весь день, поэтому я посвятил им этот плагин.

<p align="center">
  <a href="https://github.com/MairwunNx/minecraft-redbull/releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/MairwunNx/minecraft-redbull?label=release"></a>
  <a href="https://modrinth.com/plugin/minecraft-redbull"><img alt="Modrinth" src="https://img.shields.io/modrinth/dt/minecraft-redbull?logo=modrinth&label=Modrinth&color=00AF5C"></a>
  <a href="https://hangar.papermc.io/MairwunNx/RedBull"><img alt="Hangar" src="https://img.shields.io/hangar/dt/RedBull?label=Hangar&color=1E88E5"></a>
  <img alt="Paper 26.3" src="https://img.shields.io/badge/Paper-26.3-F7CF0D">
  <img alt="Fabric 26.3" src="https://img.shields.io/badge/Fabric-26.3-DBD0B4">
  <img alt="NeoForge 26.3" src="https://img.shields.io/badge/NeoForge-26.3-D7742F">
  <img alt="Java 25" src="https://img.shields.io/badge/Java-25-E76F00">
  <a href="https://github.com/MairwunNx/minecraft-redbull/actions/workflows/build.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/MairwunNx/minecraft-redbull/build.yml?branch=master&label=build"></a>
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/github/license/MairwunNx/minecraft-redbull"></a>
</p>

🥫 **Red Bull** добавляет в выживание 26 настоящих банок — плагином для серверов Paper или модом для Fabric и NeoForge. Вы варите таурин из бычьей желчи, наполняете банки на верстаке, ищете самые редкие выпуски в сундуках и летаете на их крыльях. Четыре банки слишком быстро — и сердце не выдержит.

[English version](https://github.com/MairwunNx/minecraft-redbull/blob/master/README.md)

<p align="center"><img alt="Все банки Red Bull и ингредиенты в сундуке" src="https://raw.githubusercontent.com/MairwunNx/minecraft-redbull/master/media/cans.png" width="704"></p>

[![Играйте на Moonlands: чистая ваниль, сложные мобы, ИИ, приваты. ru.minecraft.mairwunnx.com](https://raw.githubusercontent.com/MairwunNx/minecraft-redbull/master/media/og-adv-image.ru.png)](https://minecraft.mairwunnx.com)

Red Bull работает на Moonlands — моём сервере со сложным выживанием для друзей. Заходите на `ru.minecraft.mairwunnx.com` из России или на `minecraft.mairwunnx.com` откуда угодно, добудьте желчь и заработайте первую банку.

## Возможности

- 🥫 **26 настоящих банок:** Original, Sugarfree, Zero и выпуски от Red до Spring, у каждой свои баффы, вкус и слоган.
- 🧪 **Химия:** коровы роняют бычью желчь, зельеварка превращает её в сырой таурин, печь выпаривает из него таурин, а верстак наполняет пустую банку.
- 🗺️ **Банки можно найти:** большинство выпусков лежит и в сундуках своих биомов, а четыре легенды — только там.
- 🪽 **Настоящие крылья:** банка с крыльями подбрасывает вас, и вы планируете, как на элитрах, от 16 секунд до минуты с лишним.
- 💓 **У всего есть цена:** кофеин копится. Две банки — дрожь, три — отходняк, четвёртая за две минуты убивает. А ещё кофеин не даёт уснуть.
- 📖 **Коллекция:** `/redbull` показывает все банки, которые у вас были, и знаки вопроса на месте остальных.
- 🌍 **Каждый читает на своём языке:** английский, русский, немецкий, испанский и португальский — в ресурс-паке, который плагин отправляет при входе, или внутри модов.

## Как это выглядит

<p align="center"><img alt="Банка Red Bull в руке и хотбар с банками, бутылками, таурином и мятой банкой" src="https://raw.githubusercontent.com/MairwunNx/minecraft-redbull/master/media/screenshots/world.png" width="640"></p>
<p align="center"><img alt="Инвентарь с эффектами «Кофеин II» и «Крылья»" src="https://raw.githubusercontent.com/MairwunNx/minecraft-redbull/master/media/screenshots/effects.png" width="640"></p>
<p align="center"><img alt="Коллекция /redbull: 8 из 26 банок, остальные со знаками вопроса" src="https://raw.githubusercontent.com/MairwunNx/minecraft-redbull/master/media/screenshots/collection.png" width="640"></p>

## Как сделать банку

1. **Бычья желчь.** Взрослая корова или грибная корова, убитая вами, роняет её с шансом 5 %, и ещё 2 % за каждый уровень «Добычи».
2. **Сырой таурин.** Сварите желчь с бутылками воды: одна желчь — три бутылки.
3. **Таурин.** Переплавьте сырой таурин в печи.
4. **Пустая банка.** Четыре железных самородка и медный дают две банки.
5. **Банка.** Наполните её на верстаке:

   ```
   таурин   краситель     таурин
   вкус     пустая банка  вкус
   сахар    вода          сахар
   ```

   В банках без сахара его места пустые. Все рецепты есть в книге рецептов.

## Банки

Кофеина в каждой банке поровну, как в настоящих, а различаются они баффами и крыльями. Банку сделать труднее, чем зелье, поэтому её баффы держатся дольше: 8 минут у крафтовых, 12 у выпусков, которые лежат и в сундуках, и 16 у легенд с жёлтым названием, как у пузырька опыта. Лечение длится не дольше 16 секунд, а Сила II — не дольше 3 минут, иначе никто не смог бы умереть. Банка с сахаром ещё и немного утоляет голод. Шанс найти банку в сундуке — 15 %, если в таблице не сказано иначе.

| Банка | Вкус и краситель или где найти | Баффы | Крылья |
|---|---|---|---|
| Energy Drink | сахар, синий краситель | Скорость II и Спешка I на 8 мин | 16 с |
| Sugarfree | соты, голубой краситель | Скорость I на 8 мин | 16 с |
| Zero | таурин, голубой краситель | Скорость II и Спешка I на 8 мин | 16 с |
| Red Edition | ломтик арбуза, красный краситель | Сила I на 8 мин | 16 с |
| Red Edition Sugarfree | ломтик арбуза, красный краситель | Сила I на 8 мин | 16 с |
| Pink Edition Sugarfree | сладкие ягоды, розовый краситель | Регенерация I на 90 с | 16 с |
| Green Edition | цветок кактуса, зелёный краситель; болота и пышные пещеры | Удача и Скорость I на 12 мин | 32 с |
| Cherry Edition | розовые лепестки, красный краситель; вишнёвые рощи | Удача и Скорость II на 12 мин | 32 с |
| Ice Edition | плотный лёд, голубой краситель; ледяные шипы и замёрзшие океаны | Сопротивление I на 12 мин | 32 с |
| Winter Edition, яблоко фудзи и имбирь | яблоко, оранжевый краситель; снежная тайга и рощи | Поглощение II на 12 мин | 32 с |
| Winter Edition, фисташка и лесные ягоды | сладкие ягоды, лаймовый краситель; снежные равнины и пляжи | Регенерация II на 45 с | 32 с |
| Yellow Edition | подсолнух, жёлтый краситель; пустыни | Скорость I и Прыгучесть I на 12 мин | 32 с |
| Tropical Edition | какао-бобы, жёлтый краситель; джунгли | Прыгучесть II на 12 мин | 32 с |
| Amber Edition | сладкие ягоды, оранжевый краситель; саванны | Спешка II на 12 мин | 32 с |
| Amber Edition Sugarfree | сладкие ягоды, оранжевый краситель; саванны | Спешка I на 12 мин | 32 с |
| Peach Edition | розовый тюльпан, розовый краситель; равнины | Прилив здоровья I на 12 мин | 32 с |
| Lilac Edition Sugarfree | сирень, пурпурный краситель; леса | Поглощение I на 12 мин | 32 с |
| Purple Edition | плод хоруса, фиолетовый краситель; тёмные леса и бледные сады | Невидимость и Скорость I на 12 мин | 32 с |
| Sea Blue Edition | морской огурец, бирюзовый краситель; океаны | Грация дельфина и Сила источника на 12 мин | — |
| Coconut Edition | ведро молока, белый краситель; пляжи | Насыщение, Скорость I на 12 мин | 32 с |
| Coconut Edition Sugarfree | ведро молока, белый краситель; пляжи | Подводное дыхание на 12 мин | 32 с |
| Blue Edition | синяя орхидея, синий краситель; глубокая тьма | Ночное зрение и Скорость I на 12 мин | 32 с |
| Ruby Edition | только в сундуках Нижнего мира, 8 % | Огнестойкость на 16 мин, Сила II на 3 мин | — |
| Summer Edition | только в сундуках пустошей, 5 % | Скорость III на 16 мин | 64 с |
| Glacier Edition | только в сундуках ледяных и зубчатых пиков и снежных склонов, 5 % | Медленное падение и Сопротивление I на 16 мин | 64 с |
| Spring Edition | только в сундуках вишнёвых рощ, 3 % | Медленное падение на 16 мин | 64 с |

## Коллекция

`/redbull` открывает вашу коллекцию: все банки, которые у вас хоть раз были — сделанные, найденные или подаренные, а на месте остальных пустая банка со знаками вопроса. В заголовке видно, сколько из 26 вы собрали. Админы видят там все банки и ингредиенты и берут их кликом, а с Shift — целым стаком.

## Крылья

Банка с крыльями подбрасывает вас, когда вы её допиваете. Прыгните в воздухе — и вы планируете, как на элитрах, столько секунд, сколько даёт банка. Если крылья кончатся в воздухе, медленное падение мягко опустит вас на землю.

- **В модах** банка даёт эффект «Крылья» с иконкой и таймером среди других эффектов. Надетые элитры при этом не изнашиваются.
- **На Paper** плагин не может добавить эффект, поэтому в руке остаётся крылатая банка. Она планирует только из основной руки: смените слот, и вы упадёте. Когда полёт кончится или через минуту после того, как он мог бы кончиться, она сминается.

## Мятые банки

Каждая выпитая банка оставляет мятую банку своих цветов. Они складываются по 64, а печь переплавляет каждую обратно в два железных самородка, плавильная печь — вдвое быстрее. Железо возвращается, и для новой банки нужен только медный самородок.

## Кофеин

Каждая банка добавляет 30 кофеина, а за каждые шесть секунд игры уходит одно очко.

| Кофеин | Что происходит |
|---|---|
| 0–49 | Ничего. |
| 50–74 | Дрожь: вы быстрее голодаете и иногда слышите своё сердце. |
| 75–100 | Отходняк: когда баффы банки закончатся, но не позже чем через три минуты, на минуту придут Медлительность II и Слабость I. Сердце колотится. |
| Больше 100 | Сердце не выдерживает. Тотем не спасёт. |

Пока в крови есть хоть немного кофеина, лечь спать нельзя. В модах эффект «Кофеин» показывает стадию уровнем и время, через которое кофеин выветрится; на Paper остаются сердцебиение и предупреждения над хотбаром.

Ведро молока убирает 20 кофеина, смерть — весь. Бычью желчь и сырой таурин тоже можно выпить, но вам не понравится, а обе бутылки за минуту включат режим быка.

## Как начать

**Сервер Paper.** Скачайте плагин с [Modrinth](https://modrinth.com/plugin/minecraft-redbull), [Hangar](https://hangar.papermc.io/MairwunNx/RedBull) или [GitHub](https://github.com/MairwunNx/minecraft-redbull/releases/latest/download/redbull-paper.jar) и положите в `plugins/` сервера Paper 26.3 на Java 25. Игроки получат ресурс-пак при входе; без него банки показываются как отсутствующая текстура. В `config.yml` можно включить или выключить пак, передозировку и крылья, указать свой адрес для пака и изменить шансы желчи и сундуков.

**Fabric.** Поставьте мод с [Modrinth](https://modrinth.com/mod/minecraft-redbull) или [GitHub](https://github.com/MairwunNx/minecraft-redbull/releases/latest/download/redbull-fabric.jar) вместе с [Fabric API](https://modrinth.com/mod/fabric-api) и [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) для Minecraft 26.3 — и на клиент, и на сервер.

**NeoForge.** Поставьте мод с [Modrinth](https://modrinth.com/mod/minecraft-redbull) или [GitHub](https://github.com/MairwunNx/minecraft-redbull/releases/latest/download/redbull-neoforge.jar) для NeoForge 26.3 — и на клиент, и на сервер. Больше ничего не нужно.

Модам не нужен ресурс-пак: текстуры и языки лежат внутри. Их настройки — в `config/redbull.toml`, с теми же переключателями, что у плагина, кроме пака.

Дальше введите `/redbull`, возьмите банку и выпейте её. Командам `/redbull give <предмет> [игрок] [количество]` и `/redbull caffeine [игрок]` нужно право `redbull.admin` на Paper и права оператора в модах.

## Благодарности

- Рисунок банки вырос из фанатского пиксель-арта Red Bull с Reddit; автора я не знаю.
- Текстура таурина — соль из Mekanism, © Aidan C. Brady и участники, по [лицензии MIT](LICENSES/Mekanism.txt).
- Red Bull и «Red Bull окрыляет» — товарные знаки Red Bull GmbH. Это фанатский проект, Red Bull его не делал и не одобрял.

## Другие проекты

- **[Moonlands](https://minecraft.mairwunnx.com)** — сервер со сложным выживанием, где всё это работает.
- **[WorldAPI](https://github.com/MairwunNx/minecraft-worldapi)** — REST и WebSocket API к миру, игрокам и статистике сервера Paper.
- **[Minecraft MCP](https://github.com/MairwunNx/minecraft-mcp)** — даёт ИИ-клиенту читать сервер и управлять им через WorldAPI.
- **[Allay](https://github.com/allay-ai/minecraft)** — ИИ-помощник в игре: спросите через `/ask` прямо в чате.
- **[Allay Server](https://github.com/allay-ai/server)** — бэкенд Allay.
- **[Mo’Joins](https://github.com/MairwunNx/mojoins)** — сообщения о входе и выходе на языке каждого игрока.
- **[Mo’Afks](https://github.com/MairwunNx/moafks)** — безопасный режим AFK, который не мешает ночи пройти.
- **[Mo’Bosses](https://github.com/MairwunNx/mobosses)** — монстры, которые появляются боссами с классами, способностями и добычей.
- **[minecraft-exporter](https://github.com/MairwunNx/minecraft-exporter)** — метрики Prometheus из WorldAPI.
- **[minecraft-grafana](https://github.com/MairwunNx/minecraft-grafana)** — дашборд Grafana для WorldAPI.

> 🫡 Сделал Павел Ерохин (Pavel Erokhin), он же mairwunnx.

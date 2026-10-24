package ru.mairwunnx.redbull.check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Container;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.mairwunnx.redbull.core.Can;
import ru.mairwunnx.redbull.core.CansKt;
import ru.mairwunnx.redbull.core.Ids;
import ru.mairwunnx.redbull.core.Recipe;
import ru.mairwunnx.redbull.paper.Items;

/**
 * Red Bull's way to a can in a test world's blocks, as a player's hoppers would walk it: bile from a hopper into a
 * brewing stand's ingredient slot, three water bottles brewed into crude taurine, a furnace evaporating it into
 * taurine, and a crafting grid filling an empty can. A hopper goes through the same check of the ingredient slot as a
 * click, a shift-click, and the client do. A stand beside it keeps a water bottle and dirt out of that slot. It logs
 * “Red Bull check passed” or what failed; `paper_smoke.py` looks for the line.
 */
public final class RedBullCheck extends JavaPlugin {
  private static final int Y = 200;

  private final List<Step> steps = new ArrayList<>();
  private final List<Block> blocks = new ArrayList<>();
  private BukkitTask task;
  private int step;
  private int waited;

  private record Step(String name, int ticks, BooleanSupplier done, Runnable then) {}

  @Override
  public void onEnable() {
    getServer().getScheduler().runTask(this, this::start);
    // For a test client that can't open a block itself: the block's own window, where its clicks go the game's way.
    registerCommand("redbullcheck", (source, args) -> {
      Player player = getServer().getPlayerExact(args[0]);
      Block block = player.getWorld().getBlockAt(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
      if (block.getType() == Material.CRAFTING_TABLE) player.openWorkbench(block.getLocation(), true);
      else player.openInventory(((Container) block.getState()).getInventory());
    });
  }

  private void start() {
    World world = getServer().getWorlds().getFirst();
    world.setChunkForceLoaded(0, 0, true);

    Block stand = world.getBlockAt(2, Y, 2);
    stand.setType(Material.BREWING_STAND);
    blocks.add(stand);
    inventory(hopper(world.getBlockAt(2, Y + 1, 2), BlockFace.DOWN)).addItem(Items.INSTANCE.bile());
    inventory(hopper(world.getBlockAt(3, Y, 2), BlockFace.WEST))
      .addItem(Items.INSTANCE.waterBottle(), Items.INSTANCE.waterBottle(), Items.INSTANCE.waterBottle(), new ItemStack(Material.BLAZE_POWDER));

    Block other = world.getBlockAt(6, Y, 2);
    other.setType(Material.BREWING_STAND);
    blocks.add(other);
    Inventory refused = inventory(hopper(world.getBlockAt(6, Y + 1, 2), BlockFace.DOWN));
    refused.addItem(Items.INSTANCE.waterBottle(), new ItemStack(Material.DIRT));

    Block furnace = world.getBlockAt(10, Y, 2);
    furnace.setType(Material.FURNACE);
    blocks.add(furnace);
    Inventory furnaceHopper = inventory(hopper(world.getBlockAt(10, Y + 1, 2), BlockFace.DOWN));

    steps.add(new Step("a hopper puts bull bile into the ingredient slot", 100,
      () -> is(brewer(stand).getIngredient(), Ids.BULL_BILE), () -> {}));
    steps.add(new Step("one bile brews three water bottles into crude taurine", 600, () -> {
      BrewerInventory brewer = brewer(stand);
      return brewer.getIngredient() == null && is(brewer.getItem(0), Ids.CRUDE_TAURINE)
        && is(brewer.getItem(1), Ids.CRUDE_TAURINE) && is(brewer.getItem(2), Ids.CRUDE_TAURINE);
    }, () -> {
      BrewerInventory brewer = brewer(stand);
      furnaceHopper.addItem(brewer.getItem(0), brewer.getItem(1));
      brewer.setItem(0, null);
      brewer.setItem(1, null);
      furnaceInventory(furnace).setFuel(new ItemStack(Material.COAL));
    }));
    steps.add(new Step("the ingredient slot keeps a water bottle and dirt out", 0, () -> {
      BrewerInventory brewer = brewer(other);
      return brewer.getIngredient() == null && refused.contains(Material.POTION) && refused.contains(Material.DIRT);
    }, () -> {}));
    steps.add(new Step("a furnace evaporates crude taurine into taurine", 900, () -> {
      ItemStack result = furnaceInventory(furnace).getResult();
      return is(result, Ids.TAURINE) && result.getAmount() == 2;
    }, () -> {}));
    steps.add(new Step("a crafting grid makes an empty can and fills it", 0, () -> craft(world, furnaceInventory(furnace).getResult()), () -> {}));

    // Brewing takes 20 seconds and smelting 10 each; the check needn't wait for them. A sprint would skip block entities.
    getServer().dispatchCommand(getServer().getConsoleSender(), "tick rate 400");
    task = getServer().getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
  }

  private void tick() {
    Step current = steps.get(step);
    if (current.done().getAsBoolean()) {
      getLogger().info("Passed: " + current.name());
      current.then().run();
      waited = 0;
      if (++step == steps.size()) finish(null);
    } else if (++waited > current.ticks()) {
      finish(current.name());
    }
  }

  private void finish(String failed) {
    task.cancel();
    getServer().dispatchCommand(getServer().getConsoleSender(), "tick rate 20");
    if (failed == null) getLogger().info("Red Bull check passed: " + steps.size() + " steps");
    else getLogger().severe("Red Bull check failed: " + failed + "; " + describe());
  }

  /** What every block of the check holds, for a failure's line. */
  private String describe() {
    List<String> lines = new ArrayList<>();
    for (Block block : blocks) {
      Inventory inventory = ((Container) block.getState()).getInventory();
      List<String> items = Arrays.stream(inventory.getContents())
        .map(it -> it == null ? "-" : Objects.requireNonNullElse(Items.INSTANCE.id(it), it.getType().getKey().getKey()))
        .toList();
      lines.add(block.getType().getKey().getKey() + "@" + block.getX() + "," + block.getY() + " " + items);
    }
    return String.join("; ", lines);
  }

  private boolean craft(World world, ItemStack taurine) {
    ItemStack nugget = new ItemStack(Material.IRON_NUGGET);
    ItemStack[] grid = {nugget, null, nugget, nugget, null, nugget, null, new ItemStack(Material.COPPER_NUGGET), null};
    ItemStack emptyCan = getServer().craftItem(grid, world);
    if (!is(emptyCan, Ids.EMPTY_CAN)) return false;

    Can can = CansKt.getCANS().stream().filter(it -> it.getRecipe() != null).findFirst().orElseThrow();
    Recipe recipe = Objects.requireNonNull(can.getRecipe());
    ItemStack sweetener = new ItemStack(material(can.getSweetener()));
    ItemStack flavour = Ids.TAURINE_ITEM.equals(recipe.getFlavour()) ? Items.INSTANCE.taurine() : new ItemStack(material(recipe.getFlavour()));
    ItemStack one = taurine.asOne();
    ItemStack[] filled = {
      one, new ItemStack(material(recipe.getDye())), one.clone(),
      flavour, emptyCan, flavour.clone(),
      sweetener, Items.INSTANCE.waterBottle(), sweetener.clone(),
    };
    ItemStack result = getServer().craftItem(filled, world);
    getLogger().info("Crafted " + Items.INSTANCE.id(result) + " from " + Arrays.stream(filled).map(it -> it == null ? "-" : it.getType().name()).toList());
    return is(result, can.getId());
  }

  private static Material material(String id) {
    return Objects.requireNonNull(Material.matchMaterial(id), id);
  }

  private static boolean is(ItemStack item, String id) {
    return id.equals(Items.INSTANCE.id(item));
  }

  private static Block hopper(Block block, BlockFace facing) {
    block.setType(Material.HOPPER);
    Directional data = (Directional) block.getBlockData();
    data.setFacing(facing);
    block.setBlockData(data);
    return block;
  }

  private Inventory inventory(Block block) {
    blocks.add(block);
    return ((Container) block.getState()).getInventory();
  }

  private static BrewerInventory brewer(Block block) {
    return ((BrewingStand) block.getState()).getInventory();
  }

  private static FurnaceInventory furnaceInventory(Block block) {
    return ((Furnace) block.getState()).getInventory();
  }
}

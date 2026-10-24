package ru.mairwunnx.redbull.modcore

import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader
import net.minecraft.world.item.Items
import net.minecraft.world.item.trading.ItemCost
import net.minecraft.world.item.trading.MerchantOffer
import ru.mairwunnx.redbull.core.Trades
import kotlin.random.Random

/**
 * A wandering trader's cans: one or two different ones, never a legend, at core's prices. The game's trades are data
 * read from three fixed trade sets, and neither loader has an event for them, so `WanderingTraderMixin` calls this once
 * the trader has stocked the rest.
 */
object Trader {
  /** As a villager's first trades pay: one point of experience. */
  private const val XP = 1

  /** As the wandering trader's own offers. */
  private const val PRICE_MULTIPLIER = 0.05f

  @JvmStatic
  fun stock(trader: WanderingTrader) {
    for (can in Trades.pick(Random(trader.random.nextLong()))) {
      trader.offers.add(MerchantOffer(ItemCost(Items.EMERALD, Trades.price(can)), RedBullItems.can(can), Trades.MAX_USES, XP, PRICE_MULTIPLIER))
    }
  }
}

package com.nebulon.config;

/** "Item Prices" (Render): lowest BIN / bazaar prices from Coflnet in item tooltips. */
public class ItemPricesSettings {
	public boolean enabled = false;
	/** "Show Lowest BIN": the cheapest Buy It Now auction of auction house items. */
	public boolean showLowestBin = true;
	/** "Show Bazaar": instant buy and instant sell price of bazaar items. */
	public boolean showBazaar = true;
	/** "Show Stack Total": for stacks of more than one item, the price of the whole stack too. */
	public boolean showStackTotal = true;
}

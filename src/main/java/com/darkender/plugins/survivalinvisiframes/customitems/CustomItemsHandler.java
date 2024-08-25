package com.darkender.plugins.survivalinvisiframes.customitems;

import com.darkender.plugins.survivalinvisiframes.SurvivalInvisiframes;
import org.bukkit.Bukkit;
import uk.co.notnull.CustomItems.api.CustomItems;

public final class CustomItemsHandler {;
	private final SurvivalInvisiFramesItemProvider provider;
	private final CustomItems customItems = (CustomItems) Bukkit.getPluginManager().getPlugin("CustomItems");

	public CustomItemsHandler(SurvivalInvisiframes plugin) {
		provider = new SurvivalInvisiFramesItemProvider(plugin);
		assert customItems != null;
		customItems.getItemManager().registerProvider(provider);
	}

	public void unregisterProvider() {
		assert customItems != null;
		customItems.getItemManager().unregisterProvider(provider);
	}
}

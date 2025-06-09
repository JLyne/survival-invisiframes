package com.darkender.plugins.survivalinvisiframes.creativeitemfilter;

import com.darkender.plugins.survivalinvisiframes.SurvivalInvisiframes;
import org.bukkit.Bukkit;
import org.hurricanegames.creativeitemfilter.CreativeItemFilter;
import org.hurricanegames.creativeitemfilter.handler.component.ItemComponentPopulatorFactory;

public class CreativeItemFilterHandler {

	public CreativeItemFilterHandler(SurvivalInvisiframes plugin) {
		boolean cifEnabled = Bukkit.getPluginManager().isPluginEnabled("CreativeItemFilter");

		if(!cifEnabled) {
			return;
		}

		ItemComponentPopulatorFactory factory = ((CreativeItemFilter) Bukkit.getPluginManager().getPlugin("CreativeItemFilter"))
				.getComponentPopulatorFactory();

		factory.addPopulator(new SurvivalInvisiFramesComponentPopulator(plugin));
	}
}

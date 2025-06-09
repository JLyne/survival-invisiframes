package com.darkender.plugins.survivalinvisiframes.creativeitemfilter;

import com.darkender.plugins.survivalinvisiframes.SurvivalInvisiframes;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.hurricanegames.creativeitemfilter.CreativeItemFilterConfiguration;
import org.hurricanegames.creativeitemfilter.handler.component.ItemComponentPopulator;
import org.jetbrains.annotations.NotNull;

public class SurvivalInvisiFramesComponentPopulator implements ItemComponentPopulator {
	private final SurvivalInvisiframes plugin;

	public SurvivalInvisiFramesComponentPopulator(SurvivalInvisiframes plugin) {
		this.plugin = plugin;
	}

	@Override
	public void populateComponents(@NotNull ItemStack oldItem, @NotNull ItemStack newItem,
			CreativeItemFilterConfiguration creativeItemFilterConfiguration) {
		if(oldItem.getPersistentDataContainer().has(plugin.invisibleKey, PersistentDataType.BYTE)) {
			newItem.editPersistentDataContainer(
					pdc -> pdc.set(plugin.invisibleKey, PersistentDataType.BYTE, (byte) 1));
		}
	}
}

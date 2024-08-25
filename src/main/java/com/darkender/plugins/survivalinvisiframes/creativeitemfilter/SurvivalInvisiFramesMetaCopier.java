package com.darkender.plugins.survivalinvisiframes.creativeitemfilter;

import com.darkender.plugins.survivalinvisiframes.SurvivalInvisiframes;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.hurricanegames.creativeitemfilter.CreativeItemFilterConfiguration;
import org.hurricanegames.creativeitemfilter.handler.meta.MetaCopier;

public class SurvivalInvisiFramesMetaCopier implements MetaCopier<ItemMeta> {
	private final SurvivalInvisiframes plugin;

	public SurvivalInvisiFramesMetaCopier(SurvivalInvisiframes plugin) {
		this.plugin = plugin;
	}

	@Override
	public void copyValidMeta(CreativeItemFilterConfiguration creativeItemFilterConfiguration, ItemMeta oldMeta, ItemMeta newMeta) {
		if(oldMeta.getPersistentDataContainer().has(plugin.invisibleKey, PersistentDataType.BYTE)) {
			newMeta.getPersistentDataContainer().set(plugin.invisibleKey, PersistentDataType.BYTE, (byte) 1);
		}
	}

	@Override
	public Class<ItemMeta> getMetaClass() {
		return ItemMeta.class;
	}
}

package com.darkender.plugins.survivalinvisiframes.customitems;

import com.darkender.plugins.survivalinvisiframes.SurvivalInvisiframes;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import uk.co.notnull.CustomItems.api.items.CustomItem;
import uk.co.notnull.CustomItems.api.items.provider.CustomItemProvider;

import java.util.List;

public final class SurvivalInvisiFramesItemProvider implements CustomItemProvider {
	private final SurvivalInvisiframes plugin;
	private final CustomItem invisibleItem;
	private final CustomItem glowInvisibleItem;

	public SurvivalInvisiFramesItemProvider(SurvivalInvisiframes plugin) {
		this.plugin = plugin;

		invisibleItem = CustomItem.builder().id(new NamespacedKey(plugin, "invisible_item_frame"))
			.displayName(Component.text("Invisible Item Frame"))
			.generator((player, quantity) -> {
				ItemStack frame = plugin.generateInvisibleItemFrame(false);
				frame.setAmount(Math.max(quantity, frame.getMaxStackSize()));
				return frame;
			})
			.build();

		glowInvisibleItem = CustomItem.builder().id(new NamespacedKey(plugin, "glow_invisible_item_frame"))
			.displayName(Component.text("Glow Invisible Item Frame"))
			.generator((player, quantity) -> {
				ItemStack frame = plugin.generateInvisibleItemFrame(true);
				frame.setAmount(Math.max(quantity, frame.getMaxStackSize()));
				return frame;
			})
			.build();
	}

	public List<CustomItem> provideItems() {
		return List.of(invisibleItem, glowInvisibleItem);
	}

	public CustomItem identifyItem(ItemStack itemStack) {
		if(!plugin.isFrameItem(itemStack)) {
			return null;
		}

		if(!itemStack.getPersistentDataContainer().has(plugin.invisibleKey, PersistentDataType.BYTE)) {
			return null;
		}

		return itemStack.getType() == Material.GLOW_ITEM_FRAME ? glowInvisibleItem : invisibleItem;
	}
}

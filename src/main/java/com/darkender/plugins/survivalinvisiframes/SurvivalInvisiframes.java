package com.darkender.plugins.survivalinvisiframes;

import com.darkender.plugins.survivalinvisiframes.creativeitemfilter.CreativeItemFilterHandler;
import com.darkender.plugins.survivalinvisiframes.customitems.CustomItemsHandler;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public final class SurvivalInvisiframes extends JavaPlugin implements Listener {
	private final MiniMessage miniMessage = MiniMessage.miniMessage();

	private ShapedRecipe frameRecipe;
	private ShapedRecipe glowRecipe;

	private final NamespacedKey recipeKey = new NamespacedKey(this, "frame");
	private final NamespacedKey glowRecipeKey = new NamespacedKey(this, "glow_frame");
	private final NamespacedKey vanillaGlowRecipe = NamespacedKey.minecraft("glow_item_frame");

	public final NamespacedKey invisibleKey = new NamespacedKey(this, "invisible");
	private final Set<DroppedFrameLocation> droppedFrames = new HashSet<>();

	private boolean framesGlow;
	private boolean firstLoad = true;

    private Component invisibleFrameName;
    private List<Component> invisibleFrameLore;
    private Component glowInvisibleFrameName;
    private List<Component> glowInvisibleFrameLore;

	private CustomItemsHandler customItemsHandler;
	private CreativeItemFilterHandler creativeItemFilterHandler;

	@Override
	public void onEnable() {
		reload();

		getServer().getPluginManager().registerEvents(this, this);

		LifecycleEventManager<Plugin> manager = getLifecycleManager();
		manager.registerEventHandler(LifecycleEvents.COMMANDS, event ->
				new InvisiFramesCommand(this, event.registrar()));
	}

	@Override
	public void onDisable() {
		// Remove added recipes on plugin disable
		removeRecipes();
	}

	@EventHandler
	public void onPluginEnable(PluginEnableEvent event) {
		switch (event.getPlugin().getName()) {
			case "CustomItems" -> {
				getLogger().info("Registering CustomItems provider");
				customItemsHandler = new CustomItemsHandler(this);
			}
			case "CreativeItemFilter" -> {
				getLogger().info("Initialising CreativeItemFilter handler");
				creativeItemFilterHandler = new CreativeItemFilterHandler(this);
			}
		}
	}

	@EventHandler
	public void onPluginDisable(PluginDisableEvent event) {
		switch (event.getPlugin().getName()) {
			case "CustomItems" -> {
				if (customItemsHandler != null) {
					getLogger().info("Disabling CustomItems provider");
					customItemsHandler = null;
				}
			}
			case "CreativeItemFilter" -> {
				if (creativeItemFilterHandler != null) {
					getLogger().info("Disabling WorldGuard handler");
					creativeItemFilterHandler = null;
				}
			}
		}
	}

	private void removeRecipes() {
		if (frameRecipe != null) {
			Bukkit.removeRecipe(frameRecipe.getKey());
		}

		if (glowRecipe != null) {
			Bukkit.removeRecipe(glowRecipe.getKey());
		}
	}

	public void reload() {
		saveDefaultConfig();
		reloadConfig();
		getConfig().options().copyDefaults(true);
		saveConfig();
		removeRecipes();

		if (firstLoad) {
			firstLoad = false;
			framesGlow = !getConfig().getBoolean("empty-item-frames-glow");
		}
		if (getConfig().getBoolean("empty-item-frames-glow") != framesGlow) {
			framesGlow = getConfig().getBoolean("empty-item-frames-glow");
			forceRecheck();
		}

		invisibleFrameName = getConfig().getRichMessage("invisible-frame.name",
														Component.text("Invisible Item Frame"));
		glowInvisibleFrameName = getConfig().getRichMessage("glow-invisible-frame.name",
														Component.text("Glow Invisible Item Frame"));
		invisibleFrameLore = getConfig().getStringList("invisible-frame.lore")
				.stream().map(miniMessage::deserialize).toList();
		glowInvisibleFrameLore = getConfig().getStringList("glow-invisible-frame.lore")
				.stream().map(miniMessage::deserialize).toList();

		ItemStack invisibleFrame = generateInvisibleItemFrame(false);
		invisibleFrame.setAmount(8);

		ItemStack glowInvisibleFrame = generateInvisibleItemFrame(true);
		glowInvisibleFrame.setAmount(8);

		List<ItemStack> invisibilityPotions = (List<ItemStack>) getConfig().getList("recipes", Collections.emptyList());

		frameRecipe = new ShapedRecipe(recipeKey, invisibleFrame);
		frameRecipe.shape("FFF", "FPF", "FFF");
		frameRecipe.setIngredient('F', Material.ITEM_FRAME);
		frameRecipe.setIngredient('P', new RecipeChoice.ExactChoice(invisibilityPotions.toArray(new ItemStack[0])));

		glowRecipe = new ShapedRecipe(glowRecipeKey, glowInvisibleFrame);
		glowRecipe.shape("FFF", "FPF", "FFF");
		glowRecipe.setIngredient('F', Material.GLOW_ITEM_FRAME);
		glowRecipe.setIngredient('P', new RecipeChoice.ExactChoice(invisibilityPotions.toArray(new ItemStack[0])));

		Bukkit.addRecipe(frameRecipe);
		Bukkit.addRecipe(glowRecipe);
	}

	public void forceRecheck() {
		for (World world : Bukkit.getWorlds()) {
			for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) {
				if (isInvisibleItemFrame(frame)) {
					if (frame.getItem().getType() == Material.AIR && framesGlow) {
						frame.setGlowing(true);
						frame.setVisible(true);
					} else if (frame.getItem().getType() != Material.AIR) {
						frame.setGlowing(false);
						frame.setVisible(false);
					}
				}
			}
		}
	}

	private boolean isFrameEntity(Entity entity) {
		return entity instanceof ItemFrame;
	}

	public boolean isFrameItem(ItemStack item) {
		return item != null && (item.getType() == Material.ITEM_FRAME || item.getType() == Material.GLOW_ITEM_FRAME);
	}

	public boolean isInvisibleItemFrame(ItemStack item) {
		return item != null
				&& (item.getType() == Material.ITEM_FRAME || item.getType() == Material.GLOW_ITEM_FRAME)
				&& item.getPersistentDataContainer().has(invisibleKey, PersistentDataType.BYTE);
	}

	public boolean isInvisibleItemFrame(Hanging frame) {
		return frame instanceof ItemFrame
				&& frame.getPersistentDataContainer().has(invisibleKey, PersistentDataType.BYTE);
	}

	public ItemStack generateInvisibleItemFrame(boolean glowing) {
		ItemStack item = new ItemStack(glowing ? Material.GLOW_ITEM_FRAME : Material.ITEM_FRAME, 1);
		ItemMeta meta = item.getItemMeta();
		meta.getPersistentDataContainer().set(invisibleKey, PersistentDataType.BYTE, (byte) 1);
		item.setItemMeta(meta);

		item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
		item.setData(DataComponentTypes.ITEM_NAME, glowing ? glowInvisibleFrameName : invisibleFrameName);
		List<Component> lore = glowing ? glowInvisibleFrameLore : invisibleFrameLore;

		if(!lore.isEmpty()) {
		    item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
		}

		return item;
	}

	@EventHandler(ignoreCancelled = true)
	private void onCraft(PrepareItemCraftEvent event) {
		HumanEntity crafter = event.getView().getPlayer();

		if (event.getRecipe() instanceof CraftingRecipe recipe) {
			NamespacedKey key = recipe.getKey();
			getLogger().info(String.valueOf(key));

			// Permission check for new crafting recipes
			if ((key.equals(recipeKey) || key.equals(glowRecipeKey)) &&
					!crafter.hasPermission("survivalinvisiframes.craft")) {
				event.getInventory().setResult(null);
			} else if (key.equals(vanillaGlowRecipe)) { // Vanilla glow recipe with invisible frame
				for (ItemStack i : event.getInventory().getMatrix()) {
					if (i == null || i.getType() == Material.AIR) {
						continue;
					}

					if (isInvisibleItemFrame(i)) {
						boolean hasPermission = crafter.hasPermission("survivalinvisiframes.craft");
						event.getInventory().setResult(hasPermission ? generateInvisibleItemFrame(true) : null);
					}
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	private void onHangingPlace(HangingPlaceEvent event) {
		// Get the frame item that the player placed
		ItemStack frame = event.getItemStack();
		Player p = event.getPlayer();

		if (p == null || !isFrameItem(frame)) {
			return;
		}

		// If the frame item has the invisible tag, make the placed item frame invisible
		if (isInvisibleItemFrame(frame)) {
			if (!p.hasPermission("survivalinvisiframes.place")) {
				event.setCancelled(true);
				return;
			}
			ItemFrame itemFrame = (ItemFrame) event.getEntity();
			if (framesGlow) {
				itemFrame.setVisible(true);
				itemFrame.setGlowing(true);
			} else {
				itemFrame.setVisible(false);
			}
			event.getEntity().getPersistentDataContainer().set(invisibleKey, PersistentDataType.BYTE, (byte) 1);
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	private void onHangingBreak(HangingBreakEvent event) {
		if (!isFrameEntity(event.getEntity()) || !isInvisibleItemFrame(event.getEntity())) {
			return;
		}

		// This is the dumbest possible way to change the drops of an item frame
		// Apparently, there's no api to change the dropped item
		// So this sets up a bounding box that checks for items near the frame and converts them
		DroppedFrameLocation droppedFrameLocation = new DroppedFrameLocation(event.getEntity().getLocation());
		droppedFrames.add(droppedFrameLocation);
		droppedFrameLocation.setRemoval((new BukkitRunnable() {
			@Override
			public void run() {
				droppedFrames.remove(droppedFrameLocation);
			}
		}).runTaskLater(this, 20L));
	}

	@EventHandler
	private void onItemSpawn(ItemSpawnEvent event) {
		Item item = event.getEntity();
		if (!isFrameItem(item.getItemStack())) {
			return;
		}

		Iterator<DroppedFrameLocation> iter = droppedFrames.iterator();
		while (iter.hasNext()) {
			DroppedFrameLocation droppedFrameLocation = iter.next();
			if (droppedFrameLocation.isFrame(item)) {
				ItemStack frame = generateInvisibleItemFrame(item.getItemStack().getType() == Material.GLOW_ITEM_FRAME);
				event.getEntity().setItemStack(frame);

				droppedFrameLocation.getRemoval().cancel();
				iter.remove();
				break;
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onChangeItemFrame(PlayerItemFrameChangeEvent event) {
		ItemFrame frame = event.getItemFrame();

		if (!isInvisibleItemFrame(event.getItemFrame())) {
			return;
		}

		if (event.getAction() == PlayerItemFrameChangeEvent.ItemFrameChangeAction.PLACE) {
			frame.setGlowing(false);
			frame.setVisible(false);
			return;
		}

		if (event.getAction() == PlayerItemFrameChangeEvent.ItemFrameChangeAction.REMOVE) {
			frame.setGlowing(framesGlow);
			frame.setVisible(true);
		}
	}
}

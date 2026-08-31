package com.darkender.plugins.survivalinvisiframes;

import com.darkender.plugins.survivalinvisiframes.creativeitemfilter.CreativeItemFilterHandler;
import com.darkender.plugins.survivalinvisiframes.customitems.CustomItemsHandler;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.PotionContents;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
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
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@SuppressWarnings("UnstableApiUsage")
public final class SurvivalInvisiframes extends JavaPlugin implements Listener {
	private final MiniMessage miniMessage = MiniMessage.miniMessage();

	private ShapedRecipe frameRecipe;
	private ShapedRecipe glowRecipe;
	private ShapelessRecipe alternateGlowRecipe;

	private final NamespacedKey recipeKey = new NamespacedKey(this, "frame");
	private final NamespacedKey glowRecipeKey = new NamespacedKey(this, "glow_frame");
	private final NamespacedKey alternateGlowRecipeKey = new NamespacedKey(this, "glow_frame_alternate");

	public final NamespacedKey invisibleKey = new NamespacedKey(this, "invisible");
	private final Set<DroppedFrameLocation> droppedFrames = new HashSet<>();

	private boolean framesGlow;
	private boolean firstLoad = true;

    private Component invisibleFrameName;
    private NamespacedKey invisibleFrameModel;
    private List<Component> invisibleFrameLore;
    private Component glowInvisibleFrameName;
    private NamespacedKey glowInvisibleFrameModel;
    private List<Component> glowInvisibleFrameLore;

	private CustomItemsHandler customItemsHandler;
	private CreativeItemFilterHandler creativeItemFilterHandler;

	@Override
	public void onEnable() {
		initConfig();
		reload();

		getServer().getPluginManager().registerEvents(this, this);

		LifecycleEventManager<@NotNull Plugin> manager = getLifecycleManager();
		manager.registerEventHandler(LifecycleEvents.COMMANDS, event ->
				new InvisiFramesCommand(this, event.registrar()));
	}

	@Override
	public void onDisable() {
		// Remove added recipes on plugin disable
		removeRecipes();
		if (customItemsHandler != null) {
			customItemsHandler.unregisterProvider();
		}
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

	@EventHandler
	public void onServerResourcesReloaded(ServerResourcesReloadedEvent event) {
		reload();
	}

	private void initConfig() {
		getConfig().options().copyDefaults(true);
		getConfig().options().parseComments(true);

		getConfig().addDefault("empty-item-frames-glow", true);
		getConfig().addDefault("invisible-frame.item-name", "Invisible Item Frame");
		getConfig().addDefault("invisible-frame.item-model", "minecraft:item_frame");
		getConfig().addDefault("invisible-frame.lore", Collections.emptyList());
		getConfig().addDefault("glow-invisible-frame.item-name", "Glow Invisible Item Frame");
		getConfig().addDefault("glow-invisible-frame.item-model", "minecraft:glow_item_frame");
		getConfig().addDefault("glow-invisible-frame.lore", Collections.emptyList());

		ItemStack defaultRecipeItem = ItemType.LINGERING_POTION.createItemStack();
		defaultRecipeItem.setData(DataComponentTypes.POTION_CONTENTS,
								  PotionContents.potionContents().potion(PotionType.INVISIBILITY));

		ItemStack defaultRecipeItem2 = ItemType.LINGERING_POTION.createItemStack();
		defaultRecipeItem2.setData(DataComponentTypes.POTION_CONTENTS,
								   PotionContents.potionContents().potion(PotionType.LONG_INVISIBILITY));

		getConfig().addDefault("recipes", List.of(defaultRecipeItem, defaultRecipeItem2));
		saveConfig();
		reloadConfig();

		getConfig().setComments("empty-item-frames-glow", List.of(
				"Whether or not to enable invisible item frames glowing when there's no item in them",
				"This will also make them visible when there's no item in them"));
		getConfig().setComments("recipes",
								Collections.singletonList("The items which can be in the center of the recipe"));
		saveConfig();
	}

	private void removeRecipes() {
		if (frameRecipe != null) {
			Bukkit.removeRecipe(frameRecipe.getKey());
		}

		if (glowRecipe != null) {
			Bukkit.removeRecipe(glowRecipe.getKey());
		}
		
		if (alternateGlowRecipe != null) {
			Bukkit.removeRecipe(alternateGlowRecipe.getKey());
		}
	}

	public void reload() {
		reloadConfig();
		removeRecipes();

		if (firstLoad) {
			firstLoad = false;
			framesGlow = !getConfig().getBoolean("empty-item-frames-glow");
		}
		if (getConfig().getBoolean("empty-item-frames-glow") != framesGlow) {
			framesGlow = getConfig().getBoolean("empty-item-frames-glow");
			forceRecheck();
		}

		invisibleFrameName = getConfig().getRichMessage("invisible-frame.item-name",
														Component.text("Invisible Item Frame"));
		glowInvisibleFrameName = getConfig().getRichMessage("glow-invisible-frame.item-name",
														Component.text("Glow Invisible Item Frame"));
		invisibleFrameModel = NamespacedKey.fromString(getConfig().getString("invisible-frame.item-model", ""));
		glowInvisibleFrameModel = NamespacedKey.fromString(
				getConfig().getString("glow-invisible-frame.item-model", ""));
		invisibleFrameLore = getConfig().getStringList("invisible-frame.lore")
				.stream().map(miniMessage::deserialize).toList();
		glowInvisibleFrameLore = getConfig().getStringList("glow-invisible-frame.lore")
				.stream().map(miniMessage::deserialize).toList();

		ItemStack invisibleFrame = generateInvisibleItemFrame(false);
		invisibleFrame.setAmount(8);

		ItemStack glowInvisibleFrame = generateInvisibleItemFrame(true);
		glowInvisibleFrame.setAmount(8);

		List<ItemStack> invisibilityPotions = (List<ItemStack>) getConfig().getList("recipes", Collections.emptyList());

		RecipeChoice frameChoice;
		RecipeChoice glowChoice;
		RecipeChoice invisibleFrameChoice;
		
		Predicate<ItemStack> framePredicate = (ItemStack item) ->
				ItemType.ITEM_FRAME.equals(item.getType().asItemType()) && item.getPersistentDataContainer().isEmpty();
		Predicate<ItemStack> glowPredicate = (ItemStack item) ->
				ItemType.GLOW_ITEM_FRAME.equals(item.getType().asItemType()) && item.getPersistentDataContainer().isEmpty();
		Predicate<ItemStack> invisibleItemFramePredicate = (ItemStack item) -> 
				item.getType().asItemType() == ItemType.ITEM_FRAME && isInvisibleItemFrame(item);
		
		frameChoice = RecipeChoice.predicateChoice(framePredicate, ItemType.ITEM_FRAME.createItemStack());
		glowChoice = RecipeChoice.predicateChoice(glowPredicate, ItemType.GLOW_ITEM_FRAME.createItemStack());
		invisibleFrameChoice = RecipeChoice.predicateChoice(invisibleItemFramePredicate, ItemType.ITEM_FRAME.createItemStack());
		
		alternateGlowRecipe = new ShapelessRecipe(alternateGlowRecipeKey, generateInvisibleItemFrame(true));
		alternateGlowRecipe.addIngredient(invisibleFrameChoice);
		alternateGlowRecipe.addIngredient(RecipeChoice.itemType(ItemType.GLOW_INK_SAC));
		
		Bukkit.addRecipe(alternateGlowRecipe);

		frameRecipe = new ShapedRecipe(recipeKey, invisibleFrame);
		frameRecipe.shape("FFF", "FPF", "FFF");
		frameRecipe.setIngredient('F', frameChoice);
		frameRecipe.setIngredient('P', RecipeChoice.exactChoice(invisibilityPotions));

		glowRecipe = new ShapedRecipe(glowRecipeKey, glowInvisibleFrame);
		glowRecipe.shape("FFF", "FPF", "FFF");
		glowRecipe.setIngredient('F', glowChoice);
		glowRecipe.setIngredient('P', RecipeChoice.exactChoice(invisibilityPotions));

		Bukkit.addRecipe(frameRecipe);
		Bukkit.addRecipe(glowRecipe);
	}

	public void forceRecheck() {
		for (World world : Bukkit.getWorlds()) {
			for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) {
				if (isInvisibleItemFrame(frame)) {
					if (frame.isEmpty() && framesGlow) {
						frame.setGlowing(true);
						frame.setVisible(true);
					} else if (!frame.isEmpty()) {
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
		return item != null && (item.getType().asItemType() == ItemType.ITEM_FRAME
				|| item.getType().asItemType() == ItemType.GLOW_ITEM_FRAME);
	}

	public boolean isInvisibleItemFrame(ItemStack item) {
		return item != null
				&& (item.getType().asItemType() == ItemType.ITEM_FRAME
					|| item.getType().asItemType() == ItemType.GLOW_ITEM_FRAME)
				&& item.getPersistentDataContainer().has(invisibleKey, PersistentDataType.BYTE);
	}

	public boolean isInvisibleItemFrame(Hanging frame) {
		return frame instanceof ItemFrame
				&& frame.getPersistentDataContainer().has(invisibleKey, PersistentDataType.BYTE);
	}

	public ItemStack generateInvisibleItemFrame(boolean glowing) {
		ItemStack item = glowing ? ItemType.GLOW_ITEM_FRAME.createItemStack() : ItemType.ITEM_FRAME.createItemStack();
		item.editPersistentDataContainer(
				pdc -> pdc.set(invisibleKey, PersistentDataType.BYTE, (byte) 1));

		item.setData(DataComponentTypes.ITEM_NAME, glowing ? glowInvisibleFrameName : invisibleFrameName);

		if (glowing && glowInvisibleFrameModel != null) {
			item.setData(DataComponentTypes.ITEM_MODEL, glowInvisibleFrameModel);
		} else if (!glowing && invisibleFrameModel != null) {
			item.setData(DataComponentTypes.ITEM_MODEL, invisibleFrameModel);
		}

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

			// Permission check for new crafting recipes
			if ((key.equals(recipeKey) || key.equals(glowRecipeKey) || key.equals(alternateGlowRecipeKey)) &&
					!crafter.hasPermission("survivalinvisiframes.craft")) {
				event.getInventory().setResult(null);
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
				ItemStack frame = generateInvisibleItemFrame(
						item.getItemStack().getType().asItemType() == ItemType.GLOW_ITEM_FRAME);
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

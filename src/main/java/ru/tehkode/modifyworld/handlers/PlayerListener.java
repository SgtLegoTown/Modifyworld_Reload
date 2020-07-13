/*
 * Modifyworld - PermissionsEx ruleset plugin for Bukkit
 * Copyright (C) 2011 t3hk0d3 http://www.tehkode.ru
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package ru.tehkode.modifyworld.handlers;

import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.SpawnEgg;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import ru.tehkode.modifyworld.ModifyworldListener;
import ru.tehkode.modifyworld.PlayerInformer;

/**
 * @author t3hk0d3
 */
public class PlayerListener extends ModifyworldListener {

	protected boolean checkInventory = false;
	protected boolean dropRestrictedItem = false;

	public PlayerListener(Plugin plugin, ConfigurationSection config, PlayerInformer informer) {
		super(plugin, config, informer);

		this.checkInventory = config.getBoolean("item-restrictions", this.checkInventory);
		this.dropRestrictedItem = config.getBoolean("drop-restricted-item", this.dropRestrictedItem);

	}

	// иметь <itemid>в своих руках
	@EventHandler(priority = EventPriority.LOW)
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		ItemStack item = player.getInventory().getItem(event.getNewSlot());

		if (item != null && item.getType() != Material.AIR &&
				permissionDenied(player, "modifyworld.items.hold", item)) {
			int freeSlot = getFreeSlot(player.getInventory());

			if (freeSlot != 0) {
				player.getInventory().setItem(freeSlot, item);
			} else {
				player.getWorld().dropItemNaturally(player.getLocation(), item);
			}

			player.getInventory().setItem(event.getNewSlot(), new ItemStack(Material.AIR));
		}

		this.checkPlayerInventory(player);
	}

	// Что-то там с перемещением из инвентаря в руку
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerInventoryEvent(InventoryClickEvent event) {
		ItemStack item = event.getCursor();

		if (item == null || item.getType() == Material.AIR || event.getSlotType() != InventoryType.SlotType.QUICKBAR) {
			return;
		}

		Player player = (Player) event.getWhoClicked();

		int targetSlot = player.getInventory().getHeldItemSlot();

		if (event.getSlot() == targetSlot && permissionDenied(player, "modifyworld.items.hold", item)) {
			event.setCancelled(true);
		}
	}

	// использование предмета ПКМ на сущности (почему "on.entity"???)
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		if (this.checkItemUse) {
			if (permissionDenied(event.getPlayer(), "modifyworld.items.use", event.getPlayer().getItemInHand(), "on.entity", event.getRightClicked())) {
				event.setCancelled(true);
			}

			return;
		}

		// Проверка на использование
		if (!event.isCancelled() && permissionDenied(event.getPlayer(), "modifyworld.interact", event.getRightClicked())) {
			event.setCancelled(true);
		}
	}

	// использование
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Action action = event.getAction();

		if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) { // Если ПКМ по воздуху или по блоку, то проверяем инвентарь (хз зачем)
			this.checkPlayerInventory(event.getPlayer());
		}

		Player player = event.getPlayer();

		if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) { //RIGHT_CLICK_AIR по умолчанию отменен.
			switch (player.getItemInHand().getType()) {
				case POTION: //Только проверяйте зелье всплеска.
					if ((player.getItemInHand().getDurability() & 0x4000) != 0x4000) {
						break;
					}
				case EGG:
				case SNOW_BALL:
				case EXP_BOTTLE:
					if (permissionDenied(player, "modifyworld.items.throw", player.getItemInHand())) {
						event.setUseItemInHand(Result.DENY);
						//Отказ от зелья работает нормально, но клиент должен быть обновлен, потому что он уже уменьшил предмет.
						if (player.getItemInHand().getType() == Material.POTION) {
							event.getPlayer().updateInventory();
						}
					}
					return; // нет необходимости проверять дальше
				case MONSTER_EGG: // не добавляйте здесь MONSTER_EGGS
					if (permissionDenied(player, "modifyworld.spawn", ((SpawnEgg)player.getItemInHand().getData()).getSpawnedType())) {
						event.setUseItemInHand(Result.DENY);
					}
					return; // нет необходимости проверять дальше
			}
		}

		if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK && action != Action.PHYSICAL) {
			return;
		}

		// Изменяю PHYSICAL на LEFT_CLICK_BLOCK и изменяю правило (use => left)
		if (this.checkItemUse && action == Action.LEFT_CLICK_BLOCK) {
			if (permissionDenied(event.getPlayer(), "modifyworld.items.left", player.getItemInHand(), "on.block", event.getClickedBlock())) {
				event.setCancelled(true);
			}

			return;
		}
		
		// Добавляю то же действие на RIGHT_CLICK_BLOCK и изменяю правило (use => right)
		if (this.checkItemUse && action == Action.RIGHT_CLICK_BLOCK) {
			if (permissionDenied(event.getPlayer(), "modifyworld.items.right", player.getItemInHand(), "on.block", event.getClickedBlock())) {
				event.setCancelled(true);
			}

			return;
		}

		// Проверяю на использование
		if (!event.isCancelled() && permissionDenied(player, "modifyworld.blocks.interact", event.getClickedBlock())) {
			event.setCancelled(true);
		}
	}

	// зачаровать <itemid>.
	@EventHandler(priority = EventPriority.LOW)
	public void onItemEnchant(EnchantItemEvent event) {
		if (permissionDenied(event.getEnchanter(), "modifyworld.items.enchant", event.getItem())) {
			event.setCancelled(true);
		}
	}

	// Крафт
	@EventHandler(priority = EventPriority.LOW)
	public void onItemCraft(CraftItemEvent event) {
		Player player = (Player) event.getWhoClicked();

		if (permissionDenied(player, "modifyworld.items.craft", event.getRecipe().getResult())) {
			event.setCancelled(true);
		}
	}

	// Проверка разрешений иметь в инвентаре
	protected void checkPlayerInventory(Player player) {
		if (!checkInventory) {
			return;
		}

		Inventory inventory = player.getInventory();
		for (ItemStack stack : inventory.getContents()) {
			if (stack != null && permissionDenied(player, "modifyworld.items.have", stack)) {
				inventory.remove(stack);

				if (this.dropRestrictedItem) {
					player.getWorld().dropItemNaturally(player.getLocation(), stack);
				}
			}
		}
	}

	private int getFreeSlot(Inventory inventory) {
		for (int i = 9; i <= 35; i++) {
			if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
				return i;
			}
		}

		return 0;
	}
}

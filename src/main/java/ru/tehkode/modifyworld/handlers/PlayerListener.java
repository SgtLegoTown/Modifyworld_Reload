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
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ru.tehkode.modifyworld.ModifyworldListener;
import ru.tehkode.modifyworld.PlayerInformer;

/**
 * @author t3hk0d3
 */
public class PlayerListener extends ModifyworldListener {
	Logger log = Logger.getLogger("Modifyworld");

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
		log.info("PlayerListener.1");
		Player player = event.getPlayer();
		ItemStack item = player.getInventory().getItem(event.getNewSlot());
		log.info("PlayerListener.2");

		if (item != null && item.getType() != Material.AIR &&
				permissionDenied(player, "modifyworld.items.hold", item)) {
			log.info("PlayerListener.13");
			int freeSlot = getFreeSlot(player.getInventory());

			if (freeSlot != 0) {
				log.info("PlayerListener.4");
				player.getInventory().setItem(freeSlot, item);
			} else {
				log.info("PlayerListener.5");
				player.getWorld().dropItemNaturally(player.getLocation(), item);
			}

			player.getInventory().setItem(event.getNewSlot(), new ItemStack(Material.AIR));
		}

		this.checkPlayerInventory(player);
	}

	// Что-то там с перемещением из инвентаря в руку
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerInventoryEvent(InventoryClickEvent event) {
		log.info("PlayerListener.6");
		ItemStack item = event.getCursor();

		if (item == null || item.getType() == Material.AIR || event.getSlotType() != InventoryType.SlotType.QUICKBAR) {
			log.info("PlayerListener.7");
			return;
		}

		log.info("PlayerListener.8");
		Player player = (Player) event.getWhoClicked();

		int targetSlot = player.getInventory().getHeldItemSlot();

		if (event.getSlot() == targetSlot && permissionDenied(player, "modifyworld.items.hold", item)) {
			event.setCancelled(true);
		}
	}

	// использование предмета ПКМ на сущности (почему "on.entity"???)
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		log.info("PlayerListener.9");
		if (this.checkItemUse) {
			log.info("PlayerListener.10");
			if (permissionDenied(event.getPlayer(), "modifyworld.items.use", event.getPlayer().getInventory().getItemInMainHand(), "on.entity", event.getRightClicked())) {
				log.info("PlayerListener.11");
				event.setCancelled(true);
			}
			log.info("PlayerListener.12");

			return;
		}
		log.info("PlayerListener.13");

		// Проверка на использование
		if (!event.isCancelled() && permissionDenied(event.getPlayer(), "modifyworld.interact", event.getRightClicked())) {
			log.info("PlayerListener.14");
			event.setCancelled(true);
		}
	}

	// использование
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerInteract(PlayerInteractEvent event) {
		log.info("PlayerListener.15");
		Action action = event.getAction();

		if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) { // Если ПКМ по воздуху или по блоку, то проверяем инвентарь (хз зачем)
			log.info("PlayerListener.16");
			this.checkPlayerInventory(event.getPlayer());
		}
		log.info("PlayerListener.17");

		Player player = event.getPlayer();

		if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) { //RIGHT_CLICK_AIR по умолчанию отменен.
			log.info("PlayerListener.18");
			switch (player.getInventory().getItemInMainHand().getType()) {
				case POTION: //Только проверяйте зелье всплеска.
					log.info("PlayerListener.19");
					if ((player.getInventory().getItemInMainHand().getDurability() & 0x4000) != 0x4000) {
						break;
					}
				case EGG:
				case SNOW_BALL:
				case EXP_BOTTLE:
					log.info("PlayerListener.20");
					if (permissionDenied(player, "modifyworld.items.throw", player.getInventory().getItemInMainHand())) {
						log.info("PlayerListener.21");
						event.setUseItemInHand(Result.DENY);
						//Отказ от зелья работает нормально, но клиент должен быть обновлен, потому что он уже уменьшил предмет.
						if (player.getInventory().getItemInMainHand().getType() == Material.POTION) {
							log.info("PlayerListener.22");
//							event.getPlayer().updateInventory();
						}
					}
					log.info("PlayerListener.23");
					return; // нет необходимости проверять дальше
				case MONSTER_EGG: // не добавляйте здесь MONSTER_EGGS
					log.info("PlayerListener.24");
					if (permissionDenied(player, "modifyworld.spawn", ((org.bukkit.inventory.meta.SpawnEggMeta)player.getInventory().getItemInMainHand().getData()).getSpawnedType())) {
						log.info("PlayerListener.25");
						event.setUseItemInHand(Result.DENY);
					}
					log.info("PlayerListener.26");
					return; // нет необходимости проверять дальше
			default:
				log.info("PlayerListener.27");
				break;
			}
		}

		// Если не ЛКМ, не ПКМ и не нажатие весом
		if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK && action != Action.PHYSICAL) {
			log.info("PlayerListener.28");
			return;
		}

		// Изменяю PHYSICAL на LEFT_CLICK_BLOCK и изменяю правило (use => left)
		if (this.checkItemUse && action == Action.LEFT_CLICK_BLOCK) {
			log.info("PlayerListener.29");
			if (permissionDenied(event.getPlayer(), "modifyworld.items.left", player.getInventory().getItemInMainHand(), "on.block", event.getClickedBlock())) {
				log.info("PlayerListener.30");
				event.setCancelled(true);
			}
			log.info("PlayerListener.31");

			return;
		}
		
		// Добавляю то же действие на RIGHT_CLICK_BLOCK и изменяю правило (use => right)
		if (this.checkItemUse && action == Action.RIGHT_CLICK_BLOCK) {
			log.info("PlayerListener.32");
			if (permissionDenied(event.getPlayer(), "modifyworld.items.right", player.getInventory().getItemInMainHand(), "on.block", event.getClickedBlock())) {
				log.info("PlayerListener.33");
				event.setCancelled(true);
			}
			log.info("PlayerListener.34");

			return;
		}

		// Проверяю на использование
		if (!event.isCancelled() && permissionDenied(player, "modifyworld.blocks.interact", event.getClickedBlock())) {
			log.info("PlayerListener.35");
			event.setCancelled(true);
		}
	}

	// зачаровать <itemid>.
	@EventHandler(priority = EventPriority.LOW)
	public void onItemEnchant(EnchantItemEvent event) {
		log.info("PlayerListener.36");
		if (permissionDenied(event.getEnchanter(), "modifyworld.items.enchant", event.getItem())) {
			log.info("PlayerListener.37");
			event.setCancelled(true);
		}
	}

	// Крафт
	@EventHandler(priority = EventPriority.LOW)
	public void onItemCraft(CraftItemEvent event) {
		log.info("PlayerListener.38");
		Player player = (Player) event.getWhoClicked();

		if (permissionDenied(player, "modifyworld.items.craft", event.getRecipe().getResult())) {
			log.info("PlayerListener.39");
			event.setCancelled(true);
		}
	}

	// Проверка разрешений иметь в инвентаре
	protected void checkPlayerInventory(Player player) {
		log.info("PlayerListener.40");
		if (!checkInventory) {
			log.info("PlayerListener.41");
			return;
		}

		Inventory inventory = player.getInventory();
		log.info("PlayerListener.42");
		for (ItemStack stack : inventory.getContents()) {
			log.info("PlayerListener.43");
			if (stack != null && permissionDenied(player, "modifyworld.items.have", stack)) {
				log.info("PlayerListener.44");
				inventory.remove(stack);

				if (this.dropRestrictedItem) {
					log.info("PlayerListener.45");
					player.getWorld().dropItemNaturally(player.getLocation(), stack);
				}
			}
		}
	}

	private int getFreeSlot(Inventory inventory) {
		log.info("PlayerListener.46");
		for (int i = 9; i <= 35; i++) {
			log.info("PlayerListener.47");
			if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
				log.info("PlayerListener.48");
				return i;
			}
		}
		log.info("PlayerListener.49");

		return 0;
	}
}

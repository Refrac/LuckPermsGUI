/*
 * Copyright (c) BGHDDevelopment.
 * Please refer to the plugin page or GitHub page for our open-source license.
 * If you have any questions please email ceo@bghddevelopment or reach us on Discord
 */

package me.AsVaidas.LuckPemsGUI.groups;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.SuffixNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import me.AsVaidas.LuckPemsGUI.Main;
import me.AsVaidas.LuckPemsGUI.Tools;

public class Suffix implements Listener {

	public static Map<Player, Group> addPrefix = new HashMap<>();
	public static Map<Player, Group> addTempPrefix = new HashMap<>();

	@EventHandler
	public void onaddParent(AsyncPlayerChatEvent e) {
		if (!addPrefix.containsKey(e.getPlayer())) return;
		String message = e.getMessage();
		Group g = addPrefix.get(e.getPlayer());
		
		e.getPlayer().sendMessage("lp group "+g.getName()+" meta addsuffix "+message);
		Tools.sendCommand(e.getPlayer(), "lp group "+g.getName()+" meta addsuffix "+message);
		addPrefix.remove(e.getPlayer());
		Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getInstance(), () -> {
			EditGroup.open(e.getPlayer(), g);
		}, 5);
		e.setCancelled(true);
	}
	
	@EventHandler
	public void onaddTempParent(AsyncPlayerChatEvent e) {
		if (!addTempPrefix.containsKey(e.getPlayer())) return;
		String message = e.getMessage();
		Group g = addTempPrefix.get(e.getPlayer());

		Tools.sendCommand(e.getPlayer(), "lp group "+g.getName()+" meta addtempsuffix "+message);
		addTempPrefix.remove(e.getPlayer());
		Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getInstance(), () -> {
			EditGroup.open(e.getPlayer(), g);
		}, 5);
		e.setCancelled(true);
	}

	static LuckPerms l = LuckPermsProvider.get();

	public static void open(Player p, Group group, int page) {
		Inventory myInventory = Bukkit.createInventory(null, 54, ChatColor.AQUA+"LuckPerms group suffixes");
		Tools.onAsync(() -> {

		
		// ----------------------- INFO ------------------------------
		int weight = group.getWeight().orElse(0);
		ItemStack info = Tools.button(Material.ARMOR_STAND,
				"&6Info",
				Arrays.asList(
						"&cGroup info: &e"+group.getName(),
						"&cDisplay name: &e"+group.getFriendlyName(),
						"&cWeight: &e"+weight,
						"&cCounts:",
						"  &cNodes: &e"+group.getNodes().size(),
						"  &cPermissions: &e"+group.getDistinctNodes().size(),
						"  &cPrefixes: &e"+group.getCachedData().getMetaData().getPrefixes().size(),
						"  &cSuffixes: &e"+group.getCachedData().getMetaData().getSuffixes().size()),
				1);
		myInventory.setItem(4, info);
		// ----------------------- INFO ------------------------------
		
		int sk = 0;
		int i = 9;
		
		int from = 45*page-1;
		int to = 45*(page+1)-1;
		for (Node permission : group.getDistinctNodes()) {
			if (permission.getType() != NodeType.SUFFIX) continue;
			if (from <= sk && sk < to) {
				SuffixNode suffix = ((SuffixNode) permission);
				String expiration = permission.hasExpiry() ? Tools.getTime(permission.getExpiry().toEpochMilli()) : "Never";
				String server = permission.getContexts().getAnyValue(DefaultContextKeys.SERVER_KEY).orElse("global");
				String world = permission.getContexts().getAnyValue(DefaultContextKeys.WORLD_KEY).orElse("global");
				ItemStack item = Tools.button(Material.TNT,
						"&6"+suffix.getMetaValue(),
						Arrays.asList(
								"&cID: &e"+sk,
								"&cPosition: &e"+suffix.getPriority(),
								"&cExpires in: &e"+expiration,
								"&cValue: &e"+permission.getValue(),
								"&cServer: &e"+server,
								"&cWorld: &e"+world,
								"&eClick to remove"
								), 1);
				myInventory.setItem(i, item);
				i++;
			}
			sk++;
		}

		if (to < sk) {
			ItemStack next = Tools.button(Material.SIGN, "&6Next", Arrays.asList("&eNext page", "&cCurrent: &e"+page), 1);
			myInventory.setItem(53, next);
		}
		
		ItemStack back = Tools.button(Material.BARRIER, "&6Back", Arrays.asList(""), 1);
		myInventory.setItem(8, back);
		
		});
		p.openInventory(myInventory);
	}
	
	@EventHandler
	public void onInventoryClickEvent(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();

		Inventory inv = e.getClickedInventory();
		ItemStack item = e.getCurrentItem();
		if (inv != null && item != null)
			if (e.getView().getTitle().equals(ChatColor.AQUA+"LuckPerms group suffixes")) {
				e.setCancelled(true);
				if (item.hasItemMeta())
					if (item.getItemMeta().hasDisplayName()) {
						
						String group = ChatColor.stripColor(inv.getItem(4).getItemMeta().getLore().get(0).split(" ")[2]);
						Group g = l.getGroupManager().getGroup(group);
						
						String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
						if (name.equals("Next")) {
							int current = Integer.parseInt(ChatColor.stripColor(inv.getItem(53).getItemMeta().getLore().get(1).split(" ")[1]));
							open(p, g, current+1);
						} else if (name.equals("Back")) {
							EditGroup.open(p, g);
						} else if (!name.equals("Info")) {
							
							int id = Integer.parseInt(ChatColor.stripColor(item.getItemMeta().getLore().get(0).split(" ")[1]));

							int sk = 0;
							for (Node permission : g.getDistinctNodes()) {
								if (permission.getType() != NodeType.SUFFIX) continue;
								if (sk == id) {
									SuffixNode suffix = ((SuffixNode) permission);

									String server = permission.getContexts().getAnyValue(DefaultContextKeys.SERVER_KEY).orElse("global");
									String world = permission.getContexts().getAnyValue(DefaultContextKeys.WORLD_KEY).orElse("global");

									if (permission.hasExpiry())
										Tools.sendCommand(p, "lp group " + g.getName() + " meta removetempsuffix " + suffix.getPriority() + " " + '"' + suffix.getMetaValue() + '"' + " " + server + " " + world);
									else
										Tools.sendCommand(p, "lp group " + g.getName() + " meta removesuffix " + suffix.getPriority() + " " + '"' + suffix.getMetaValue() + '"' + " " + server + " " + world);
									break;
								}
								sk++;
							}
								
							int current = 0;
							if (inv.getItem(53) != null)
								current = Integer.parseInt(ChatColor.stripColor(inv.getItem(53).getItemMeta().getLore().get(1).split(" ")[1]));

							int page = current;
							Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
								open(p, g, page);
							}, 5);
						}
					}
			}
	}
}

package com.trc202.CombatTagListeners;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import com.topcat.npclib.entity.NPC;
import com.trc202.CombatTag.CombatTag;

public class NoPvpEntityListener implements Listener {

	CombatTag plugin;

	public NoPvpEntityListener(CombatTag combatTag) {
		this.plugin = combatTag;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDamage(EntityDamageByEntityEvent e) {
		if (e.isCancelled() || (e.getDamage() == 0)) {
			return;
		}
		Entity dmgr = e.getDamager();
		if (dmgr instanceof Projectile) {
			if (((Projectile) dmgr).getShooter() instanceof Entity) {
				dmgr = (Entity) ((Projectile) dmgr).getShooter();
			}
		}
		if (e.getEntity() instanceof Player) {
			Player tagged = (Player) e.getEntity();

			if (plugin.npcm.isNPC(tagged)
					|| disallowedWorld(tagged.getWorld().getName())) {
				return;
			} // If the damaged player is an npc do nothing

			if ((dmgr instanceof Player) && plugin.settings.playerTag()) {
				Player damagerPlayer = (Player) dmgr;
				if (damagerPlayer != tagged && damagerPlayer != null) {
					onPlayerDamageByPlayer(damagerPlayer, tagged);
				}
			} 
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent event) {
		if (plugin.npcm.isNPC(event.getEntity())) {
			onNPCDeath(event.getEntity());
		} else if (event.getEntity() instanceof Player) {
			onPlayerDeath((Player) event.getEntity());
		}
	}

	public void onNPCDeath(Entity entity) {
		UUID id = plugin.getPlayerUUID(entity);
		NPC npc = plugin.npcm.getNPC(id);
		plugin.updatePlayerData(npc, id);
		plugin.removeTagged(id);
	}

	public void onPlayerDeath(Player deadPlayer) {
		plugin.removeTagged(deadPlayer.getUniqueId());
	}

	public void onPlayerDamageByPlayer(Player damager, Player damaged) {
		if (plugin.npcm.isNPC(damaged)) {
			return;
		} // If the damaged player is an npc do nothing

		if (!damaged.hasPermission("combattag.ignore")
				&& !plugin.settings.onlyDamagerTagged()) {
			if (!plugin.isInCombat(damaged.getUniqueId())) {
				if (plugin.settings.isSendMessageWhenTagged()) {
					String tagMessage = plugin.settings.getTagMessageDamaged();
					tagMessage = tagMessage.replace("[player]",
							damager.getName());
					damaged.sendMessage(ChatColor.RED + "[CombatTag] "
							+ tagMessage);
				}
			}
			plugin.addTagged(damaged);

			if (plugin.settings.blockFly() && damaged.isFlying()) {
				damaged.sendMessage(ChatColor.RED
						+ "[CombatTag] Disabling fly!");
				damaged.setFlying(false);
			}
		}
	}

	public boolean disallowedWorld(String worldName) {
		for (String disallowedWorld : plugin.settings.getDisallowedWorlds()) {
			if (worldName.equalsIgnoreCase(disallowedWorld)) {
				// Skip this tag the world they are in is not to be tracked by
				// combat tag
				return true;
			}
		}
		return false;
	}
}
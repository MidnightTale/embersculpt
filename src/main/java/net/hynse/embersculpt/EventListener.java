package net.hynse.embersculpt;

import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class EventListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        double temperature = Embersculpt.playerDataManager.loadPlayerTemperature(player);
        Embersculpt.util.setBodyTemperature(player, temperature);
        Embersculpt.actionBar.updateActionBar(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Embersculpt.playerDataManager.savePlayerTemperature(player);
        Embersculpt.playerDataManager.bodyTemperatureMap.remove(player);
    }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Reset temperature data for the dead player
        Player player = event.getEntity();
        Embersculpt.playerDataManager.bodyTemperatureMap.remove(player);
        player.clearActivePotionEffects();
    }
    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if the consumed item is water or a potion
        if (Embersculpt.cooldown.isWaterOrPotion(item.getType())) {
            // Apply temperature reduction for 30 seconds at a rate of 3 per second
            Embersculpt.cooldown.reduceTemperatureOnDrink(player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check if the player is in the area of a lingering potion
        if (Embersculpt.cooldown.isPlayerInLingeringCloud(player)) {
            // Apply low temperature effect
            new WrappedRunnable() {
                @Override
                public void run() {
                    Embersculpt.cooldown.reduceTemperatureOnLingeringCloud(player);
                }
            }.runTaskAtEntity(Embersculpt.instance,player);
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        Collection<LivingEntity> affectedEntities = event.getAffectedEntities();
        for (LivingEntity entity : affectedEntities) {
            if (entity instanceof Player player) {
                // Apply temperature reduction for 30 seconds at a rate of 3 per second
                Embersculpt.cooldown.reduceTemperaturenPotionSplash(player);
            }
        }
    }
    @EventHandler
    public void onPlayerSprintStart(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        if (event.isSprinting()) {
            Embersculpt.instance.sprintStartTime.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onPlayerWalkStart(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getTo().distanceSquared(event.getFrom()) > 0.001) {
            Embersculpt.instance.walkStartTime.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }
}

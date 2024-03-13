package net.hynse.embersculpt;

import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Collection;

public class Cooldown {
    public boolean isWaterOrPotion(Material material) {
        return material == Material.POTION
                || material == Material.LINGERING_POTION
                || material == Material.SPLASH_POTION;
        // Add more potion types if necessary
    }
    public void reduceTemperaturenPotionSplash(Player player) {
        // Apply temperature reduction for 30 seconds at a rate of 3 per second
        int durationInSeconds = 1;
        double reductionRate = 26.6;

        new WrappedRunnable() {
            int secondsRemaining = durationInSeconds;

            @Override
            public void run() {
                // Check if the player is still online
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                // Reduce the player's body temperature
                double currentTemperature = Embersculpt.util.getBodyTemperature(player);
                currentTemperature = Math.max(-100.0, currentTemperature - reductionRate);

                // Update the player's body temperature
                Embersculpt.util.setBodyTemperature(player, currentTemperature);

                // Update the action bar for the player (if needed)
                Embersculpt.actionBar.updateActionBar(player);

                // Check if the duration has elapsed
                if (--secondsRemaining <= 0) {
                    cancel(); // Stop the task
                }
            }
        }.runTaskTimer(Embersculpt.instance, 1, 20); // Run the task every second
    }
    public void reduceTemperatureOnLingeringCloud(Player player) {
        // Apply temperature reduction for 30 seconds at a rate of 3 per second
        int durationInSeconds = 3;
        double reductionRate = 0.3;

        new WrappedRunnable() {
            int secondsRemaining = durationInSeconds;

            @Override
            public void run() {
                // Check if the player is still online
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                // Reduce the player's body temperature
                double currentTemperature = Embersculpt.util.getBodyTemperature(player);
                currentTemperature = Math.max(-100.0, currentTemperature - reductionRate);

                // Update the player's body temperature
                Embersculpt.util.setBodyTemperature(player, currentTemperature);

                // Update the action bar for the player (if needed)
                Embersculpt.actionBar.updateActionBar(player);

                // Check if the duration has elapsed
                if (--secondsRemaining <= 0) {
                    cancel(); // Stop the task
                }
            }
        }.runTaskTimer(Embersculpt.instance, 1, 20); // Run the task every second
    }
    public void reduceTemperatureOnDrink(Player player) {
        // Apply temperature reduction for 30 seconds at a rate of 3 per second
        int durationInSeconds = 8;
        double reductionRate = 3.3;

        new WrappedRunnable() {
            int secondsRemaining = durationInSeconds;

            @Override
            public void run() {
                // Check if the player is still online
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                // Reduce the player's body temperature
                double currentTemperature = Embersculpt.util.getBodyTemperature(player);
                currentTemperature = Math.max(-100.0, currentTemperature - reductionRate);

                // Update the player's body temperature
                Embersculpt.util.setBodyTemperature(player, currentTemperature);

                // Update the action bar for the player (if needed)
                Embersculpt.actionBar.updateActionBar(player);

                // Check if the duration has elapsed
                if (--secondsRemaining <= 0) {
                    cancel(); // Stop the task
                }
            }
        }.runTaskTimer(Embersculpt.instance, 1, 20); // Run the task every second
    }

    public boolean isPlayerInLingeringCloud(Player player) {
        Location playerLocation = player.getLocation();

        // Define the radius within which to check for lingering clouds
        double checkRadius = 2.0;

        // Get nearby entities within the specified radius
        Collection<Entity> nearbyEntities = playerLocation.getWorld().getNearbyEntities(playerLocation, checkRadius, checkRadius, checkRadius);

        // Check if any of the nearby entities are AreaEffectCloud (lingering cloud)
        for (Entity entity : nearbyEntities) {
            if (entity.getType() == EntityType.AREA_EFFECT_CLOUD) {
                // You may want to further refine the check, for example, by checking the cloud's location or other properties
                return true; // Player is in the area of a lingering cloud
            }
        }

        // No lingering clouds found in the vicinity
        return false;
    }
}

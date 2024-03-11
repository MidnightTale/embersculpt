package net.hynse.embersculpt;

import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BlockTemperature {
    public void BlockTemperature(Player player) {
        Block block = player.getLocation().getBlock();

        double currentTemperature = Embersculpt.instance.bodyTemperatureMap.getOrDefault(player, 0.0);

        if (block.getType() == Material.WATER || block.getType() == Material.WATER_CAULDRON) {
            // Decrease the player's temperature until it reaches 0
            if (currentTemperature > -100) {
                currentTemperature -= (currentTemperature < -30 ? 0.7 : 0.2); // Adjust the rate at which temperature decreases
            }
        } else if (block.getType() == Material.POWDER_SNOW || block.getType() == Material.POWDER_SNOW_CAULDRON) {
            // Decrease the player's temperature until it reaches -100
            if (currentTemperature > -100) {
                currentTemperature -= (currentTemperature < -50 ? 1.0 : 0.5); // Adjust the rate at which temperature decreases
            }
        } else if (block.getType() == Material.LAVA || block.getType() == Material.LAVA_CAULDRON) {
            // Decrease the player's temperature until it reaches 0
            if (currentTemperature < 100) {
                currentTemperature += (currentTemperature > 0 ? 0.2 : 0.7); // Adjust the rate at which temperature decreases
            }
        } else if (block.getType() == Material.FIRE || block.getType() == Material.SOUL_FIRE || block.getType() == Material.CAMPFIRE || block.getType() == Material.SOUL_CAMPFIRE) {
            // Decrease the player's temperature until it reaches 0
            if (currentTemperature < 100) {
                currentTemperature += (currentTemperature > 0 ? 0.1 : 0.3); // Adjust the rate at which temperature decreases
            }
        }

        // Update the player's body temperature
        Embersculpt.instance.bodyTemperatureMap.put(player, Math.min(100.0, Math.max(-100.0, currentTemperature)));
    }
    public double calculateTemperatureHeatSources(Player player) {
        final double[] heatSourceTemperatureChange = {0.0};

        // Define the radius to search for heat sources
        int searchRadius = 5;

        // Get the player's location
        Location playerLocation = player.getLocation();
        new WrappedRunnable() {
            @Override
            public void run() {
                for (int x = -searchRadius; x <= searchRadius; x++) {
                    for (int y = -searchRadius; y <= searchRadius; y++) {
                        for (int z = -searchRadius; z <= searchRadius; z++) {
                            // Get the block at the current position
                            Location currentLocation = playerLocation.clone().add(x, y, z);
                            Material blockMaterial = currentLocation.getBlock().getType();

                            // Check if the block is a torch or lava
                            if (blockMaterial == Material.TORCH || blockMaterial == Material.LAVA || blockMaterial == Material.CAMPFIRE || blockMaterial == Material.SOUL_CAMPFIRE || blockMaterial == Material.LANTERN || blockMaterial == Material.SOUL_LANTERN || blockMaterial == Material.LIGHT || blockMaterial == Material.SHROOMLIGHT || blockMaterial == Material.GLOWSTONE || blockMaterial == Material.GLOW_ITEM_FRAME || blockMaterial == Material.GLOW_BERRIES || blockMaterial == Material.FIRE || blockMaterial == Material.SOUL_FIRE || blockMaterial == Material.SEA_LANTERN || blockMaterial == Material.OCHRE_FROGLIGHT || blockMaterial == Material.PEARLESCENT_FROGLIGHT || blockMaterial == Material.VERDANT_FROGLIGHT) {
                                // Calculate the distance from the player to the heat source
                                double distance = playerLocation.distance(currentLocation);

                                // Adjust this value based on the impact of heat sources on temperature
                                double proximityEffect = 1.0 - (distance / searchRadius);
                                heatSourceTemperatureChange[0] += 0.6 * proximityEffect;
                            }
                        }
                    }
                }
            }
        }.runTaskAtLocation(Embersculpt.instance, playerLocation);
        return heatSourceTemperatureChange[0];
    }
}

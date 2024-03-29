package net.hynse.embersculpt;

import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BlockTemperature {
    int waterTimer = 0;
    int snowTimer = 0;
    int lavaTimer = 0;
    int fireTimer = 0;
    public void BlockTemperature(Player player) {
        Block block = player.getLocation().getBlock();

        double currentTemperature = Embersculpt.playerDataManager.bodyTemperatureMap.getOrDefault(player, 0.0);

        if (block.getType() == Material.WATER || block.getType() == Material.WATER_CAULDRON && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL) {
            // Increment the water timer
            waterTimer++;
            // Decrease the player's temperature until it reaches 0
            if (currentTemperature > -100) {
                currentTemperature -= (currentTemperature < -30 ? 0.6 : 0.1) * waterTimer; // Adjust the rate at which temperature decreases
            }
        } else if (block.getType() == Material.POWDER_SNOW || block.getType() == Material.POWDER_SNOW_CAULDRON && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL) {
            // Increment the snow timer
            snowTimer++;
            // Decrease the player's temperature until it reaches -100
            if (currentTemperature > -100) {
                currentTemperature -= (currentTemperature < -50 ? 0.9 : 0.1) * snowTimer; // Adjust the rate at which temperature decreases
            }
        } else if (block.getType() == Material.LAVA || block.getType() == Material.LAVA_CAULDRON && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL) {
            // Increment the lava timer
            lavaTimer++;
            // Decrease the player's temperature until it reaches 0
            if (currentTemperature < 100) {
                currentTemperature += (currentTemperature > 0 ? 0.7 : 0.5) * lavaTimer; // Adjust the rate at which temperature decreases
            }
        } else if (block.getType() == Material.FIRE || block.getType() == Material.SOUL_FIRE || block.getType() == Material.CAMPFIRE || block.getType() == Material.SOUL_CAMPFIRE && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL) {
            // Increment the fire timer
            fireTimer++;
            // Decrease the player's temperature until it reaches 0
            if (currentTemperature < 100) {
                currentTemperature += (currentTemperature > 0 ? 0.3 : 0.2) * fireTimer; // Adjust the rate at which temperature decreases
            }
        } else {
            // Reset timers when not in any specific environment
            waterTimer = 0;
            snowTimer = 0;
            lavaTimer = 0;
            fireTimer = 0;
        }

        // Update the player's body temperature
        Embersculpt.playerDataManager.bodyTemperatureMap.put(player, Math.min(100.0, Math.max(-100.0, currentTemperature)));
    }

    public void calculateTemperatureHeatSources(Player player, int searchRadius) {
        final double[] currentTemperature = {Embersculpt.playerDataManager.bodyTemperatureMap.getOrDefault(player, 0.0)};
        final double ratesky = Embersculpt.skyLight.calculateTemperatureSkyLightChange(player, player.getLocation().getBlock().getLightFromSky());

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

                            // Check if the block is a heat source and the player's location block is AIR
                            if (isHeatSource(blockMaterial) && currentLocation.getBlock().getType() == Material.AIR) {
                                // Adjust the temperature based on the rate
                                double rate = calculateTemperatureRate(currentTemperature[0]);
                                currentTemperature[0] += (currentTemperature[0] > 0 ? 0.1 : 16) * rate * ratesky;

                                // Update the player's body temperature
                                Embersculpt.util.setBodyTemperature(player, currentTemperature[0]);
                            }
                        }
                    }
                }
            }
        }.runTaskAtLocation(Embersculpt.instance, playerLocation);
    }


    // Calculate the rate of temperature change based on the current temperature
    private double calculateTemperatureRate(double temperature) {
        if (temperature < -100) {
            return 0.7; // Very slow rate
        } else if (temperature < -70) {
            return 0.5; // Slower rate
        } else if (temperature < -30) {
            return 0.2; // Slow rate
        } else if (temperature < -10) {
            return 0.1; // Moderate rate
        } else if (temperature < 37) {
            return 0.02; // Normal rate
        } else if (temperature < 46) {
            return 0.005; // Moderate rate
        } else if (temperature < 80) {
            return 0.001; // Slow rate
        } else if (temperature < 100) {
            return 0.0005; // Slower rate
        } else {
            return 0.0005; // Very slow rate
        }
    }





    // Method to check if a material is a heat source
    private boolean isHeatSource(Material material) {
        switch (material) {
            case TORCH:
            case LAVA:
            case CAMPFIRE:
            case SOUL_CAMPFIRE:
            case LANTERN:
            case SOUL_LANTERN:
            case LIGHT:
            case SHROOMLIGHT:
            case GLOWSTONE:
            case GLOW_ITEM_FRAME:
            case GLOW_BERRIES:
            case FIRE:
            case SOUL_FIRE:
            case SEA_LANTERN:
            case OCHRE_FROGLIGHT:
            case PEARLESCENT_FROGLIGHT:
            case VERDANT_FROGLIGHT:
                return true;
            default:
                return false;
        }
    }
}

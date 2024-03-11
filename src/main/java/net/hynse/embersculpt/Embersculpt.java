package net.hynse.embersculpt;

import me.nahu.scheduler.wrapper.FoliaWrappedJavaPlugin;
import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.HashMap;

public final class Embersculpt extends FoliaWrappedJavaPlugin implements Listener {
    private HashMap<Player, Double> bodyTemperatureMap;
    private static final double MAX_TEMPERATURE = 100.0;
    private static final double MIN_TEMPERATURE = -100.0;

    @Override
    public void onEnable() {
        bodyTemperatureMap = new HashMap<>();

        new WrappedRunnable() {
            @Override
            public void run() {
                updateBodyTemperature();
            }
        }.runTaskTimer(this, 1, 20);
    }

    @Override
    public void onDisable() {
        bodyTemperatureMap.clear();
    }

    private double calculateTemperatureSkyLightChange(Player player, int skylightLevel, double biomeTemperatureChange) {
        // Define parameters for skylight influence on temperature
        int minSkylight = 0;
        int maxSkylight = 15;
        double minRiseChangeRate, maxRiseChangeRate, minDecreaseChangeRate, maxDecreaseChangeRate;

        // Get the current time in the world where the player is located
        long time = player.getWorld().getTime();

        // Check if it is day (time between 0 and 12000)
        boolean isDay = time >= 0 && time < 12000;
        Location biomeLocation = player.getLocation();
        double biomeTemperature = biomeLocation.getBlock().getTemperature();

// Check if the player is in the shadow during the day in a hot biome
        boolean isShadowDuringDayInHotBiome = isDay && skylightLevel < minSkylight && biomeTemperature >= 0.7;

// Check if the player is in the shadow during the night in a hot biome
        boolean isShadowDuringNightInHotBiome = !isDay && skylightLevel < minSkylight && biomeTemperature >= 0.7;


        if (isDay) {
            minRiseChangeRate = 0.07;
            maxRiseChangeRate = 0.9;
            minDecreaseChangeRate = -0.1;
            maxDecreaseChangeRate = -1.3;
        } else {
            minRiseChangeRate = 0.0001;
            maxRiseChangeRate = 0.002;
            minDecreaseChangeRate = -0.2;
            maxDecreaseChangeRate = -1.5;
        }

        // Calculate the original changeRate without biomeTemperatureChange
        double changeRate;
        if (skylightLevel >= minSkylight && skylightLevel <= maxSkylight) {
            if (skylightLevel >= 10) {
                // Interpolate between minRiseChangeRate and maxRiseChangeRate for rising temperatures
                changeRate = minRiseChangeRate + ((maxRiseChangeRate - minRiseChangeRate) / (maxSkylight - 10))
                        * (skylightLevel - 10);
            } else {
                // Interpolate between maxDecreaseChangeRate and minDecreaseChangeRate for decreasing temperatures
                changeRate = minDecreaseChangeRate + ((maxDecreaseChangeRate - minDecreaseChangeRate) / (10 - minSkylight))
                        * (10 - skylightLevel);
            }

            double adjustedChangeRate = changeRate + biomeTemperatureChange;

            // Adjust the temperature to not go below 37 during the day in the shadow of a hot biome
            if (isShadowDuringDayInHotBiome) {
                adjustedChangeRate = Math.max(0.0, adjustedChangeRate);
            }

            return adjustedChangeRate;
        } else if (isShadowDuringNightInHotBiome && bodyTemperatureMap.getOrDefault(player, 0.0) > 24) {
            // If in the shadow during the night and body temperature is greater than 24, slowly decrease temperature
            return Math.max(-1.0, (24 - bodyTemperatureMap.getOrDefault(player, 0.0)));
        } else {
            // If skylight level is above the maximum or not in the shadow, no additional change
            return 0.0;
        }
    }


    private double calculateTemperatureSkyLightChange(Player player, int skylightLevel) {
        // Calculate biome temperature change
        double biomeTemperatureChange = calculateTemperatureBiome(player);

        // Call the original method with biomeTemperatureChange
        return calculateTemperatureSkyLightChange(player, skylightLevel, biomeTemperatureChange);
    }

    private void updateBodyTemperature() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Get skylight level at player's location
            int skylightLevel = player.getLocation().getBlock().getLightFromSky();

            // Calculate temperature changes based on skylight level and biome factor
            double skylightTemperatureChange = calculateTemperatureSkyLightChange(player, skylightLevel);

            // Update player's temperature
            double currentTemperature = bodyTemperatureMap.getOrDefault(player, 0.0);
            currentTemperature += skylightTemperatureChange;

            // Ensure the temperature stays within the specified range
            currentTemperature = Math.max(MIN_TEMPERATURE, Math.min(MAX_TEMPERATURE, currentTemperature));

            bodyTemperatureMap.put(player, currentTemperature);

            updateActionBar(player);
        }
    }

    private double getFreezingFactor(double biomeTemperature, boolean isDay) {
        if (!isDay) {
            if (biomeTemperature <= -0.7) {
                return -Math.pow(Math.abs(biomeTemperature), 1.7) * 67.22; // Adjust the factor based on your preference
            } else if (biomeTemperature <= 0.247) {
                return -Math.pow(Math.abs(biomeTemperature), 1.64) * 53.61; // Adjust the factor based on your preference
            }
        }
        if (isDay) {
            if (biomeTemperature <= -0.7) {
                return -Math.pow(Math.abs(biomeTemperature), 1.56) * 28.61; // Adjust the factor based on your preference
            } else if (biomeTemperature <= 0.247) {
                return -Math.pow(Math.abs(biomeTemperature), 1.52) * 24.24; // Adjust the factor based on your preference
            }
        }
        return 0.0; // Default: no freezing factor
    }

    private double getHeatFactor(double biomeTemperature, boolean isDay) {
        if (isDay) {
            if (biomeTemperature >= 1.46) {
                return Math.pow(Math.abs(biomeTemperature), 2.2) * 32.22; // Adjust the factor based on your preference
            } else if (biomeTemperature >= 0.76) {
                return Math.pow(Math.abs(biomeTemperature), 1.7) * 17.61; // Adjust the factor based on your preference
            }
        }
        if (!isDay) {
            if (biomeTemperature >= 1.46) {
                return Math.pow(Math.abs(biomeTemperature), 1.32) * 0.01; // Adjust the factor based on your preference
            } else if (biomeTemperature >= 0.76) {
                return Math.pow(Math.abs(biomeTemperature), 1.15) * 0.005; // Adjust the factor based on your preference
            }
        }
        return 0.0; // Default: no heating factor
    }





    private double calculateTemperatureBiome(Player player) {
        Location location = player.getLocation();
        long time = player.getWorld().getTime();
        boolean isDay = time >= 0 && time < 12000;

        // Get the temperature factor of the biome
        double biomeTemperature = location.getBlock().getTemperature();

        // Get the freezing factor based on the biome temperature
        double freezingFactor = getFreezingFactor(biomeTemperature, isDay);

        // Get the heating factor based on the biome temperature and day/night status
        double heatingFactor = getHeatFactor(biomeTemperature, isDay);

        // Ensure the biome-specific temperature change stays within a reasonable range
        double biomeTemperatureChange = Math.max(-0.1, Math.min(0.1, freezingFactor + heatingFactor));

        return biomeTemperatureChange;
    }


    private void updateActionBar(Player player) {
        double temperature = bodyTemperatureMap.getOrDefault(player, 0.0);

        // Get skylight level at player's location
        int skylightLevel = player.getLocation().getBlock().getLightFromSky();

        // Format the temperature and temperatureChangeRate to display four decimal places
        String formattedTemperature = String.format("%.4f", temperature);
        String formattedChangeRate = String.format("%.4f", calculateTemperatureSkyLightChange(player, skylightLevel));

        // Get biome information asynchronously
        Location location = player.getLocation();
        new WrappedRunnable() {
            @Override
            public void run() {
                Biome biome = location.getBlock().getBiome();
                double biomeTemperature = location.getBlock().getTemperature();
                long time = player.getWorld().getTime();


                // Calculate biome-specific factors
                double freezingFactor = getFreezingFactor(biomeTemperature, time < 12000);
                double heatingFactor = getHeatFactor(biomeTemperature, time >= 12000);

                // Format biome information
                String biomeInfo = ChatColor.GRAY + " | Bi:" + ChatColor.YELLOW + biome.name() +
                        ChatColor.GRAY + " (" + ChatColor.AQUA + String.format("%.4f", biomeTemperature) + ChatColor.GRAY + ")";

                // Construct the action bar message
                String actionBarMessage = ChatColor.GRAY + "T:" + ChatColor.YELLOW + formattedTemperature +
                        ChatColor.GRAY + " | SL:" + ChatColor.AQUA + skylightLevel +
                        ChatColor.GRAY + " | R:" + ChatColor.GREEN + formattedChangeRate +
                        ChatColor.GRAY + " | HF:" + ChatColor.RED + String.format("%.4f", heatingFactor) +
                        ChatColor.GRAY + " | FF:" + ChatColor.BLUE + String.format("%.4f", freezingFactor) +
                        biomeInfo;

                // Send action bar message to the player
                player.sendActionBar(actionBarMessage);
            }
        }.runTaskAtLocation(this, location);
    }

}

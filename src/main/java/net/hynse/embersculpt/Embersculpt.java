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
        boolean isShadowDuringDayInHotBiome = isDay && skylightLevel < minSkylight && biomeTemperature > 0.4;

// Check if the player is in the shadow during the night in a hot biome
        boolean isShadowDuringNightInHotBiome = !isDay && skylightLevel < minSkylight && biomeTemperature > 0.4;


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

            // Introduce stabilizing factor to bring temperature back to a central value
            double stabilizingFactor = (bodyTemperatureMap.getOrDefault(player, 0.0) - 18.0) * 0.01;
            adjustedChangeRate -= stabilizingFactor;

            // Adjust the temperature to not go below 37 during the day in the shadow of a hot biome
            if (isShadowDuringDayInHotBiome) {
                adjustedChangeRate = Math.max(0.0, adjustedChangeRate);
            }
            // Apply time-based adjustments
            if (isDay) {
                adjustedChangeRate *= getDaytimeMultiplier(time);
            } else {
                adjustedChangeRate /= getNighttimeDeMultiplier(time);
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
    private double getDaytimeMultiplier(long time) {
        // Adjust this function to make the rate stronger during daytime

        // Sunrise and sunset times
        long sunriseStart = 5000; // 5:00 AM
        long sunriseEnd = 7000;   // 7:00 AM
        long sunsetStart = 17000; // 5:00 PM
        long sunsetEnd = 19000;   // 7:00 PM

        // Get the current multiplier based on time of day
        if (time >= sunriseStart && time <= sunriseEnd) {
            // Increase temperature slightly during sunrise
            return 0.9; // Adjust the multiplier as needed
        } else if (time > sunriseEnd && time < sunsetStart) {
            // Gradually increase temperature from sunrise to sunset
            return 1.0; // Adjust the multiplier as needed
        } else if (time >= sunsetStart && time <= sunsetEnd) {
            // Decrease temperature slightly during sunset
            return 0.9; // Adjust the multiplier as needed
        } else {
            return 1.0; // Default multiplier
        }
    }

    private double getNighttimeDeMultiplier(long time) {
        // Adjust this function to make the rate decrease stronger during nighttime

        // Midnight time
        long midnightStart = 18000; // 6:00 PM
        long midnightEnd = 24000;   // 12:00 AM

        // Get the current multiplier based on time of night
        if ((time >= 0 && time <= 4000) || (time >= midnightStart && time <= midnightEnd)) {
            // Increase temperature slightly during midnight and early morning
            return 0.8; // Example: Make the rate decrease 20% stronger during nighttime
        } else {
            return 1.0; // Default multiplier
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
            double stabilizingFactor = (currentTemperature - 18.0) * 0.01;
            currentTemperature -= stabilizingFactor;

            // Ensure the temperature stays within the specified range
            currentTemperature = Math.max(MIN_TEMPERATURE, Math.min(MAX_TEMPERATURE, currentTemperature));

            bodyTemperatureMap.put(player, currentTemperature);

            updateActionBar(player);
        }
    }

    private double getFreezingFactor(double biomeTemperature, boolean isDay) {
        double factor = 0.0;

        if (!isDay) {
            if (biomeTemperature <= -0.7) {
                factor = -Math.pow(Math.abs(biomeTemperature), 1.7) * 67.22; // Adjust the factor based on your preference
            } else if (biomeTemperature <= 0.247) {
                factor = -Math.pow(Math.abs(biomeTemperature), 1.64) * 53.61; // Adjust the factor based on your preference
            }
        } else {
            if (biomeTemperature <= -0.7) {
                factor = -Math.pow(Math.abs(biomeTemperature), 1.56) * 28.61; // Adjust the factor based on your preference
            } else if (biomeTemperature <= 0.247) {
                factor = -Math.pow(Math.abs(biomeTemperature), 1.52) * 24.24; // Adjust the factor based on your preference
            }
        }

        // Introduce non-linear function to make it harder to go up and down near temperature limits
        factor *= Math.exp(-Math.pow((Math.abs(biomeTemperature) - 10.0) / 5.0, 2));

        return factor;
    }

    private double getHeatFactor(double biomeTemperature, boolean isDay) {
        double factor = 0.0;

        if (isDay) {
            if (biomeTemperature >= 1.46) {
                factor = Math.pow(Math.abs(biomeTemperature), 2.2) * 32.22; // Adjust the factor based on your preference
            } else if (biomeTemperature >= 0.76) {
                factor = Math.pow(Math.abs(biomeTemperature), 1.7) * 17.61; // Adjust the factor based on your preference
            }
        } else {
            if (biomeTemperature >= 1.46) {
                factor = Math.pow(Math.abs(biomeTemperature), 1.32) * 0.01; // Adjust the factor based on your preference
            } else if (biomeTemperature >= 0.76) {
                factor = Math.pow(Math.abs(biomeTemperature), 1.15) * 0.005; // Adjust the factor based on your preference
            }
        }

        // Introduce non-linear function to make it harder to go up and down near temperature limits
        factor *= Math.exp(-Math.pow((Math.abs(biomeTemperature) - 10.0) / 5.0, 2));

        return factor;
    }

    private double calculateTemperatureBiome(Player player) {
        Location location = player.getLocation();
        long time = player.getWorld().getTime();
        boolean isDay = time >= 0 && time < 12000;

        // Synchronize chunk loading on the main thread
        double biomeTemperature = 0.0;
        synchronized (this) {
            // Ensure chunk is loaded before accessing block information
            if (location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                biomeTemperature = location.getBlock().getTemperature();
            }
        }

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
        long time = player.getWorld().getTime();  // Add this line to get the current time

        new WrappedRunnable() {
            @Override
            public void run() {
                Biome biome = location.getBlock().getBiome();
                double biomeTemperature = location.getBlock().getTemperature();

                // Get time-specific multipliers
                double daytimeMultiplier = getDaytimeMultiplier(time);
                double nighttimeMultiplier = getNighttimeDeMultiplier(time);

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
                        ChatColor.GRAY + " | DayMult:" + ChatColor.GOLD + String.format("%.2f", daytimeMultiplier) +
                        ChatColor.GRAY + " | NightMult:" + ChatColor.DARK_PURPLE + String.format("%.2f", nighttimeMultiplier) +
                        biomeInfo;

                // Send action bar message to the player
                player.sendActionBar(actionBarMessage);
            }
        }.runTaskAtLocation(this, location);
    }


}

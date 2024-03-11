package net.hynse.embersculpt;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SkyLight {
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

        // Check if the player is in the Nether
        boolean isNether = player.getWorld().getEnvironment().equals(org.bukkit.World.Environment.NETHER);
        boolean isEnd = player.getWorld().getEnvironment().equals(org.bukkit.World.Environment.THE_END);

// Check if the player is in the shadow during the day in a hot biome
        boolean isShadowDuringDayInHotBiome = isDay && skylightLevel < minSkylight && biomeTemperature > 0.4;

// Check if the player is in the shadow during the night in a hot biome
        boolean isShadowDuringNightInHotBiome = !isDay && skylightLevel < minSkylight && biomeTemperature > 0.4;

        if (isDay) {
            // Adjust rates for the Nether
            if (isNether) {
                minRiseChangeRate = 0.3; // Adjust this value
                maxRiseChangeRate = 1.5; // Adjust this value
                minDecreaseChangeRate = 0.2; // Adjust this value
                maxDecreaseChangeRate = 0.7; // Adjust this value
            } else if (isEnd) {
                // Adjust rates for the End
                minRiseChangeRate = 0.01; // Adjust this value
                maxRiseChangeRate = 0.06; // Adjust this value
                minDecreaseChangeRate = -0.1; // Adjust this value
                maxDecreaseChangeRate = -0.6; // Adjust this value
            } else {
                // Adjust rates for the Overworld (day)
                minRiseChangeRate = 0.05; // Adjust this value
                maxRiseChangeRate = 1.13; // Adjust this value
                minDecreaseChangeRate = -0.02; // Adjust this value
                maxDecreaseChangeRate = -0.13; // Adjust this value
            }
        } else {
            // Adjust rates for the Nether
            if (isNether) {
                minRiseChangeRate = 0.3; // Adjust this value
                maxRiseChangeRate = 1.5; // Adjust this value
                minDecreaseChangeRate = 0.2; // Adjust this value
                maxDecreaseChangeRate = 0.7; // Adjust this value
            } else if (isEnd) {
                // Adjust rates for the End
                minRiseChangeRate = 0.01; // Adjust this value
                maxRiseChangeRate = 0.06; // Adjust this value
                minDecreaseChangeRate = -0.1; // Adjust this value
                maxDecreaseChangeRate = -0.6; // Adjust this value
            } else {
                // Adjust rates for the Overworld (night)
                minRiseChangeRate = 0.0001; // Adjust this value
                maxRiseChangeRate = 0.002; // Adjust this value
                minDecreaseChangeRate = -0.5; // Adjust this value
                maxDecreaseChangeRate = -0.3; // Adjust this value
            }
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
            double stabilizingFactor = (Embersculpt.instance.bodyTemperatureMap.getOrDefault(player, 0.0) - 18.0) * 0.01;
            adjustedChangeRate -= stabilizingFactor;

            // Adjust the temperature to not go below 37 during the day in the shadow of a hot biome
            if (isShadowDuringDayInHotBiome) {
                adjustedChangeRate = Math.max(0.0, adjustedChangeRate);
            }
            // Apply time-based adjustments
            ItemStack[] playerArmor = player.getInventory().getArmorContents();
            if (isDay) {
                adjustedChangeRate *= Embersculpt.multiplier.getDaytimeMultiplier(time, true, playerArmor);
            }
            if (!isDay && !isNether) {
                adjustedChangeRate /= Embersculpt.multiplier.getNighttimeDeMultiplier(time, false, playerArmor);
            }

            return adjustedChangeRate;
        } else if (isShadowDuringNightInHotBiome && Embersculpt.instance.bodyTemperatureMap.getOrDefault(player, 0.0) > 24) {
            // If in the shadow during the night and body temperature is greater than 24, slowly decrease temperature
            return Math.max(-1.0, (24 - Embersculpt.instance.bodyTemperatureMap.getOrDefault(player, 0.0)));
        } else {
            // If skylight level is above the maximum or not in the shadow, no additional change
            return 0.0;
        }
    }
    public double calculateTemperatureSkyLightChange(Player player, int skylightLevel) {
        // Calculate biome temperature change
        double biomeTemperatureChange = Embersculpt.factor.calculateTemperatureBiome(player);

        // Call the original method with biomeTemperatureChange
        return calculateTemperatureSkyLightChange(player, skylightLevel, biomeTemperatureChange);
    }
}

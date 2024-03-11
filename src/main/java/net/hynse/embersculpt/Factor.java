package net.hynse.embersculpt;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Factor {
    public double getFreezingFactor(double biomeTemperature, boolean isDay) {
        double factor = 0.0;

        if (!isDay) {
            if (biomeTemperature <= -0.6) {
                factor = Math.pow(Math.abs(biomeTemperature), 5.0) * 1.22; // Adjust the factor based on your preference
            } else if (biomeTemperature <= 0.247) {
                factor = Math.pow(Math.abs(biomeTemperature), 3.0) * 4.61; // Adjust the factor based on your preference
            }
        }

        if (isDay) {
            if (biomeTemperature <= -0.6) {
                factor = Math.pow(Math.abs(biomeTemperature), 5.7) * 28.61; // Adjust the factor based on your preference
            } else if (biomeTemperature <= 0.247) {
                factor = Math.pow(Math.abs(biomeTemperature), 2.52) * 24.24; // Adjust the factor based on your preference
            }
        }

        // Introduce non-linear function to make it harder to go up and down near temperature limits
        double decay = 1.0 / (1.0 + Math.exp(-((Math.abs(biomeTemperature) - 10.0) / 5.0)));
        factor *= decay;

        return factor;
    }


    public double getHeatFactor(double biomeTemperature, boolean isDay) {
        double factor = 0.0;

        if (isDay) {
            if (biomeTemperature >= 1.46) {
                factor = Math.pow(Math.abs(biomeTemperature), 2.2) * 32.22; // Adjust the factor based on your preference
            } else if (biomeTemperature >= 0.76) {
                factor = Math.pow(Math.abs(biomeTemperature), 1.7) * 17.61; // Adjust the factor based on your preference
            }
        }
        if (!isDay) {
            if (biomeTemperature >= 1.46) {
                factor = Math.pow(Math.abs(biomeTemperature), 1.2) * 0.01; // Adjust the factor based on your preference
            } else if (biomeTemperature >= 0.76) {
                factor = Math.pow(Math.abs(biomeTemperature), 0.7) * 0.005; // Adjust the factor based on your preference
            }
        }

        // Introduce non-linear function to make it harder to go up and down near temperature limits
        factor *= Math.exp(-Math.pow((Math.abs(biomeTemperature) - 10.0) / 5.0, 2));

        return factor;
    }


    public double calculateTemperatureBiome(Player player) {
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

        // Get the weather factor based on the current weather conditions
        double weatherFactor = getWeatherFactor(player.getWorld().hasStorm(), player.getWorld().isThundering());

        // Ensure the biome-specific temperature change stays within a reasonable range

        return Math.max(-0.1, Math.min(0.1, freezingFactor + heatingFactor + weatherFactor));
    }


    private double getWeatherFactor(boolean isRaining, boolean isThundering) {
        double weatherFactor = 0.0;

        // Add weather effects to the temperature change
        if (isRaining) {
            weatherFactor -= 1.6; // Adjust this value based on the impact of rain on temperature
        }

        if (isThundering) {
            weatherFactor -= 3.2; // Adjust this value based on the impact of thunderstorm on temperature
        }

        return weatherFactor;
    }
}

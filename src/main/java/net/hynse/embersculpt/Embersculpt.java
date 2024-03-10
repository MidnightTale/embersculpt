package net.hynse.embersculpt;

import com.google.gson.Gson;
import me.nahu.scheduler.wrapper.FoliaWrappedJavaPlugin;
import me.nahu.scheduler.wrapper.WrappedScheduler;
import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public final class Embersculpt extends FoliaWrappedJavaPlugin implements Listener {

    private static final int MAX_TEMPERATURE = 100;
    private static final int MIN_TEMPERATURE = -100;
    private final Map<UUID, Double> temperatureStorage = new HashMap<>();
    private final Gson gson = new Gson();

    @Override
    public void onEnable() {
        final WrappedScheduler scheduler = getScheduler();
        Bukkit.getPluginManager().registerEvents(this, this);

        new WrappedRunnable() {
            @Override
            public void run() {
                updatePlayerTemperature(scheduler);
            }
        }.runTaskTimer(this, 1L, 20L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load player temperature when they join
        Player player = event.getPlayer();
        loadPlayerTemperature(player);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Reset player temperature on death
        Player player = event.getEntity();
        setPlayerTemperature(player, 0.0);
        savePlayerTemperature(player);
    }

    private void updatePlayerTemperature(WrappedScheduler scheduler) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Skip dead players
            if (player.isDead()) {
                continue;
            }

            World world = player.getWorld();
            double temperatureChange = 0;

            int skylightLevel = player.getLocation().getBlock().getLightFromSky();
            int blockLightLevel = player.getLocation().getBlock().getLightFromBlocks();
            double biomeTemperature = player.getLocation().getBlock().getTemperature();

            // Determine if it's day or night based on the time in the world
            boolean isDay = world.getTime() < 13000;
            double skylightFactor = igroneskylightonnight(skylightLevel, isDay);


            // Calculate freezing factor based on biome and skylight level
            double freezingFactor = getFreezingFactor(biomeTemperature, skylightLevel, isDay);
            // Calculate weather factor
            double weatherFactor = getWeatherFactor(world.hasStorm(), world.isThundering());


            temperatureChange += getBiomeTemperatureChange(biomeTemperature);

            double timeFactor;
            if (isDay) {
                // Daytime
                timeFactor = 1.0 - (world.getTime() % 24000) / 24000.0; // Adjust as needed
            } else {
                // Nighttime
                timeFactor = -1.0 + (world.getTime() % 24000) / 24000.0; // Adjust as needed
            }
            double temperatureFactor = getPlayerTemperature(player) / MAX_TEMPERATURE;
            double exponentialFactor = Math.exp(-5 * temperatureFactor);
            exponentialFactor = Math.max(0.01, Math.min(0.99, exponentialFactor));
            // Adjust temperature based on light sources
            // Adjust temperature based on light sources
            if (isDay) {
                if (blockLightLevel >= 14) { // Near light source during daytime
                    temperatureChange += 0.1; // Increase temperature change directly
                    exponentialFactor += 0.05; // Adjust exponential factor
                }
            } else {
                if (blockLightLevel >= 14) { // Near light source during nighttime
                    temperatureChange -= 0.1; // Decrease temperature change directly
                    exponentialFactor -= 0.05; // Adjust exponential factor
                }
            }

// Additional adjustment for lower light levels
            if (blockLightLevel >= 8 && blockLightLevel < 14) {
                temperatureChange += 0.05; // Slight increase in temperature change
            }

// Adjust exponential factor limits
            exponentialFactor = Math.max(0.01, Math.min(0.99, exponentialFactor));

            if ((skylightLevel >= 9 && skylightLevel <= 15)) {
                temperatureChange += (0.01 + (timeFactor * 0.5) + (1.0 - 0.05) * ((skylightFactor - 8.0) / 7.0)) * exponentialFactor;
                updateActionBar(player, getPlayerTemperature(player), temperatureChange, skylightFactor, temperatureFactor, biomeTemperature, timeFactor, exponentialFactor, freezingFactor, weatherFactor);
            } else if (skylightLevel == 8) {
                temperatureChange -= Math.min(0.25, (timeFactor * 0.5) + (8 - skylightFactor) * 0.1) * exponentialFactor;
                if (getPlayerTemperature(player) + temperatureChange < 0) {
                    temperatureChange = -getPlayerTemperature(player);
                }
                updateActionBar(player, getPlayerTemperature(player), temperatureChange, skylightFactor, temperatureFactor, biomeTemperature, timeFactor, exponentialFactor, freezingFactor, weatherFactor);
            } else if (skylightLevel == 7) {
                temperatureChange -= Math.min(0.46, (timeFactor * 0.5) + (8 - skylightFactor) * 0.1) * exponentialFactor;
                if (getPlayerTemperature(player) + temperatureChange < 0) {
                    temperatureChange = -getPlayerTemperature(player);
                }
                updateActionBar(player, getPlayerTemperature(player), temperatureChange, skylightFactor, temperatureFactor, biomeTemperature, timeFactor, exponentialFactor, freezingFactor, weatherFactor);
            } else if (skylightLevel <= 6) {
                temperatureChange -= Math.min(0.66, (timeFactor * 0.5) + (8 - skylightFactor) * 0.1) * exponentialFactor;
                if (getPlayerTemperature(player) + temperatureChange < 0) {
                    temperatureChange = -getPlayerTemperature(player);
                }
                updateActionBar(player, getPlayerTemperature(player), temperatureChange, skylightFactor, temperatureFactor, biomeTemperature, timeFactor, exponentialFactor, freezingFactor, weatherFactor);
            }


            // Apply the freezing factor and weather factor
            temperatureChange += freezingFactor;
            temperatureChange += weatherFactor;

            double currentTemperature = getPlayerTemperature(player);
            double newTemperature = Math.min(MAX_TEMPERATURE, Math.max(MIN_TEMPERATURE, currentTemperature + temperatureChange));

            setPlayerTemperature(player, newTemperature);
        }
    }

    private double getFreezingFactor(double biomeTemperature, int skylightLevel, boolean isDay) {
        double freezingFactor = 0.0;

        // Adjust freezing factor based on biome temperature
        if (biomeTemperature < 0.2) {
            freezingFactor -= 0.2; // Example: Colder biome
        }

        // Adjust freezing factor based on time of day
        if (isDay) {
            // During daytime, reduce freezing factor
            freezingFactor -= 0.3;
        }

        // Adjust freezing factor based on skylight level
        if (skylightLevel < 6) {
            // Example: adjust freezing factor for low skylight levels
            freezingFactor -= 0.1;
        }

        return freezingFactor;
    }



    private double getBiomeTemperatureChange(double biomeTemperature) {
        // Adjust temperature based on biome temperature
        // You can return different values based on the biome temperature
        if (biomeTemperature < 0.2) {
            return -0.5; // Example: colder biome
        } else if (biomeTemperature > 0.8) {
            return 0.5; // Example: warmer biome
        } else {
            return 0; // Default: no biome-specific temperature change
        }
    }
    private double igroneskylightonnight(int skylightLevel, boolean isDay) {
        if (!isDay) {
            return 0;
        }

        return skylightLevel;
    }
    private double getWeatherFactor(boolean isStorming, boolean isThundering) {
        double weatherFactor = 0.0;

        if (isStorming) {
            weatherFactor -= 0.2; // Example: Reduce temperature during rain
        }

        if (isThundering) {
            weatherFactor -= 0.3; // Example: Reduce temperature even more during thunderstorm
        }

        return weatherFactor;
    }


    private void loadPlayerTemperature(Player player) {
        File playerFile = new File(getDataFolder() + "/data/", player.getUniqueId().toString() + ".json");
        try {
            if (!playerFile.exists()) {
                return; // No saved temperature for this player yet
            }

            try (Reader reader = new FileReader(playerFile)) {
                double temperature = gson.fromJson(reader, Double.class);
                setPlayerTemperature(player, temperature);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePlayerTemperature(Player player) {
        File playerFile = new File(getDataFolder() + "/data/", player.getUniqueId().toString() + ".json");
        try (Writer writer = new FileWriter(playerFile)) {
            gson.toJson(getPlayerTemperature(player), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double getPlayerTemperature(Player player) {
        return temperatureStorage.getOrDefault(player.getUniqueId(), 0.0);
    }

    private void setPlayerTemperature(Player player, double temperature) {
        temperatureStorage.put(player.getUniqueId(), temperature);
    }

    private void updateActionBar(Player player, double temperature, double temperatureChange, double skylightFactor, double temperatureFactor, double biomeTemperature, double timeFactor, double exponentialFactor, double freezingFactor, double weatherFactor) {
        int roundedTemperature = (int) Math.round(temperature);
        String formattedTemperatureChange = String.format("%.2f", temperatureChange);
        String formattedTemperatureFactor = String.format("%.2f", temperatureFactor);
        String formattedBiomeTemperature = String.format("%.2f", biomeTemperature);
        String formattedTimeFactor = String.format("%.2f", timeFactor);
        String formattedExponentialFactor = String.format("%.2f", exponentialFactor);
        String formattedFreezingFactor = String.format("%.2f", freezingFactor);
        String formattedWeatherFactor = String.format("%.2f", weatherFactor);

        String actionBarMessage = ChatColor.GOLD + "Body: " + ChatColor.RED + roundedTemperature
                + ChatColor.YELLOW + " | R: " + formattedTemperatureChange
                + ChatColor.YELLOW + " | S: " + skylightFactor
                + ChatColor.BLUE + " | TempF: " + formattedTemperatureFactor
                + ChatColor.DARK_GREEN + " | B: " + formattedBiomeTemperature
                + ChatColor.DARK_PURPLE + " | TimeF: " + formattedTimeFactor
                + ChatColor.AQUA + " | ExpF: " + formattedExponentialFactor
                + ChatColor.BLUE + " | FreezeF: " + formattedFreezingFactor
                + ChatColor.GRAY + " | WeatherF: " + formattedWeatherFactor;
        player.sendActionBar(actionBarMessage);
    }
}

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
    private final File configFile = new File(getDataFolder(), "player_temperatures.json");
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
            World world = player.getWorld();
            double temperatureChange = 0;

            int skylightLevel = player.getLocation().getBlock().getLightFromSky();
            double biomeTemperature = player.getLocation().getBlock().getTemperature();

            // Determine if it's day or night based on the time in the world
            boolean isDay = world.getTime() < 13000;

            temperatureChange += getBiomeTemperatureChange(biomeTemperature);

            if (skylightLevel >= 8 && skylightLevel <= 15) {
                // Adjust temperature based on day or night
                double temperatureFactor = getPlayerTemperature(player) / MAX_TEMPERATURE;
                double exponentialFactor = Math.exp(-5 * temperatureFactor);
                double timeFactor = isDay ? 1.0 : 0.5; // Adjust as needed
                temperatureChange += (0.05 + (timeFactor * 0.5) + (1.0 - 0.05) * ((skylightLevel - 8.0) / 7.0)) * exponentialFactor;
                updateActionBar(player, getPlayerTemperature(player), temperatureChange, skylightLevel, temperatureFactor, biomeTemperature, timeFactor);
            } else if (skylightLevel == 8) {
                // Adjust temperature based on day or night
                double temperatureFactor = getPlayerTemperature(player) / MAX_TEMPERATURE;
                double exponentialFactor = Math.exp(-5 * temperatureFactor);
                double timeFactor = isDay ? 1.0 : 0.5; // Adjust as needed
                temperatureChange -= Math.min(0.3, (timeFactor * 0.5) + (8 - skylightLevel) * 0.1);
                if (getPlayerTemperature(player) + temperatureChange < 0) {
                    temperatureChange = -getPlayerTemperature(player);
                }
                updateActionBar(player, getPlayerTemperature(player), temperatureChange, skylightLevel, temperatureFactor, biomeTemperature, timeFactor);
            } else if (skylightLevel == 7) {
                // Adjust temperature based on day or night
                double temperatureFactor = getPlayerTemperature(player) / MAX_TEMPERATURE;
                double exponentialFactor = Math.exp(-5 * temperatureFactor);
                double timeFactor = isDay ? 1.0 : 0.5; // Adjust as needed
                temperatureChange -= Math.min(0.6, (timeFactor * 0.5) + (8 - skylightLevel) * 0.1);
                if (getPlayerTemperature(player) + temperatureChange < 0) {
                    temperatureChange = -getPlayerTemperature(player);
                }
                updateActionBar(player, getPlayerTemperature(player), temperatureChange, skylightLevel, temperatureFactor, biomeTemperature, timeFactor);
            } else if (skylightLevel < 6) {
                // Adjust temperature based on day or night
                double temperatureFactor = getPlayerTemperature(player) / MAX_TEMPERATURE;
                double exponentialFactor = Math.exp(-5 * temperatureFactor);
                double timeFactor = isDay ? 1.0 : 0.5; // Adjust as needed
                temperatureChange -= Math.min(0.9, (timeFactor * 0.5) + (8 - skylightLevel) * 0.1);
                if (getPlayerTemperature(player) + temperatureChange < 0) {
                    temperatureChange = -getPlayerTemperature(player);
                }
                updateActionBar(player, getPlayerTemperature(player), temperatureChange, skylightLevel, temperatureFactor, biomeTemperature, timeFactor);
            }

            double currentTemperature = getPlayerTemperature(player);
            double newTemperature = Math.min(MAX_TEMPERATURE, Math.max(MIN_TEMPERATURE, currentTemperature + temperatureChange));

            setPlayerTemperature(player, newTemperature);
        }
    }

    private double getBiomeTemperatureChange(double biomeTemperature) {
        // Implement your logic for biome-based temperature changes here
        // You can return different values based on the biome temperature
        // For example, colder biomes return negative values, warmer biomes return positive values
        if (biomeTemperature < 0.2) {
            return -0.5; // Example: colder biome
        } else if (biomeTemperature > 0.8) {
            return 0.5; // Example: warmer biome
        } else {
            return 0; // Default: no biome-specific temperature change
        }
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

    private void updateActionBar(Player player, double temperature, double temperatureChange, int skylightLevel, double temperatureFactor, double biomeTemperature, double timeFactor) {
        int roundedTemperature = (int) Math.round(temperature);
        String formattedTemperatureChange = String.format("%.2f", temperatureChange);
        String formattedTemperatureFactor = String.format("%.2f", temperatureFactor);
        String formattedBiomeTemperature = String.format("%.2f", biomeTemperature);
        String formattedTimeFactor = String.format("%.2f", timeFactor);

        String actionBarMessage = ChatColor.GOLD + "Temperature: " + ChatColor.RED + roundedTemperature
                + ChatColor.YELLOW + " | Rate: " + formattedTemperatureChange
                + ChatColor.YELLOW + " | S-Level: " + skylightLevel
                + ChatColor.BLUE + " | H-Factor: " + formattedTemperatureFactor
                + ChatColor.DARK_GREEN + " | Biome: " + formattedBiomeTemperature
                + ChatColor.DARK_PURPLE + " | T-Factor: " + formattedTimeFactor;
        player.sendActionBar(actionBarMessage);
    }
}

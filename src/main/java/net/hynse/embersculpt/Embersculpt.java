package net.hynse.embersculpt;

import me.nahu.scheduler.wrapper.FoliaWrappedJavaPlugin;
import me.nahu.scheduler.wrapper.WrappedScheduler;
import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Embersculpt extends FoliaWrappedJavaPlugin implements Listener {

    private static final int MAX_TEMPERATURE = 100;
    private static final int MIN_TEMPERATURE = -100;
    private final Map<UUID, Double> temperatureStorage = new HashMap<>();
    private final File configFile = new File(getDataFolder(), "player_temperatures.yml");

    @Override
    public void onEnable() {
        loadPlayerTemperatures(); // Load player temperatures on server start
        final WrappedScheduler scheduler = getScheduler();
        Bukkit.getPluginManager().registerEvents(this, this);

        new WrappedRunnable() {
            @Override
            public void run() {
                updatePlayerTemperature(scheduler);
            }
        }.runTaskTimer(this, 1L, 20L);
    }

    @Override
    public void onDisable() {
        savePlayerTemperatures(); // Save player temperatures on server shutdown
    }

    private void updatePlayerTemperature(WrappedScheduler scheduler) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            World world = player.getWorld();
            double temperatureChange = 0;

            int skylightLevel = player.getLocation().getBlock().getLightFromSky();

            if (skylightLevel >= 8 && skylightLevel <= 15) {
                temperatureChange += 0.05 + (1.0 - 0.05) * ((skylightLevel - 8.0) / 7.0);
            } else if (skylightLevel < 8) {
                temperatureChange -= Math.min(0.1, (8 - skylightLevel) * 0.1);
            }

            double currentTemperature = getPlayerTemperature(player);
            double newTemperature = Math.min(MAX_TEMPERATURE, Math.max(MIN_TEMPERATURE, currentTemperature + temperatureChange));

            setPlayerTemperature(player, newTemperature);
            updateActionBar(player, newTemperature, temperatureChange, skylightLevel);
        }
    }

    private void loadPlayerTemperatures() {
        if (!configFile.exists()) {
            return; // No saved temperatures yet
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        for (String uuidString : config.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            double temperature = config.getDouble(uuidString);
            temperatureStorage.put(uuid, temperature);
        }
    }

    private void savePlayerTemperatures() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, Double> entry : temperatureStorage.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }

        try {
            config.save(configFile);
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

    private void updateActionBar(Player player, double temperature, double temperatureChange, int skylightLevel) {
        int roundedTemperature = (int) Math.round(temperature);
        String formattedTemperatureChange = String.format("%.2f", temperatureChange);

        String actionBarMessage = ChatColor.GOLD + "Temperature: " + ChatColor.RED + roundedTemperature
                + ChatColor.YELLOW + " | Rate Change: " + formattedTemperatureChange
                + ChatColor.YELLOW + " | Skylight Level: " + skylightLevel;
        player.sendActionBar(actionBarMessage);
    }
}

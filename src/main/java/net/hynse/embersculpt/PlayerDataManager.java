package net.hynse.embersculpt;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class PlayerDataManager {
    private final Embersculpt plugin;

    public PlayerDataManager(Embersculpt plugin) {
        this.plugin = plugin;
    }

    public void savePlayerTemperature(Player player) {
        File playerFile = getPlayerFile(player);
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        double temperature = plugin.getBodyTemperature(player);

        config.set("temperature", temperature);

        try {
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double loadPlayerTemperature(Player player) {
        File playerFile = getPlayerFile(player);
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        if (playerFile.exists() && config.contains("temperature")) {
            return config.getDouble("temperature");
        }

        return 0.0; // Default temperature
    }

    private File getPlayerFile(Player player) {
        String playerId = player.getUniqueId().toString();
        return new File(plugin.getDataFolder(), "playerdata/" + playerId + ".yml");
    }
}
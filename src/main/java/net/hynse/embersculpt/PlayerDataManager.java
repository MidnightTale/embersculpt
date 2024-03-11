package net.hynse.embersculpt;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerDataManager {
    private final Embersculpt plugin;

    public PlayerDataManager(Embersculpt plugin) {
        this.plugin = plugin;
    }

    public void savePlayerTemperature(UUID playerId) {
        File playerFile = getPlayerFile(playerId);
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        double temperature = plugin.getBodyTemperature(playerId);

        config.set("temperature", temperature);

        try {
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double loadPlayerTemperature(UUID playerId) {
        File playerFile = getPlayerFile(playerId);
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        if (playerFile.exists() && config.contains("temperature")) {
            return config.getDouble("temperature");
        }

        return 0.0; // Default temperature
    }

    public File getPlayerFile(UUID playerId) {
        String playerIdString = playerId.toString();
        return new File(plugin.getDataFolder(), "playerdata/" + playerIdString + ".yml");
    }

}
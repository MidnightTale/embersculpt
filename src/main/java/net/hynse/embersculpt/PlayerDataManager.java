package net.hynse.embersculpt;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class PlayerDataManager {
    public HashMap<Player, Double> bodyTemperatureMap;
    private final Embersculpt plugin;

    public PlayerDataManager(Embersculpt plugin) {
        this.plugin = plugin;
    }

    public void savePlayerTemperature(Player player) {
        File playerFile = getPlayerFile(player);
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        double temperature = Embersculpt.util.getBodyTemperature(player);

        config.set("temperature", temperature);
        Embersculpt.instance.getLogger().info("Saved temperature for player " + player.getName() + ": " + temperature);

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
        Embersculpt.instance.getLogger().info("Loaded temperature for player " + player.getName() + ": " + config);


        return 0.0; // Default temperature
    }

    private File getPlayerFile(Player player) {
        String playerId = player.getUniqueId().toString();
        return new File(plugin.getDataFolder(), "playerdata/" + playerId + ".yml");
    }
}
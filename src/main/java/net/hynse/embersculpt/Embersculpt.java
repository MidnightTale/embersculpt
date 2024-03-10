package net.hynse.embersculpt;

import com.google.gson.Gson;
import me.nahu.scheduler.wrapper.FoliaWrappedJavaPlugin;
import me.nahu.scheduler.wrapper.WrappedScheduler;
import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.bukkit.Material.*;

public final class Embersculpt extends FoliaWrappedJavaPlugin implements Listener {

    private static final int MAX_TEMPERATURE = 100;
    private static final int MIN_TEMPERATURE = -100;
    private final Map<UUID, Double> temperatureStorage = new HashMap<>();
    private final Gson gson = new Gson();
    private static final long MAX_SPRINT_DURATION = 60000; // Maximum sprinting duration in milliseconds (e.g., 60 seconds)
    private static final long MAX_WALK_DURATION = 120000;  // Maximum walking duration in milliseconds (e.g., 120 seconds)
    private Map<UUID, Long> sprintStartTime = new HashMap<>();
    private Map<UUID, Long> walkStartTime = new HashMap<>();

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
        Player player = event.getPlayer();
        loadPlayerTemperature(player);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        setPlayerTemperature(player, 37.0); // Set default temperature to 37°C (normal human body temperature)
        savePlayerTemperature(player);
    }

    @Override
    public void onDisable() {
        // Save temperatures of online players when the server stops
        saveAllPlayerTemperatures();
    }

    private void updatePlayerTemperature(WrappedScheduler scheduler) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead()) {
                continue;
            }

            World world = player.getWorld();
            double temperatureChange = 0;

            int skylightLevel = player.getLocation().getBlock().getLightFromSky();
            int blockLightLevel = player.getLocation().getBlock().getLightFromBlocks();
            double biomeTemperature = player.getLocation().getBlock().getTemperature();

            boolean isDay = world.getTime() < 13000;
            double skylightFactor = isDay ? skylightLevel : 0;

            double freezingFactor = getFreezingFactor(biomeTemperature, skylightFactor, isDay);
            double weatherFactor = getWeatherFactor(world.hasStorm(), world.isThundering());

            temperatureChange += getBiomeTemperatureChange(biomeTemperature);

            double timeFactor = isDay ? (1.0 - (world.getTime() % 24000) / 24000.0) : (-1.0 + (world.getTime() % 24000) / 24000.0);
            double temperatureFactor = getPlayerTemperature(player) / MAX_TEMPERATURE;
            double exponentialFactor = Math.exp(-5 * temperatureFactor);
            exponentialFactor = Math.max(0.01, Math.min(0.99, exponentialFactor));

            // Adjust temperature based on light sources
            temperatureChange += adjustTemperatureBasedOnLight(player, blockLightLevel, isDay, exponentialFactor, biomeTemperature);

            // Additional adjustment for lower light levels
            if (blockLightLevel >= 8 && blockLightLevel < 14) {
                temperatureChange += 0.02; // Slight increase in temperature change
            }

            // Adjust temperature based on skylight level
            temperatureChange += adjustTemperatureBasedOnSkylight(skylightFactor, timeFactor, skylightFactor, exponentialFactor);

            // Apply the freezing factor and weather factor
            temperatureChange += freezingFactor + weatherFactor;
            double armorInsulationFactor = getArmorInsulationFactor(player);
            double physicalActivityFactor = getPhysicalActivityFactor(player);

            // Apply armor insulation factor
            temperatureChange += armorInsulationFactor;

            // Apply physical activity factor
            temperatureChange += physicalActivityFactor;
            double currentTemperature = getPlayerTemperature(player);
            double newTemperature = Math.min(MAX_TEMPERATURE, Math.max(MIN_TEMPERATURE, currentTemperature + temperatureChange));

            setPlayerTemperature(player, newTemperature);
            updateActionBar(player, newTemperature, temperatureChange, skylightFactor, temperatureFactor, biomeTemperature, timeFactor, exponentialFactor, freezingFactor, weatherFactor, physicalActivityFactor, armorInsulationFactor);
        }
    }

    private double adjustTemperatureBasedOnLight(Player player, int blockLightLevel, boolean isDay, double exponentialFactor, double biomeTemperature) {
        double temperatureChange = 0;

        // Set target temperature based on block light levels during day and night
        double targetTemperature = 0;

        if (isDay) {
            if (blockLightLevel >= 14) {
                targetTemperature = 38.0; // High target temperature during the day with bright sunlight
            } else if (blockLightLevel >= 11 && blockLightLevel < 14) {
                // Moderate light level during the day
                if (biomeTemperature < 0.2) {
                    targetTemperature = 34.0;
                } else if (biomeTemperature < 0.5) {
                    targetTemperature = 36.0;
                }
            } else if (blockLightLevel >= 8 && blockLightLevel < 11) {
                // Lower light level during the day
                if (biomeTemperature < 0.2) {
                    targetTemperature = 30.0;
                } else if (biomeTemperature < 0.5) {
                    targetTemperature = 32.0;
                }
            }
        } else {
            if (blockLightLevel >= 14) {
                targetTemperature = 25.0; // Lower target temperature during the night with bright moonlight
            } else if (blockLightLevel >= 11 && blockLightLevel < 14) {
                // Moderate light level during the night
                if (biomeTemperature < 0.2) {
                    targetTemperature = 22.0;
                } else if (biomeTemperature < 0.5) {
                    targetTemperature = 23.0;
                }
            } else if (blockLightLevel >= 8 && blockLightLevel < 11) {
                // Lower light level during the night
                if (biomeTemperature < 0.2) {
                    targetTemperature = 20.0;
                } else if (biomeTemperature < 0.5) {
                    targetTemperature = 21.0;
                }
            }
        }

        // Adjust the current temperature towards the target
        double currentTemperature = getPlayerTemperature(player);
        temperatureChange += (targetTemperature - currentTemperature) * 0.03; // Fine-tuned adjustment factor

        return temperatureChange;
    }

    private double adjustTemperatureBasedOnSkylight(double skylightLevel, double timeFactor, double skylightFactor, double exponentialFactor) {
        double temperatureChange = 0;

        // Adjust temperature based on skylight levels
        if (skylightLevel >= 12 && skylightLevel <= 15) {
            temperatureChange += (0.01 + (timeFactor * 0.5) + (1.0 - 0.05) * ((skylightFactor - 8.0) / 7.0)) * exponentialFactor;
        } else if (skylightLevel == 11) {
            temperatureChange += (0.005 + (timeFactor * 0.5) + (1.0 - 0.05) * ((skylightFactor - 8.0) / 3.0)) * exponentialFactor;
        } else if (skylightLevel == 10) {
            temperatureChange += (0.003 + (timeFactor * 0.5) + (1.0 - 0.05) * ((skylightFactor - 8.0) / 2.0)) * exponentialFactor;
        } else if (skylightLevel == 9) {
            temperatureChange += (0.002 + (timeFactor * 0.5) + (1.0 - 0.05) * ((skylightFactor - 8.0) / 2.0)) * exponentialFactor;
        } else if (skylightLevel == 8) {
            temperatureChange -= Math.min(0.25, (timeFactor * 0.5) + (8 - skylightFactor) * 0.1) * exponentialFactor;
        } else if (skylightLevel == 7) {
            temperatureChange -= Math.min(0.46, (timeFactor * 0.5) + (8 - skylightFactor) * 0.1) * exponentialFactor;
        } else if (skylightLevel <= 6) {
            temperatureChange -= Math.min(0.66, (timeFactor * 0.5) + (8 - skylightFactor) * 0.1) * exponentialFactor;
        }

        return temperatureChange;
    }

    private double getFreezingFactor(double biomeTemperature, double skylightFactor, boolean isDay) {
        double freezingFactor = 0.0;

        // Adjust freezing factor based on biome temperature, skylight levels, and time of day
        if (biomeTemperature < 0.1) {
            freezingFactor -= 0.3; // Extremely cold biome
        } else if (biomeTemperature < 0.2) {
            freezingFactor -= 0.2; // Very cold biome
        }

        if (isDay) {
            if (skylightFactor < 6) {
                freezingFactor -= 0.1; // Adjust freezing factor for low skylight levels during the day
            }
        } else {
            if (skylightFactor < 6) {
                freezingFactor -= 0.05; // Adjust freezing factor for low skylight levels during the night
            }
        }

        return freezingFactor;
    }

    private double getBiomeTemperatureChange(double biomeTemperature) {
        // Adjust temperature based on biome
        if (biomeTemperature < 0.1) {
            return -0.5; // Extremely cold biome
        } else if (biomeTemperature < 0.2) {
            return -0.3; // Very cold biome
        } else if (biomeTemperature > 0.8) {
            return 0.5; // Extremely warm biome
        } else if (biomeTemperature > 0.7) {
            return 0.3; // Very warm biome
        } else {
            return 0; // Default: no biome-specific temperature change
        }
    }

    private double getWeatherFactor(boolean isStorming, boolean isThundering) {
        double weatherFactor = 0.0;

        // Adjust temperature based on weather conditions
        if (isStorming) {
            weatherFactor -= 0.1; // Reduce temperature during rain
        }

        if (isThundering) {
            weatherFactor -= 0.2; // Reduce temperature even more during thunderstorm
        }

        return weatherFactor;
    }
    private double getArmorInsulationFactor(Player player) {
        double armorInsulationFactor = 0.0;

        // Calculate insulation factor based on equipped armor
        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (armorPiece != null && armorPiece.getType() != Material.AIR) {
                double insulationValue = getInsulationValue(armorPiece.getType());
                armorInsulationFactor += insulationValue;
            }
        }

        return armorInsulationFactor;
    }

    private double getInsulationValue(Material material) {
        // Define insulation values for different armor materials
        switch (material) {
            case LEATHER_HELMET:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_BOOTS:
                return 0.1;
            case IRON_HELMET:
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case IRON_BOOTS:
                return 0.2;
            case GOLDEN_HELMET:
            case GOLDEN_CHESTPLATE:
            case GOLDEN_LEGGINGS:
            case GOLDEN_BOOTS:
                return 0.15;
            case DIAMOND_HELMET:
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_BOOTS:
                return 0.3;
            default:
                return 0.0;
        }
    }

    private double getPhysicalActivityFactor(Player player) {
        double physicalActivityFactor = 0.0;

        // Check if the player is currently sprinting
        boolean isSprinting = player.isSprinting();

        // Check if the player is currently walking
        boolean isWalking = !isSprinting;

        // Define activity factor based on sprinting or walking duration
        if (isSprinting) {
            long sprintingDuration = System.currentTimeMillis() - sprintStartTime.getOrDefault(player.getUniqueId(), 0L);
            double sprintingFactor = Math.min(1.0, sprintingDuration / MAX_SPRINT_DURATION);
            physicalActivityFactor += 0.1 * sprintingFactor;
        } else if (isWalking) {
            long walkingDuration = System.currentTimeMillis() - walkStartTime.getOrDefault(player.getUniqueId(), 0L);
            double walkingFactor = Math.min(1.0, walkingDuration / MAX_WALK_DURATION);
            physicalActivityFactor += 0.05 * walkingFactor;
        }

        return physicalActivityFactor;
    }
    @EventHandler
    public void onPlayerSprintStart(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        if (event.isSprinting()) {
            sprintStartTime.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onPlayerWalkStart(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getTo().distanceSquared(event.getFrom()) > 0.001) {
            walkStartTime.put(player.getUniqueId(), System.currentTimeMillis());
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

        // Ensure parent directories exist
        File parentDirectory = playerFile.getParentFile();
        if (!parentDirectory.exists()) {
            parentDirectory.mkdirs();
        }

        try (Writer writer = new FileWriter(playerFile)) {
            gson.toJson(getPlayerTemperature(player), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAllPlayerTemperatures() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayerTemperature(player);
        }
    }

    private double getPlayerTemperature(Player player) {
        return temperatureStorage.getOrDefault(player.getUniqueId(), 37.0); // Default temperature to 37°C (normal human body temperature)
    }

    private void setPlayerTemperature(Player player, double temperature) {
        temperatureStorage.put(player.getUniqueId(), temperature);
    }

    private void updateActionBar(Player player, double temperature, double temperatureChange, double skylightFactor, double temperatureFactor, double biomeTemperature, double timeFactor, double exponentialFactor, double freezingFactor, double weatherFactor, double armorInsulationFactor, double physicalActivityFactor) {
        int roundedTemperature = (int) Math.round(temperature);
        String formattedTemperatureChange = String.format("%.2f", temperatureChange);
        String formattedTemperatureFactor = String.format("%.2f", temperatureFactor);
        String formattedBiomeTemperature = String.format("%.2f", biomeTemperature);
        String formattedTimeFactor = String.format("%.2f", timeFactor);
        String formattedExponentialFactor = String.format("%.2f", exponentialFactor);
        String formattedFreezingFactor = String.format("%.2f", freezingFactor);
        String formattedWeatherFactor = String.format("%.2f", weatherFactor);
        String formattedArmorInsulationFactor = String.format("%.2f", armorInsulationFactor);
        String formattedPhysicalActivityFactor = String.format("%.2f", physicalActivityFactor);

        String actionBarMessage = ChatColor.GOLD + "B: " + ChatColor.RED + roundedTemperature
                + ChatColor.YELLOW + " | R: " + formattedTemperatureChange
                + ChatColor.YELLOW + " | S: " + skylightFactor
                + ChatColor.BLUE + " | TF: " + formattedTemperatureFactor
                + ChatColor.DARK_GREEN + " | B: " + formattedBiomeTemperature
                + ChatColor.DARK_PURPLE + " | TiF: " + formattedTimeFactor
                + ChatColor.AQUA + " | EF: " + formattedExponentialFactor
                + ChatColor.BLUE + " | FF: " + formattedFreezingFactor
                + ChatColor.GRAY + " | WF: " + formattedWeatherFactor
                + ChatColor.DARK_GRAY + " | AI: " + formattedArmorInsulationFactor
                + ChatColor.DARK_GRAY + " | PA: " + formattedPhysicalActivityFactor;
        player.sendActionBar(actionBarMessage);
    }

}

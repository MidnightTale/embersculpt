package net.hynse.embersculpt;

import com.google.gson.Gson;
import me.nahu.scheduler.wrapper.FoliaWrappedJavaPlugin;
import me.nahu.scheduler.wrapper.WrappedScheduler;
import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
import org.bukkit.*;
import org.bukkit.block.Block;
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

public final class Embersculpt extends FoliaWrappedJavaPlugin implements Listener {

    private static final int MAX_TEMPERATURE = 100;
    private static final int MIN_TEMPERATURE = -100;
    private final Map<UUID, Double> temperatureStorage = new HashMap<>();
    private final Gson gson = new Gson();
    private static final long MAX_SPRINT_DURATION = 180000; // Maximum sprinting duration in milliseconds (e.g., 60 seconds)
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
        }.runTaskTimer(this, 1, 10);
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
            double HeatFactor = getHeatFactor(biomeTemperature, skylightFactor, isDay);
            double weatherFactor = getWeatherFactor(world.hasStorm(), world.isThundering());

            temperatureChange += getBiomeTemperatureChange(biomeTemperature);

            double timeFactor = isDay ? (1.0 - (world.getTime() % 24000) / 24000.0) : (-1.0 + (world.getTime() % 24000) / 24000.0);
            double temperatureFactor = getPlayerTemperature(player) / MAX_TEMPERATURE;
            double exponentialFactor = Math.exp(-5 * temperatureFactor);
            exponentialFactor = Math.max(0.01, Math.min(0.99, exponentialFactor));

            // Additional adjustment for lower light levels
            if (blockLightLevel >= 8 && blockLightLevel < 14) {
                temperatureChange += 0.02; // Slight increase in temperature change
            }

            // Adjust temperature based on skylight level
            temperatureChange += adjustTemperatureBasedOnLightAndBiome(player, isDay);
            temperatureChange += adjustTemperatureBasedOnSkylight(skylightFactor, timeFactor, skylightFactor, exponentialFactor);

            // Apply the freezing factor and weather factor
            temperatureChange += freezingFactor + weatherFactor;
            temperatureChange += HeatFactor;
            double armorInsulationFactor = getArmorInsulationFactor(player);
            double physicalActivityFactor = getPhysicalActivityFactor(player);

            // Apply armor insulation factor
            temperatureChange += armorInsulationFactor;

            // Apply physical activity factor
            temperatureChange += physicalActivityFactor;
            double currentTemperature = getPlayerTemperature(player);
            double newTemperature = Math.min(MAX_TEMPERATURE, Math.max(MIN_TEMPERATURE, currentTemperature + temperatureChange));

            setPlayerTemperature(player, newTemperature);
            updateActionBar(player, newTemperature, temperatureChange, skylightFactor, temperatureFactor, biomeTemperature, timeFactor, exponentialFactor, freezingFactor, HeatFactor, weatherFactor, physicalActivityFactor, armorInsulationFactor);
        }
    }

    private double adjustTemperatureBasedOnLightAndBiome(Player player, boolean isDay) {
        // Create a class to hold the variables
        class LightData {
            int totalLightLevel = 0;
            int validBlockCount = 0;
        }

        double temperatureChange = 0;

        // Get the player's location
        Location playerLocation = player.getLocation();

        // Set the range to search for nearby blocks
        int searchRange = 5;

        // Create an instance of the LightData class
        LightData lightData = new LightData();

        // Iterate through nearby blocks
        new WrappedRunnable() {
            @Override
            public void run() {
                for (int x = -searchRange; x <= searchRange; x++) {
                    for (int y = -searchRange; y <= searchRange; y++) {
                        for (int z = -searchRange; z <= searchRange; z++) {
                            Location blockLocation = playerLocation.clone().add(x, y, z);
                            Block block = blockLocation.getBlock();

                            // Check if the block is air (not solid) to avoid considering transparent blocks
                            if (!block.getType().isSolid()) {
                                int blockLightLevel = block.getLightFromBlocks();

                                // Accumulate the total light level and increment the valid block count
                                lightData.totalLightLevel += blockLightLevel;
                                lightData.validBlockCount++;
                            }
                        }
                    }
                }
            }
        }.runTaskAtLocation(this, playerLocation);

        // Calculate the average light level
        int averageLightLevel = (lightData.validBlockCount > 0) ? lightData.totalLightLevel / lightData.validBlockCount : 0;

        // Get biome temperature
        double biomeTemperature = playerLocation.getBlock().getTemperature();

        // Set target temperature based on average light level during day and night and biome temperature
        double targetTemperature = 0;

        if (isDay) {
            // Adjustments for daytime based on the average light level and biome temperature
            // (You can customize this part based on your desired temperature adjustments)
            if (averageLightLevel >= 14) {
                targetTemperature = 38.0 + (biomeTemperature * 0.2);
            } else if (averageLightLevel >= 11) {
                targetTemperature = 36.0 + (biomeTemperature * 0.6);
            } else if (averageLightLevel <= 8) {
                targetTemperature = 32.0 + (biomeTemperature * 0.7);
            }
        } else {
            // Adjustments for nighttime based on the average light level and biome temperature
            // (You can customize this part based on your desired temperature adjustments)
            if (averageLightLevel >= 14) {
                targetTemperature = 25.0 + (biomeTemperature * 4.6);
            } else if (averageLightLevel >= 11) {
                targetTemperature = 23.0 + (biomeTemperature * 3.7);
            } else if (averageLightLevel <= 8) {
                targetTemperature = 21.0 + (biomeTemperature * 3.2);
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
            temperatureChange += (0.0001 + (timeFactor * 0.7) + (1.0 - 0.05) * ((skylightFactor - 8.0) / 7.0)) * exponentialFactor;
        } else if (skylightLevel == 11) {
            temperatureChange += (0.0003 + (timeFactor * 0.65) + (1.0 - 0.05) * ((skylightFactor - 8.0) / 3.0)) * exponentialFactor;
        } else if (skylightLevel == 10) {
            temperatureChange += (0.0007 + (timeFactor * 0.63) + (1.0 - 0.05) * ((skylightFactor - 8.0) / 2.0)) * exponentialFactor;
        } else if (skylightLevel == 9) {
            temperatureChange += (0.002 + (timeFactor * 0.59) + (1.0 - 0.05) * ((skylightFactor - 8.0) / 2.0)) * exponentialFactor;
        } else if (skylightLevel == 8) {
            temperatureChange -= Math.min(0.01, (timeFactor * 0.53) + (8 - skylightFactor) * 0.1) * exponentialFactor;
        } else if (skylightLevel == 7) {
            temperatureChange -= Math.min(0.16, (timeFactor * 0.52) + (8 - skylightFactor) * 0.1) * exponentialFactor;
        } else if (skylightLevel == 6) {
            temperatureChange -= Math.min(0.24, (timeFactor * 0.48) + (8 - skylightFactor) * 0.1) * exponentialFactor;
        } else if (skylightLevel == 5) {
            temperatureChange -= Math.min(0.24, (timeFactor * 0.56) + (8 - skylightFactor) * 0.1) * exponentialFactor;
        } else if (skylightLevel == 4) {
            temperatureChange -= Math.min(0.32, (timeFactor * 0.72) + (8 - skylightFactor) * 0.1) * exponentialFactor;
        } else if (skylightLevel == 3) {
            temperatureChange -= Math.min(0.44, (timeFactor * 0.79) + (8 - skylightFactor) * 0.1) * exponentialFactor;
        } else if (skylightLevel == 2) {
            temperatureChange -= Math.min(0.56, (timeFactor * 0.82) + (8 - skylightFactor) * 0.1) * exponentialFactor;
        } else if (skylightLevel <= 1) {
            temperatureChange -= Math.min(0.69, (timeFactor * 0.84) + (8 - skylightFactor) * 0.1) * exponentialFactor;
        }

        return temperatureChange;
    }
    private double getHeatFactor(double biomeTemperature, double skylightFactor, boolean isDay) {
        double heatFactor = 0.0;
        if (isDay) {
            // Adjust heat factor based on biome temperature, skylight levels, and time of day
            if (skylightFactor <= 15 && biomeTemperature > 1.6) {
                heatFactor += 0.23; // Extremely warm biome
            } else if (skylightFactor <= 15 && biomeTemperature > 0.7) {
                heatFactor += 0.12; // Very warm biome
            }
        }
        if (isDay) {
            if (skylightFactor < 12) {
                heatFactor += 0.02; // Adjust heat factor for high skylight levels during the day
            }
        }
        if (skylightFactor < 6) {
            heatFactor += 0; // Adjust heat factor for high skylight levels during the night
        }

        return heatFactor;
    }

    private double getFreezingFactor(double biomeTemperature, double skylightFactor, boolean isDay) {
        double freezingFactor = 0.0;

        // Adjust freezing factor based on biome temperature, skylight levels, and time of day
        if (biomeTemperature < -0.5) {
            freezingFactor -= 0.7; // Extremely cold biome
        } else if (biomeTemperature < -0.2) {
            freezingFactor -= 0.5; // Very cold biome
        }

        if (isDay) {
            if (skylightFactor < 6) {
                freezingFactor -= 0.01; // Adjust freezing factor for low skylight levels during the day
            }
        }
        if (!isDay) {
            if (skylightFactor < 6) {
                freezingFactor -= 0.27; // Adjust freezing factor for low skylight levels during the night
            }
        }

        return freezingFactor;
    }

    private double getBiomeTemperatureChange(double biomeTemperature) {
        // Adjust temperature based on biome
        if (biomeTemperature < 0.1) {
            return -0.05; // Extremely cold biome
        } else if (biomeTemperature < 0.2) {
            return -0.03; // Very cold biome
        } else if (biomeTemperature > 1.6) {
            return 0.005; // Extremely warm biome
        } else if (biomeTemperature > 0.7) {
            return 0.01; // Very warm biome
        } else {
            return 0; // Default: no biome-specific temperature change
        }
    }

    private double getWeatherFactor(boolean isStorming, boolean isThundering) {
        double weatherFactor = 0.0;

        // Adjust temperature based on weather conditions
        if (isStorming) {
            weatherFactor -= 0.2; // Reduce temperature during rain
        }

        if (isThundering) {
            weatherFactor -= 0.5; // Reduce temperature even more during thunderstorm
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

        // Define activity factor based on sprinting or walking duration
        if (isSprinting) {
            long sprintingDuration = System.currentTimeMillis() - sprintStartTime.getOrDefault(player.getUniqueId(), 0L);
            double sprintingFactor = Math.min(1.0, sprintingDuration / MAX_SPRINT_DURATION);
            physicalActivityFactor += 0.1 * sprintingFactor;
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
        File playerFile = new File(getDataFolder() + "/data/", player.getUniqueId() + ".json");
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

    private void updateActionBar(Player player, double temperature, double temperatureChange, double skylightFactor, double temperatureFactor, double biomeTemperature, double timeFactor, double exponentialFactor, double freezingFactor, double weatherFactor, double armorInsulationFactor, double physicalActivityFactor, double HeatFactor) {
        int roundedTemperature = (int) Math.round(temperature);
        String formattedTemperatureFactor = String.format("%.2f", temperatureFactor);
        String formattedTemperatureChange = String.format("%.2f", temperatureChange);
        String formattedBiomeTemperature = String.format("%.2f", biomeTemperature);
        String formattedTimeFactor = String.format("%.2f", timeFactor);
        String formattedExponentialFactor = String.format("%.2f", exponentialFactor);
        String formattedFreezingFactor = String.format("%.2f", freezingFactor);
        String formattedHeatFactor = String.format("%.2f", HeatFactor);
        String formattedWeatherFactor = String.format("%.2f", weatherFactor);
        String formattedArmorInsulationFactor = String.format("%.2f", armorInsulationFactor);
        String formattedPhysicalActivityFactor = String.format("%.2f", physicalActivityFactor);

        String actionBarMessage = ChatColor.GOLD + "P: " + ChatColor.RED + roundedTemperature
                + ChatColor.YELLOW + "/R: " + formattedTemperatureChange
                + ChatColor.YELLOW + "/S: " + skylightFactor
                + ChatColor.BLUE + "/TF: " + formattedTemperatureFactor
                + ChatColor.DARK_GREEN + "/B: " + formattedBiomeTemperature
                + ChatColor.DARK_PURPLE + "/TiF: " + formattedTimeFactor
                + ChatColor.AQUA + "/EF: " + formattedExponentialFactor
                + ChatColor.BLUE + "/FF: " + formattedFreezingFactor
                + ChatColor.RED + "/HF: " + formattedHeatFactor
                + ChatColor.GRAY + "/WF: " + formattedWeatherFactor
                + ChatColor.DARK_GRAY + "/AI: " + formattedArmorInsulationFactor
                + ChatColor.DARK_GRAY + "/PA: " + formattedPhysicalActivityFactor;
        player.sendActionBar(actionBarMessage);
    }

}

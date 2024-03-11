package net.hynse.embersculpt;

import me.nahu.scheduler.wrapper.FoliaWrappedJavaPlugin;
import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public final class Embersculpt extends FoliaWrappedJavaPlugin implements Listener {
    private HashMap<Player, Double> bodyTemperatureMap;
    private PlayerDataManager playerDataManager;
    private static final double MAX_TEMPERATURE = 100.0;
    private static final double MIN_TEMPERATURE = -100.0;
    private static final long MAX_SPRINT_DURATION = 180000; // Maximum sprinting duration in milliseconds (e.g., 60 seconds)
    private int timeadd = 0;  // Add this line to declare 'timeadd'
    private int bodyTemperatureAddition = 30;  // Add this line to declare 'bodyTemperatureAddition'
    private Map<UUID, Long> sprintStartTime = new HashMap<>();
    private Map<UUID, Long> walkStartTime = new HashMap<>();

    @Override
    public void onEnable() {
        bodyTemperatureMap = new HashMap<>();
        playerDataManager = new PlayerDataManager(this);


        new WrappedRunnable() {
            @Override
            public void run() {
                updateBodyTemperature();
            }
        }.runTaskTimer(this, 1, 20);
        new WrappedRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    applyBodyTemperatureEffects(player);
                }
            }
        }.runTaskTimer(this, 1, 20);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (Player player : bodyTemperatureMap.keySet()) {
            playerDataManager.savePlayerTemperature(player);
        }
        bodyTemperatureMap.clear();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        double temperature = playerDataManager.loadPlayerTemperature(player);
        setBodyTemperature(player, temperature);
        updateActionBar(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerDataManager.savePlayerTemperature(player);
        bodyTemperatureMap.remove(player);
    }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Reset temperature data for the dead player
        Player player = event.getEntity();
        bodyTemperatureMap.remove(player);
    }

    public double getBodyTemperature(Player player) {
        return bodyTemperatureMap.getOrDefault(player, 0.0);
    }

    public void setBodyTemperature(Player player, double temperature) {
        bodyTemperatureMap.put(player, temperature);
    }

    private void applyBodyTemperatureEffects(Player player) {
        double bodyTemperature = getBodyTemperature(player);
        new WrappedRunnable() {
            @Override
            public void run() {
                if (bodyTemperature >= 50 && bodyTemperature <= 75) {
                    // Normal Conditions (No immediate effects)
                    // Characters feel comfortable with no adverse effects.
                    // No additional effects need to be applied.
                } else if (bodyTemperature > 75 && bodyTemperature <= 85) {
                    applyPotionEffect(player, PotionEffectType.SPEED, 30, 0);
                } else if (bodyTemperature > 85 && bodyTemperature <= 95) {
                    applyPotionEffect(player, PotionEffectType.WEAKNESS, 30, 0);
                } else if (bodyTemperature > 95 && bodyTemperature < 100) {
                    applyPotionEffect(player, PotionEffectType.CONFUSION, 20 * 10, 0);
                    applyPotionEffect(player, PotionEffectType.SPEED, 30, 1);
                } else if (bodyTemperature >= 100) {
                    applyPotionEffect(player, PotionEffectType.CONFUSION, 20 * 10, 3);
                    timeadd++;

                    if (timeadd >= bodyTemperatureAddition) {
                        player.setFireTicks(20 * 10);
                        applyPotionEffect(player, PotionEffectType.BLINDNESS, 30, 1);
                        applyPotionEffect(player, PotionEffectType.WEAKNESS, 30, 0);
                        applyPotionEffect(player, PotionEffectType.SPEED, 30, 3);
                        applyPotionEffect(player, PotionEffectType.WITHER, 30, 2);
                    }
                } else if (bodyTemperature >= 0 && bodyTemperature <= 25) {
                    // Normal Conditions (No immediate effects)
                    // Characters are comfortable without experiencing cold-related effects.
                    // No additional effects need to be applied.
                } else if (bodyTemperature > -20 && bodyTemperature <= 0) {
                    applyPotionEffect(player, PotionEffectType.SLOW, 30, 0);
                } else if (bodyTemperature > -40 && bodyTemperature <= -20) {
                    applyPotionEffect(player, PotionEffectType.SLOW_DIGGING, 30, 0);
                } else if (bodyTemperature > -60 && bodyTemperature <= -40) {
                    applyPotionEffect(player, PotionEffectType.DARKNESS, 30, 0);
                } else if (bodyTemperature > -80 && bodyTemperature <= -60) {
                    applyPotionEffect(player, PotionEffectType.CONFUSION, 20 * 10, 0);
                    applyPotionEffect(player, PotionEffectType.SLOW, 30, 1);
                } else if (bodyTemperature > -100 && bodyTemperature <= -80) {
                    player.setFreezeTicks(20 * 10);
                } else if (bodyTemperature == -100) {
                    timeadd++;
                    applyPotionEffect(player, PotionEffectType.SLOW, 30, 1);

                    if (timeadd >= bodyTemperatureAddition) {
                        player.setFireTicks(20 * 10);
                        applyPotionEffect(player, PotionEffectType.SLOW_DIGGING, 30, 1);
                        applyPotionEffect(player, PotionEffectType.WITHER, 20 * 10, 1);
                        applyPotionEffect(player, PotionEffectType.CONFUSION, 20 * 10, 0);
                        applyPotionEffect(player, PotionEffectType.SLOW, 30, 2);
                    }
                }
            }
        }.runTaskAtEntity(this, player);
    }

    private void applyPotionEffect(Player player, PotionEffectType type, int duration, int amplifier) {
        // Remove existing effects of the same type
        player.removePotionEffect(type);

        // Apply the new effect
        player.addPotionEffect(new PotionEffect(type, duration, amplifier, true));
    }


    private List<Double> getPlayerTemperatureHistory(Player player) {
        // Retrieve or initialize the player's temperature history
        List<Double> temperatureHistory = (List<Double>) player.getMetadata("temperatureHistory").get(0).value();

        if (temperatureHistory == null) {
            temperatureHistory = new ArrayList<>();
            player.setMetadata("temperatureHistory", new FixedMetadataValue(this, temperatureHistory));
        }

        return temperatureHistory;
    }

    // Scheduler to update temperature histor
    private void updateBodyTemperature(Player player) {
        // Add the current temperature to the player's history
        List<Double> temperatureHistory = getPlayerTemperatureHistory(player);
        double currentTemperature = getBodyTemperature(player);
        temperatureHistory.add(currentTemperature);

        // Limit the history size to prevent excessive memory usage
        int maxHistorySize = 100;
        if (temperatureHistory.size() > maxHistorySize) {
            temperatureHistory.remove(0);
        }
    }


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
            double stabilizingFactor = (bodyTemperatureMap.getOrDefault(player, 0.0) - 18.0) * 0.01;
            adjustedChangeRate -= stabilizingFactor;

            // Adjust the temperature to not go below 37 during the day in the shadow of a hot biome
            if (isShadowDuringDayInHotBiome) {
                adjustedChangeRate = Math.max(0.0, adjustedChangeRate);
            }
            // Apply time-based adjustments
            ItemStack[] playerArmor = player.getInventory().getArmorContents();
            if (isDay) {
                adjustedChangeRate *= getDaytimeMultiplier(time, isDay, playerArmor);
            }
            if (!isDay && !isNether) {
                adjustedChangeRate /= getNighttimeDeMultiplier(time, isDay, playerArmor);
            }

            return adjustedChangeRate;
        } else if (isShadowDuringNightInHotBiome && bodyTemperatureMap.getOrDefault(player, 0.0) > 24) {
            // If in the shadow during the night and body temperature is greater than 24, slowly decrease temperature
            return Math.max(-1.0, (24 - bodyTemperatureMap.getOrDefault(player, 0.0)));
        } else {
            // If skylight level is above the maximum or not in the shadow, no additional change
            return 0.0;
        }
    }

    private double getDaytimeMultiplier(long time, boolean isDay, ItemStack[] armorContents) {
        // Adjust this function to make the rate stronger during daytime

        // Sunrise and sunset times
        long sunriseStart = 5000; // 5:00 AM
        long sunriseEnd = 7000;   // 7:00 AM
        long sunsetStart = 17000; // 5:00 PM
        long sunsetEnd = 19000;   // 7:00 PM

        // Get the current multiplier based on time of day
        if (isDay) {
            if (time >= sunriseStart && time <= sunriseEnd) {
                // Increase temperature slightly during sunrise
                return interpolate(0.85, 1.0, (double) (time - sunriseStart) / (sunriseEnd - sunriseStart));
            } else if (time > sunriseEnd && time < sunsetStart) {
                // Gradually increase temperature from sunrise to sunset
                return interpolate(1.0, 1.9, (double) (time - sunriseEnd) / (sunsetStart - sunriseEnd));
            } else if (time >= sunsetStart && time <= sunsetEnd) {
                // Decrease temperature slightly during sunset
                return interpolate(1.9, 0.94, (double) (time - sunsetStart) / (sunsetEnd - sunsetStart));
            } else {
                double baseMultiplier = 1.2; // Default multiplier

                // Reduce the impact of armorContents on daytimeMultiplier
                double armorImpact = calculateArmorImpact(armorContents);
                return baseMultiplier - armorImpact;
            }
        }
        return 0;
    }

    // Update this method to reduce the impact of armorContents on the deMultiplier value
    private double getNighttimeDeMultiplier(long time, boolean isDay, ItemStack[] armorContents) {
        // Adjust this function to make the rate decrease stronger during nighttime

        // Midnight time
        long midnightStart = 18000; // 6:00 PM
        long midnightEnd = 24000;   // 12:00 AM

        // Get the current multiplier based on time of night
        if (!isDay) {
            if ((time >= 0 && time <= 4000) || (time >= midnightStart && time <= midnightEnd)) {
                // Increase temperature slightly during midnight and early morning
                return interpolate(1.62, 1.12, (double) (time - midnightStart) / (4000 + midnightStart));
            } else {
                double baseMultiplier = 1.12; // Default multiplier

                // Reduce the impact of armorContents on nighttimeDeMultiplier
                double armorImpact = calculateArmorImpact(armorContents);
                return baseMultiplier - armorImpact;
            }
        }
        return 0;
    }

    // New method to calculate the impact of armorContents on the multiplier values
    private double calculateArmorImpact(ItemStack[] armorContents) {
        double armorImpact = 0.0;

        for (ItemStack armorPiece : armorContents) {
            if (armorPiece != null && armorPiece.getType() != Material.AIR) {
                Material armorMaterial = armorPiece.getType();
                // Adjust these values based on your preference
                double impactValue = getArmorImpactValue(armorMaterial);
                armorImpact += impactValue;
            }
        }

        return armorImpact;
    }

    // New method to get the impact value of each armor piece
    private double getArmorImpactValue(Material armorMaterial) {
        // Adjust these values based on your preference
        switch (armorMaterial) {
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_BOOTS:
                return 0.1;
            case LEATHER_HELMET:
                return 0.15;
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case IRON_BOOTS:
                return 0.08;
            case IRON_HELMET:
                return 0.12;
            case GOLDEN_CHESTPLATE:
            case GOLDEN_LEGGINGS:
            case GOLDEN_BOOTS:
                return 0.13;
            case GOLDEN_HELMET:
                return 0.18;
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_BOOTS:
                return 0.2;
            case DIAMOND_HELMET:
                return 0.25;
            case NETHERITE_CHESTPLATE:
            case NETHERITE_LEGGINGS:
            case NETHERITE_BOOTS:
                return 0.25;
            case NETHERITE_HELMET:
                return 0.3;
            default:
                return 0.0;
        }
    }

    // Helper method for linear interpolation between two values
    private double interpolate(double start, double end, double t) {
        return start + t * (end - start);
    }


    private double calculateTemperatureSkyLightChange(Player player, int skylightLevel) {
        // Calculate biome temperature change
        double biomeTemperatureChange = calculateTemperatureBiome(player);

        // Call the original method with biomeTemperatureChange
        return calculateTemperatureSkyLightChange(player, skylightLevel, biomeTemperatureChange);
    }

    private void updateBodyTemperature() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Check if the player is in a death state
            if (player.isDead()) {
                continue; // Skip updating temperature for dead players
            }

            // Get skylight level at player's location
            int skylightLevel = player.getLocation().getBlock().getLightFromSky();

            // Calculate temperature changes based on skylight level and biome factor
            double skylightTemperatureChange = calculateTemperatureSkyLightChange(player, skylightLevel);

            // Get physical activity factor based on player's sprinting
            double physicalActivityFactor = getPhysicalActivityFactor(player);

            // Get the player's armor contents
            ItemStack[] armorContents = player.getInventory().getArmorContents();

            // Get temperature change based on proximity to heat sources
            double heatSourceTemperatureChange = calculateTemperatureHeatSources(player);
            new WrappedRunnable() {
                @Override
                public void run() {
                    // Check if the player is in water or lava
                    Block block = player.getLocation().getBlock();

                    double currentTemperature = bodyTemperatureMap.getOrDefault(player, 0.0);

                    if (block.getType() == Material.WATER || block.getType() == Material.WATER_CAULDRON) {
                        // Decrease the player's temperature until it reaches 0
                        if (currentTemperature > -100) {
                            currentTemperature -= (currentTemperature < -30 ? 0.7 : 0.2); // Adjust the rate at which temperature decreases
                        }
                    } else if (block.getType() == Material.POWDER_SNOW || block.getType() == Material.POWDER_SNOW_CAULDRON) {
                        // Decrease the player's temperature until it reaches -100
                        if (currentTemperature > -100) {
                            currentTemperature -= (currentTemperature < -50 ? 1.0 : 0.5); // Adjust the rate at which temperature decreases
                        }
                    } else if (block.getType() == Material.LAVA || block.getType() == Material.LAVA_CAULDRON) {
                        // Decrease the player's temperature until it reaches 0
                        if (currentTemperature < 100) {
                            currentTemperature += (currentTemperature > 0 ? 0.2 : 0.7); // Adjust the rate at which temperature decreases
                        }
                    } else if (block.getType() == Material.FIRE || block.getType() == Material.SOUL_FIRE || block.getType() == Material.CAMPFIRE || block.getType() == Material.SOUL_CAMPFIRE) {
                        // Decrease the player's temperature until it reaches 0
                        if (currentTemperature < 100) {
                            currentTemperature += (currentTemperature > 0 ? 0.5 : 0.8); // Adjust the rate at which temperature decreases
                        }
                    }

                    // Update the player's body temperature
                    bodyTemperatureMap.put(player, Math.min(100.0, Math.max(-100.0, currentTemperature)));
                }
            }.runTaskAtLocation(this, player.getLocation());


            // Combine all factors to update player's temperature
            double currentTemperature = bodyTemperatureMap.getOrDefault(player, 0.0);
            currentTemperature += skylightTemperatureChange;
            currentTemperature += physicalActivityFactor;
            currentTemperature += heatSourceTemperatureChange;

            // Ensure the temperature stays within the specified range
            double stabilizingFactor = calculateStabilizingFactor(currentTemperature, armorContents, player);
            currentTemperature -= stabilizingFactor;

            // Ensure the temperature stays within the specified range
            currentTemperature = Math.max(MIN_TEMPERATURE, Math.min(MAX_TEMPERATURE, currentTemperature));

            // Update the player's body temperature
            bodyTemperatureMap.put(player, currentTemperature);

            // Update the action bar for the player
            updateActionBar(player);
        }
    }


    private double calculateTemperatureHeatSources(Player player) {
        final double[] heatSourceTemperatureChange = {0.0};

        // Define the radius to search for heat sources
        int searchRadius = 5;

        // Get the player's location
        Location playerLocation = player.getLocation();
        new WrappedRunnable() {
            @Override
            public void run() {
                for (int x = -searchRadius; x <= searchRadius; x++) {
                    for (int y = -searchRadius; y <= searchRadius; y++) {
                        for (int z = -searchRadius; z <= searchRadius; z++) {
                            // Get the block at the current position
                            Location currentLocation = playerLocation.clone().add(x, y, z);
                            Material blockMaterial = currentLocation.getBlock().getType();

                            // Check if the block is a torch or lava
                            if (blockMaterial == Material.TORCH || blockMaterial == Material.LAVA || blockMaterial == Material.CAMPFIRE || blockMaterial == Material.SOUL_CAMPFIRE || blockMaterial == Material.LANTERN || blockMaterial == Material.SOUL_LANTERN || blockMaterial == Material.LIGHT || blockMaterial == Material.SHROOMLIGHT || blockMaterial == Material.GLOWSTONE || blockMaterial == Material.GLOW_ITEM_FRAME || blockMaterial == Material.GLOW_BERRIES || blockMaterial == Material.FIRE || blockMaterial == Material.SOUL_FIRE || blockMaterial == Material.SEA_LANTERN || blockMaterial == Material.OCHRE_FROGLIGHT || blockMaterial == Material.PEARLESCENT_FROGLIGHT || blockMaterial == Material.VERDANT_FROGLIGHT) {
                                // Calculate the distance from the player to the heat source
                                double distance = playerLocation.distance(currentLocation);

                                // Adjust this value based on the impact of heat sources on temperature
                                double proximityEffect = 1.0 - (distance / searchRadius);
                                heatSourceTemperatureChange[0] += 0.6 * proximityEffect;
                            }
                        }
                    }
                }
            }
        }.runTaskAtLocation(this, playerLocation);
        return heatSourceTemperatureChange[0];
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if the consumed item is water or a potion
        if (isWaterOrPotion(item.getType())) {
            // Apply temperature reduction for 30 seconds at a rate of 3 per second
            reduceTemperatureOnDrink(player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check if the player is in the area of a lingering potion
        if (isPlayerInLingeringCloud(player)) {
            // Apply low temperature effect
            reduceTemperatureOnLingeringCloud(player);
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();

        // Check if the thrown potion is either SPLASH_POTION or LINGERING_POTION
        if (potion.getEffects().size() > 0) {
            Collection<LivingEntity> affectedEntities = event.getAffectedEntities();
            for (LivingEntity entity : affectedEntities) {
                if (entity instanceof Player) {
                    Player player = (Player) entity;
                    // Apply temperature reduction for 30 seconds at a rate of 3 per second
                    reduceTemperaturenPotionSplash(player);
                }
            }
        }
    }

    private boolean isWaterOrPotion(Material material) {
        return material == Material.POTION
                || material == Material.LINGERING_POTION
                || material == Material.SPLASH_POTION;
        // Add more potion types if necessary
    }
    private void reduceTemperaturenPotionSplash(Player player) {
        // Apply temperature reduction for 30 seconds at a rate of 3 per second
        int durationInSeconds = 3;
        double reductionRate = 0.2;

        new WrappedRunnable() {
            int secondsRemaining = durationInSeconds;

            @Override
            public void run() {
                // Check if the player is still online
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                // Reduce the player's body temperature
                double currentTemperature = getBodyTemperature(player);
                currentTemperature = Math.max(-100.0, currentTemperature - reductionRate);

                // Update the player's body temperature
                setBodyTemperature(player, currentTemperature);

                // Update the action bar for the player (if needed)
                updateActionBar(player);

                // Check if the duration has elapsed
                if (--secondsRemaining <= 0) {
                    cancel(); // Stop the task
                }
            }
        }.runTaskTimer(this, 1, 1); // Run the task every second
    }
    private void reduceTemperatureOnLingeringCloud(Player player) {
        // Apply temperature reduction for 30 seconds at a rate of 3 per second
        int durationInSeconds = 3;
        double reductionRate = 0.01;

        new WrappedRunnable() {
            int secondsRemaining = durationInSeconds;

            @Override
            public void run() {
                // Check if the player is still online
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                // Reduce the player's body temperature
                double currentTemperature = getBodyTemperature(player);
                currentTemperature = Math.max(-100.0, currentTemperature - reductionRate);

                // Update the player's body temperature
                setBodyTemperature(player, currentTemperature);

                // Update the action bar for the player (if needed)
                updateActionBar(player);

                // Check if the duration has elapsed
                if (--secondsRemaining <= 0) {
                    cancel(); // Stop the task
                }
            }
        }.runTaskTimer(this, 1, 1); // Run the task every second
    }
    private void reduceTemperatureOnDrink(Player player) {
        // Apply temperature reduction for 30 seconds at a rate of 3 per second
        int durationInSeconds = 60;
        double reductionRate = 0.005;

        new WrappedRunnable() {
            int secondsRemaining = durationInSeconds;

            @Override
            public void run() {
                // Check if the player is still online
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                // Reduce the player's body temperature
                double currentTemperature = getBodyTemperature(player);
                currentTemperature = Math.max(-30.0, currentTemperature - reductionRate);

                // Update the player's body temperature
                setBodyTemperature(player, currentTemperature);

                // Update the action bar for the player (if needed)
                updateActionBar(player);

                // Check if the duration has elapsed
                if (--secondsRemaining <= 0) {
                    cancel(); // Stop the task
                }
            }
        }.runTaskTimer(this, 1, 1); // Run the task every second
    }

    private boolean isPlayerInLingeringCloud(Player player) {
        Location playerLocation = player.getLocation();

        // Define the radius within which to check for lingering clouds
        double checkRadius = 2.0;

        // Get nearby entities within the specified radius
        Collection<Entity> nearbyEntities = playerLocation.getWorld().getNearbyEntities(playerLocation, checkRadius, checkRadius, checkRadius);

        // Check if any of the nearby entities are AreaEffectCloud (lingering cloud)
        for (Entity entity : nearbyEntities) {
            if (entity.getType() == EntityType.AREA_EFFECT_CLOUD) {
                // You may want to further refine the check, for example, by checking the cloud's location or other properties
                return true; // Player is in the area of a lingering cloud
            }
        }

        // No lingering clouds found in the vicinity
        return false;
    }


    private double getArmorStabilizationFactor(Material material) {
        double armorStabilizationFactor = 0.0;

        switch (material) {
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_BOOTS:
                armorStabilizationFactor = 0.20; // Adjust this value
                break;
            case LEATHER_HELMET:
                armorStabilizationFactor = 0.26; // Adjust this value
                break;
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case IRON_BOOTS:
                armorStabilizationFactor = 0.13; // Adjust this value
                break;
            case IRON_HELMET:
                armorStabilizationFactor = 0.19; // Adjust this value
                break;
            case GOLDEN_CHESTPLATE:
            case GOLDEN_LEGGINGS:
            case GOLDEN_BOOTS:
                armorStabilizationFactor = 0.23; // Adjust this value
                break;
            case GOLDEN_HELMET:
                armorStabilizationFactor = 0.28; // Adjust this value
                break;
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_BOOTS:
                armorStabilizationFactor = 0.31; // Adjust this value
                break;
            case DIAMOND_HELMET:
                armorStabilizationFactor = 0.37; // Adjust this value
                break;
            case NETHERITE_CHESTPLATE:
            case NETHERITE_LEGGINGS:
            case NETHERITE_BOOTS:
                armorStabilizationFactor = 0.38; // Adjust this value
                break;
            case NETHERITE_HELMET:
                armorStabilizationFactor = 0.46; // Adjust this value
                break;

            default:
                break;
        }

        return armorStabilizationFactor;
    }


    private double calculateStabilizingFactor(double currentTemperature, ItemStack[] armorContents, Player player) {
        // Adjust the temperature limits based on your requirements
        double minTemperature = -10;
        double maxTemperature = 47;

        // Calculate a stabilizing factor that decreases as the temperature approaches its limits
        double temperatureRange = maxTemperature - minTemperature;
        double temperatureRatio = (currentTemperature - minTemperature) / temperatureRange;

        // Adjust the rate of decrease based on your preference
        double decreaseRate = 0.01; // You can adjust this value

        // Apply a nonlinear function to decrease the stabilizing factor
        final double[] stabilizingFactor = new double[1];

        // Adjust stabilizing factor based on different temperature ranges
        if (currentTemperature < -5) {
            // Strong stabilizing factor when temperature is far below -5
            stabilizingFactor[0] = (1 - Math.pow(temperatureRatio, 5)) * decreaseRate;
        } else if (currentTemperature < 10) {
            // Moderate stabilizing factor when temperature is between -5 and 10
            stabilizingFactor[0] = (1 - Math.pow(temperatureRatio, 3)) * decreaseRate;
        } else if (currentTemperature < 30) {
            // Weaker stabilizing factor when temperature is between 10 and 30
            stabilizingFactor[0] = (1 - Math.pow(temperatureRatio, 2)) * decreaseRate;
        } else {
            // Very weak stabilizing factor when temperature is above 30
            stabilizingFactor[0] = (1 - Math.pow(temperatureRatio, 1.5)) * decreaseRate;
        }

        // Additional resistance based on the type of armor worn
        new WrappedRunnable() {
            @Override
            public void run() {
                for (ItemStack armorPiece : armorContents) {
                    if (armorPiece != null && armorPiece.getType() != Material.AIR) {
                        stabilizingFactor[0] += getArmorStabilizationFactor(armorPiece.getType());
                    }
                }
            }
        }.runTaskLaterAtEntity(this, player, 1);

        return stabilizingFactor[0];
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
    private double getFreezingFactor(double biomeTemperature, boolean isDay) {
        double factor = 0.0;

        if (!isDay) {
            if (biomeTemperature <= -0.6) {
                factor = Math.pow(Math.abs(biomeTemperature), 5.0) * 12.22; // Adjust the factor based on your preference
            } else if (biomeTemperature <= 0.247) {
                factor = Math.pow(Math.abs(biomeTemperature), 3.0) * 16.61; // Adjust the factor based on your preference
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
        factor *= Math.exp(-Math.pow((Math.abs(biomeTemperature) - 10.0) / 5.0, 2));

        return factor;
    }


    private double getHeatFactor(double biomeTemperature, boolean isDay) {
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


    private double calculateTemperatureBiome(Player player) {
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
        double biomeTemperatureChange = Math.max(-0.1, Math.min(0.1, freezingFactor + heatingFactor + weatherFactor));

        return biomeTemperatureChange;
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




    private void updateActionBar(Player player) {
        double temperature = bodyTemperatureMap.getOrDefault(player, 0.0);

        // Get skylight level at player's location
        int skylightLevel = player.getLocation().getBlock().getLightFromSky();

        // Format the temperature and temperatureChangeRate to display four decimal places
        String formattedTemperature = String.format("%.2f", temperature);
        String formattedChangeRate = String.format("%.2f", calculateTemperatureSkyLightChange(player, skylightLevel));

        // Get biome information asynchronously
        Location location = player.getLocation();
        long time = player.getWorld().getTime();  // Add this line to get the current time

        new WrappedRunnable() {
            @Override
            public void run() {
                Biome biome = location.getBlock().getBiome();
                double biomeTemperature = location.getBlock().getTemperature();
                boolean isDay = time >= 0 && time < 12000;
                ItemStack[] playerArmor = player.getInventory().getArmorContents();

                // Get time-specific multipliers
                double daytimeMultiplier = getDaytimeMultiplier(time, isDay, playerArmor);
                double nighttimeMultiplier = getNighttimeDeMultiplier(time, isDay, playerArmor);


                // Calculate biome-specific factors
                double freezingFactor = getFreezingFactor(biomeTemperature, time < 12000);
                double heatingFactor = getHeatFactor(biomeTemperature, time >= 12000);

                // Format biome information
                String biomeInfo = ChatColor.GRAY + " | Bi:" + ChatColor.YELLOW + biome.name() +
                        ChatColor.GRAY + " (" + ChatColor.AQUA + String.format("%.2f", biomeTemperature) + ChatColor.GRAY + ")";

                // Construct the action bar message
                String actionBarMessage = ChatColor.GRAY + "T:" + ChatColor.YELLOW + formattedTemperature +
                        ChatColor.GRAY + " | SL:" + ChatColor.AQUA + skylightLevel +
                        ChatColor.GRAY + " | R:" + ChatColor.GREEN + formattedChangeRate +
                        ChatColor.GRAY + " | HF:" + ChatColor.RED + String.format("%.2f", heatingFactor) +
                        ChatColor.GRAY + " | FF:" + ChatColor.BLUE + String.format("%.2f", freezingFactor) +
                        ChatColor.GRAY + " | Day x" + ChatColor.GOLD + String.format("%.2f", daytimeMultiplier) +
                        ChatColor.GRAY + " | Night x:" + ChatColor.DARK_PURPLE + String.format("%.2f", nighttimeMultiplier) +
                        biomeInfo;

                // Send action bar message to the player
                player.sendActionBar(actionBarMessage);
            }
        }.runTaskAtLocation(this, location);
    }


}

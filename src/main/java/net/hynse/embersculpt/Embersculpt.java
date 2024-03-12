package net.hynse.embersculpt;

import me.nahu.scheduler.wrapper.FoliaWrappedJavaPlugin;
import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Embersculpt extends FoliaWrappedJavaPlugin implements Listener {
    public static Embersculpt instance;
    public static PlayerDataManager playerDataManager;
    public static ActionBar actionBar;
    public static BlockTemperature blockTemperature;
    public static Cooldown cooldown;
    public static EffectPlayer effectPlayer;
    public static Factor factor;
    public static Multiplier multiplier;
    public static SkyLight skyLight;
    public static Util util;
    private static final double MAX_TEMPERATURE = 100.0;
    private static final double MIN_TEMPERATURE = -100.0;
    public static final long MAX_SPRINT_DURATION = 180000; // Maximum sprinting duration in milliseconds (e.g., 60 seconds)
    public int timeadd = 0;  // Add this line to declare 'timeadd'
    public int bodyTemperatureAddition = 2;  // Add this line to declare 'bodyTemperatureAddition'
    public Map<UUID, Long> sprintStartTime = new HashMap<>();
    public Map<UUID, Long> walkStartTime = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        playerDataManager = new PlayerDataManager(instance);
        playerDataManager.bodyTemperatureMap = new HashMap<>();
        actionBar = new ActionBar();
        blockTemperature = new BlockTemperature();
        cooldown = new Cooldown();
        effectPlayer = new EffectPlayer();
        factor = new Factor();
        multiplier = new Multiplier();
        skyLight = new SkyLight();
        util = new Util();
        getServer().getPluginManager().registerEvents(new EventListener(), this);
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

                    effectPlayer.applyBodyTemperatureEffects(player);
                }
            }
        }.runTaskTimer(this, 1, 300);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (Player player : playerDataManager.bodyTemperatureMap.keySet()) {
            playerDataManager.savePlayerTemperature(player);
        }
        playerDataManager.bodyTemperatureMap.clear();
    }

    private void updateBodyTemperature() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Check if the player is in a death state
            if (player.isDead()) {
                continue; // Skip updating temperature for dead players
            }
            new WrappedRunnable() {
                @Override
                public void run() {
                    // Get skylight level at player's location
                    int skylightLevel = player.getLocation().getBlock().getLightFromSky();

                    // Calculate temperature changes based on skylight level and biome factor
                    double skylightTemperatureChange = skyLight.calculateTemperatureSkyLightChange(player, skylightLevel);

                    // Get physical activity factor based on player's sprinting
                    double physicalActivityFactor = util.getPhysicalActivityFactor(player);

                    // Get the player's armor contents
                    ItemStack[] armorContents = player.getInventory().getArmorContents();

                    // Get temperature change based on proximity to heat sources
//                    double heatSourceTemperatureChange = blockTemperature.calculateTemperatureHeatSources(player,7);
                    Embersculpt.blockTemperature.calculateTemperatureHeatSources(player,3);

                    // Combine all factors to update player's temperature
                    double currentTemperature = playerDataManager.bodyTemperatureMap.getOrDefault(player, 0.0);
                    currentTemperature += skylightTemperatureChange;
                    currentTemperature += physicalActivityFactor;
//                    currentTemperature += heatSourceTemperatureChange;

                    // Ensure the temperature stays within the specified range
                    double stabilizingFactor = util.calculateStabilizingFactor(currentTemperature, armorContents, player);
                    currentTemperature -= stabilizingFactor;

                    // Ensure the temperature stays within the specified range
                    currentTemperature = Math.max(MIN_TEMPERATURE, Math.min(MAX_TEMPERATURE, currentTemperature));

                    // Update the player's body temperature
                    playerDataManager.bodyTemperatureMap.put(player, currentTemperature);

                    // Update the action bar for the player
                    actionBar.updateActionBar(player);
                    }
            }.runTaskAtLocation(this,player.getLocation());
            new WrappedRunnable() {
                @Override
                public void run() {
                    blockTemperature.BlockTemperature(player);
                }
            }.runTaskAtLocation(this, player.getLocation());
        }
    }
}

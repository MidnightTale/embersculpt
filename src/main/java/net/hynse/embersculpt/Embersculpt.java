package net.hynse.embersculpt;

import me.nahu.scheduler.wrapper.FoliaWrappedJavaPlugin;
import me.nahu.scheduler.wrapper.WrappedScheduler;
import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.concurrent.atomic.AtomicInteger;

public final class Embersculpt extends FoliaWrappedJavaPlugin implements Listener {

    private static final double TEMPERATURE_CHANGE_RATE = 0.5; // Adjust this value as needed
    private static final int MAX_BLOCKS_ABOVE_PLAYER = 5; // Adjust this value as needed
    private static final int MAX_TEMPERATURE = 100;
    private static final int MIN_TEMPERATURE = -100;

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

    private void updatePlayerTemperature(WrappedScheduler scheduler) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            World world = player.getWorld();
            double temperatureChange = 0;

            int skylightLevel = player.getLocation().getBlock().getLightFromSky();

            if (skylightLevel >= 8 && skylightLevel <= 15) {
                // Skylight level is high
                temperatureChange += Math.min(1.0, (15 - skylightLevel) * 0.05); // Increase temperature by max 1, minimum 0.05 per skylight level
            } else if (skylightLevel <= 7) {
                // Skylight level is low or zero
                // No change in temperature
            }

            double currentTemperature = getPlayerTemperature(player);
            double newTemperature = Math.min(MAX_TEMPERATURE, Math.max(MIN_TEMPERATURE, currentTemperature + temperatureChange));

            setPlayerTemperature(player, newTemperature);
            updateActionBar(player, newTemperature, temperatureChange, skylightLevel);
        }
    }



    private double getPlayerTemperature(Player player) {
        // Implement your method to retrieve player temperature from storage
        // Example: return temperatureStorage.get(player.getUniqueId());
        return 0; // Replace with actual implementation
    }

    private void setPlayerTemperature(Player player, double temperature) {
        // Implement your method to store player temperature
        // Example: temperatureStorage.put(player.getUniqueId(), temperature);
    }

    private void updateActionBar(Player player, double temperature, double temperatureChange, int skylightLevel) {
        int roundedTemperature = (int) Math.round(temperature);
        String actionBarMessage = ChatColor.GOLD + "Temperature: " + ChatColor.RED + roundedTemperature
                + ChatColor.YELLOW + " | Rate Change: " + temperatureChange
                + ChatColor.YELLOW + " | Skylight Level: " + skylightLevel;
        player.sendActionBar(actionBarMessage);
    }

}

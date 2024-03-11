package net.hynse.embersculpt;

import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ActionBar {
    public void updateActionBar(Player player) {
        double temperature = Embersculpt.instance.bodyTemperatureMap.getOrDefault(player, 0.0);

        // Get skylight level at player's location
        int skylightLevel = player.getLocation().getBlock().getLightFromSky();

        // Format the temperature and temperatureChangeRate to display four decimal places
        String formattedTemperature = String.format("%.2f", temperature);
        String formattedChangeRate = String.format("%.2f", Embersculpt.skyLight.calculateTemperatureSkyLightChange(player, skylightLevel));

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
                double daytimeMultiplier = Embersculpt.multiplier.getDaytimeMultiplier(time, isDay, playerArmor);
                double nighttimeMultiplier = Embersculpt.multiplier.getNighttimeDeMultiplier(time, isDay, playerArmor);


                // Calculate biome-specific factors
                double freezingFactor = Embersculpt.factor.getFreezingFactor(biomeTemperature, time < 12000);
                double heatingFactor = Embersculpt.factor.getHeatFactor(biomeTemperature, time >= 12000);

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
        }.runTaskAtLocation(Embersculpt.instance, location);
    }
}

package net.hynse.embersculpt;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class ActionBar {

    public void updateActionBar(Player player) {
        double temperature = Embersculpt.playerDataManager.bodyTemperatureMap.getOrDefault(player, 0.0);
        World world = Bukkit.getWorlds().get(0);

        long dayCount = world.getFullTime() / 24000;
        long time = world.getTime();
        boolean isStormy = world.hasStorm();

        long hours = (time / 1000) % 24;
        long minutes = (time % 1000) * 60 / 1000;

        boolean isDay = time >= 0 && time < 12000;
        String timeicon = (hours >= 6 && hours < 18) ? "\u2600" : "\u263D";
        String thunderEmoji = "\u26C8"; // Thunder emoji
        String rainEmoji = "\uD83C\uDF27"; // Rain emoji
        String cloudEmoji = "\u2601"; // Cloud emoji
        String weathericon = isStormy ? thunderEmoji : (world.isClearWeather() ? cloudEmoji : rainEmoji);
        String timeString = String.format("%02d:%02d", hours, minutes);
        //String line2color = String.valueOf(net.md_5.bungee.api.ChatColor.of("#ffffff"));

        // Determine the symbols for temperature change
        String temperatureSymbol = temperatureChangeSymbol(temperature, isDay);

        // Get the appropriate multiplier based on the time of day
        Multiplier multiplier = new Multiplier();
        double multiplierValue = isDay
                ? multiplier.getDaytimeMultiplier(time, true, player.getInventory().getArmorContents())
                : multiplier.getNighttimeDeMultiplier(time, false, player.getInventory().getArmorContents());

        // Construct the action bar message
        String temperatureString = String.format("%.2f", temperature);
        String multiplierValueString = String.format("%.2f", multiplierValue);
        String actionBarMessage = "" + ChatColor.YELLOW + temperatureString + " " + temperatureSymbol +
                ChatColor.GRAY + "x" + multiplierValueString +
                ChatColor.GRAY + " | " + timeicon + " " + ChatColor.YELLOW + dayCount+ ChatColor.GRAY + " (" + ChatColor.YELLOW + timeString + ChatColor.GRAY + ")";


        // Send action bar message to the player
        player.sendActionBar(actionBarMessage);
    }

    private String temperatureChangeSymbol(double temperature, boolean isDay) {
        if (temperature > 0) {
            return isDay ? ChatColor.RED + "▲" : ChatColor.GREEN + "▼";
        } else if (temperature < 0) {
            return isDay ? ChatColor.GREEN + "▼" : ChatColor.RED + "▲";
        } else {
            return "-";
        }
    }
}

package net.hynse.embersculpt;

import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Locale;

public class ActionBar {

    private final MiniMessage miniMessage;

    public ActionBar() {
        // Initialize MiniMessage
        this.miniMessage = MiniMessage.builder().build();
    }

    public void updateActionBar(Player player) {
        double temperature = Embersculpt.playerDataManager.bodyTemperatureMap.getOrDefault(player, 0.0);
        double rate = Embersculpt.skyLight.calculateTemperatureSkyLightChange(player, player.getLocation().getBlock().getLightFromSky());
        World world = Bukkit.getWorlds().get(0);
        new WrappedRunnable() {
            @Override
            public void run() {
                long fullTime = world.getFullTime(); // Add an offset of 6 hours

                long dayCount = fullTime / 24000;
                long time = fullTime % 24000;
                boolean isStormy = world.hasStorm();

                long hours = (time / 1000 + 6) % 24; // Adjusted to start from 06:00
                long minutes = (time % 1000) * 60 / 1000;

                boolean isDay = time >= 0 && time < 12000;
                String timeIcon = (hours >= 6 && hours < 18) ? "\u2600" : "\u263D";
                String thunderEmoji = "\u26C8"; // Thunder emoji
                String rainEmoji = "\uD83C\uDF27"; // Rain emoji
                String cloudEmoji = "\u2601"; // Cloud emoji
                String weatherIcon = isStormy ? thunderEmoji : (world.isClearWeather() ? cloudEmoji : rainEmoji);
                String timeString = String.format("%02d:%02d", hours, minutes);


                // Determine the symbols for temperature change
        Component temperatureSymbol = temperatureChangeSymbol(temperature);

        // Get the appropriate multiplier based on the time of day
        Multiplier multiplier = new Multiplier();
        double multiplierValue = isDay
                ? multiplier.getDaytimeMultiplier(time, true, player.getInventory().getArmorContents())
                : multiplier.getNighttimeDeMultiplier(time, false, player.getInventory().getArmorContents());

        String temperatureString = getTemperatureString(temperature);

        String multiplierValueString = String.format(Locale.ENGLISH, "%.2f", multiplierValue);
        String rateString = String.format(Locale.ENGLISH, "%.2f", rate);

        TextColor temperatureColor = getTemperatureColor(temperature);

                Component actionBarMessage = Component.text()
                        .append(Component.text(temperatureString).color(temperatureColor))
                        .append(temperatureSymbol) // Use temperatureSymbol directly
                        .append(Component.text().color(NamedTextColor.GRAY).content(" x" + rateString))
//                        .append(Component.text().color(NamedTextColor.GRAY).content(" | " + timeIcon + " "))
//                        .append(Component.text().color(NamedTextColor.YELLOW).content(String.valueOf(dayCount)))
//                        .append(Component.text().color(NamedTextColor.GRAY).content(" (" + timeString + ")"))
                        .build();

        // Send action bar message to the player
        player.sendActionBar(actionBarMessage);
            }
        }.runTask(Embersculpt.instance);
    }

    private Component temperatureChangeSymbol(double temperature) {
        TextColor baseColor = TextColor.color(255, 255, 255); // Default color for no temperature change

        // Define colors for upwards and downwards triangles
        TextColor upwardsColor = TextColor.color(255, 0, 0); // Red color for positive temperature change
        TextColor downwardsColor = TextColor.color(0, 255, 0); // Blue color for negative temperature change

        // Determine the color based on temperature
        TextColor color;
        if (temperature > 0) {
            color = interpolateColor(baseColor, upwardsColor, temperature / 100.0); // Interpolate towards red for positive temperatures
        } else if (temperature < 0) {
            color = interpolateColor(baseColor, downwardsColor, Math.abs(temperature) / 100.0); // Interpolate towards blue for negative temperatures
        } else {
            color = baseColor; // No temperature change, use default color
        }

        // Create the component with the appropriate color and symbol
        String symbol = temperature > 0 ? "\u25B2" : (temperature < 0 ? "\u25BC" : "-"); // Upwards, downwards, or no change symbol
        return Component.text(symbol).color(color);
    }

    // Interpolate between two colors based on a ratio
    private TextColor interpolateColor(TextColor color1, TextColor color2, double ratio) {
        int red = (int) (color1.red() + (color2.red() - color1.red()) * ratio);
        int green = (int) (color1.green() + (color2.green() - color1.green()) * ratio);
        int blue = (int) (color1.blue() + (color2.blue() - color1.blue()) * ratio);
        return TextColor.color(red, green, blue);
    }



    private String getTemperatureString(double temperature) {
        return String.format("%.2f°C ", temperature);
    }

    private TextColor getTemperatureColor(double temperature) {
        // Define color thresholds and corresponding colors
        double[] thresholds = {-100, -70, -50, -30, 55, 70, 100};
        TextColor[] colors = {
                TextColor.fromHexString("#0000FF"), // Blue
                TextColor.fromHexString("#0076ff"), // Blue
                TextColor.fromHexString("#00FFFF"), // Aqua
                TextColor.fromHexString("#fffff1"), // Green
                TextColor.fromHexString("#FFFF00"), // Yellow
                TextColor.fromHexString("#ff6f00"), // Red
                TextColor.fromHexString("#ff0000")  // Red
        };

        // Handle temperatures below the lowest defined threshold
        if (temperature <= thresholds[0]) {
            return colors[0];
        }

        // Find the appropriate color range
        int range = 1;
        while (range < thresholds.length && temperature > thresholds[range]) {
            range++;
        }

        // Interpolate between the two nearest colors
        double lowerThreshold = thresholds[range - 1];
        double upperThreshold = thresholds[range];
        double ratio = (temperature - lowerThreshold) / (upperThreshold - lowerThreshold);

        // Calculate interpolated color
        double red = interpolateColorComponent(colors[range - 1].red(), colors[range].red(), ratio);
        double green = interpolateColorComponent(colors[range - 1].green(), colors[range].green(), ratio);
        double blue = interpolateColorComponent(colors[range - 1].blue(), colors[range].blue(), ratio);

        // Create and return the interpolated color
        return TextColor.color((int) red, (int) green, (int) blue);
    }



    private double interpolateColorComponent(int lowerComponent, int upperComponent, double ratio) {
        return lowerComponent + (upperComponent - lowerComponent) * ratio;
    }

}

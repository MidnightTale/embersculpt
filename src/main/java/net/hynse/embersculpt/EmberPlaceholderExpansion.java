package net.hynse.embersculpt;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.hynse.embersculpt.Embersculpt;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.text.format.TextColor;

public class EmberPlaceholderExpansion extends PlaceholderExpansion {

    @Override
    public boolean canRegister() {
        // Ensure that PlaceholderAPI is available
        return super.canRegister();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "embersculpt";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MidnightTale";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        // Handle %reputify_player%
        if (identifier.equals("player")) {
            double temperatureDouble = Embersculpt.playerDataManager.bodyTemperatureMap.getOrDefault(player, 0.0);
            int temperature = (int) temperatureDouble; // Convert double to int
            return String.valueOf(temperature);
        }

        // Handle %reputify_player_colored%
        if (identifier.equals("player_colored")) {
            double temperatureDouble = Embersculpt.playerDataManager.bodyTemperatureMap.getOrDefault(player, 0.0);
            int temperature = (int) temperatureDouble; // Convert double to int
            TextColor textColor = getTemperatureColor((int) temperature); // Cast temperature to integer
            return textColor.toString() + temperature;
        }

        return null;
    }

    private TextColor getTemperatureColor(int temperature) { // Change parameter type to int
        // Define color thresholds and corresponding colors
        int[] thresholds = {-100, -70, -50, -30, 55, 70, 100}; // Change to int array
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

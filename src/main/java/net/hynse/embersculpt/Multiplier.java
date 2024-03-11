package net.hynse.embersculpt;

import org.bukkit.inventory.ItemStack;

public class Multiplier {
    public double getDaytimeMultiplier(long time, boolean isDay, ItemStack[] armorContents) {
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
                return Embersculpt.util.interpolate(0.85, 1.0, (double) (time - sunriseStart) / (sunriseEnd - sunriseStart));
            } else if (time > sunriseEnd && time < sunsetStart) {
                // Gradually increase temperature from sunrise to sunset
                return Embersculpt.util.interpolate(1.0, 1.9, (double) (time - sunriseEnd) / (sunsetStart - sunriseEnd));
            } else if (time >= sunsetStart && time <= sunsetEnd) {
                // Decrease temperature slightly during sunset
                return Embersculpt.util.interpolate(1.9, 0.94, (double) (time - sunsetStart) / (sunsetEnd - sunsetStart));
            } else {
                double baseMultiplier = 1.2; // Default multiplier

                // Reduce the impact of armorContents on daytimeMultiplier
                double armorImpact = Embersculpt.util.calculateArmorImpact(armorContents);
                return baseMultiplier - armorImpact;
            }
        }
        return 0;
    }

    // Update this method to reduce the impact of armorContents on the deMultiplier value
    public double getNighttimeDeMultiplier(long time, boolean isDay, ItemStack[] armorContents) {
        // Adjust this function to make the rate decrease stronger during nighttime

        // Midnight time
        long midnightStart = 18000; // 6:00 PM
        long midnightEnd = 24000;   // 12:00 AM

        // Get the current multiplier based on time of night
        if (!isDay) {
            if ((time >= 0 && time <= 4000) || (time >= midnightStart && time <= midnightEnd)) {
                // Increase temperature slightly during midnight and early morning
                return Embersculpt.util.interpolate(1.62, 1.12, (double) (time - midnightStart) / (4000 + midnightStart));
            } else {
                double baseMultiplier = 1.12; // Default multiplier

                // Reduce the impact of armorContents on nighttimeDeMultiplier
                double armorImpact = Embersculpt.util.calculateArmorImpact(armorContents);
                return baseMultiplier - armorImpact;
            }
        }
        return 0;
    }
}

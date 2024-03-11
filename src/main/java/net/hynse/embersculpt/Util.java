package net.hynse.embersculpt;

import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Util {
    public double getBodyTemperature(Player player) {
        return Embersculpt.instance.bodyTemperatureMap.getOrDefault(player, 0.0);
    }

    public void setBodyTemperature(Player player, double temperature) {
        Embersculpt.instance.bodyTemperatureMap.put(player, temperature);
    }
    public double calculateArmorImpact(ItemStack[] armorContents) {
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
        return switch (armorMaterial) {
            case LEATHER_CHESTPLATE, LEATHER_LEGGINGS, LEATHER_BOOTS -> 0.1;
            case LEATHER_HELMET -> 0.15;
            case IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS -> 0.08;
            case IRON_HELMET -> 0.12;
            case GOLDEN_CHESTPLATE, GOLDEN_LEGGINGS, GOLDEN_BOOTS -> 0.13;
            case GOLDEN_HELMET -> 0.18;
            case DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS -> 0.2;
            case DIAMOND_HELMET, NETHERITE_CHESTPLATE, NETHERITE_LEGGINGS, NETHERITE_BOOTS -> 0.25;
            case NETHERITE_HELMET -> 0.3;
            default -> 0.0;
        };
    }

    // Helper method for linear interpolation between two values
    public double interpolate(double start, double end, double t) {
        return start + t * (end - start);
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


    public double calculateStabilizingFactor(double currentTemperature, ItemStack[] armorContents, Player player) {
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
        }.runTaskLaterAtEntity(Embersculpt.instance, player, 1);

        return stabilizingFactor[0];
    }


    public double getPhysicalActivityFactor(Player player) {
        double physicalActivityFactor = 0.0;

        // Check if the player is currently sprinting
        boolean isSprinting = player.isSprinting();

        // Define activity factor based on sprinting or walking duration
        if (isSprinting) {
            long sprintingDuration = System.currentTimeMillis() - Embersculpt.instance.sprintStartTime.getOrDefault(player.getUniqueId(), 0L);
            double sprintingFactor = Math.min(1.0, (double) sprintingDuration / Embersculpt.MAX_SPRINT_DURATION);
            physicalActivityFactor += 0.1 * sprintingFactor;
        }

        return physicalActivityFactor;
    }
}

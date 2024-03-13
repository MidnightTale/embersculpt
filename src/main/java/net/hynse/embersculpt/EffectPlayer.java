package net.hynse.embersculpt;

import me.nahu.scheduler.wrapper.runnable.WrappedRunnable;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EffectPlayer {
    public void applyBodyTemperatureEffects(Player player) {
        double bodyTemperature = Embersculpt.util.getBodyTemperature(player);
        new WrappedRunnable() {
            @Override
            public void run() {
                if (bodyTemperature >= 50 && bodyTemperature <= 75 && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL) {
                    // Normal Conditions (No immediate effects)
                    // Characters feel comfortable with no adverse effects.
                    // No additional effects need to be applied.
                } else if (bodyTemperature > 75 && bodyTemperature <= 85 && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL) {
                    applyPotionEffect(player, PotionEffectType.SPEED, 300, 0);
                } else if (bodyTemperature > 85 && bodyTemperature <= 95 && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL) {
                    applyPotionEffect(player, PotionEffectType.WEAKNESS, 300, 0);
                    applyPotionEffect(player, PotionEffectType.SPEED, 300, 0);
                } else if (bodyTemperature > 95 && bodyTemperature < 100 && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL) {
                    applyPotionEffect(player, PotionEffectType.HUNGER, 300, 0);
                    applyPotionEffect(player, PotionEffectType.WEAKNESS, 300, 0);
                    applyPotionEffect(player, PotionEffectType.SPEED, 300, 1);
                } else if (bodyTemperature >= 100 && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL) {
                    applyPotionEffect(player, PotionEffectType.DARKNESS, 300, 0);
                    applyPotionEffect(player, PotionEffectType.HUNGER, 300, 0);
                    applyPotionEffect(player, PotionEffectType.SPEED, 300, 1);
                    applyPotionEffect(player, PotionEffectType.WEAKNESS, 300, 0);
                    Embersculpt.instance.timeadd++;

                    if (Embersculpt.instance.timeadd >= Embersculpt.instance.bodyTemperatureAddition && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL) {
                        player.setFireTicks(20 * 60);
                        applyPotionEffect(player, PotionEffectType.HUNGER, 300, 1);
                        applyPotionEffect(player, PotionEffectType.BLINDNESS, 300, 1);
                        applyPotionEffect(player, PotionEffectType.WEAKNESS, 300, 2);
                        applyPotionEffect(player, PotionEffectType.SPEED, 300, 3);
                        applyPotionEffect(player, PotionEffectType.WITHER, 300, 1);
                        Embersculpt.instance.timeadd = 0;
                    }
                } else if (bodyTemperature >= 0 && bodyTemperature <= 25 && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL) {
                    // Normal Conditions (No immediate effects)
                    // Characters are comfortable without experiencing cold-related effects.
                    // No additional effects need to be applied.
                } else if (bodyTemperature <= -50 && bodyTemperature >= -60 && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL) {
                    applyPotionEffect(player, PotionEffectType.SLOW, 300, 0);
                } else if (bodyTemperature <= -61 && bodyTemperature >= -75 && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL) {
                    applyPotionEffect(player, PotionEffectType.SLOW_DIGGING, 300, 0);
                    applyPotionEffect(player, PotionEffectType.SLOW, 300, 0);
                } else if (bodyTemperature <= -75 && bodyTemperature >= -85 &&!player.isDead() && player.getGameMode() == GameMode.SURVIVAL) {
                    applyPotionEffect(player, PotionEffectType.DARKNESS, 300, 0);
                    applyPotionEffect(player, PotionEffectType.SLOW_DIGGING, 300, 0);
                    applyPotionEffect(player, PotionEffectType.SLOW, 300, 0);
                } else if (bodyTemperature <= -86 && bodyTemperature >= -100 && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL) {
                    applyPotionEffect(player, PotionEffectType.SLOW_DIGGING, 300, 0);
                    applyPotionEffect(player, PotionEffectType.SLOW, 300, 1);
                } else if (bodyTemperature == -100 && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL) {
                    Embersculpt.instance.timeadd++;
                    applyPotionEffect(player, PotionEffectType.SLOW, 600, 1);
                    applyPotionEffect(player, PotionEffectType.SLOW_DIGGING, 600, 0);

                    if (Embersculpt.instance.timeadd >= Embersculpt.instance.bodyTemperatureAddition && !player.isDead() && player.getGameMode() == GameMode.SURVIVAL) {
                        player.setFreezeTicks(600);
                        applyPotionEffect(player, PotionEffectType.SLOW_DIGGING, 600, 1);
                        applyPotionEffect(player, PotionEffectType.WITHER, 600, 0);
                        applyPotionEffect(player, PotionEffectType.SLOW, 600, 2);
                        Embersculpt.instance.timeadd = 0;
                    }
                }
            }
        }.runTaskAtEntity(Embersculpt.instance, player);
    }
    private void applyPotionEffect(Player player, PotionEffectType type, int duration, int amplifier) {
        // Check if the player already has an effect of the same type
        PotionEffect existingEffect = getPlayerEffectByType(player, type);

        if (existingEffect == null) {
            // If no existing effect, apply the new effect
            player.addPotionEffect(new PotionEffect(type, duration, amplifier, true));
        } else {
            // If an effect already exists, extend the duration and amplify if necessary
            int newDuration = existingEffect.getDuration() + duration;
            int newAmplifier = Math.max(existingEffect.getAmplifier(), amplifier);
            player.removePotionEffect(type);
            player.addPotionEffect(new PotionEffect(type, newDuration, newAmplifier, true));
        }
    }

    private PotionEffect getPlayerEffectByType(Player player, PotionEffectType type) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(type)) {
                return effect;
            }
        }
        return null;
    }
}

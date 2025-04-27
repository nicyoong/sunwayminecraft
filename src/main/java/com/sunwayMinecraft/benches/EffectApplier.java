package com.sunwayMinecraft.benches;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Player;

public class EffectApplier {
    private static final int DURATION = 200;
    private static final int AMPLIFIER = 0;

    public void applyRegeneration(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.REGENERATION,
                200,  // 10 seconds
                0,    // Level I
                true, // Ambient particles
                true  // Show icon
        ));
    }
}

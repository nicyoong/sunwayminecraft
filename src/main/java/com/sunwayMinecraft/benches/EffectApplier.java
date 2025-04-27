package com.sunwayMinecraft.benches;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.bukkit.entity.Player;

public class EffectApplier {
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
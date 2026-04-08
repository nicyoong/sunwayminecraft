package com.sunwayMinecraft.containerfinder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Analyzes item stacks for grouping, metadata splitting, and nested shulker contents.
 */
public class ContainerItemAnalyzer {

    /**
     * Converts one item stack into a grouping key + label + amount.
     */
    public ItemGroup toItemGroup(ItemStack item) {
        Material material = item.getType();
        ItemMeta meta = item.getItemMeta();

        List<String> parts = new ArrayList<>();
        String baseLabel = prettifyEnum(material.name());

        if (meta != null) {
            String customName = extractDisplayName(meta);
            if (customName != null && !customName.isBlank()) {
                parts.add("Name=" + customName);
            }

            if (meta.hasEnchants()) {
                List<String> enchants = new ArrayList<>();
                meta.getEnchants()
                        .forEach((enchant, level) -> enchants.add(enchant.getKey().getKey() + ":" + level));
                Collections.sort(enchants);
                parts.add("Enchants=" + String.join(",", enchants));
            }

            if (meta instanceof PotionMeta potionMeta) {
                List<String> potionParts = new ArrayList<>();

                if (potionMeta.hasBasePotionType() && potionMeta.getBasePotionType() != null) {
                    potionParts.add("Base=" + potionMeta.getBasePotionType().name());
                }

                if (potionMeta.hasCustomEffects()) {
                    List<String> effects = new ArrayList<>();
                    potionMeta.getCustomEffects()
                            .forEach(
                                    effect ->
                                            effects.add(
                                                    effect.getType().getKey().getKey()
                                                            + ":"
                                                            + effect.getAmplifier()
                                                            + ":"
                                                            + effect.getDuration()));
                    Collections.sort(effects);
                    potionParts.add("Effects=" + String.join(",", effects));
                }

                if (!potionParts.isEmpty()) {
                    parts.add("Potion=" + String.join("|", potionParts));
                }
            }

            if (meta instanceof BookMeta bookMeta) {
                List<String> bookParts = new ArrayList<>();

                if (bookMeta.hasTitle() && bookMeta.getTitle() != null) {
                    bookParts.add("Title=" + bookMeta.getTitle());
                }
                if (bookMeta.hasAuthor() && bookMeta.getAuthor() != null) {
                    bookParts.add("Author=" + bookMeta.getAuthor());
                }

                if (!bookParts.isEmpty()) {
                    parts.add("Book=" + String.join("|", bookParts));
                }
            }

            List<Component> lore = meta.lore();
            if (meta.hasLore() && lore != null && !lore.isEmpty()) {
                List<String> loreLines = new ArrayList<>();
                for (Component line : lore) {
                    loreLines.add(PlainTextComponentSerializer.plainText().serialize(line));
                }
                parts.add("Lore=" + String.join(" / ", loreLines));
            }

            if (meta instanceof BlockStateMeta blockStateMeta && blockStateMeta.hasBlockState()) {
                BlockState blockState = blockStateMeta.getBlockState();
                if (blockState instanceof ShulkerBox shulkerBox && shulkerBox.getColor() != null) {
                    parts.add("ShulkerColor=" + shulkerBox.getColor().name());
                }
            }
        }

        String key = material.name();
        String label = baseLabel;

        if (!parts.isEmpty()) {
            key += "|" + String.join("|", parts);
            label += " {" + String.join("; ", parts) + "}";
        }

        return new ItemGroup(key, label, item.getAmount());
    }

    /**
     * Adds one group's amount into a count map.
     */
    public void mergeCount(
            Map<String, Long> counts, Map<String, String> labels, ItemGroup group) {
        counts.merge(group.key(), (long) group.amount(), Long::sum);
        labels.putIfAbsent(group.key(), group.label());
    }

    /**
     * Inspects one item stack for nested shulker contents and adds them one level deep.
     */
    public void extractNestedShulkerContents(
            ItemStack item,
            Map<String, Long> nestedCounts,
            Map<String, String> nestedLabels) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof BlockStateMeta blockStateMeta)) {
            return;
        }

        if (!blockStateMeta.hasBlockState()) {
            return;
        }

        BlockState state = blockStateMeta.getBlockState();
        if (!(state instanceof ShulkerBox shulkerBox)) {
            return;
        }

        for (ItemStack nested : shulkerBox.getInventory().getContents()) {
            if (nested == null || nested.getType() == Material.AIR || nested.getAmount() <= 0) {
                continue;
            }

            ItemGroup nestedGroup = toItemGroup(nested);
            mergeCount(nestedCounts, nestedLabels, nestedGroup);
        }
    }

    private String extractDisplayName(ItemMeta meta) {
        try {
            Component component = meta.displayName();
            if (component != null) {
                return PlainTextComponentSerializer.plainText().serialize(component);
            }
        } catch (Throwable ignored) {
            // Safe fallback
        }

        return null;
    }

    private String prettifyEnum(String raw) {
        String[] parts = raw.toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    /**
     * One grouped item entry.
     */
    public record ItemGroup(String key, String label, int amount) {}
}
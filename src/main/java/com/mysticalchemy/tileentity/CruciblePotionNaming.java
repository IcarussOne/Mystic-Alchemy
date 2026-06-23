package com.mysticalchemy.tileentity;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CruciblePotionNaming {
    private CruciblePotionNaming() {
    }

    @Nullable
    public static PotionType findVanillaNamedType(PotionEffect effect) {
        ResourceLocation effectId = effect.getPotion().getRegistryName();
        if (effectId == null || !"minecraft".equals(effectId.getNamespace())) {
            return null;
        }

        for (PotionType potionType : PotionType.REGISTRY) {
            if (potionType == null || potionType.getEffects().size() != 1) {
                continue;
            }

            ResourceLocation potionTypeId = PotionType.REGISTRY.getNameForObject(potionType);
            if (potionTypeId == null || !"minecraft".equals(potionTypeId.getNamespace())) {
                continue;
            }

            PotionEffect typeEffect = potionType.getEffects().get(0);
            if (typeEffect.getPotion() == effect.getPotion() && typeEffect.getAmplifier() == effect.getAmplifier()) {
                return potionType;
            }
        }

        return null;
    }

    public static String getPotionNamePrefix(boolean splash, boolean lingering) {
        if (splash) {
            return "splash_potion.effect.";
        }
        if (lingering) {
            return "lingering_potion.effect.";
        }
        return "potion.effect.";
    }

    @Nullable
    public static String generateDisplayName(List<PotionEffect> effects, boolean splash, boolean lingering) {
        List<String> names = getSortedEffectNames(effects);
        if (names.isEmpty()) {
            return null;
        }

        String effectList = joinEffectNames(names);
        if (names.size() == 1) {
            if (splash) {
                return I18n.translateToLocalFormatted("item.mysticalchemy.named_splash_potion", effectList);
            }
            if (lingering) {
                return I18n.translateToLocalFormatted("item.mysticalchemy.named_lingering_potion", effectList);
            }
            return I18n.translateToLocalFormatted("item.mysticalchemy.named_potion", effectList);
        }

        if (splash) {
            return I18n.translateToLocalFormatted("item.mysticalchemy.named_splash_elixir", effectList);
        }
        if (lingering) {
            return I18n.translateToLocalFormatted("item.mysticalchemy.named_lingering_elixir", effectList);
        }
        return I18n.translateToLocalFormatted("item.mysticalchemy.named_elixir", effectList);
    }

    private static List<String> getSortedEffectNames(List<PotionEffect> effects) {
        List<PotionEffect> sortedEffects = new ArrayList<PotionEffect>(effects);
        Collections.sort(sortedEffects, (left, right) -> {
            if (left.getAmplifier() != right.getAmplifier()) {
                return Integer.compare(right.getAmplifier(), left.getAmplifier());
            }
            return translateEffectName(left).compareToIgnoreCase(translateEffectName(right));
        });

        List<String> names = new ArrayList<String>(sortedEffects.size());
        for (PotionEffect effect : sortedEffects) {
            names.add(translateEffectName(effect));
        }
        return names;
    }

    private static String translateEffectName(PotionEffect effect) {
        Potion potion = effect.getPotion();
        if (potion == null || potion.getRegistryName() == null) {
            return I18n.translateToLocal(effect.getEffectName()).trim();
        }

        String elixirKey = "effect.elixir." + potion.getRegistryName().getPath();
        String translatedElixirName = I18n.translateToLocal(elixirKey).trim();
        if (!elixirKey.equals(translatedElixirName)) {
            return translatedElixirName;
        }

        return I18n.translateToLocal(effect.getEffectName()).trim();
    }

    private static String joinEffectNames(List<String> names) {
        if (names.size() == 1) {
            return names.get(0);
        }

        String andWord = I18n.translateToLocal("item.mysticalchemy.and");
        if (names.size() == 2) {
            return names.get(0) + " " + andWord + " " + names.get(1);
        }

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < names.size(); index++) {
            if (index > 0) {
                if (index == names.size() - 1) {
                    builder.append(", ").append(andWord).append(" ");
                } else {
                    builder.append(", ");
                }
            }
            builder.append(names.get(index));
        }
        return builder.toString();
    }
}

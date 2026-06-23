package com.mysticalchemy.config;

import com.mysticalchemy.MysticAlchemy;
import com.mysticalchemy.registry.IngredientLoader;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@net.minecraftforge.common.config.Config(modid = MysticAlchemy.MODID, name = "MysticAlchemy")
public final class Config {
    @net.minecraftforge.common.config.Config.Name("Brewing")
    @net.minecraftforge.common.config.Config.LangKey("settings.mysticalchemy:brewing")
    public static Brewing brewing = new Brewing();

    @net.minecraftforge.common.config.Config.Name("Modifier Tweaks")
    @net.minecraftforge.common.config.Config.LangKey("settings.mysticalchemy:modifier_tweaks")
    public static ModifierTweaks modifierTweaks = new ModifierTweaks();

    @net.minecraftforge.common.config.Config.Name("Randomization")
    @net.minecraftforge.common.config.Config.LangKey("settings.mysticalchemy:randomization")
    public static Randomization randomization = new Randomization();

    @net.minecraftforge.common.config.Config.Name("Ingredient List")
    @net.minecraftforge.common.config.Config.LangKey("settings.mysticalchemy:ingredient_list")
    public static IngredientList ingredientList = new IngredientList();

    private Config() {
    }

    @Mod.EventBusSubscriber(modid = MysticAlchemy.MODID)
    private static final class EventHandler {
        private EventHandler() {
        }

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (MysticAlchemy.MODID.equals(event.getModID())) {
                ConfigManager.sync(MysticAlchemy.MODID, net.minecraftforge.common.config.Config.Type.INSTANCE);
                IngredientLoader.reloadPotionIngredientRecipes();
            }
        }

    }

    public static final class Brewing {
        @net.minecraftforge.common.config.Config.Name("Disallowed Effects")
        @net.minecraftforge.common.config.Config.Comment("Disallow the following potion effects by registry id, for example minecraft:nausea")
        public String[] ma_disallowed_effects = new String[0];

        @net.minecraftforge.common.config.Config.Name("Disable Vanilla Brewing")
        @net.minecraftforge.common.config.Config.Comment("When true, brewed outputs are always replaced with awkward potions in the vanilla Brewing Stand.")
        public boolean ma_disable_vanilla_brewing = true;

        @net.minecraftforge.common.config.Config.Name("Global Amplifier Cap")
        @net.minecraftforge.common.config.Config.Comment("Maximum amplifier allowed for crucible-generated potion effects (0 = level I). Default: 4")
        @net.minecraftforge.common.config.Config.RangeInt(min = 0, max = 127)
        public int ma_global_amplifier_cap = 4;

        @net.minecraftforge.common.config.Config.Name("Per Effect Amplifier Caps")
        @net.minecraftforge.common.config.Config.Comment({
                "Optional per-effect amplifier caps as <registry_name>=<cap>.",
                "Example: minecraft:strength=2",
                "Leave empty for no per-effect overrides."
        })
        public String[] ma_per_effect_amplifier_caps = new String[0];

        @net.minecraftforge.common.config.Config.Name("Max Effects Above 1.0")
        @net.minecraftforge.common.config.Config.Comment("Maximum number of different effects that may have magnitude above 1.0 in a crucible brew.")
        @net.minecraftforge.common.config.Config.RangeInt(min = 0, max = 127)
        public int ma_max_effects_above_one = 4;
    }

    public static final class ModifierTweaks {
        @net.minecraftforge.common.config.Config.Name("Disabled Modifiers")
        @net.minecraftforge.common.config.Config.Comment({
                "Disable special crucible modifiers by key.",
                "Examples: evaporate_to_gas, explode, blinding_flash, verdant_growth, ignition_burst, spawn_magma_cube, spawn_slime,",
                "jellify_potion, potion_burst, potion_reverse, turn_to_slime, stonification, melt_crucible"
        })
        public String[] ma_disabled_modifiers = new String[0];

        @net.minecraftforge.common.config.Config.Name("Blinding Flash Radius")
        @net.minecraftforge.common.config.Config.Comment("Radius in blocks for blinding_flash.")
        @net.minecraftforge.common.config.Config.RangeDouble(min = 0.0d, max = 64.0d)
        public double ma_blinding_flash_radius = 3.0d;

        @net.minecraftforge.common.config.Config.Name("Blinding Flash Duration Ticks")
        @net.minecraftforge.common.config.Config.Comment("Duration of blindness and nausea from blinding_flash.")
        @net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 1200)
        public int ma_blinding_flash_duration_ticks = 100;

        @net.minecraftforge.common.config.Config.Name("Ignition Burst Radius")
        @net.minecraftforge.common.config.Config.Comment("Radius in blocks for ignition_burst.")
        @net.minecraftforge.common.config.Config.RangeDouble(min = 0.0d, max = 64.0d)
        public double ma_ignition_burst_radius = 3.0d;

        @net.minecraftforge.common.config.Config.Name("Ignition Burst Fire Seconds")
        @net.minecraftforge.common.config.Config.Comment("How many seconds ignition_burst sets entities on fire.")
        @net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 120)
        public int ma_ignition_burst_fire_seconds = 5;

        @net.minecraftforge.common.config.Config.Name("Potion Slime Jelly Drop Chance")
        @net.minecraftforge.common.config.Config.Comment("Chance [0.0-1.0] for potion slimes to drop one potion jelly chunk on death.")
        @net.minecraftforge.common.config.Config.RangeDouble(min = 0.0d, max = 1.0d)
        public double ma_potion_slime_jelly_drop_chance = 0.2d;
    }

    public static final class Randomization {
        @net.minecraftforge.common.config.Config.Name("Randomization Mode")
        @net.minecraftforge.common.config.Config.Comment({
                "Controls ingredient randomization behavior.",
                "With this setting, you can randomize what each brewing ingredient does, based on the world seed.",
                "Modes:",
                "off: Uses each ingredient exactly as written in ingredient_definitions (no randomization).",
                "merge: Keeps configured effects/modifiers and also adds randomized ones.",
                "replace: Ignores configured effects/modifiers and uses only randomized ones.",
                "Tip - use 'replace' for a fully randomized playthrough.",
                "Use 'merge' when you want to keep some configured effects/modifiers and add random ones.",
                "In merge mode, you should clean up the 'Ingredient Definitions' list by only keeping the item name+metadata+nbt",
                "information for those ingredients that you want to randomize. With the fully default list in merge mode you won't see much randomization."
        })
        @net.minecraftforge.common.config.Config.RequiresMcRestart
        public String randomization_mode = "off";

        @net.minecraftforge.common.config.Config.Name("Forced Randomization Seed")
        @net.minecraftforge.common.config.Config.Comment({
                "Optional fixed seed used for ingredient randomization instead of the world seed.",
                "Leave empty to use the world seed.",
                "Use this to reuse the same randomization setup across different worlds."
        })
        @net.minecraftforge.common.config.Config.RequiresMcRestart
        public String forced_randomization_seed = "";

        @net.minecraftforge.common.config.Config.Name("Randomization Effect Whitelist")
        @net.minecraftforge.common.config.Config.Comment({
                "Optional allow-list of potion effect ids used by seed randomization (for example minecraft:speed).",
                "When non-empty, randomization only selects effects listed here.",
                "If this whitelist is set, the Brewing 'Disallowed Effects' blacklist is ignored for randomization."
        })
        @net.minecraftforge.common.config.Config.RequiresMcRestart
        public String[] randomization_effect_whitelist = new String[]{
                "ebwizardry:frost",
                "ebwizardry:fireskin",
                "ebwizardry:ice_shroud",
                "ebwizardry:static_aura",
                "ebwizardry:decay",
                "ebwizardry:sixth_sense",
                "ebwizardry:arcane_jammer",
                "ebwizardry:font_of_mana",
                "ebwizardry:curse_of_soulbinding",
                "ebwizardry:paralysis",
                "ebwizardry:muffle",
                "ebwizardry:ward",
                "ebwizardry:slow_time",
                "ebwizardry:empowerment",
                "ebwizardry:curse_of_enfeeblement",
                "ebwizardry:curse_of_undeath",
                "ebwizardry:frost_step",
                "ebwizardry:mirage",
                "ebwizardry:oakflesh",
                "ebwizardry:ironflesh",
                "ebwizardry:diamondflesh",
                "minecraft:speed",
                "minecraft:slowness",
                "minecraft:haste",
                "minecraft:mining_fatigue",
                "minecraft:strength",
                "minecraft:instant_health",
                "minecraft:instant_damage",
                "minecraft:jump_boost",
                "minecraft:nausea",
                "minecraft:regeneration",
                "minecraft:resistance",
                "minecraft:fire_resistance",
                "minecraft:water_breathing",
                "minecraft:invisibility",
                "minecraft:blindness",
                "minecraft:night_vision",
                "minecraft:hunger",
                "minecraft:weakness",
                "minecraft:poison",
                "minecraft:wither",
                "minecraft:health_boost",
                "minecraft:absorption",
                "minecraft:saturation",
                "minecraft:glowing",
                "minecraft:levitation",
                "minecraft:luck",
                "minecraft:unluck",
                "arsmagica2:agility",
                "arsmagica2:burnout_reduction",
                "arsmagica2:charm",
                "arsmagica2:clarity",
                "arsmagica2:entangle",
                "arsmagica2:flight",
                "arsmagica2:frost_slow",
                "arsmagica2:haste",
                "arsmagica2:illumination",
                "arsmagica2:leap",
                "arsmagica2:levitation",
                "arsmagica2:mana_regeneration",
                "arsmagica2:regeneration",
                "arsmagica2:scramble_synapses",
                "arsmagica2:shrink",
                "arsmagica2:silence",
                "arsmagica2:slowfall",
                "arsmagica2:spell_reflect",
                "arsmagica2:swift_swim",
                "arsmagica2:temporal_anchor",
                "arsmagica2:true_sight",
                "arsmagica2:water_breathing",
                "arsmagica2:watery_grave",
                "arsmagica2:mana_boost"
        };

        @net.minecraftforge.common.config.Config.Name("Randomized Property Coverage Percent")
        @net.minecraftforge.common.config.Config.Comment({
                "Percentage [0-100] of configured ingredients that receive alchemical properties in randomization modes.",
                "Applies only when Randomization Mode is merge or replace.",
                "Example: 70 means 7 out of 10 ingredients get properties and 3 become inert."
        })
        @net.minecraftforge.common.config.Config.RangeInt(min = 0, max = 100)
        public int randomized_property_coverage_percent = 100;

    }

    public static final class IngredientList {
        @net.minecraftforge.common.config.Config.Name("Ingredient Definitions")
        @net.minecraftforge.common.config.Config.Comment({
                "All crucible ingredient definitions in one multiline setting.",
                "One entry per line. Use item=<registry> or oredict=<name> and optional fields separated by ';'.",
                "Comment tags are supported: anything inside //...// is ignored.",
                "Use item ids and meta for variants (for example item=minecraft:red_flower; meta=2).",
                "Supported optional fields: meta=<int>; nbt=<snbt>; potion_effects=<effect@strength,...>; potion_all_effects_delta=<strength>; potion_corruption=<true|false>; potion_duration=<ticks>; potion_splash=<true|false>; potion_lingering=<true|false>",
                "Gameplay/action fields: explode, evaporate_to_gas, blinding_flash, verdant_growth, ignition_burst, spawn_magma_cube, spawn_slime, jellify_potion, potion_burst, potion_reverse, turn_to_slime, stonification, melt_crucible (all boolean)",
                "Effect strengths can be negative to reduce/remove an effect (example: minecraft:mining_fatigue@-0.4).",
                "Set Randomization Mode to merge/replace to randomize ingredient effects/modifiers per world seed.",
                "Use Randomization Mode to decide whether configured effects/modifiers are kept (merge) or replaced (replace).",
                "Example entries: oredict=treeSapling; potion_effects=minecraft:haste@0.1",
                "NBT example (specific potion item): item=minecraft:potion; nbt={Potion:\"minecraft:water\"}; potion_duration=200",
                "NBT example (custom named item): item=minecraft:dye; meta=4; nbt={display:{Name:\"Arcane Dust\"}}; potion_effects=minecraft:luck@0.2",
                "Changes require a restart on dedicated servers."
        })
        @net.minecraftforge.common.config.Config.RequiresMcRestart
        public String[] ingredient_definitions = new String[]{
                "item=minecraft:redstone; potion_duration=600",
                "oredict=treeSapling; potion_duration=200",
                "item=arsmagica2:aum; potion_effects=minecraft:regeneration@0.25,minecraft:instant_damage@0.3",
                "item=arsmagica2:cerublossom; potion_effects=arsmagica2:levitation@0.3,minecraft:hunger@0.3",
                "item=arsmagica2:desert_nova; potion_effects=arsmagica2:mana_boost@0.5,minecraft:fire_resistance@0.25,minecraft:blindness@0.4",
                "item=arsmagica2:purified_vinteum_dust; potion_effects=arsmagica2:mana_regeneration@0.8,arsmagica2:mana_boost@0.5,minecraft:weakness@0.8",
                "item=arsmagica2:tarma_root; potion_effects=arsmagica2:true_sight@0.25,minecraft:silence@0.5",
                "item=arsmagica2:vinteum_dust; potion_effects=arsmagica2:shrink@0.5",
                "item=arsmagica2:wakebloom; potion_effects=arsmagica2:instant_mana@0.1,arsmagica2:water_breathing@0.25,minecraft:mining_fatigue@0.5",
                "item=ebwizardry:magic_crystal; meta=0; //magic// potion_effects=ebwizardry:sixth_sense@0.35,ebwizardry:arcane_jammer@0.35",
                "item=ebwizardry:magic_crystal; meta=1; //fire// potion_effects=ebwizardry:fireskin@0.45,minecraft:fire_resistance@0.25; ignition_burst=true",
                "item=ebwizardry:magic_crystal; meta=2; //ice// potion_effects=ebwizardry:ice_shroud@0.4,ebwizardry:frost@0.25,minecraft:slowness@0.2",
                "item=ebwizardry:magic_crystal; meta=3; //necromancy// ebwizardry:curse_of_undeath@0.3,minecraft:wither@0.15; potion_duration=2147483647",
                "item=ebwizardry:magic_crystal; meta=4; //lightning// potion_effects=ebwizardry:static_aura@0.35,minecraft:haste@0.25,ebwizardry:paralysis@0.25,minecraft:instant_damage@0.15",
                "item=ebwizardry:magic_crystal; meta=5; //sorcery// potion_effects=ebwizardry:muffle@0.45,ebwizardry:sixth_sense@0.25,minecraft:invisibility@0.2",
                "item=ebwizardry:magic_crystal; meta=6; //earth// potion_effects=ebwizardry:oakflesh@0.45,ebwizardry:diamondflesh@0.2,minecraft:resistance@0.2",
                "item=ebwizardry:magic_crystal; meta=7; //healing// potion_effects=minecraft:instant_health@0.4,minecraft:regeneration@0.35,ebwizardry:oakflesh@0.2",
                "item=minecraft:red_flower; meta=2; //allium// potion_effects=minecraft:absorption@0.5,minecraft:mining_fatigue@0.3",
                "item=minecraft:beetroot; potion_effects=minecraft:instant_health@0.3,minecraft:poison@0.5",
                "item=minecraft:blaze_powder; potion_effects=minecraft:haste@0.5,minecraft:instant_damage@0.5; ignition_burst=true",
                "item=minecraft:blaze_rod; potion_effects=minecraft:fire_resistance@0.25,minecraft:instant_damage@0.5; melt_crucible=true",
                "item=minecraft:red_flower; meta=1; //blue_orchid// potion_effects=minecraft:luck@0.5,minecraft:instant_health@0.3",
                "item=minecraft:bone; potion_effects=minecraft:weakness@0.5,minecraft:hunger@0.5,minecraft:resistance@0.4",
                "item=minecraft:brown_mushroom; potion_effects=minecraft:mining_fatigue@0.5,minecraft:speed@0.5",
                "item=minecraft:web; //cobweb// potion_effects=minecraft:slowness@0.35,minecraft:mining_fatigue@0.25",
                "item=minecraft:yellow_flower; meta=0; //dandelion// potion_effects=minecraft:unluck@0.5,minecraft:haste@0.3,minecraft:weakness@0.3,minecraft:resistance@0.25",
                "item=minecraft:ender_eye; potion_effects=minecraft:night_vision@0.8,minecraft:nausea@0.35,minecraft:instant_damage@0.5",
                "item=minecraft:fermented_spider_eye; potion_effects=minecraft:poison@0.5,minecraft:strength@0.5; potion_reverse=true",
                "item=minecraft:ghast_tear; potion_effects=minecraft:fire_resistance@0.5,minecraft:levitation@0.5",
                "item=minecraft:glowstone_dust; potion_effects=minecraft:glowing@0.5,minecraft:night_vision@0.5,minecraft:blindness@0.5; blinding_flash=true",
                "item=minecraft:golden_carrot; potion_effects=minecraft:glowing@0.5,minecraft:haste@0.5,minecraft:speed@0.5,minecraft:mining_fatigue@0.5",
                "item=minecraft:waterlily; //lily_pad// potion_effects=minecraft:jump_boost@0.4; minecraft:blindness@0.4;verdant_growth=true",
                "item=minecraft:magma_cream; potion_effects=minecraft:slowness@0.5,minecraft:instant_damage@0.5,minecraft:strength@0.4; spawn_magma_cube=true",
                "item=minecraft:nether_wart; potion_effects=minecraft:health_boost@0.25,minecraft:nausea@0.5",
                "item=minecraft:red_flower; meta=8; //oxeye_daisy// potion_effects=minecraft:instant_health@0.25,minecraft:blindness@0.35,minecraft:hunger@0.4",
                "item=minecraft:poisonous_potato; potion_effects=minecraft:poison@0.5; jellify_potion=true",
                "item=minecraft:red_flower; meta=0; //poppy// potion_effects=minecraft:luck@0.3,minecraft:nausea@0.3",
                "item=minecraft:rabbit_foot; potion_effects=minecraft:speed@0.5,minecraft:poison@0.35,minecraft:hunger@0.25,minecraft:luck@0.2",
                "item=minecraft:red_mushroom; potion_effects=minecraft:strength@0.5,minecraft:weakness@0.5",
                "item=minecraft:slime_ball; potion_effects=minecraft:slowness@0.5,minecraft:jump_boost@0.5; spawn_slime=true",
                "item=minecraft:spider_eye; potion_effects=minecraft:instant_health@0.3,minecraft:poison@0.3,minecraft:slowness@0.5,minecraft:night_vision@0.5",
                "item=minecraft:sugar; potion_effects=minecraft:haste@0.5,minecraft:speed@0.5,minecraft:nausea@0.3,minecraft:hunger@0.3",
                "item=minecraft:quartz; potion_all_effects_delta=0.2; potion_burst=true",
                "item=minecraft:chorus_fruit; potion_corruption=true",
                "item=minecraft:dye; meta=4; potion_effects=minecraft:luck@0.2,minecraft:nausea@0.3",
                "item=minecraft:dragon_breath; potion_lingering=true;",
                "item=minecraft:gunpowder; potion_splash=true"
        };
    }
}

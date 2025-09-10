package igentuman.spatialcrafter;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

public class CommonConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final General GENERAL = new General(BUILDER);
    public static final ForgeConfigSpec spec = BUILDER.build();

    private static boolean loaded = false;
    private static List<Runnable> loadActions = new ArrayList<>();

    public static void setLoaded() {
        if (!loaded)
            loadActions.forEach(Runnable::run);
        loaded = true;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void onLoad(Runnable action) {
        if (loaded)
            action.run();
        else
            loadActions.add(action);
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, spec, "spatial-crafter.toml");
    }

    public static class General {
        public final ForgeConfigSpec.IntValue recipe_energy_multiplier;
        public final ForgeConfigSpec.DoubleValue recipe_time_multiplier;
        public final ForgeConfigSpec.BooleanValue enable_entity_spawning;
        public final ForgeConfigSpec.BooleanValue enable_structure_preview;
        public final ForgeConfigSpec.BooleanValue replace_vanilla_recipes;

        public General(ForgeConfigSpec.Builder builder) {

            builder.push("Recipes");
            
            recipe_energy_multiplier = builder
                    .comment("Multiplier for recipe energy consumption")
                    .defineInRange("recipe_energy_multiplier", 100, 1, 100000);
                    
            recipe_time_multiplier = builder
                    .comment("Multiplier for recipe processing time")
                    .defineInRange("recipe_time_multiplier", 1.0, 0.1, 100.0);
                    
            enable_entity_spawning = builder
                    .comment("Enable entity spawning from recipes")
                    .define("enable_entity_spawning", true);
                    
            enable_structure_preview = builder
                    .comment("Enable structure preview rendering when a recipe is active")
                    .define("enable_structure_preview", true);

            replace_vanilla_recipes = builder
                    .comment("Repace vanilla crafting table recipes with spatial versions")
                    .define("replace_vanilla_recipes", true);

            builder.pop();
        }
    }

    public final static List<String> toHide = List.of(
            "compactmachines:wall",
            "compactmachines:machine_giant",
            "compactmachines:machine_large",
            "compactmachines:machine_maximum",
            "compactmachines:machine_normal",
            "compactmachines:machine_small",
            "compactmachines:machine_tiny",
            "compactmekanismmachinesplus:compact_thermoelectric_boiler",
            "compactmekanismmachinesplus:compact_fusion_reactor",
            "compactmekanismmachinesplus:compact_sps",
            "compactmekanismmachines:compact_thermal_evaporation",
            "compactmekanismmachines:compact_fission_reactor",
            "compactmekanismmachines:compact_industrial_turbine"
    );
}
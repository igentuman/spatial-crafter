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
        public final ForgeConfigSpec.ConfigValue<Integer> fe_per_tick;

        public General(ForgeConfigSpec.Builder builder) {
            builder.push("General");

            fe_per_tick = builder
                    .comment("Base FE per tick consumption")
                    .define("fe_per_tick", 5000);

            builder.pop();
        }
    }
}
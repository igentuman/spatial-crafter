package igentuman.spatialcrafter.integration.compactmachines;

import dev.compactmods.machines.config.CommonConfig;

public class CCUtil {

    public static void disableVanillaRecipes()
    {
        CommonConfig.ENABLE_VANILLA_RECIPES.set(false);
    }
}

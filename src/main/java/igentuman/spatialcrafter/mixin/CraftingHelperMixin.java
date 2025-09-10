package igentuman.spatialcrafter.mixin;

import com.google.gson.JsonObject;
import igentuman.spatialcrafter.CommonConfig;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static igentuman.spatialcrafter.CommonConfig.toHide;

@Mixin(CraftingHelper.class)
public class CraftingHelperMixin {

    @Inject(method = "processConditions(Lcom/google/gson/JsonObject;Ljava/lang/String;Lnet/minecraftforge/common/crafting/conditions/ICondition$IContext;)Z", at = @At("HEAD"), remap = false, cancellable = true)
    private static void onProcessConditions(JsonObject json, String memberName, ICondition.IContext context, CallbackInfoReturnable<Boolean> returnable) {
        if(!CommonConfig.GENERAL.replace_vanilla_recipes.get()) return;
        if(
                json.has("type")
                        && json.getAsJsonPrimitive("type").getAsString().equals("minecraft:crafting_shaped")
                && json.has("result")
                && json.getAsJsonObject("result").has("item")
        ) {
            if(toHide.contains(json.getAsJsonObject("result").getAsJsonPrimitive("item").getAsString())) {
                returnable.cancel();
                returnable.setReturnValue(false);
            }
        }
    }
}

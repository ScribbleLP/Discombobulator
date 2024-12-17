package com.minecrafttas.test.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//# 1.14.4
//$$import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
//$$@Mixin(SelectWorldScreen.class)
//# 1.12.2
import net.minecraft.client.gui.GuiWorldSelection;
@Mixin(GuiWorldSelection.class)
//# end
public class MixinWorldSelection {

	//# 1.14.4
//$$	@Inject(method = "init", at = @At("HEAD"), cancellable = true)
	//# 1.12.2
	@Inject(method = "initGui", at = @At("HEAD"), cancellable = true)
	//# end
	public void cancelInit(CallbackInfo ci) {
		//# 1.14.4
//$$		((SelectWorldScreen) (Object) this).blit(0, 0, 50, 50, 50, 50);
		//# 1.12.2
		((GuiWorldSelection) (Object) this).drawTexturedModalRect(0, 0, 50, 50, 50, 50);
		//# end
		ci.cancel();
	}

}

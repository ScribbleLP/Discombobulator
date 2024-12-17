package com.minecrafttas.test.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//# 1.14.4
//$$import net.minecraft.client.gui.Font;
//$$import net.minecraft.client.gui.screens.TitleScreen;
//$$
//$$@Mixin(TitleScreen.class)
//# def
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.FontRenderer;

@Mixin(GuiMainMenu.class)
//# end
public class MixinTitleScreen {

	//# 1.14.4
//$$	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
//$$	public void onDraw(TitleScreen g, Font font, String string, int i, int j, int k) {
	//# def
	@Redirect(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiMainMenu;drawString(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;III)V"))
	public void onDraw(GuiMainMenu g, FontRenderer font, String string, int i, int j, int k) {
	//# end
		g.drawString(font, "haha pancake was here", i, j, k);
	}

}

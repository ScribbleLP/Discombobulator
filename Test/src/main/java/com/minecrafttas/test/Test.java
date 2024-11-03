package com.minecrafttas.test;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.Minecraft;

public class Test implements ModInitializer {

	@Override
	public void onInitialize() {
		System.out.println("pancake tampered with your game");
		// # 1.14.4
//$$		Minecraft.getInstance();
		// # def
		Minecraft.getMinecraft();
		// # end
	}
}

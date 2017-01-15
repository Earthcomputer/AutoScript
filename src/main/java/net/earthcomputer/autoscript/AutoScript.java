package net.earthcomputer.autoscript;

import java.util.Map;

import org.lwjgl.input.Keyboard;

import net.earthcomputer.autoscript.scripts.Script;
import net.earthcomputer.autoscript.scripts.ScriptInput;
import net.earthcomputer.autoscript.scripts.Scripts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = AutoScript.MODID, name = AutoScript.NAME, version = AutoScript.VERSION, clientSideOnly = true)
public class AutoScript {
	public static final String MODID = "autoscript";
	public static final String NAME = "AutoScript";
	public static final String VERSION = "0.1-alpha";

	public static final KeyBinding keyBindStartScript = new KeyBinding("key.autoscript.startScript",
			KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL, Keyboard.KEY_O, "key.category.autoscript");

	@NetworkCheckHandler
	public boolean checkConnect(Side otherSide, Map<String, String> otherSideMods) {
		return true;
	}

	@EventHandler
	public void preinit(FMLPreInitializationEvent e) {
		ClientRegistry.registerKeyBinding(keyBindStartScript);
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(InputBlocker.class);
		InputBlocker.registerBlockKeys();
	}

	@EventHandler
	public void init(FMLInitializationEvent e) {
		InputBlocker.registerBlockingMouseHelper();
	}

	@SubscribeEvent
	public void onClientTick(ClientTickEvent e) {
		if (e.phase != Phase.START) {
			return;
		}
		if (!Minecraft.getMinecraft().isGamePaused()) {
			if (keyBindStartScript.isPressed()) {
				Minecraft.getMinecraft().displayGuiScreen(new GuiStartScript(Minecraft.getMinecraft().currentScreen));
			}
			ScriptInput.updateTick();
			Scripts.updateScripts();
		}
	}

	@SubscribeEvent
	public void onLogOut(ClientDisconnectionFromServerEvent e) {
		for (Script script : Scripts.getScripts()) {
			Scripts.stopScript(script);
		}
	}

}

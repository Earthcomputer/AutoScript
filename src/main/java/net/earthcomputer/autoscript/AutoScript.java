package net.earthcomputer.autoscript;

import java.util.Map;

import org.lwjgl.input.Keyboard;

import net.earthcomputer.autoscript.scripts.ScriptMovementInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MouseHelper;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraftforge.client.settings.IKeyConflictContext;
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

	private static boolean isBlockingInput = false;

	@NetworkCheckHandler
	public boolean checkConnect(Side otherSide, Map<String, String> otherSideMods) {
		return true;
	}

	@EventHandler
	public void preinit(FMLPreInitializationEvent e) {
		ClientRegistry.registerKeyBinding(keyBindStartScript);
		MinecraftForge.EVENT_BUS.register(this);
		GameSettings gameSettings = Minecraft.getMinecraft().gameSettings;
		registerBlockKey(gameSettings.keyBindInventory);
		registerBlockKey(gameSettings.keyBindSwapHands);
		registerBlockKey(gameSettings.keyBindDrop);
		gameSettings.keyBindUseItem.setKeyConflictContext(
				new KeyConflictContextBlockInput(gameSettings.keyBindUseItem.getKeyConflictContext()) {
					@Override
					public boolean isActive() {
						return super.isActive() || ScriptMovementInput.isRightClickPressed;
					}
				});
		registerBlockKey(gameSettings.keyBindAttack);
		registerBlockKey(gameSettings.keyBindPickBlock);
		for (int i = 0; i < 9; i++) {
			registerBlockKey(gameSettings.keyBindsHotbar[i]);
		}
	}

	@EventHandler
	public void init(FMLInitializationEvent e) {
		Minecraft.getMinecraft().mouseHelper = new MouseHelper() {
			@Override
			public void mouseXYChange() {
				if (isBlockingInput) {
					deltaX = 0;
					deltaY = 0;
				} else {
					super.mouseXYChange();
				}
			}
		};
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
			ScriptMovementInput.updateTick();
			Scripts.updateScripts();
		}
	}

	@SubscribeEvent
	public void onLogOut(ClientDisconnectionFromServerEvent e) {
		for (Script script : Scripts.getScripts()) {
			Scripts.stopScript(script);
		}
	}

	public static ScriptMovementInput startBlockingInput() {
		System.out.println(Minecraft.getMinecraft().mouseHelper.getClass());
		isBlockingInput = true;
		ScriptMovementInput movementInput = new ScriptMovementInput();
		Minecraft.getMinecraft().player.movementInput = movementInput;
		ScriptMovementInput.startPlayerInventoryHack(Minecraft.getMinecraft().player);
		return movementInput;
	}

	public static void stopBlockingInput() {
		isBlockingInput = false;
		Minecraft.getMinecraft().player.movementInput = new MovementInputFromOptions(
				Minecraft.getMinecraft().gameSettings);
		ScriptMovementInput.stopPlayerInventoryHack(Minecraft.getMinecraft().player);
	}

	public static void registerBlockKey(KeyBinding keyBinding) {
		keyBinding.setKeyConflictContext(new KeyConflictContextBlockInput(keyBinding.getKeyConflictContext()));
	}

	private static class KeyConflictContextBlockInput implements IKeyConflictContext {

		private IKeyConflictContext parent;

		public KeyConflictContextBlockInput(IKeyConflictContext parent) {
			this.parent = parent;
		}

		@Override
		public boolean isActive() {
			return !isBlockingInput && parent.isActive();
		}

		@Override
		public boolean conflicts(IKeyConflictContext other) {
			if (other instanceof KeyConflictContextBlockInput) {
				return conflicts(((KeyConflictContextBlockInput) other).parent);
			}
			return parent.conflicts(other);
		}

	}

}

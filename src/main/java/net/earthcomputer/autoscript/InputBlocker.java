package net.earthcomputer.autoscript;

import java.util.Deque;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.google.common.collect.Queues;

import net.earthcomputer.autoscript.fake.EntityPlayerProxy;
import net.earthcomputer.autoscript.scripts.ScriptInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MouseHelper;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;

public class InputBlocker {

	private InputBlocker() {
	}

	private static boolean isBlockingInput;
	private static boolean isCloseContainerEnabled = true;
	private static Deque<EntityPlayerProxy> fakePlayers = Queues.newArrayDeque();

	public static void registerBlockKeys() {
		GameSettings gameSettings = Minecraft.getMinecraft().gameSettings;
		registerBlockKey(gameSettings.keyBindSwapHands);
		registerBlockKey(gameSettings.keyBindDrop);
		gameSettings.keyBindUseItem.setKeyConflictContext(
				new KeyConflictContextBlockInput(gameSettings.keyBindUseItem.getKeyConflictContext()) {
					@Override
					public boolean isActive() {
						return super.isActive() || ScriptInput.isRightClickPressed;
					}
				});
		registerBlockKey(gameSettings.keyBindAttack);
		registerBlockKey(gameSettings.keyBindPickBlock);
		for (int i = 0; i < 9; i++) {
			registerBlockKey(gameSettings.keyBindsHotbar[i]);
		}
	}

	public static void registerBlockKey(KeyBinding keyBinding) {
		keyBinding.setKeyConflictContext(new KeyConflictContextBlockInput(keyBinding.getKeyConflictContext()));
	}

	public static void registerBlockingMouseHelper() {
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

	public static ScriptInput startBlockingInput() {
		isBlockingInput = true;
		ScriptInput movementInput = new ScriptInput();
		Minecraft.getMinecraft().player.movementInput = movementInput;
		ScriptInput.startPlayerInventoryHack(Minecraft.getMinecraft().player);
		return movementInput;
	}

	public static void stopBlockingInput() {
		isBlockingInput = false;
		Minecraft.getMinecraft().player.movementInput = new MovementInputFromOptions(
				Minecraft.getMinecraft().gameSettings);
		ScriptInput.stopPlayerInventoryHack(Minecraft.getMinecraft().player);
	}

	public static void disableCloseContainer() {
		isCloseContainerEnabled = false;
	}

	public static void enableCloseContainer() {
		isCloseContainerEnabled = true;
	}

	public static void pushFakePlayer(EntityPlayerProxy fakePlayer) {
		fakePlayers.push(fakePlayer);
	}

	public static void popFakePlayer() {
		fakePlayers.pop();
	}

	@SubscribeEvent
	public static void onGuiKeyInput(GuiScreenEvent.KeyboardInputEvent.Pre e) {
		if (isBlockingInput && e.getGui() instanceof GuiContainer) {
			e.setCanceled(true);
			if (isCloseContainerEnabled && Keyboard.getEventKey() == Keyboard.KEY_ESCAPE
					&& Keyboard.getEventKeyState()) {
				Minecraft.getMinecraft().displayGuiScreen(null);
			}
		}
	}

	@SubscribeEvent
	public static void onGuiMouseInput(GuiScreenEvent.MouseInputEvent.Pre e) {
		if (isBlockingInput && e.getGui() instanceof GuiContainer) {
			GuiContainer container = (GuiContainer) e.getGui();
			e.setCanceled(true);
			if (isCloseContainerEnabled && Minecraft.getMinecraft().gameSettings.touchscreen) {
				if (Mouse.getEventButtonState()) {
					if (container.touchValue++ > 0) {
						return;
					}
					int mouseX = Mouse.getEventX() * container.width / Minecraft.getMinecraft().displayWidth;
					int mouseY = Mouse.getEventY() * container.height / Minecraft.getMinecraft().displayHeight;
					if (mouseX < container.getGuiLeft() || mouseY < container.getGuiTop()
							|| mouseX >= container.getGuiLeft() + container.getXSize()
							|| mouseY >= container.getGuiTop() + container.getYSize()) {
						Minecraft.getMinecraft().displayGuiScreen(null);
					}
				} else if (Mouse.getEventButton() != -1) {
					container.touchValue--;
				}
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent e) {
		if (fakePlayers.isEmpty()) {
			return;
		}
		if (e.side != Side.CLIENT) {
			return;
		}
		if (e.phase != Phase.START) {
			return;
		}
		fakePlayers.peek().onUpdate();
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

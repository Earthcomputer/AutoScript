package net.earthcomputer.autoscript;

import java.io.IOException;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.earthcomputer.autoscript.scripts.Script;
import net.earthcomputer.autoscript.scripts.Scripts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.GuiScrollingList;

public class GuiStartScript extends GuiScreen {

	private GuiScreen parent;

	private String title;
	private GuiButton doneButton;
	private GuiButton cancelButton;
	private ScriptList scriptList;

	public GuiStartScript(GuiScreen parent) {
		this.parent = parent;
	}

	@Override
	public boolean doesGuiPauseGame() {
		return parent == null ? false : parent.doesGuiPauseGame();
	}

	@Override
	public void initGui() {
		title = I18n.format("gui.startScript.title");
		addButton(doneButton = new GuiButton(0, width / 2 - 110, height - 30, 100, 20,
				I18n.format("gui.startScript.start")));
		doneButton.enabled = false;
		addButton(cancelButton = new GuiButton(1, width / 2 + 10, height - 30, 100, 20, I18n.format("gui.cancel")));
		scriptList = new ScriptList(this, Scripts.getScripts());
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == Keyboard.KEY_ESCAPE) {
			actionPerformed(cancelButton);
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button == doneButton) {
			startScript();
			Minecraft.getMinecraft().displayGuiScreen(parent);
		} else if (button == cancelButton) {
			Minecraft.getMinecraft().displayGuiScreen(parent);
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, title, width / 2, 15, 0xffffff);
		scriptList.drawScreen(mouseX, mouseY, partialTicks);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void handleMouseInput() throws IOException {
		int mouseX = Mouse.getEventX() * width / mc.displayWidth;
		int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;

		super.handleMouseInput();

		scriptList.handleMouseInput(mouseX, mouseY);
	}

	private void onScriptClicked() {
		doneButton.enabled = true;
		Script script = scriptList.getSelectedScript();
		String text = "gui.startScript.start";
		if (Scripts.isScriptRunning(script)) {
			text = "gui.startScript.stop";
		}
		text = I18n.format(text);
		doneButton.displayString = text;
	}

	private void startScript() {
		Script script = scriptList.getSelectedScript();
		if (Scripts.isScriptRunning(script)) {
			Scripts.stopScript(script);
		} else {
			Scripts.startScript(script);
		}
	}

	private static class ScriptList extends GuiScrollingList {
		private GuiStartScript parent;
		private List<Script> scriptList;
		private int selectedIndex = -1;

		public ScriptList(GuiStartScript parent, List<Script> scriptList) {
			super(parent.mc, 200, parent.height, 32, parent.height - 40 + 4, parent.width / 2 - 210, 35, parent.width,
					parent.height);
			this.parent = parent;
			this.scriptList = scriptList;
		}

		@Override
		protected int getSize() {
			return scriptList.size();
		}

		@Override
		protected void elementClicked(int index, boolean doubleClick) {
			selectedIndex = index;
			parent.onScriptClicked();
		}

		@Override
		public boolean isSelected(int index) {
			return selectedIndex == index;
		}

		@Override
		protected void drawBackground() {
		}

		@Override
		protected int getContentHeight() {
			return getSize() * 35 + 1;
		}

		@Override
		protected void drawSlot(int slotIdx, int entryRight, int slotTop, int slotBuffer, Tessellator tess) {
			Script script = scriptList.get(slotIdx);
			parent.drawString(parent.fontRendererObj, script.getLocalizedName(), left + 3, slotTop + 3, 0xffffff);
			int color = 0xff0000;
			String text = "gui.startScript.notRunning";
			if (Scripts.isScriptRunning(script)) {
				color = 0x00ff00;
				text = "gui.startScript.running";
			}
			text = I18n.format(text);
			parent.drawString(parent.fontRendererObj, text, left + 3, slotTop + 15, color);
		}

		public Script getSelectedScript() {
			return selectedIndex == -1 ? null : scriptList.get(selectedIndex);
		}
	}

}

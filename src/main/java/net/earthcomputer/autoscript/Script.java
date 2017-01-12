package net.earthcomputer.autoscript;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public abstract class Script implements Comparable<Script> {

	protected Minecraft mc = Minecraft.getMinecraft();
	private String unlocalizedName;

	public String getLocalizedName() {
		return I18n.format(getUnlocalizedName());
	}

	public String getUnlocalizedName() {
		return "script." + unlocalizedName;
	}

	public Script setUnlocalizedName(String unlocalizedName) {
		this.unlocalizedName = unlocalizedName;
		return this;
	}

	@Override
	public int compareTo(Script other) {
		return getLocalizedName().compareTo(other.getLocalizedName());
	}

	public boolean canRun() {
		for (Script script : Scripts.getScripts()) {
			if (Scripts.isScriptRunning(script)) {
				return false;
			}
		}
		return true;
	}

	public void onStart() {
	}

	public void onUpdate() {
	}

	public void onStop() {
	}

}

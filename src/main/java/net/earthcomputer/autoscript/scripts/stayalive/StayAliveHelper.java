package net.earthcomputer.autoscript.scripts.stayalive;

import net.earthcomputer.autoscript.job.Job;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

public class StayAliveHelper {

	private boolean eating;

	public void modifyJobToStayAlive(Job job) {
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayerSP player = mc.player;
		if (!eating && player.getFoodStats().getFoodLevel() <= 17) {
			eating = true;
			job.startChild(new EatJob(this));
		}
	}

	public boolean isEating() {
		return eating;
	}

	public void setEating(boolean eating) {
		this.eating = eating;
	}

}

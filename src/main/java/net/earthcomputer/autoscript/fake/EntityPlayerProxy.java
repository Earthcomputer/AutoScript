package net.earthcomputer.autoscript.fake;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.EntityCreature;

public class EntityPlayerProxy extends EntityCreature {

	private EntityPlayerSP player;

	public EntityPlayerProxy(EntityPlayerSP player) {
		super(new WorldServerProxy((WorldClient) player.world));
		this.player = player;
		copyLocationAndAnglesFrom(player);
	}

	@Override
	public void onUpdate() {
		copyLocationAndAnglesFrom(player);
		super.onUpdate();
		player.moveForward = moveForward;
		player.moveStrafing = moveStrafing;
		player.setJumping(isJumping);
		player.setSneaking(isSneaking());
		player.setSprinting(isSprinting());
		player.setLocationAndAngles(player.posX, player.posY, player.posZ, rotationYaw, rotationPitch);
	}

}

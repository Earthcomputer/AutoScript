package net.earthcomputer.autoscript.scripts;

import net.earthcomputer.autoscript.AutoScript;
import net.earthcomputer.autoscript.Script;

public class ScriptBranchMine extends Script {

	private StayAliveHelper stayAliveHelper;

	@Override
	public void onStart() {
		stayAliveHelper = new StayAliveHelper();
		AutoScript.startBlockingInput();
	}

	@Override
	public void onUpdate() {
		stayAliveHelper.onUpdate();
	}

	@Override
	public void onStop() {
		AutoScript.stopBlockingInput();
	}

}

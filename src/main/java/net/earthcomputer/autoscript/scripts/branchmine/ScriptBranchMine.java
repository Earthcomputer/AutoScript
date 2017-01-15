package net.earthcomputer.autoscript.scripts.branchmine;

import net.earthcomputer.autoscript.AutoScript;
import net.earthcomputer.autoscript.job.Job;
import net.earthcomputer.autoscript.job.NeverEndingNopJob;
import net.earthcomputer.autoscript.scripts.Script;
import net.earthcomputer.autoscript.scripts.Scripts;
import net.earthcomputer.autoscript.scripts.stayalive.StayAliveHelper;

public class ScriptBranchMine extends Script {

	private StayAliveHelper stayAliveHelper;
	private Job job = new NeverEndingNopJob();

	@Override
	public void onStart() {
		stayAliveHelper = new StayAliveHelper();
		AutoScript.startBlockingInput();
		job.start();
	}

	@Override
	public void onUpdate() {
		stayAliveHelper.modifyJobToStayAlive(job);
		if (!job.isRunning()) {
			Scripts.stopScript(this);
		}
		job.update();
	}

	@Override
	public void onStop() {
		AutoScript.stopBlockingInput();
		job.stop();
	}

}

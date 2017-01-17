package net.earthcomputer.autoscript.job;

public class InstantActionJob extends Job {

	private Runnable action;

	public InstantActionJob(Runnable action) {
		this.action = action;
	}

	@Override
	public void startExecute() {
		action.run();
		stop();
	}

	@Override
	protected void updateExecute() {
		// nop
	}

	@Override
	protected void pauseExecute() {
		// nop
	}

	@Override
	protected void resumeExecute() {
		// nop
	}

}

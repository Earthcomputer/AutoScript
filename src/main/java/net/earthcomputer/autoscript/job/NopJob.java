package net.earthcomputer.autoscript.job;

public class NopJob extends Job {

	@Override
	public void startExecute() {
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

package net.earthcomputer.autoscript.job;

public class NeverEndingNopJob extends Job {

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

package net.earthcomputer.autoscript.job;

public abstract class Job {

	private boolean running = false;
	private boolean failed = false;
	private Job currentJob = this;

	/**
	 * Called when this Job is started
	 */
	protected void startExecute() {
		resumeExecute();
	}

	/**
	 * Called every tick when this Job is not paused
	 */
	protected abstract void updateExecute();

	/**
	 * Called to pause this Job, usually because a child Job is to execute
	 */
	protected abstract void pauseExecute();

	/**
	 * Called to resume this Job, usually because a child Job has finished
	 * executing
	 */
	protected abstract void resumeExecute();

	/**
	 * Called when this Job has either failed or completed
	 */
	protected void stopExecute() {
		pauseExecute();
	}

	protected void onChildJobFinished(Job childJob) {
	}

	/**
	 * Add a child Job to be executed now
	 * 
	 * @param child
	 */
	public final void startChild(Job child) {
		if (!running) {
			throw new IllegalStateException("Must be running");
		}
		if (currentJob != this) {
			currentJob.startChild(child);
			return;
		}
		currentJob = child;
		child.start();
		pauseExecute();
	}

	/**
	 * Causes this Job to fail, which stops the execution
	 */
	public final void fail() {
		stop();
		failed = true;
	}

	/**
	 * Returns whether this Job has failed
	 * 
	 * @return Whether this Job has failed
	 */
	public final boolean hasFailed() {
		return failed;
	}

	/**
	 * Returns whether this Job is running
	 * 
	 * @return Whether this Job is running
	 */
	public final boolean isRunning() {
		return running;
	}

	public final void start() {
		running = true;
		failed = false;
		currentJob = this;
		startExecute();
	}

	public final void update() {
		if (currentJob != this) {
			if (!currentJob.isRunning()) {
				Job childJob = currentJob;
				currentJob = this;
				onChildJobFinished(childJob);
				resumeExecute();
			} else {
				currentJob.update();
			}
		} else {
			updateExecute();
		}
	}

	public final void stop() {
		if (currentJob != this) {
			currentJob.stop();
		}
		stopExecute();
		running = false;
	}

}

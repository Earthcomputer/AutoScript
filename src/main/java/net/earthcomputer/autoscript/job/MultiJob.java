package net.earthcomputer.autoscript.job;

import com.google.common.collect.Iterables;

public class MultiJob extends Job {

	private Job[] children;
	private int index = 0;

	public MultiJob(Job... children) {
		this.children = children;
	}

	public MultiJob(Iterable<Job> children) {
		this(Iterables.toArray(children, Job.class));
	}

	@Override
	protected void startExecute() {
		index = 0;
		if (children.length != 0) {
			children[0].start();
		}
	}

	@Override
	protected void updateExecute() {
		if (!children[index].isRunning()) {
			if (children[index].hasFailed()) {
				fail();
			} else {
				index++;
				if (index == children.length) {
					stop();
				}
			}
		} else {
			children[index].update();
		}
	}

	@Override
	protected void pauseExecute() {
		children[index].pauseExecute();
	}

	@Override
	protected void resumeExecute() {
		children[index].resumeExecute();
	}

	@Override
	protected void stopExecute() {
	}

}

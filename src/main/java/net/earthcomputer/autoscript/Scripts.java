package net.earthcomputer.autoscript;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import net.earthcomputer.autoscript.scripts.ScriptBranchMine;

public class Scripts {

	private Scripts() {
	}

	private static final List<Script> scriptRegistry = Lists.newArrayList();
	private static final List<Script> runningScripts = Lists.newArrayList();

	public static void registerScript(Script script) {
		scriptRegistry.add(script);
	}

	public static List<Script> getScripts() {
		List<Script> scripts = Lists.newArrayList(scriptRegistry);
		Collections.sort(scripts);
		return scripts;
	}

	public static boolean isScriptRunning(Script script) {
		return runningScripts.contains(script);
	}

	public static boolean startScript(Script script) {
		if (isScriptRunning(script)) {
			return false;
		}
		if (!script.canRun()) {
			return false;
		}
		runningScripts.add(script);
		script.onStart();
		return true;
	}

	public static boolean stopScript(Script script) {
		if (!isScriptRunning(script)) {
			return false;
		}
		runningScripts.remove(script);
		script.onStop();
		return true;
	}

	public static void updateScripts() {
		for (int i = 0; i < runningScripts.size(); i++) {
			Script script = runningScripts.get(i);
			script.onUpdate();
			if (!isScriptRunning(script)) {
				i--;
			}
		}
	}

	static {
		registerScript(new ScriptBranchMine().setUnlocalizedName("branchMine"));
	}

}

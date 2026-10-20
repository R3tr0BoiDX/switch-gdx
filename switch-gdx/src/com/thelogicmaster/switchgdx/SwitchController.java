package com.thelogicmaster.switchgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.controllers.ControllerPowerLevel;
import com.badlogic.gdx.utils.Array;

public class SwitchController implements Controller {

	final Array<ControllerListener> listeners = new Array<>();
	final float[] axes = new float[4];
	final float[] prevAxes = new float[4];
	int buttons;
	int prevButtons;
	int id;
	boolean disposed;
	long vibrationEnd;
	boolean vibrating;

	SwitchController(int id) {
		this.id = id;
	}

	boolean getButtonPressed(int buttonCode) {
		return (buttons & (1 << buttonCode)) != 0 && (prevButtons & (1 << buttonCode)) == 0;
	}

	boolean getButtonReleased(int buttonCode) {
		return (buttons & (1 << buttonCode)) == 0 && (prevButtons & (1 << buttonCode)) != 0;
	}

	boolean isAnyButtonPressed() {
		// Todo: Mask joystick direction buttons
		return buttons != 0;
	}

	boolean isAnyButtonJustPressed() {
		return (buttons & ~prevButtons) != 0;
	}

	void updateVibration() {
		if (vibrating && System.currentTimeMillis() >= vibrationEnd)
			cancelVibration();
	}

	void dispose() {
		id = Controller.PLAYER_IDX_UNSET;
		disposed = true;
	}

	@Override
	public boolean getButton (int buttonCode) {
		return (buttons & (1 << buttonCode)) != 0;
	}

	@Override
	public float getAxis (int axisCode) {
		return axes[axisCode];
	}

	@Override
	public String getName () {
		return "Switch Controller";
	}

	@Override
	public String getUniqueId () {
		return "Switch Controller " + id;
	}

	@Override
	public int getMinButtonIndex () {
		return 0;
	}

	@Override
	public int getMaxButtonIndex () {
		return 31;
	}

	@Override
	public int getAxisCount () {
		return 4;
	}

	@Override
	public boolean isConnected () {
		return !disposed;
	}

	@Override
	public boolean canVibrate () {
		return true;
	}

	@Override
	public boolean isVibrating () {
		return vibrating;
	}

	@Override
	public void startVibration (int duration, float strength) {
		startVibration(duration, strength, strength);
	}

	/** Rumble with a strength (0-1) per side, e.g. for hits from the left. A single Joy-Con takes the stronger one */
	public void startVibration (int duration, float left, float right) {
		int result = SwitchControllerManager.vibrate(id, Math.max(0, Math.min(1, left)), Math.max(0, Math.min(1, right)));
		if (result != 0)
			Gdx.app.error("SwitchController", "Rumble failed: 0x" + Integer.toHexString(result));
		vibrationEnd = System.currentTimeMillis() + duration;
		vibrating = true;
	}

	@Override
	public void cancelVibration () {
		int result = SwitchControllerManager.vibrate(id, 0, 0);
		if (result != 0)
			Gdx.app.error("SwitchController", "Stopping the rumble failed: 0x" + Integer.toHexString(result));
		vibrating = false;
	}

	@Override
	public boolean supportsPlayerIndex () {
		return true;
	}

	@Override
	public int getPlayerIndex () {
		return id;
	}

	@Override
	public void setPlayerIndex (int index) {

	}

	@Override
	public ControllerMapping getMapping () {
		return SwitchControllerMapping.getInstance();
	}

	@Override
	public ControllerPowerLevel getPowerLevel () {
		return ControllerPowerLevel.POWER_UNKNOWN;
	}

	@Override
	public void addListener (ControllerListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener (ControllerListener listener) {
		listeners.removeValue(listener, true);
	}
}

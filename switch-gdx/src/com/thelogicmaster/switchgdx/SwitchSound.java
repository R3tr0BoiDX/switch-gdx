package com.thelogicmaster.switchgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.ReflectionPool;

import java.util.ArrayList;

public class SwitchSound implements Sound {

	private static class SoundInstance {
		int channel;
		boolean looping;
		float pitch;
	}
	
	@FunctionalInterface
	public interface Listener {
		void soundFinished(int id);
	}

	private long handle;

	private final IntMap<SoundInstance> instanceMap = new IntMap<>();
	private boolean disposed;
	private int id;
	private final Pool<SoundInstance> soundPool = new ReflectionPool<>(SoundInstance.class, 32);
	private final ArrayList<Listener> listeners = new ArrayList<>();
	private final byte[] wavData;

	public SwitchSound (FileHandle file) {
		wavData = null;
		create(file.file().getPath());
	}

	public SwitchSound (byte[] wavData) {
		this.wavData = wavData;
		create(wavData);
	}

	boolean update() {
		return disposed;
	}

	void onSoundFinished(int channel) {
		IntMap.Entry<SoundInstance> instanceEntry = null;
		for (IntMap.Entry<SoundInstance> entry: instanceMap.entries())
			if (entry.value.channel == channel) {
				instanceEntry = entry;
				break;
			}
		if (instanceEntry == null || instanceEntry.value.looping)
			return;
		soundPool.free(instanceMap.remove(instanceEntry.key));
		for (Listener listener : listeners)
			listener.soundFinished(channel);
	}

	public void addListener (Listener listener) {
		listeners.add(listener);
	}
	
	public void removeListener (Listener listener) {
		listeners.remove(listener);
	}
	
	private static int failedPlays;

	/** Logs how many sounds found no free channel, at exit: while running only every 100th is logged */
	static void logFailedPlays () {
		Gdx.app.log("SwitchSound", "No channel for " + failedPlays + " sounds in total");
	}

	private long play(float volume, float pitch, float pan, boolean looping) {
		int channel = play0(looping, volume, pitch, pan);
		// -1 means no free channel (or no chunk). Used as a channel below, it would set the volume of every channel.
		if (channel < 0) {
			if (failedPlays++ % 100 == 0)
				Gdx.app.log("SwitchSound", "No channel for a sound (" + (handle == 0 ? "disposed" : "all busy") + "), "
					+ failedPlays + " so far");
			return -1;
		}
		SoundInstance instance = soundPool.obtain();
		instanceMap.put(id, instance);
		instance.channel = channel;
		instance.looping = looping;
		instance.pitch = pitch;
		return id++;
	}

	@Override
	public long play () {
		return play(1);
	}

	@Override
	public long play (float volume) {
		return play(volume, 1, 0);
	}

	@Override
	public long play (float volume, float pitch, float pan) {
		return play(volume, pitch, pan, false);
	}

	@Override
	public long loop () {
		return loop(1);
	}

	@Override
	public long loop (float volume) {
		return loop(volume, 1, 0);
	}

	@Override
	public long loop (float volume, float pitch, float pan) {
		return play(volume, pitch, pan, true);
	}

	@Override
	public void stop () {
		for (IntMap.Entry<SoundInstance> entry : new IntMap.Entries<SoundInstance>(instanceMap)) {
			stop0(entry.value.channel);
			soundPool.free(entry.value);
		}
		instanceMap.clear();
	}

	@Override
	public void pause () {
		for (IntMap.Entry<SoundInstance> entry : new IntMap.Entries<SoundInstance>(instanceMap))
			pause0(entry.value.channel);
	}

	@Override
	public void resume () {
		for (IntMap.Entry<SoundInstance> entry : new IntMap.Entries<SoundInstance>(instanceMap))
			resume0(entry.value.channel);
	}

	@Override
	public void dispose () {
		if (disposed)
			return;

		stop();
		dispose0();
		disposed = true;
	}

	@Override
	public void stop (long soundId) {
		if (instanceMap.containsKey((int)soundId)) {
			stop0(instanceMap.get((int)soundId, null).channel);
			SoundInstance removed = instanceMap.remove((int)soundId);
			soundPool.free(removed);
		}
	}

	@Override
	public void pause (long soundId) {
		if (instanceMap.containsKey((int)soundId))
			pause0(instanceMap.get((int)soundId).channel);
	}

	@Override
	public void resume (long soundId) {
		if (instanceMap.containsKey((int)soundId))
			resume0(instanceMap.get((int)soundId).channel);
	}

	@Override
	public void setLooping (long soundId, boolean looping) {
		// It's not possible to change looping flag, so restart sound...
		SoundInstance instance = instanceMap.get((int)soundId);
		if (instance != null && instance.looping != looping) {
			setLooping0(instance.channel, looping, instance.pitch);
			instance.looping = looping;
		}
	}

	/** Not supported: SDL_mixer can't change the pitch of a playing sound. Pass the pitch to play or loop instead */
	@Override
	public void setPitch (long soundId, float pitch) {
	}

	@Override
	public void setVolume (long soundId, float volume) {
		if (instanceMap.containsKey((int)soundId))
			setVolume0(instanceMap.get((int)soundId).channel, volume);
	}

	@Override
	public void setPan (long soundId, float pan, float volume) {
		if (instanceMap.containsKey((int)soundId)) {
			int channel = instanceMap.get((int)soundId).channel;
			setPan0(channel, pan);
			setVolume0(channel, volume);
		}
	}

	private native void create(String path);

	private native void create(byte[] wavData);

	private native void dispose0();

	/** Starts the sound on a free channel with this volume, pitch (0.5-2) and pan, returns the channel or -1 */
	private native int play0(boolean looping, float volume, float pitch, float pan);

	/** Restarts the sound on its channel */
	private native void setLooping0(int channel, boolean looping, float pitch);

	private static native void stop0(int channel);

	private static native void pause0(int channel);

	private static native void resume0(int channel);

	private static native void setVolume0(int channel, float volume);

	private static native void setPan0(int channel, float pan);
}

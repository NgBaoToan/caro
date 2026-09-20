package caroai;

import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/** Bundled move recording; audio device work never runs on Swing's event thread. */
final class SoundEffects {
    private static final Logger LOG = Logger.getLogger(SoundEffects.class.getName());
    private static final Preferences PREFS = Preferences.userNodeForPackage(SoundEffects.class);
    private static final ThreadPoolExecutor AUDIO = new ThreadPoolExecutor(1, 1, 0,
            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1), runnable -> {
                Thread thread = new Thread(runnable, "caro-sound");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.DiscardOldestPolicy());
    private static volatile boolean enabled = PREFS.getBoolean("moveSound", true);
    private static volatile long generation;
    private static Clip clip; // Used only by the audio worker.
    private static boolean unavailable;

    private SoundEffects() {}

    static boolean isEnabled() { return enabled; }

    static void setEnabled(boolean value) {
        enabled = value;
        generation++;
        PREFS.putBoolean("moveSound", value);
        if (!value) AUDIO.execute(() -> { if (clip != null) clip.stop(); });
    }

    static void playMove() {
        if (!enabled || GraphicsEnvironment.isHeadless()) return;
        long request = generation;
        AUDIO.execute(() -> {
            if (!enabled || request != generation || unavailable) return;
            try {
                if (clip == null) {
                    Clip candidate = AudioSystem.getClip();
                    try (AudioInputStream recording = openMoveAudio()) {
                        candidate.open(recording);
                        clip = candidate;
                    } catch (Exception error) {
                        candidate.close();
                        throw error;
                    }
                }
                if (!enabled || request != generation) return;
                clip.stop();
                clip.setFramePosition(0);
                clip.start();
            } catch (Exception error) {
                unavailable = true;
                LOG.log(Level.WARNING, "Move sound unavailable; continuing without audio", error);
            }
        });
    }

    /** Single trimmed tap from the supplied chess-pieces recording, packaged as PCM WAV. */
    static AudioInputStream openMoveAudio() throws IOException, UnsupportedAudioFileException {
        var resource = SoundEffects.class.getResourceAsStream("/caroai/audio/move.wav");
        if (resource == null) throw new IOException("Missing move sound resource");
        var buffered = new BufferedInputStream(resource);
        try {
            return AudioSystem.getAudioInputStream(buffered);
        } catch (IOException | UnsupportedAudioFileException | RuntimeException error) {
            buffered.close();
            throw error;
        }
    }
}

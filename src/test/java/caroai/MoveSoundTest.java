package caroai;

import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MoveSoundTest {
    @Test void localPlayersSoundOnlyOnAcceptedMoves() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = new GameEngine(new Player("X", "X", false), new Player("O", "O", false));
            AtomicInteger sounds = new AtomicInteger();
            BoardPanel panel = new BoardPanel(engine, null, sounds::incrementAndGet);
            click(panel, 20, 20);
            click(panel, 20, 20); // occupied
            click(panel, 2040, 20); // outside 50x50
            assertThat(sounds.get()).isEqualTo(1);
            click(panel, 60, 20);
            assertThat(sounds.get()).isEqualTo(2);
            assertThat(engine.getBoard()).hasSize(2);
        });
    }

    @Test void computerOpeningSoundsOnce() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = new GameEngine(new Player("AI", "X", true), new Player("Human", "O", false));
            engine.setPlayerSymbol(GameEngine.Cell.O);engine.setAISymbol(GameEngine.Cell.X);
            AtomicInteger sounds = new AtomicInteger();
            BoardPanel panel = new BoardPanel(engine, new AI(50, 5), sounds::incrementAndGet);
            panel.maybeAIMove();panel.maybeAIMove();
            assertThat(sounds.get()).isEqualTo(1);
            assertThat(engine.getBoard()).hasSize(1);
        });
    }

    @Test void bundledRecordingIsShortPcmWithImmediateAttack() throws Exception {
        try (var audio = SoundEffects.openMoveAudio()) {
            var format = audio.getFormat();
            assertThat(format.getEncoding()).isEqualTo(javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED);
            assertThat(format.getSampleSizeInBits()).isEqualTo(16);
            assertThat(format.isBigEndian()).isFalse();
            assertThat(audio.getFrameLength() / (double) format.getFrameRate()).isBetween(0.1, 0.3);
            byte[] pcm = audio.readAllBytes();
            assertThat(pcm.length).isEqualTo(audio.getFrameLength() * format.getFrameSize());
            int peak = 0;
            int firstAudibleFrame = -1;
            for (int i = 0; i < pcm.length; i += 2) {
                short sample = (short) ((pcm[i] & 255) | (pcm[i + 1] << 8));
                peak = Math.max(peak, Math.abs((int) sample));
                if (firstAudibleFrame < 0 && Math.abs((int) sample) > 583) {
                    firstAudibleFrame = i / format.getFrameSize();
                }
            }
            assertThat(peak).isGreaterThan(1000);
            assertThat(firstAudibleFrame).isGreaterThanOrEqualTo(0);
            assertThat(firstAudibleFrame / (double) format.getFrameRate()).isLessThan(0.02);
        }
    }

    private static void click(BoardPanel panel,int x,int y) {
        panel.dispatchEvent(new MouseEvent(panel,MouseEvent.MOUSE_CLICKED,0,0,x,y,1,false));
    }
}

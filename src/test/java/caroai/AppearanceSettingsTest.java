package caroai;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

class AppearanceSettingsTest {
    private static List<AbstractButton> buttons(Container parent) {
        List<AbstractButton> result = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (child instanceof AbstractButton button) result.add(button);
            if (child instanceof Container container) result.addAll(buttons(container));
        }
        return result;
    }

    @Test void previewIsStagedAndApplyPassesBothChoices() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Theme.Mode initialMode = Theme.mode();
            Theme.StoneStyle initialStyle = Theme.stoneStyle();
            List<Object> applied = new ArrayList<>();
            JPanel panel = AppearanceSettings.createContent((mode,style) -> {applied.add(mode);applied.add(style);}, () -> {});
            List<AbstractButton> choices = buttons(panel);
            choices.stream().filter(b -> b.getText().startsWith("Sáng")).findFirst().orElseThrow().doClick(0);
            choices.stream().filter(b -> b.getText().equals("Huy hiệu")).findFirst().orElseThrow().doClick(0);
            assertThat(Theme.mode()).isEqualTo(initialMode);
            assertThat(Theme.stoneStyle()).isEqualTo(initialStyle);
            assertThat(applied).isEmpty();
            choices.stream().filter(b -> b.getText().equals("Áp dụng")).findFirst().orElseThrow().doClick(0);
            assertThat(applied).containsExactly(Theme.Mode.LIGHT,Theme.StoneStyle.BADGE);
        });
    }

    @Test void cancelDoesNotApplyPreview() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger applied = new AtomicInteger(), canceled = new AtomicInteger();
            JPanel panel = AppearanceSettings.createContent((mode,style) -> applied.incrementAndGet(), canceled::incrementAndGet);
            List<AbstractButton> choices = buttons(panel);
            choices.stream().filter(b -> b.getText().equals("Neon")).findFirst().orElseThrow().doClick(0);
            choices.stream().filter(b -> b.getText().equals("Hủy")).findFirst().orElseThrow().doClick(0);
            assertThat(applied.get()).isZero();assertThat(canceled.get()).isEqualTo(1);
        });
    }

    @Test void explicitStonePreviewPreservesCallerGraphicsState() {
        BufferedImage image = new BufferedImage(80,80,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.MAGENTA); g.setStroke(new BasicStroke(3));
            Paint paint=g.getPaint();Stroke stroke=g.getStroke();Composite composite=g.getComposite();
            for (Theme.StoneStyle style : Theme.StoneStyle.values()) {
                Theme.drawStone(g,true,40,40,20,.5f,style,Color.WHITE);
                Theme.drawStone(g,false,40,40,20,1,style,Color.ORANGE);
                assertThat(g.getPaint()).isEqualTo(paint);
                assertThat(g.getStroke()).isEqualTo(stroke);
                assertThat(g.getComposite()).isEqualTo(composite);
            }
        } finally {g.dispose();}
    }
}


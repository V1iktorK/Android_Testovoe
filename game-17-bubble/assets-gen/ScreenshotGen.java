package bubbles.gen;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Офлайн-генерация screenshot.png и иконки «Пузырьков глубин» (подводный мир).
 * Запуск: java bubbles.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color DEEP = new Color(12, 40, 74);
    private static final Color MID = new Color(22, 74, 120);
    private static final Color TEXT = new Color(222, 240, 255);
    private static final Color CORAL = new Color(255, 140, 120);
    private static final Color[] BUBBLES = {
            new Color(255, 111, 97), new Color(255, 183, 77), new Color(129, 222, 164),
            new Color(94, 168, 255), new Color(186, 122, 255)
    };

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "screenshot.png");
        int w = args.length > 1 ? Integer.parseInt(args[1]) : 1080;
        int h = args.length > 2 ? Integer.parseInt(args[2]) : 1920;
        drawScreenshot(w, h, out);
        drawIcon(new File("src/res/drawable/ic_launcher.png"));
        System.out.println("OK: " + out.getAbsolutePath());
    }

    private static void drawScreenshot(int w, int h, File out) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, MID, 0, h, DEEP));
        g.fillRect(0, 0, w, h);

        // световые столбы
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
        g.setColor(new Color(120, 200, 255));
        g.fill(new Ellipse2D.Double(w * 0.1, -h * 0.2, w * 0.4, h * 0.9));
        g.fill(new Ellipse2D.Double(w * 0.55, -h * 0.1, w * 0.4, h * 0.7));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // поле пузырей 8x10
        int cell = w / 8;
        Random rng = new Random(42);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 8; x++) {
                if (y > 6 && rng.nextDouble() < 0.35) continue;
                int cx = (int) ((x + 0.5) * cell + (y % 2 == 0 ? 0 : cell * 0.5));
                int cy = (int) (h * 0.16 + y * cell * 0.92);
                drawBubble(g, cx, cy, (int) (cell * 0.44), BUBBLES[rng.nextInt(BUBBLES.length)]);
            }
        }

        // пушка
        int cellW = w / 8;
        drawBubble(g, w / 2, (int) (h * 0.88), (int) (cellW * 0.5), BUBBLES[3]);
        g.setColor(new Color(40, 90, 140));
        g.fillOval(w / 2 - (int) (cellW * 0.32), (int) (h * 0.88) + (int) (cellW * 0.23), (int) (cellW * 0.64), (int) (cellW * 0.64));

        // кораллы
        g.setColor(CORAL);
        g.fillOval((int) (w * 0.04), (int) (h * 0.93), (int) (w * 0.16), (int) (w * 0.16));
        g.fillOval((int) (w * 0.16), (int) (h * 0.95), (int) (w * 0.12), (int) (w * 0.12));
        g.setColor(new Color(129, 222, 164));
        g.fillOval((int) (w * 0.78), (int) (h * 0.94), (int) (w * 0.18), (int) (w * 0.18));

        // HUD
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.05)));
        g.drawString("Уровень 6", (int) (w * 0.04), (int) (w * 0.09));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.038)));
        g.drawString("очки: 1450   ходов: 9", (int) (w * 0.04), (int) (w * 0.14));

        g.setColor(new Color(129, 222, 164));
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.045)));
        String cap = "подводный bubble shooter · цепочки и бонусы";
        g.drawString(cap, (w - g.getFontMetrics().stringWidth(cap)) / 2, (int) (h * 0.78));

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawBubble(Graphics2D g, int cx, int cy, int r, Color color) {
        g.setColor(color);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setColor(new Color(255, 255, 255, 90));
        g.fill(new Ellipse2D.Double(cx - r * 0.6, cy - r * 0.7, r * 0.64, r * 0.64));
    }

    private static void drawIcon(File out) throws Exception {
        int s = 192;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, MID, 0, s, DEEP));
        g.fillRoundRect(0, 0, s, s, 30, 30);
        drawBubble(g, 62, 62, 38, BUBBLES[0]);
        drawBubble(g, 128, 62, 38, BUBBLES[3]);
        drawBubble(g, 95, 128, 38, BUBBLES[2]);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

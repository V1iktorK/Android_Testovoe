package neonbeat.gen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Офлайн-генерация screenshot.png и иконки «Неон-Бита» (неоновый музыкальный стиль).
 * Запуск: java neonbeat.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color BG = new Color(13, 10, 31);
    private static final Color LANE_BG = new Color(20, 18, 51);
    private static final Color[] LANE_COLORS = {
            new Color(51, 242, 242), new Color(255, 89, 191),
            new Color(140, 230, 77), new Color(242, 204, 64)
    };

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "screenshot.png");
        int w = args.length > 1 ? Integer.parseInt(args[1]) : 1080;
        int h = args.length > 2 ? Integer.parseInt(args[2]) : 1920;
        drawScreenshot(w, h, out);
        drawIcon(new File("src/icon.png"));
        System.out.println("OK: " + out.getAbsolutePath());
    }

    private static void drawScreenshot(int w, int h, File out) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(26, 12, 51), 0, h, BG));
        g.fillRect(0, 0, w, h);

        int laneW = w / 4;
        int hitY = (int) (h * 0.72);

        // полосы
        for (int l = 0; l < 4; l++) {
            g.setColor(LANE_BG);
            g.fillRect(l * laneW + 16, 0, laneW - 32, h);
            // линия попадания со свечением
            for (int glow = 3; glow >= 0; glow--) {
                g.setColor(new Color(LANE_COLORS[l].getRed(), LANE_COLORS[l].getGreen(),
                        LANE_COLORS[l].getBlue(), 40 + glow * 40));
                g.fillRect(l * laneW + 16, hitY - 14 - glow * 6, laneW - 32, 28 + glow * 12);
            }
        }

        // падающие ноты (детерминированно для кадра)
        double[][] notes = {
                {0, 0.12}, {2, 0.22}, {1, 0.30}, {3, 0.38}, {0, 0.46},
                {2, 0.52}, {3, 0.56}, {1, 0.63}, {0, 0.68}, {2, 0.80}
        };
        for (double[] n : notes) {
            int x = (int) n[0];
            float t = (float) n[1];
            int y = (int) (hitY - (t - 0.75) * h * 0.9);
            if (y < -80 || y > h + 80) continue;
            g.setColor(LANE_COLORS[x]);
            g.fill(new RoundRectangle2D.Double(x * laneW + 50, y - 42, laneW - 100, 84, 26, 26));
        }

        // HUD
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.055)));
        g.drawString("очки: 24680", 40, 110);
        g.setColor(new Color(255, 214, 128));
        String combo = "комбо: 87";
        g.drawString(combo, w - g.getFontMetrics().stringWidth(combo) - 40, 110);

        // фидбек
        g.setColor(new Color(255, 255, 255, 220));
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.09)));
        String fb = "PERFECT";
        g.drawString(fb, (w - g.getFontMetrics().stringWidth(fb)) / 2, (int) (h * 0.42));

        // заголовок
        g.setColor(new Color(230, 236, 255));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.035)));
        String sub = "Неон-Бит · ритм по 4 полосам · процедурный саундтрек";
        g.drawString(sub, (w - g.getFontMetrics().stringWidth(sub)) / 2, (int) (h * 0.95));

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawIcon(File out) throws Exception {
        int s = 192;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(26, 12, 51), 0, s, BG));
        g.fillRoundRect(0, 0, s, s, 30, 30);
        for (int i = 0; i < 4; i++) {
            g.setColor(LANE_COLORS[i]);
            g.fill(new RoundRectangle2D.Double(18 + i * 42, 30 + (i % 2) * 40, 30, 90, 14, 14));
        }
        g.setColor(new Color(255, 214, 128));
        g.setStroke(new BasicStroke(10f));
        g.drawLine(20, 152, 172, 152);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

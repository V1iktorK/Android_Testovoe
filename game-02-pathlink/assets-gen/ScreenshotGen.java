package gravity.gen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.MultipleGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Офлайн-генерация screenshot.png и иконки «Гравити-Дрифт» (космос, туманность).
 * Запуск: java gravity.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color SPACE = new Color(33, 26, 71);
    private static final Color WELL = new Color(38, 26, 77);
    private static final Color STAR = new Color(255, 217, 77);
    private static final Color GOAL = new Color(51, 230, 178);
    private static final Color SHIP = new Color(242, 242, 255);
    private static final Color TEXT = new Color(235, 240, 255);

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
        g.setColor(SPACE);
        g.fillRect(0, 0, w, h);

        Random rng = new Random(42);
        // Туманности
        for (int i = 0; i < 6; i++) {
            int cx = rng.nextInt(w), cy = (int) (h * 0.12) + rng.nextInt((int) (h * 0.72));
            int r = (int) (w * (0.22 + rng.nextDouble() * 0.3));
            Color tint = i % 2 == 0 ? new Color(217, 140, 166, 80) : new Color(128, 166, 217, 80);
            g.setPaint(new RadialGradientPaint(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r).getBounds2D(),
                    new float[]{0f, 1f}, new Color[]{tint, new Color(0, 0, 0, 0)},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE));
            g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        }
        g.setPaint(null);
        // Звёзды фона
        for (int i = 0; i < 130; i++) {
            g.setColor(new Color(255, 255, 255, 60 + rng.nextInt(180)));
            int s = 2 + rng.nextInt(5);
            g.fillOval(rng.nextInt(w), (int) (h * 0.1) + rng.nextInt((int) (h * 0.8)), s, s);
        }

        // Гравитационные колодцы
        int[][] wells = {{330, 800, 150}, {760, 1150, 110}, {520, 1520, 130}};
        for (int[] wl : wells) {
            int r = wl[2];
            g.setPaint(new RadialGradientPaint(new Ellipse2D.Double(wl[0] - r, wl[1] - r, 2 * r, 2 * r).getBounds2D(),
                    new float[]{0f, 0.7f, 1f}, new Color[]{new Color(10, 6, 26), new Color(72, 45, 160, 220), new Color(72, 45, 160, 0)},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE));
            g.fill(new Ellipse2D.Double(wl[0] - r, wl[1] - r, 2 * r, 2 * r));
        }
        g.setPaint(null);

        // Звёзды-цели
        int[][] stars = {{420, 950}, {640, 1230}, {720, 1560}};
        for (int[] s : stars) {
            g.setColor(STAR);
            g.fillOval(s[0] - 26, s[1] - 26, 52, 52);
            g.setColor(new Color(255, 217, 77, 90));
            g.fillOval(s[0] - 48, s[1] - 48, 96, 96);
        }

        // Порт
        g.setColor(new Color(51, 230, 178, 120));
        g.fillOval(w - 260, (int) (h * 0.22) - 120, 240, 240);
        g.setColor(GOAL);
        g.setStroke(new BasicStroke(12f));
        g.drawOval(w - 260, (int) (h * 0.22) - 120, 240, 240);

        // Корабль и траектория
        g.setColor(new Color(255, 255, 255, 70));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                new float[]{24f, 18f}, 0f));
        g.drawOval(120, (int) (h * 0.62), w - 380, (int) (h * 0.28));
        g.setColor(SHIP);
        g.fillOval(150, (int) (h * 0.66), 84, 84);
        g.setColor(new Color(80, 160, 255));
        g.fillOval(162, (int) (h * 0.66) + 12, 60, 60);

        // HUD
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.05)));
        g.drawString("Уровень 7", 50, (int) (h * 0.075));
        g.drawString("топливо: 8.4   звёзды: 2/3", 50, (int) (h * 0.115));

        g.setColor(new Color(255, 217, 77));
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.045)));
        String cap = "наклон ведёт корабль сквозь гравитацию";
        g.drawString(cap, (w - g.getFontMetrics().stringWidth(cap)) / 2, (int) (h * 0.955));

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawIcon(File out) throws Exception {
        int s = 192;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(SPACE);
        g.fillRoundRect(0, 0, s, s, 30, 30);
        g.setPaint(new RadialGradientPaint(96, 120, 60f,
                new float[]{0f, 1f}, new Color[]{new Color(72, 45, 160), new Color(72, 45, 160, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fillOval(36, 60, 120, 120);
        g.setColor(SHIP);
        g.fillOval(70, 30, 52, 52);
        g.setColor(STAR);
        g.fillOval(30, 140, 26, 26);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

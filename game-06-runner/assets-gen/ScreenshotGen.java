package runner.gen;

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
 * Офлайн-генерация screenshot.png и иконки «Мультяшного раннера» (яркий мультяшный стиль).
 * Запуск: java runner.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color SKY_TOP = new Color(116, 192, 252);
    private static final Color SKY_BOTTOM = new Color(201, 240, 255);
    private static final Color HILL_FAR = new Color(120, 200, 130);
    private static final Color HILL_NEAR = new Color(86, 175, 105);
    private static final Color ROAD = new Color(238, 226, 200);
    private static final Color TEXT = new Color(45, 60, 85);
    private static final Color HERO = new Color(255, 122, 89);
    private static final Color SKIN = new Color(255, 226, 190);
    private static final Color COIN = new Color(255, 206, 68);

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
        g.setPaint(new GradientPaint(0, 0, SKY_TOP, 0, h * 0.55f, SKY_BOTTOM));
        g.fillRect(0, 0, w, h);

        // Облака
        g.setColor(Color.WHITE);
        for (int i = 0; i < 3; i++) {
            int cx = (int) (w * (0.18 + 0.34 * i));
            int cy = (int) (h * (0.13 + 0.05 * (i % 2)));
            g.fillOval(cx, cy, (int) (w * 0.18), (int) (w * 0.12));
            g.fillOval(cx + (int) (w * 0.1), cy - (int) (w * 0.04), (int) (w * 0.14), (int) (w * 0.1));
        }

        // Холмы
        g.setColor(HILL_FAR);
        g.fillOval((int) (-w * 0.15), (int) (h * 0.42), (int) (w * 0.7), (int) (h * 0.3));
        g.fillOval((int) (w * 0.6), (int) (h * 0.44), (int) (w * 0.6), (int) (h * 0.28));
        g.setColor(HILL_NEAR);
        g.fillOval((int) (w * 0.1), (int) (h * 0.5), (int) (w * 0.9), (int) (h * 0.3));

        // Дорога с полосами
        g.setColor(ROAD);
        g.fillRect(0, (int) (h * 0.55), w, (int) (h * 0.45));
        g.setStroke(new BasicStroke(8f));
        g.setColor(new Color(255, 255, 255, 90));
        g.drawLine(w / 3, (int) (h * 0.55), w / 3, h);
        g.drawLine(2 * w / 3, (int) (h * 0.55), 2 * w / 3, h);

        // Препятствия
        g.setColor(new Color(214, 74, 74));
        g.fill(new RoundRectangle2D.Double(w * 0.08, h * 0.60, w * 0.2, h * 0.08, 24, 24));
        g.setColor(new Color(255, 172, 64));
        g.fill(new RoundRectangle2D.Double(w * 0.42, h * 0.66, w * 0.2, h * 0.055, 20, 20));
        g.setColor(new Color(88, 132, 222));
        g.setStroke(new BasicStroke(18f));
        g.drawLine((int) (w * 0.72), (int) (h * 0.62), (int) (w * 0.95), (int) (h * 0.62));
        g.drawLine((int) (w * 0.72), (int) (h * 0.68), (int) (w * 0.95), (int) (h * 0.68));

        // Монеты
        for (int i = 0; i < 3; i++) {
            int cx = (int) (w * (0.2 + 0.3 * i));
            int cy = (int) (h * 0.78);
            g.setColor(COIN);
            g.fillOval(cx, cy, 70, 70);
            g.setColor(new Color(216, 160, 32));
            g.fillOval(cx + 18, cy + 18, 34, 34);
        }

        // Герой
        int hx = (int) (w * 0.46), hy = (int) (h * 0.80);
        g.setColor(HERO);
        g.fill(new RoundRectangle2D.Double(hx, hy, w * 0.12, h * 0.115, 36, 36));
        g.setColor(SKIN);
        g.fillOval(hx + (int) (w * 0.012), hy - (int) (h * 0.045), (int) (w * 0.095), (int) (w * 0.095));
        g.setColor(TEXT);
        g.fillOval(hx + (int) (w * 0.033), hy - (int) (h * 0.028), 14, 14);
        g.fillOval(hx + (int) (w * 0.073), hy - (int) (h * 0.028), 14, 14);

        // HUD
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.05)));
        g.drawString("Дистанция: 1248 м", (int) (w * 0.04), (int) (h * 0.10));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.042)));
        g.drawString("Монеты: 37", (int) (w * 0.04), (int) (h * 0.155));
        String best = "Рекорд: 2130";
        g.drawString(best, w - g.getFontMetrics().stringWidth(best) - (int) (w * 0.04), (int) (h * 0.10));

        g.setColor(new Color(255, 138, 96));
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.045)));
        String cap = "свайпы: полосы, прыжок и кувырок";
        g.drawString(cap, (w - g.getFontMetrics().stringWidth(cap)) / 2, (int) (h * 0.93));

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawIcon(File out) throws Exception {
        int s = 192;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, SKY_TOP, 0, s, SKY_BOTTOM));
        g.fillRoundRect(0, 0, s, s, 30, 30);
        g.setColor(HERO);
        g.fill(new RoundRectangle2D.Double(62, 82, 68, 78, 22, 22));
        g.setColor(SKIN);
        g.fillOval(68, 38, 56, 56);
        g.setColor(HILL_NEAR);
        g.fillOval(-10, 150, 120, 80);
        g.fillOval(90, 155, 110, 70);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

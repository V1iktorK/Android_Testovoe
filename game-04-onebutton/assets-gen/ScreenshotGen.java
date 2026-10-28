package impulse.gen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Офлайн-генерация screenshot.png и icon.png игры «Импульс-раннер» (ретро-футуризм).
 * Запуск: java impulse.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color SKY_TOP = new Color(23, 13, 61);
    private static final Color SKY_BOTTOM = new Color(217, 71, 115);
    private static final Color SUN = new Color(255, 183, 64);
    private static final Color GRID = new Color(64, 217, 242, 140);
    private static final Color NEON_A = new Color(51, 242, 242);
    private static final Color NEON_B = new Color(255, 89, 191);
    private static final Color SHIP = new Color(255, 242, 153);

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "screenshot.png");
        int w = args.length > 1 ? Integer.parseInt(args[1]) : 1920;
        int h = args.length > 2 ? Integer.parseInt(args[2]) : 1080;
        drawScreenshot(w, h, out);
        drawIcon(new File("src/icon.png"));
        System.out.println("OK: " + out.getAbsolutePath());
    }

    private static void drawScreenshot(int w, int h, File out) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Небо
        g.setPaint(new GradientPaint(0, 0, SKY_TOP, 0, h * 0.55f, SKY_BOTTOM));
        g.fillRect(0, 0, w, (int) (h * 0.55));
        g.setColor(SKY_BOTTOM);
        g.fillRect(0, (int) (h * 0.55), w, h);

        // Солнце со штрихами
        double cx = w / 2.0;
        double horizon = h * 0.55;
        g.setColor(SUN);
        g.fillOval((int) (cx - 300), (int) (horizon - 300), 600, 600);
        g.setColor(SKY_TOP);
        for (int i = 0; i < 6; i++) {
            g.fillRect((int) (cx - 320), (int) (horizon - 220 + i * 78), 640, 26);
        }

        // Перспективная сетка
        g.setStroke(new BasicStroke(3f));
        for (int i = 0; i < 16; i++) {
            double x = -w / 2.0 + i * (w / 8.0);
            g.setColor(GRID);
            g.draw(new Line2D.Double(cx, horizon, x * 3.2 + cx, h));
        }
        for (int i = 0; i < 9; i++) {
            double yy = horizon + Math.pow(1.3, i) * 60;
            if (yy > h) break;
            g.setColor(GRID);
            g.draw(new Line2D.Double(0, yy, w, yy));
        }

        // Препятствия (две плитки с проходом)
        int[][] obs = {{360, 260, 320}, {820, 120, 420}, {1280, 380, 280}, {1740, 200, 360}};
        for (int k = 0; k < obs.length; k++) {
            int x = obs[k][0], gapY = obs[k][1], gapH = obs[k][2];
            Color c = (k % 2 == 0) ? NEON_A : NEON_B;
            g.setColor(c);
            g.fillRect(x - 70, 0, 140, gapY);
            g.fillRect(x - 70, gapY + gapH, 140, (int) (h - gapY - gapH));
            g.setColor(Color.WHITE);
            g.fillRect(x - 70, gapY - 16, 140, 16);
            g.fillRect(x - 70, gapY + gapH, 140, 16);
        }

        // След корабля
        for (int i = 0; i < 8; i++) {
            float a = 0.30f - i * 0.035f;
            g.setColor(new Color(255, 150, 80, (int) (255 * Math.max(a, 0))));
            g.fillOval((int) (cx - 420 + i * 70), (int) (h * 0.42 - 40 + i * 6), 80 - i * 6, 60 - i * 5);
        }
        // Корабль
        Polygon ship = new Polygon();
        int sx = (int) cx, sy = (int) (h * 0.42);
        ship.addPoint(sx + 62, sy);
        ship.addPoint(sx - 48, sy - 42);
        ship.addPoint(sx - 48, sy + 42);
        g.setColor(SHIP);
        g.fillPolygon(ship);
        g.setColor(new Color(51, 102, 229));
        g.fillOval(sx - 2, sy - 16, 32, 32);

        // HUD
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 64));
        g.drawString("ДИСТАНЦИЯ 8240", 60, 110);
        g.setColor(new Color(255, 217, 128));
        g.setFont(new Font("SansSerif", Font.BOLD, 52));
        String best = "РЕКОРД 12650";
        g.drawString(best, w - g.getFontMetrics().stringWidth(best) - 60, 110);
        g.setFont(new Font("SansSerif", Font.BOLD, 56));
        g.drawString("★ 7", 60, h - 60);

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawIcon(File out) throws Exception {
        int s = 192;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, SKY_TOP, 0, s, SKY_BOTTOM));
        g.fillRoundRect(0, 0, s, s, 32, 32);
        g.setColor(SUN);
        g.fillOval(56, 36, 80, 80);
        g.setColor(SKY_TOP);
        for (int i = 0; i < 3; i++) g.fillRect(50, 56 + i * 18, 92, 6);
        Polygon ship = new Polygon();
        ship.addPoint(130, 128);
        ship.addPoint(62, 102);
        ship.addPoint(62, 154);
        g.setColor(SHIP);
        g.fillPolygon(ship);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

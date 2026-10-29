package ninja.gen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Офлайн-генерация screenshot.png и иконки «Тени-Клинка».
 * Визуальная подпись: СИЛУЭТЫ НА ЗАКАТЕ — чёрные фигуры на охристом градиенте, алый акцент.
 * (Осознанно отличается от пиксель-арта игры 01.)
 * Запуск: java ninja.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color SKY_TOP = new Color(250, 184, 107);
    private static final Color SKY_BOTTOM = new Color(235, 97, 71);
    private static final Color INK = new Color(20, 15, 26);
    private static final Color SUN = new Color(255, 220, 140);
    private static final Color STAR = new Color(255, 235, 166);
    private static final Color SCARF = new Color(217, 46, 41);
    private static final Color TEXT = new Color(48, 28, 32);

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
        g.setPaint(new GradientPaint(0, 0, SKY_TOP, 0, h, SKY_BOTTOM));
        g.fillRect(0, 0, w, h);

        // Солнце-диск с полосами
        int sunC = (int) (w * 0.52), sunY = (int) (h * 0.52), sunR = (int) (w * 0.36);
        g.setColor(SUN);
        g.fill(new Ellipse2D.Double(sunC - sunR, sunY - sunR, sunR * 2, sunR * 2));
        g.setColor(SKY_BOTTOM);
        for (int i = 0; i < 5; i++) {
            g.fillRect(sunC - sunR, sunY + 20 + i * 70, sunR * 2, 26);
        }

        // Силуэты сопок
        g.setColor(INK);
        int[] hx = {0, 180, 380, 620, 860};
        int[] hh = {200, 320, 240, 380, 260};
        for (int i = 0; i < hx.length; i++) {
            Polygon p = new Polygon();
            p.addPoint(hx[i] - 60, (int) (h * 0.78));
            p.addPoint(hx[i] + 110, (int) (h * 0.78) - hh[i]);
            p.addPoint(hx[i] + 290, (int) (h * 0.78));
            g.fillPolygon(p);
        }

        // Платформы-силуэты
        g.setColor(INK);
        g.fill(new Rectangle2D.Double(w * 0.05, h * 0.72, w * 0.38, 26));
        g.fill(new Rectangle2D.Double(w * 0.52, h * 0.62, w * 0.2, 26));
        g.fill(new Rectangle2D.Double(w * 0.8, h * 0.52, w * 0.18, 26));
        g.fill(new Rectangle2D.Double(w * 0.3, h * 0.42, w * 0.18, 24));

        // Ниндзя-силуэт с алой повязкой
        int nx = (int) (w * 0.2), ny = (int) (h * 0.72);
        g.setColor(INK);
        g.fill(new Rectangle2D.Double(nx, ny - 92, 66, 92));
        g.fill(new Ellipse2D.Double(nx + 2, ny - 132, 62, 62));
        g.setColor(SCARF);
        g.fill(new Rectangle2D.Double(nx - 30, ny - 104, 42, 14));

        // Звёзды
        for (int i = 0; i < 3; i++) {
            int sx = (int) (w * (0.44 + i * 0.13));
            int sy = (int) (h * (0.36 + (i % 2) * 0.07));
            g.setColor(STAR);
            drawStar(g, sx, sy, 30);
        }

        // Алые врата-финиш
        g.setColor(SCARF);
        g.fill(new Rectangle2D.Double(w * 0.86, h * 0.47, 22, h * 0.06));
        g.fill(new Rectangle2D.Double(w * 0.92, h * 0.47, 22, h * 0.06));
        g.fill(new Rectangle2D.Double(w * 0.86, h * 0.455, w * 0.08, 16));

        // HUD
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.05)));
        g.drawString("Уровень 8", (int) (w * 0.04), (int) (w * 0.09));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.038)));
        g.drawString("звёзды: 4   сюрикены: 3", (int) (w * 0.04), (int) (w * 0.135));

        g.setColor(SCARF);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.045)));
        String cap = "силуэты на закате · двойной прыжок · сюрикены";
        g.drawString(cap, (w - g.getFontMetrics().stringWidth(cap)) / 2, (int) (h * 0.9));

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawStar(Graphics2D g, int cx, int cy, int r) {
        Polygon p = new Polygon();
        for (int i = 0; i < 10; i++) {
            double ang = Math.PI * i / 5 - Math.PI / 2;
            int rr = (i % 2 == 0) ? r : r / 2;
            p.addPoint((int) (cx + Math.cos(ang) * rr), (int) (cy + Math.sin(ang) * rr));
        }
        g.fillPolygon(p);
    }

    private static void drawIcon(File out) throws Exception {
        int s = 192;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, SKY_TOP, 0, s, SKY_BOTTOM));
        g.fillRoundRect(0, 0, s, s, 30, 30);
        g.setColor(SUN);
        g.fillOval(56, 26, 80, 80);
        g.setColor(INK);
        g.fill(new Ellipse2D.Double(64, 52, 64, 64));
        g.fill(new Rectangle2D.Double(58, 106, 76, 58));
        g.setColor(SCARF);
        g.fill(new Rectangle2D.Double(42, 96, 44, 14));
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

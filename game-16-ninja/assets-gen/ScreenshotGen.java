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
 * Офлайн-генерация screenshot.png и иконки «Тени-Клинка» (ниндзя, пиксель-арт, ночь).
 * Запуск: java ninja.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color NIGHT = new Color(23, 26, 46);
    private static final Color NIGHT_BOTTOM = new Color(36, 42, 74);
    private static final Color TILE = new Color(56, 64, 97);
    private static final Color TILE_DARK = new Color(30, 35, 58);
    private static final Color NINJA = new Color(38, 38, 51);
    private static final Color NINJA_SCARF = new Color(217, 68, 74);
    private static final Color STAR = new Color(255, 217, 77);
    private static final Color TEXT = new Color(232, 238, 255);

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
        g.setPaint(new GradientPaint(0, 0, NIGHT, 0, h * 0.7f, NIGHT_BOTTOM));
        g.fillRect(0, 0, w, h);

        // Луна
        g.setColor(new Color(230, 235, 255));
        g.fill(new Ellipse2D.Double(w * 0.68, h * 0.12, w * 0.22, w * 0.22));
        g.setColor(new Color(196, 205, 240));
        g.fill(new Ellipse2D.Double(w * 0.72, h * 0.14, w * 0.05, w * 0.05));

        // Платформы (пиксельные блоки)
        drawPlatform(g, (int) (w * 0.05), (int) (h * 0.72), (int) (w * 0.4), 60);
        drawPlatform(g, (int) (w * 0.52), (int) (h * 0.62), (int) (w * 0.22), 60);
        drawPlatform(g, (int) (w * 0.8), (int) (h * 0.52), (int) (w * 0.18), 60);
        drawPlatform(g, (int) (w * 0.3), (int) (h * 0.42), (int) (w * 0.18), 50);

        // Ниндзя
        int nx = (int) (w * 0.22), ny = (int) (h * 0.68);
        g.setColor(NINJA);
        g.fill(new Rectangle2D.Double(nx, ny - 90, 70, 90));
        g.fill(new Ellipse2D.Double(nx + 4, ny - 130, 62, 62));
        g.setColor(NINJA_SCARF);
        g.fill(new Rectangle2D.Double(nx - 26, ny - 100, 40, 16));
        g.setColor(Color.WHITE);
        g.fill(new Rectangle2D.Double(nx + 18, ny - 108, 12, 8));
        g.fill(new Rectangle2D.Double(nx + 42, ny - 108, 12, 8));

        // Звёзды
        for (int i = 0; i < 3; i++) {
            int sx = (int) (w * (0.42 + i * 0.14));
            int sy = (int) (h * (0.36 + (i % 2) * 0.08));
            g.setColor(STAR);
            drawStar(g, sx, sy, 32);
        }

        // HUD
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.05)));
        g.drawString("Уровень 8", (int) (w * 0.04), (int) (w * 0.09));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.038)));
        g.drawString("звёзды: 4   сюрикены: 3", (int) (w * 0.04), (int) (w * 0.135));

        g.setColor(new Color(217, 68, 74));
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.045)));
        String cap = "двойной прыжок · лазание по стенам · сюрикены";
        g.drawString(cap, (w - g.getFontMetrics().stringWidth(cap)) / 2, (int) (h * 0.9));

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawPlatform(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(TILE);
        g.fill(new Rectangle2D.Double(x, y, width, height));
        g.setColor(TILE_DARK);
        g.fill(new Rectangle2D.Double(x, y + height - 14, width, 14));
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
        g.setColor(NIGHT);
        g.fillRoundRect(0, 0, s, s, 30, 30);
        g.setColor(NINJA);
        g.fill(new Ellipse2D.Double(60, 40, 72, 72));
        g.fill(new Rectangle2D.Double(56, 100, 80, 62));
        g.setColor(NINJA_SCARF);
        g.fill(new Rectangle2D.Double(40, 92, 44, 16));
        g.setColor(Color.WHITE);
        g.fill(new Rectangle2D.Double(76, 68, 14, 10));
        g.fill(new Rectangle2D.Double(104, 68, 14, 10));
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

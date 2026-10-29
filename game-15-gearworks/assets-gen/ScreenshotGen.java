package mechanosaur.gen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Офлайн-генерация screenshot.png и иконки «Механозавра» (стимпанк, латунь).
 * Запуск: java mechanosaur.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color BG = new Color(43, 33, 26);
    private static final Color BRASS = new Color(196, 150, 74);
    private static final Color COPPER = new Color(178, 106, 66);
    private static final Color PANEL = new Color(62, 48, 36);
    private static final Color TEXT = new Color(242, 226, 196);

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
        g.setColor(BG);
        g.fillRect(0, 0, w, h);

        // штриховка патентного чертежа
        g.setColor(new Color(58, 45, 35));
        g.setStroke(new BasicStroke(2f));
        for (int gx = 0; gx <= w; gx += 96) g.drawLine(gx, 0, gx, h);
        for (int gy = 0; gy <= h; gy += 96) g.drawLine(0, gy, w, gy);
        // Верхняя панель
        g.setColor(PANEL);
        g.fillRect(0, 0, w, (int) (h * 0.15));
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.05)));
        g.drawString("Уровень 7", (int) (w * 0.04), (int) (w * 0.08));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.038)));
        g.drawString("цель: передаточное число ×4.0", (int) (w * 0.04), (int) (w * 0.135));
        String rpm = "вход: 30 об/мин";
        g.drawString(rpm, w - g.getFontMetrics().stringWidth(rpm) - (int) (w * 0.04), (int) (w * 0.08));

        // Сетка-стол
        float gridTop = (float) (h * 0.18);
        float cell = w / 14f;
        g.setColor(new Color(52, 40, 30));
        g.setStroke(new BasicStroke(2f));
        for (int gx = 0; gx <= 14; gx++) g.drawLine((int) (gx * cell), (int) gridTop, (int) (gx * cell), (int) (gridTop + cell * 9));
        for (int gy = 0; gy <= 9; gy++) g.drawLine(0, (int) (gridTop + gy * cell), w, (int) (gridTop + gy * cell));

        // Шестерни (цепь: r=4 -> r=1)
        drawGear(g, (int) (2 * cell + cell / 2), (int) (gridTop + 4 * cell + cell / 2), (int) (cell * 2), BRASS);
        drawGear(g, (int) (6 * cell + cell / 2), (int) (gridTop + 4 * cell + cell / 2), (int) (cell * 0.5), COPPER);
        drawGear(g, (int) (9 * cell + cell / 2), (int) (gridTop + 2 * cell + cell / 2), (int) (cell * 1.1), BRASS);
        drawGear(g, (int) (9 * cell + cell / 2), (int) (gridTop + 6 * cell + cell / 2), (int) (cell * 0.8), COPPER);

        // Инвентарь
        float yInv = (float) (h * 0.82);
        String[] inv = {"r=1 ×2", "r=2 ×1", "r=4 ×1"};
        for (int i = 0; i < inv.length; i++) {
            int xi = (int) (w * 0.05 + i * w * 0.25);
            g.setColor(i == 2 ? BRASS : PANEL);
            g.fill(new RoundRectangle2D.Double(xi, yInv, w * 0.22, h * 0.07, 18, 18));
            g.setColor(TEXT);
            g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.038)));
            g.drawString(inv[i], xi + 16, yInv + h * 0.045f);
        }

        g.setColor(COPPER);
        g.fill(new RoundRectangle2D.Double(w * 0.54, h * 0.9, w * 0.42, h * 0.06, 60, 60));
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.036)));
        String check = "Проверить";
        g.drawString(check, (int) (w * 0.75 - g.getFontMetrics().stringWidth(check) / 2), (int) (h * 0.938));

        g.setColor(BRASS);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.045)));
        String cap = "стимпанк-пазл: собери передачу";
        g.drawString(cap, (w - g.getFontMetrics().stringWidth(cap)) / 2, (int) (h * 0.77));

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawGear(Graphics2D g, int cx, int cy, int radius, Color color) {
        g.setColor(color);
        g.fill(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
        int teeth = Math.max(6, radius / 12);
        for (int i = 0; i < teeth; i++) {
            double ang = Math.PI * 2 * i / teeth;
            int tx = (int) (cx + Math.cos(ang) * radius * 1.12);
            int ty = (int) (cy + Math.sin(ang) * radius * 1.12);
            int tr = (int) (radius * 0.16);
            g.fill(new Ellipse2D.Double(tx - tr, ty - tr, tr * 2, tr * 2));
        }
        g.setColor(BG);
        g.fill(new Ellipse2D.Double(cx - radius * 0.3, cy - radius * 0.3, radius * 0.6, radius * 0.6));
        g.setColor(color);
        g.setStroke(new BasicStroke(Math.max(2f, radius * 0.08f)));
        g.draw(new Ellipse2D.Double(cx - radius * 0.62, cy - radius * 0.62, radius * 1.24, radius * 1.24));
    }

    private static void drawIcon(File out) throws Exception {
        int s = 192;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(BG);
        g.fillRoundRect(0, 0, s, s, 30, 30);
        drawGear(g, 80, 80, 52, BRASS);
        drawGear(g, 146, 138, 32, COPPER);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

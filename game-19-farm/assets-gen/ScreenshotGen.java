package farm.gen;

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
 * Офлайн-генерация screenshot.png и иконки «Уютной фермы» (пастель, уют).
 * Запуск: java farm.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color SKY = new Color(250, 246, 233);
    private static final Color GRASS = new Color(216, 226, 188);
    private static final Color SOIL = new Color(150, 122, 96);
    private static final Color PANEL = new Color(255, 250, 240);
    private static final Color TEXT = new Color(104, 72, 52);
    private static final Color ACCENT = new Color(240, 160, 90);
    private static final Color SPROUT = new Color(120, 180, 100);
    private static final Color RIPE = new Color(242, 190, 90);

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
        g.setPaint(new GradientPaint(0, 0, SKY, 0, h * 0.55f, new Color(255, 220, 190)));
                g.fillRect(0, 0, w, h);

        // клетчатая бумага скетчбука
        g.setColor(new Color(205, 215, 228));
        g.setStroke(new BasicStroke(2f));
        for (int x = 0; x <= w; x += 96) g.drawLine(x, 0, x, h);
        for (int y = 0; y <= h; y += 96) g.drawLine(0, y, w, y);
        g.setColor(new Color(238, 190, 190));
        g.setStroke(new BasicStroke(5f));
        g.drawLine((int) (w * 0.1), 0, (int) (w * 0.1), h);
        g.setColor(GRASS);
        g.fillRect(0, h / 2, w, h / 2);

        // цветочки
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(255, 180, 190));
            g.fillOval((int) (w * (0.08 + i * 0.17)), (int) (h * 0.545), 22, 22);
            g.fillOval((int) (w * (0.12 + i * 0.17)), (int) (h * 0.575), 18, 18);
        }

        // HUD
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.055)));
        g.drawString("Монеты: 240", (int) (w * 0.05), (int) (w * 0.10));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.036)));
        g.drawString("семя: starfruit", (int) (w * 0.05), (int) (w * 0.15));

        // грядки 3x2
        double[][] growth = {{1.0, 0.6, 0.3}, {0.85, 1.0, 0.15}};
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                double x = w * 0.06 + col * w * 0.31;
                double y = h * 0.24 + row * h * 0.17;
                g.setColor(SOIL);
                g.fill(new RoundRectangle2D.Double(x, y, w * 0.28, h * 0.13, 22, 22));
                double gg = growth[row][col];
                g.setColor(gg >= 1.0 ? RIPE : SPROUT);
                double r = w * 0.03 + gg * w * 0.05;
                g.fill(new Ellipse2D.Double(x + w * 0.14 - r, y + h * 0.07 - r, r * 2, r * 2));
                if (gg >= 1.0) {
                    g.setColor(new Color(255, 240, 200));
                    g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.03)));
                    String harvest = "собрать!";
                    g.drawString(harvest, (int) (x + w * 0.14 - g.getFontMetrics().stringWidth(harvest) / 2), (int) (y + h * 0.115));
                }
            }
        }

        // кнопки
        String[] labels = {"Скрещивание", "Магазин семян"};
        for (int i = 0; i < 2; i++) {
            int cx = (int) (w * (0.27 + i * 0.46));
            g.setColor(ACCENT);
            g.fill(new RoundRectangle2D.Double(cx - w * 0.21, h * 0.62 - h * 0.033, w * 0.42, h * 0.065, 60, 60));
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.036)));
            g.drawString(labels[i], cx - g.getFontMetrics().stringWidth(labels[i]) / 2, (int) (h * 0.628));
        }

        // инвентарь
        g.setColor(PANEL);
        g.fill(new RoundRectangle2D.Double(w * 0.05, h * 0.72, w * 0.9, h * 0.23, 24, 24));
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.034)));
        g.drawString("Собранные культуры:", (int) (w * 0.08), (int) (h * 0.76));
        String[] inv = {"wheat ×6", "berry ×4", "starfruit ×2", "pumpkin ×1", "corn ×8"};
        for (int i = 0; i < inv.length; i++) {
            g.drawString(inv[i], (int) (w * 0.08) + (i % 3) * (int) (w * 0.3), (int) (h * 0.81) + (i / 3) * (int) (w * 0.055));
        }

        g.setColor(ACCENT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.045)));
        String cap = "выращивай · скрещивай · собирай урожай";
        g.drawString(cap, (w - g.getFontMetrics().stringWidth(cap)) / 2, (int) (h * 0.7));

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawIcon(File out) throws Exception {
        int s = 192;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(SKY);
        g.fillRoundRect(0, 0, s, s, 30, 30);
        g.setColor(GRASS);
        g.fill(new RoundRectangle2D.Double(0, 110, s, 82, 0, 0));
        g.setColor(SOIL);
        g.fill(new RoundRectangle2D.Double(28, 96, 136, 64, 18, 18));
        g.setColor(SPROUT);
        g.fillOval(60, 60, 40, 40);
        g.setColor(RIPE);
        g.fillOval(110, 50, 44, 44);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

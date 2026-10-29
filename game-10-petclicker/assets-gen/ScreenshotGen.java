package petclicker.gen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Офлайн-генерация screenshot.png и иконки «Пушистого кликера» (милые животные, пастель).
 * Запуск: java petclicker.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color BG = new Color(214, 242, 236);
    private static final Color BODY = new Color(255, 183, 120);
    private static final Color BODY_DARK = new Color(240, 150, 90);
    private static final Color HEAD = new Color(255, 206, 150);
    private static final Color TEXT = new Color(72, 42, 92);
    private static final Color ACCENT = new Color(255, 105, 180);
    private static final Color MINT = new Color(126, 214, 194);
    private static final Color CARD = new Color(255, 255, 255);

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
        g.setPaint(new GradientPaint(0, 0, BG, 0, h, new Color(255, 226, 210)));
        g.fillRect(0, 0, w, h);

        // HUD
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.06)));
        g.drawString("🪙 12.4K", (int) (w * 0.05), (int) (w * 0.10));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.038)));
        g.drawString("в секунду: 48", (int) (w * 0.05), (int) (w * 0.155));
        g.setColor(ACCENT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.055)));
        String combo = "комбо ×12";
        g.drawString(combo, w - g.getFontMetrics().stringWidth(combo) - (int) (w * 0.05), (int) (w * 0.10));
        g.setColor(MINT);
        String lvl = "уровень 7";
        g.drawString(lvl, w - g.getFontMetrics().stringWidth(lvl) - (int) (w * 0.05), (int) (w * 0.155));

        // Зверёк
        double cx = w / 2.0, cy = h * 0.45, r = w * 0.22;
        g.setColor(new Color(94, 66, 52, 30));
        g.fill(new Ellipse2D.Double(cx - r * 0.8, cy + r * 0.9, r * 1.6, r * 0.3));
        g.setColor(BODY);
        g.fill(new Ellipse2D.Double(cx - r * 0.85, cy - r * 0.25, r * 1.7, r * 1.25));
        // ушки
        g.setColor(BODY_DARK);
        Path2D earL = new Path2D.Double();
        earL.moveTo(cx - r * 0.75, cy - r * 0.35);
        earL.lineTo(cx - r * 0.55, cy - r * 1.15);
        earL.lineTo(cx - r * 0.25, cy - r * 0.55);
        earL.closePath();
        g.fill(earL);
        Path2D earR = new Path2D.Double();
        earR.moveTo(cx + r * 0.75, cy - r * 0.35);
        earR.lineTo(cx + r * 0.55, cy - r * 1.15);
        earR.lineTo(cx + r * 0.25, cy - r * 0.55);
        earR.closePath();
        g.fill(earR);
        // голова
        g.setColor(HEAD);
        g.fill(new Ellipse2D.Double(cx - r * 0.72, cy - r * 0.97, r * 1.44, r * 1.44));
        // глаза, нос, улыбка
        g.setColor(TEXT);
        g.fillOval((int) (cx - r * 0.37), (int) (cy - r * 0.41), (int) (r * 0.18), (int) (r * 0.18));
        g.fillOval((int) (cx + r * 0.19), (int) (cy - r * 0.41), (int) (r * 0.18), (int) (r * 0.18));
        g.setColor(new Color(235, 130, 120));
        g.fillOval((int) (cx - r * 0.08), (int) (cy - r * 0.2), (int) (r * 0.16), (int) (r * 0.16));
        g.setStroke(new BasicStroke((float) (r * 0.05)));
        g.drawArc((int) (cx - r * 0.22), (int) (cy - r * 0.18), (int) (r * 0.44), (int) (r * 0.3), 20, 140);

        // плавающие награды
        g.setColor(new Color(235, 130, 120));
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.05)));
        g.drawString("+14", (int) (cx + r * 1.1), (int) (cy - r * 0.8));
        g.drawString("+14", (int) (cx - r * 1.5), (int) (cy - r * 0.3));

        // ивент-баннер
        g.setColor(new Color(255, 214, 120, 220));
        g.fill(new RoundRectangle2D.Double(w * 0.1, w * 0.2, w * 0.8, w * 0.12, 24, 24));
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.05)));
        String ev = "Событие! Золотая лапка!";
        g.drawString(ev, (w - g.getFontMetrics().stringWidth(ev)) / 2, (int) (w * 0.33));

        // кнопки
        g.setColor(ACCENT);
        g.fill(new RoundRectangle2D.Double(w * 0.19, h * 0.65, w * 0.62, h * 0.065, 60, 60));
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.042)));
        String shop = "Магазин питомцев";
        g.drawString(shop, (w - g.getFontMetrics().stringWidth(shop)) / 2, (int) (h * 0.693));

        // подпись
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.036)));
        String cap = "тап по зверьку — монеты · комбо растёт от быстрых тапов";
        g.drawString(cap, (w - g.getFontMetrics().stringWidth(cap)) / 2, (int) (h * 0.86));

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawIcon(File out) throws Exception {
        int s = 192;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(BG);
        g.fillRoundRect(0, 0, s, s, 30, 30);
        g.setColor(HEAD);
        g.fill(new Ellipse2D.Double(36, 56, 120, 110));
        g.setColor(BODY_DARK);
        Path2D earL = new Path2D.Double();
        earL.moveTo(46, 70); earL.lineTo(62, 18); earL.lineTo(88, 62); earL.closePath();
        g.fill(earL);
        Path2D earR = new Path2D.Double();
        earR.moveTo(146, 70); earR.lineTo(130, 18); earR.lineTo(104, 62); earR.closePath();
        g.fill(earR);
        g.setColor(TEXT);
        g.fillOval(68, 96, 14, 14);
        g.fillOval(112, 96, 14, 14);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

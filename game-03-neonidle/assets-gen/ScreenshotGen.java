package neonidle.gen;

import com.neonidle.game.Game;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Офлайн-генерация screenshot.png и иконки (headless AWT), стиль: неон на тёмном фоне.
 * Запуск: java neonidle.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color BG = new Color(10, 10, 26);
    private static final Color NEON = new Color(0, 255, 200);
    private static final Color NEON2 = new Color(255, 60, 160);
    private static final Color DIM = new Color(70, 76, 110);
    private static final Color TEXT = new Color(220, 226, 255);

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

        // сетка
        g.setColor(new Color(22, 24, 48));
        int step = w / 12;
        for (int x = 0; x < w; x += step) g.draw(new Line2D.Double(x, 0, x, h));
        for (int y = 0; y < h; y += step) g.draw(new Line2D.Double(0, y, w, y));

        // счётчики
        g.setColor(NEON);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.052)));
        g.drawString("⚡ 12.4M", (int) (w * 0.05), (int) (w * 0.115));
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.034)));
        g.drawString("в секунду: 48.2K", (int) (w * 0.05), (int) (w * 0.162));
        g.setColor(NEON2);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.052)));
        String shards = "◆ 128 осколков";
        g.drawString(shards, w - g.getFontMetrics().stringWidth(shards) - (int) (w * 0.05), (int) (w * 0.115));

        // реактор со свечением
        double cx = w / 2.0;
        double cy = w * 0.52;
        double r = w * 0.19;
        g.setPaint(new RadialGradientPaint(new Ellipse2D.Double(cx - r * 2.2, cy - r * 2.2, r * 4.4, r * 4.4).getBounds2D(),
                new float[]{0f, 1f}, new Color[]{new Color(0, 255, 200, 120), new Color(0, 255, 200, 0)},
                java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fill(new Ellipse2D.Double(cx - r * 2.2, cy - r * 2.2, r * 4.4, r * 4.4));
        g.setPaint(null);
        g.setColor(new Color(14, 18, 40));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setColor(NEON);
        g.setStroke(new java.awt.BasicStroke((float) (w * 0.012)));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setColor(NEON2);
        g.draw(new Ellipse2D.Double(cx - r * 0.62, cy - r * 0.62, r * 1.24, r * 1.24));
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.055)));
        drawCentered(g, "+2.4K", (int) cx, (int) (cy + w * 0.018));
        g.setColor(DIM);
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.032)));
        drawCentered(g, "нажми ядро", (int) cx, (int) (cy + w * 0.08));

        // панель генераторов
        String[][] rows = {
                {"Светодиод  ×240", "1.2K ⚡"},
                {"Катушка  ×85", "18.4K ⚡"},
                {"Трансформатор  ×12", "220K ⚡"},
                {"Плазменный контур  ×3", "4.1M ⚡"}
        };
        float rowH = (float) (h * 0.045);
        float top = (float) (h * 0.80);
        for (String[] row : rows) {
            g.setColor(new Color(16, 40, 48));
            g.fill(new RoundRectangle2D.Double(w * 0.03, top, w * 0.94, rowH * 0.92, 16, 16));
            g.setFont(new Font("SansSerif", Font.PLAIN, (int) (rowH * 0.34)));
            g.setColor(NEON);
            g.drawString(row[0], (int) (w * 0.06), (int) (top + rowH * 0.58));
            g.setColor(TEXT);
            g.drawString(row[1], w - g.getFontMetrics().stringWidth(row[1]) - (int) (w * 0.06), (int) (top + rowH * 0.58));
            top += rowH;
        }

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawIcon(File out) throws Exception {
        int s = 192;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, BG, s, s, new Color(24, 18, 56)));
        g.fill(new RoundRectangle2D.Double(0, 0, s, s, 36, 36));
        g.setPaint(new RadialGradientPaint(s / 2f, s / 2f, 60f,
                new float[]{0f, 1f}, new Color[]{new Color(0, 255, 200, 160), new Color(0, 255, 200, 0)},
                java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fill(new Ellipse2D.Double(26, 26, 140, 140));
        g.setColor(new Color(14, 18, 40));
        g.fill(new Ellipse2D.Double(56, 56, 80, 80));
        g.setColor(NEON);
        g.setStroke(new java.awt.BasicStroke(9f));
        g.draw(new Ellipse2D.Double(56, 56, 80, 80));
        g.setColor(NEON2);
        g.draw(new Ellipse2D.Double(76, 76, 40, 40));
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }

    private static void drawCentered(Graphics2D g, String s, int cx, int y) {
        g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, y);
    }
}

package wasteland.gen;

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
 * Офлайн-генерация screenshot.png и иконки «Пепла» (постапокалипсис, выцветшие тона).
 * Запуск: java wasteland.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color SKY = new Color(196, 178, 146);
    private static final Color SKY_TOP = new Color(217, 156, 100);
    private static final Color PANEL = new Color(62, 56, 48);
    private static final Color ACCENT = new Color(217, 120, 68);
    private static final Color TEXT = new Color(242, 236, 222);
    private static final Color DANGER = new Color(178, 64, 54);

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
        g.setPaint(new GradientPaint(0, 0, SKY_TOP, 0, h * 0.5f, SKY));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(170, 152, 122));
        g.fillRect(0, h / 2, w, h / 2);

        // Силуэты руин
        g.setColor(new Color(120, 106, 86));
        int[] xs = {40, 180, 330, 470, 620, 760, 900};
        int[] hs = {260, 420, 300, 500, 330, 460, 280};
        for (int i = 0; i < xs.length; i++) {
            g.fillRect(xs[i], h / 2 - hs[i], 110, hs[i]);
        }
        // окна-разрушения
        g.setColor(new Color(90, 80, 64));
        for (int i = 0; i < xs.length; i += 2) {
            g.fillRect(xs[i] + 20, h / 2 - hs[i] + 40, 28, 36);
            g.fillRect(xs[i] + 62, h / 2 - hs[i] + 90, 28, 36);
        }

        // верхняя панель
        g.setColor(PANEL);
        g.fillRect(0, 0, w, (int) (h * 0.16));
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.045)));
        g.drawString("День 7 · день", (int) (w * 0.04), (int) (w * 0.07));
        drawBar(g, (int) (w * 0.04), (int) (w * 0.11), (int) (w * 0.28), 0.82f, new Color(120, 200, 120), "ЗДОРОВЬЕ");
        drawBar(g, (int) (w * 0.36), (int) (w * 0.11), (int) (w * 0.28), 0.55f, ACCENT, "ГОЛОД");
        drawBar(g, (int) (w * 0.68), (int) (w * 0.11), (int) (w * 0.28), 0.7f, new Color(120, 170, 230), "ТЕПЛО");

        // событие
        g.setColor(new Color(40, 36, 30, 220));
        g.fill(new RoundRectangle2D.Double(w * 0.06, h * 0.17, w * 0.88, h * 0.065, 18, 18));
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.036)));
        String ev = "Добыча: medicine water cloth canned_food";
        g.drawString(ev, (w - g.getFontMetrics().stringWidth(ev)) / 2, (int) (h * 0.212));

        // кнопки локаций
        String[] labels = {"Обыскать: Улица", "Обыскать: Больница", "Обыскать: Завод", "Обыскать: База"};
        for (int i = 0; i < 4; i++) {
            int x = (int) (w * 0.06) + (i % 2) * (int) (w * 0.47);
            int y = (int) (h * 0.29) + (i / 2) * (int) (h * 0.13);
            g.setColor(PANEL);
            g.fill(new RoundRectangle2D.Double(x, y, w * 0.42, h * 0.085, 60, 60));
            g.setColor(TEXT);
            g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.036)));
            g.drawString(labels[i], x + 30, y + (int) (h * 0.055));
        }

        // инвентарь
        g.setColor(PANEL);
        g.fill(new RoundRectangle2D.Double(w * 0.05, h * 0.82, w * 0.9, h * 0.15, 20, 20));
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.032)));
        String[] inv = {"wood ×4", "water ×2", "canned_food ×1", "cloth ×3", "medicine ×1"};
        for (int i = 0; i < inv.length; i++) {
            g.drawString(inv[i], (int) (w * 0.08) + (i % 3) * (int) (w * 0.3), (int) (h * 0.86) + (i / 3) * (int) (w * 0.055));
        }

        g.setColor(ACCENT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.045)));
        String cap = "день и ночь · голод и холод · крафт";
        g.drawString(cap, (w - g.getFontMetrics().stringWidth(cap)) / 2, (int) (h * 0.78));

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawBar(Graphics2D g, int x, int y, int w, float frac, Color c, String label) {
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRect(x, y, w, 18);
        g.setColor(c);
        g.fillRect(x, y, (int) (w * frac), 18);
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString(label, x, y + 38);
    }

    private static void drawIcon(File out) throws Exception {
        int s = 192;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, SKY_TOP, 0, s, SKY));
        g.fillRoundRect(0, 0, s, s, 30, 30);
        g.setColor(new Color(120, 106, 86));
        g.fillRect(30, 100, 40, 70);
        g.fillRect(86, 76, 46, 94);
        g.fillRect(146, 112, 30, 58);
        g.setColor(ACCENT);
        g.fillOval(78, 30, 36, 36);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

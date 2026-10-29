package powergrid.gen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Офлайн-генерация screenshot.png и иконки «Энерго-Щита».
 * Визуальная подпись: ТЕХНИЧЕСКИЙ ЧЕРТЁЖ — синий blueprint, белые линии, размерные стрелки.
 * Запуск: java powergrid.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color PAPER = new Color(26, 51, 97);
    private static final Color GRID = new Color(64, 96, 150);
    private static final Color LINE = new Color(225, 236, 255);
    private static final Color AMBER = new Color(255, 166, 70);
    private static final Color TEXT = new Color(225, 236, 255);

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
        g.setColor(PAPER);
        g.fillRect(0, 0, w, h);

        // Миллиметровка
        g.setColor(GRID);
        g.setStroke(new BasicStroke(1.5f));
        for (int x = 0; x <= w; x += 54) g.drawLine(x, 0, x, h);
        for (int y = 0; y <= h; y += 54) g.drawLine(0, y, w, y);
        g.setStroke(new BasicStroke(3f));
        for (int x = 0; x <= w; x += 270) g.drawLine(x, 0, x, h);
        for (int y = 0; y <= h; y += 270) g.drawLine(0, y, w, y);

        // Рамка чертежа
        g.setColor(LINE);
        g.setStroke(new BasicStroke(6f));
        g.draw(new Rectangle2D.Double(30, 30, w - 60, h - 60));

        // Коридор атаки (штриховыми линиями как на схеме)
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{24f, 16f}, 0f));
        g.draw(new Line2D.Double(w * 0.05, h * 0.34, w * 0.95, h * 0.34));
        g.draw(new Line2D.Double(w * 0.05, h * 0.56, w * 0.95, h * 0.56));

        // Башни — контурные прямоугольники с обводкой
        int[][] towers = {{180, 620}, {430, 880}, {680, 620}, {880, 880}};
        for (int[] t : towers) {
            g.setColor(LINE);
            g.setStroke(new BasicStroke(5f));
            g.draw(new Rectangle2D.Double(t[0] - 52, t[1] - 52, 104, 104));
            g.drawOval(t[0] - 26, t[1] - 26, 52, 52);
            // размерная выноска
            g.setStroke(new BasicStroke(2.5f));
            g.draw(new Line2D.Double(t[0] - 52, t[1] + 86, t[0] + 52, t[1] + 86));
            g.draw(new Line2D.Double(t[0] - 52, t[1] + 78, t[0] - 52, t[1] + 94));
            g.draw(new Line2D.Double(t[0] + 52, t[1] + 78, t[0] + 52, t[1] + 94));
        }

        // Враги — оранжевые маркеры
        int[][] enemies = {{120, 590}, {300, 590}, {470, 760}, {640, 760}, {800, 590}};
        for (int[] e : enemies) {
            g.setColor(AMBER);
            g.setStroke(new BasicStroke(5f));
            g.drawOval(e[0] - 30, e[1] - 30, 60, 60);
            g.draw(new Line2D.Double(e[0] - 44, e[1], e[0] - 34, e[1]));
            g.draw(new Line2D.Double(e[0] + 34, e[1], e[0] + 44, e[1]));
        }

        // Электростанция — блок с «электрической» подписью
        g.setColor(LINE);
        g.draw(new Rectangle2D.Double(w * 0.82, h * 0.2, w * 0.13, h * 0.13));
        g.setColor(AMBER);
        g.drawPolyline(new int[]{(int) (w * 0.87), (int) (w * 0.85), (int) (w * 0.88), (int) (w * 0.86)},
                new int[]{(int) (h * 0.185), (int) (h * 0.225), (int) (h * 0.225), (int) (h * 0.265)}, 4);

        // HUD и штамп чертежа
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.045)));
        g.drawString("Волна 5/10", (int) (w * 0.05), (int) (h * 0.085));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.036)));
        g.drawString("станция: 82   счёт: 1450   энергия: 45", (int) (w * 0.05), (int) (h * 0.125));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.028)));
        g.drawString("СХЕМА ОБОРОНЫ ЭЛЕКТРОСТАНЦИИ · ЛИСТ 5", (int) (w * 0.05), (int) (h * 0.955));

        // Панель строительства как спецификация
        String[] labels = {"1 — Пилон 50 кВт", "2 — Турбина 90 кВт", "3 — Реактор 220 кВт"};
        for (int i = 0; i < 3; i++) {
            int x = (int) (w * 0.04 + i * w * 0.32);
            g.setColor(LINE);
            g.setStroke(new BasicStroke(3f));
            g.draw(new Rectangle2D.Double(x, h * 0.86, w * 0.28, h * 0.065));
            g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.03)));
            g.drawString(labels[i], x + 14, (int) (h * 0.9));
        }

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawIcon(File out) throws Exception {
        int s = 192;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(PAPER);
        g.fillRoundRect(0, 0, s, s, 30, 30);
        g.setColor(GRID);
        g.setStroke(new BasicStroke(2f));
        for (int x = 0; x <= s; x += 24) { g.drawLine(x, 0, x, s); g.drawLine(0, x, s, x); }
        g.setColor(LINE);
        g.setStroke(new BasicStroke(6f));
        g.draw(new Rectangle2D.Double(84, 34, 74, 92));
        g.drawOval(104, 58, 34, 34);
        g.setColor(AMBER);
        g.drawPolyline(new int[]{40, 24, 52, 34}, new int[]{44, 92, 92, 140}, 4);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

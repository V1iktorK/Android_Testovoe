package powergrid.gen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Офлайн-генерация screenshot.png и иконки «Энерго-Щита» (промышленный стиль).
 * Запуск: java powergrid.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color FLOOR = new Color(41, 44, 51);
    private static final Color PATH = new Color(71, 66, 56);
    private static final Color TOWER = new Color(89, 140, 178);
    private static final Color ENEMY = new Color(217, 89, 76);
    private static final Color PLANT = new Color(229, 191, 51);
    private static final Color TEXT = new Color(235, 240, 250);
    private static final Color HAZARD = new Color(255, 200, 60);

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
        g.setColor(FLOOR);
        g.fillRect(0, 0, w, h);

        // коридор атаки (змейка)
        g.setColor(PATH);
        g.fillRect(0, (int) (h * 0.30), w, (int) (h * 0.10));
        g.fillRect((int) (w * 0.28), (int) (h * 0.30), (int) (w * 0.10), (int) (h * 0.28));
        g.fillRect((int) (w * 0.28), (int) (h * 0.48), (int) (w * 0.44), (int) (h * 0.10));
        g.fillRect((int) (w * 0.62), (int) (h * 0.30), (int) (w * 0.10), (int) (h * 0.28));
        g.fillRect((int) (w * 0.62), (int) (h * 0.30), (int) (w * 0.38), (int) (h * 0.10));

        // предупреждающая разметка
        g.setColor(HAZARD);
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{28f, 20f}, 0f));
        g.draw(new Rectangle2D.Double(0, (int) (h * 0.30), w, (int) (h * 0.10)));

        // электростанция
        g.setColor(PLANT);
        g.fill(new Rectangle2D.Double(w * 0.82, h * 0.22, w * 0.14, h * 0.12));
        g.setColor(new Color(180, 140, 30));
        g.fill(new Rectangle2D.Double(w * 0.84, h * 0.16, w * 0.02, h * 0.07));
        g.fill(new Rectangle2D.Double(w * 0.9, h * 0.14, w * 0.02, h * 0.09));

        // башни
        int[][] towers = {{180, 620}, {430, 880}, {680, 620}, {880, 880}};
        for (int[] t : towers) {
            g.setColor(TOWER);
            g.fill(new Rectangle2D.Double(t[0] - 46, t[1] - 46, 92, 92));
            g.setColor(new Color(60, 100, 130));
            g.fillOval(t[0] - 22, t[1] - 22, 44, 44);
            // радиус
            g.setColor(new Color(89, 140, 178, 50));
            g.setStroke(new BasicStroke(4f));
            g.drawOval(t[0] - 180, t[1] - 180, 360, 360);
        }

        // враги на коридоре
        int[][] enemies = {{120, 590}, {300, 590}, {470, 760}, {640, 760}, {800, 590}};
        for (int[] e : enemies) {
            g.setColor(ENEMY);
            g.fillOval(e[0] - 32, e[1] - 32, 64, 64);
            g.setColor(new Color(150, 50, 40));
            g.fillOval(e[0] - 14, e[1] - 14, 28, 28);
        }

        // HUD
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.045)));
        g.drawString("Волна 5/10", (int) (w * 0.03), (int) (h * 0.075));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.038)));
        g.drawString("станция: 82   счёт: 1450   энергия: 45", (int) (w * 0.03), (int) (h * 0.115));

        // панель строительства
        String[] labels = {"Пилон (50⚡)", "Турбина (90⚡)", "Реактор (220⚡)"};
        for (int i = 0; i < 3; i++) {
            int x = (int) (w * 0.02 + i * w * 0.33);
            g.setColor(i == 0 ? TOWER : new Color(70, 80, 95));
            g.fill(new Rectangle2D.Double(x, h * 0.88, w * 0.3, h * 0.08));
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.032)));
            g.drawString(labels[i], x + 20, (int) (h * 0.93));
        }

        g.setColor(PLANT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.045)));
        String cap = "оборона электростанции · энергобюджет";
        g.drawString(cap, (w - g.getFontMetrics().stringWidth(cap)) / 2, (int) (h * 0.82));

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawIcon(File out) throws Exception {
        int s = 192;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(FLOOR);
        g.fillRoundRect(0, 0, s, s, 30, 30);
        g.setColor(PLANT);
        g.fill(new Rectangle2D.Double(96, 60, 66, 96));
        g.setColor(new Color(180, 140, 30));
        g.fill(new Rectangle2D.Double(108, 30, 12, 34));
        g.fill(new Rectangle2D.Double(138, 22, 12, 42));
        g.setColor(TOWER);
        g.fill(new Rectangle2D.Double(30, 96, 48, 48));
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

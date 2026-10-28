package siege.gen;

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
 * Офлайн-генерация screenshot.png и иконки «Тёмной осады» (тёмное фэнтези).
 * Запуск: java siege.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color GRASS = new Color(26, 33, 26);
    private static final Color PATH = new Color(71, 56, 41);
    private static final Color TOWER = new Color(51, 36, 71);
    private static final Color FORTRESS = new Color(89, 31, 31);
    private static final Color CAMP = new Color(41, 71, 46);
    private static final Color GOLD = new Color(255, 217, 102);
    private static final Color TEXT = new Color(232, 226, 218);

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "screenshot.png");
        int w = args.length > 1 ? Integer.parseInt(args[1]) : 1280;
        int h = args.length > 2 ? Integer.parseInt(args[2]) : 800;
        drawScreenshot(w, h, out);
        drawIcon(new File("src/icon.png"));
        System.out.println("OK: " + out.getAbsolutePath());
    }

    private static void drawScreenshot(int w, int h, File out) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(GRASS);
        g.fillRect(0, 0, w, h);

        int cell = w / 12;
        int top = (int) (h * 0.18);

        // путь (змейка)
        int[][] path = {{0, 4}, {1, 4}, {2, 4}, {2, 3}, {3, 3}, {4, 3}, {4, 4}, {5, 4}, {6, 4},
                {6, 5}, {7, 5}, {8, 5}, {8, 4}, {9, 4}, {10, 4}, {11, 4}};
        g.setColor(PATH);
        for (int[] p : path) {
            g.fill(new Rectangle2D.Double(p[0] * cell, top + p[1] * cell * 0.7, cell, cell * 0.7));
        }

        // башни
        int[][] towers = {{2, 2}, {4, 2}, {5, 5}, {7, 4}, {9, 5}};
        g.setColor(TOWER);
        for (int[] t : towers) {
            g.fill(new Rectangle2D.Double(t[0] * cell, top + t[1] * cell * 0.7, cell, cell * 0.7));
            g.setColor(new Color(128, 51, 153));
            g.fillOval(t[0] * cell + cell / 3, top + (int) (t[1] * cell * 0.7), cell / 3, cell / 3);
            g.setColor(TOWER);
        }

        // лагерь и крепость
        g.setColor(CAMP);
        g.fillRect(0, top, cell, (int) (h * 0.7));
        g.setColor(FORTRESS);
        g.fillRect(11 * cell, top, cell, (int) (h * 0.7));
        g.setColor(new Color(255, 90, 60));
        g.fillRect(11 * cell + cell / 4, top + 20, cell / 2, cell / 2);

        // колонна войск
        for (int i = 0; i < 10; i++) {
            int px = (int) (cell * 1.2 + i * cell * 0.85);
            int pi = Math.min(i, path.length - 1);
            int py = top + (int) (path[Math.min(pi + 2, path.length - 1)][1] * cell * 0.7) + cell / 4;
            g.setColor(new Color(217, 166, 89));
            g.fillOval(px, py, cell / 3, cell / 3);
        }

        // HUD
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.032)));
        g.drawString("Уровень 7   волна 3/6", (int) (w * 0.02), (int) (h * 0.07));
        g.setColor(GOLD);
        g.drawString("казна: 340 золота", (int) (w * 0.55), (int) (h * 0.07));
        // полоса прочности
        g.setColor(new Color(77, 26, 26));
        g.fillRect((int) (w * 0.02), (int) (h * 0.105), (int) (w * 0.45), 24);
        g.setColor(new Color(217, 64, 51));
        g.fillRect((int) (w * 0.02), (int) (h * 0.105), (int) (w * 0.45 * 0.62), 24);

        // панель найма
        String[] names = {"Воин x12", "Катапульта x4", "Виверна x2", "В АТАКУ!"};
        for (int i = 0; i < 4; i++) {
            int x = (int) (w * 0.02 + i * w * 0.25);
            g.setColor(i == 3 ? new Color(153, 51, 38) : new Color(41, 31, 51));
            g.fill(new Rectangle2D.Double(x, h * 0.84, w * 0.23, h * 0.12));
            g.setColor(TEXT);
            g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.022)));
            g.drawString(names[i], x + 12, (int) (h * 0.89));
            if (i < 3) {
                g.setColor(new Color(200, 190, 180));
                g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.017)));
                g.drawString("50 зол. · ур. " + i, x + 12, (int) (h * 0.935));
            }
        }

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawIcon(File out) throws Exception {
        int s = 192;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(26, 33, 26));
        g.fillRoundRect(0, 0, s, s, 30, 30);
        g.setColor(FORTRESS);
        g.fillRect(56, 46, 80, 110);
        g.setColor(new Color(255, 90, 60));
        g.fillRect(78, 70, 36, 36);
        g.setColor(TOWER);
        g.fillRect(38, 76, 22, 80);
        g.fillRect(132, 76, 22, 80);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

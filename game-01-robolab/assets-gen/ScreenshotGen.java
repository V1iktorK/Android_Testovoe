package robolab.gen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Офлайн-генерация screenshot.png и иконки «Робо-Лаборатории» (пиксель-арт, лаборатория роботов).
 * Запуск: java robolab.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color BG_TOP = new Color(18, 23, 41);
    private static final Color BG_BOTTOM = new Color(32, 42, 71);
    private static final Color WALL = new Color(82, 97, 133);
    private static final Color WALL_DARK = new Color(31, 38, 64);
    private static final Color RIVET = new Color(230, 230, 242);
    private static final Color ROBOT = new Color(191, 209, 235);
    private static final Color ROBOT_DARK = new Color(102, 115, 140);
    private static final Color EYE = new Color(38, 217, 229);
    private static final Color BATTERY = new Color(242, 209, 64);
    private static final Color SPIKE = new Color(217, 76, 89);
    private static final Color DOOR = new Color(64, 178, 242);
    private static final Color DOOR_IN = new Color(140, 255, 217);
    private static final Color TEXT = new Color(232, 238, 255);
    private static final Color ACCENT = new Color(255, 214, 92);

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
        g.setPaint(new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM));
        g.fillRect(0, 0, w, h);

        // Заголовок
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.085)));
        drawCentered(g, "РОБО-ЛАБОРАТОРИЯ", w / 2, (int) (h * 0.09));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.038)));
        drawCentered(g, "уровень 7 · батарейки 4/8 · жизни 3", w / 2, (int) (h * 0.125));

        // Игровое поле: пиксельные тайлы (масштаб 1/10 от ширины)
        int tile = w / 10;   // карта 20x10 -> рисуем масштабом tile/2
        int half = tile / 2;
        int left = w / 2 - 10 * half;
        int top = (int) (h * 0.18);

        String[] map = {
            "####################",
            "#..................#",
            "#......#####.......#",
            "#..b..........b....#",
            "#.......L..........#",
            "#.^^...........^^..#",
            "#........P.........#",
            "#S.........E.......#",
            "#...#####...#####..#",
            "####################",
        };
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length(); x++) {
                int px = left + x * half;
                int py = top + y * half;
                char c = map[y].charAt(x);
                switch (c) {
                    case '#':
                        drawTile(g, px, py, half);
                        break;
                    case '^':
                        g.setColor(SPIKE);
                        g.fillPolygon(new int[]{px, px + half / 2, px + half},
                                new int[]{py + half, py + half / 3, py + half}, 3);
                        break;
                    case 'L':
                        g.setColor(new Color(255, 90, 100));
                        g.fillRect(px + half / 3, py, half / 3, half);
                        break;
                    case 'P':
                        g.setColor(new Color(140, 71, 82));
                        g.fillRect(px + 2, py + 2, half - 4, half - 4);
                        g.setColor(SPIKE);
                        g.fillRect(px + half / 3, py + half / 2, half / 4, half / 5);
                        break;
                    case 'b':
                        g.setColor(BATTERY);
                        g.fillRect(px + half / 4, py + half / 6, half / 2, half * 2 / 3);
                        g.setColor(new Color(89, 230, 115));
                        g.fillRect(px + half / 3, py + half / 3, half / 3, half / 3);
                        break;
                    case 'E':
                        g.setColor(DOOR);
                        g.fillRect(px + 2, py - half / 2, half - 4, half * 3 / 2);
                        g.setColor(DOOR_IN);
                        g.fillRect(px + half / 3, py, half / 3, half);
                        break;
                    case 'S':
                        drawRobot(g, px, py, half);
                        break;
                }
            }
        }

        // Нижняя подпись
        g.setColor(ACCENT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.042)));
        drawCentered(g, "двойной прыжок · сбор батареек · 20 уровней", w / 2, (int) (h * 0.53));
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.034)));
        drawCentered(g, "пиксель-арт лаборатории роботов, процедурная графика", w / 2, (int) (h * 0.57));

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawTile(Graphics2D g, int x, int y, int s) {
        g.setColor(WALL);
        g.fillRect(x, y, s, s);
        g.setColor(WALL_DARK);
        g.fillRect(x, y + s - 3, s, 3);
        g.fillRect(x + s - 3, y, 3, s);
        g.setColor(RIVET);
        g.fillRect(x + s / 5, y + s / 5, 3, 3);
        g.fillRect(x + s * 3 / 5, y + s * 3 / 5, 3, 3);
    }

    private static void drawRobot(Graphics2D g, int x, int y, int s) {
        g.setColor(ROBOT_DARK);
        g.fillRect(x + s / 5, y + s * 3 / 4, s / 5, s / 4);
        g.fillRect(x + s * 3 / 5, y + s * 3 / 4, s / 5, s / 4);
        g.setColor(ROBOT);
        g.fillRect(x + s / 5, y + s / 3, s * 3 / 5, s / 2);
        g.fillRect(x + s / 4, y + s / 10, s / 2, s / 4);
        g.setColor(EYE);
        g.fillRect(x + s / 3, y + s / 6, s / 6, s / 8);
        g.fillRect(x + s / 2, y + s / 6, s / 6, s / 8);
    }

    private static void drawIcon(File out) throws Exception {
        int s = 192;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, BG_TOP, 0, s, BG_BOTTOM));
        g.fillRoundRect(0, 0, s, s, 30, 30);
        drawRobot(g, 30, 40, 132);
        g.setColor(BATTERY);
        g.fillRect(142, 20, 22, 34);
        g.setColor(new Color(89, 230, 115));
        g.fillRect(147, 28, 12, 14);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }

    private static void drawCentered(Graphics2D g, String s, int cx, int y) {
        g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, y);
    }
}

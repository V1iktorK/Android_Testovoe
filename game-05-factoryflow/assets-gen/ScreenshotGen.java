package factoryflow.gen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Офлайн-генерация screenshot.png и иконки «Фабрики-Флоу» (low-poly промышленный цех).
 * Запуск: java factoryflow.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color SKY = new Color(33, 38, 48);
    private static final Color FLOOR = new Color(56, 61, 71);
    private static final Color FLOOR_GRID = new Color(72, 79, 92);
    private static final Color BELT = new Color(41, 44, 51);
    private static final Color STATION = new Color(74, 115, 158);
    private static final Color STATION_TOP = new Color(92, 217, 140);
    private static final Color LAMP = new Color(255, 214, 92);
    private static final Color TEXT = new Color(236, 240, 250);
    private static final Color ACCENT = new Color(122, 199, 255);
    private static final Color PANEL = new Color(24, 28, 36, 210);

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
        g.setPaint(new GradientPaint(0, 0, SKY, 0, h * 0.5f, FLOOR));
        g.fillRect(0, 0, w, h);

        // Пол с перспективной сеткой
        g.setColor(FLOOR);
        g.fillRect(0, h * 2 / 5, w, h * 3 / 5);
        g.setColor(FLOOR_GRID);
        g.setStroke(new BasicStroke(2f));
        for (int i = -6; i <= 6; i++) {
            g.drawLine(w / 2, h * 2 / 5, w / 2 + i * w / 4, h);
        }
        for (int i = 1; i < 8; i++) {
            int yy = h * 2 / 5 + i * i * 12;
            if (yy > h) break;
            g.drawLine(0, yy, w, yy);
        }

        // Конвейер (низкий параллелепипед)
        int beltY = h * 5 / 9;
        g.setColor(BELT);
        g.fillRect(w / 10, beltY, w * 8 / 10, h / 14);
        g.setColor(new Color(58, 63, 74));
        g.fillRect(w / 10, beltY, w * 8 / 10, h / 60);

        // Станции: изометрические коробки
        for (int i = 0; i < 4; i++) {
            int cx = w / 8 + i * w * 3 / 16;
            drawIsoBox(g, cx, beltY - h / 30, w / 12, h / 8, STATION, STATION_TOP);
            g.setColor(LAMP);
            g.fillOval(cx + w / 40, beltY - h / 30 - h / 8 - w / 30, w / 40, w / 40);
        }

        // Верхняя панель: заголовок
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.085)));
        drawCentered(g, "ФАБРИКА-ФЛОУ", w / 2, (int) (h * 0.09));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.036)));
        drawCentered(g, "VR-тренажёр оператора промышленного цеха", w / 2, (int) (h * 0.125));

        // Панель заказов (как HUD)
        int panelX = w / 12, panelY = (int) (h * 0.68), panelW = w * 10 / 12, panelH = (int) (h * 0.17);
        g.setColor(PANEL);
        g.fillRoundRect(panelX, panelY, panelW, panelH, 24, 24);
        g.setColor(ACCENT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.038)));
        g.drawString("Смена 3.2 мин   энергия: 42   выпущено: 118   счёт: 2450", panelX + 40, panelY + 60);
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.028)));
        g.drawString("Шестерня   12 ед/мин   срок 14 мин", panelX + 40, panelY + 110);
        g.drawString("Плата   9 ед/мин   срок 11 мин", panelX + 40, panelY + 160);
        // прогресс-бары
        g.setColor(new Color(70, 78, 92));
        g.fillRoundRect(panelX + panelW - 320, panelY + 88, 280, 26, 12, 12);
        g.fillRoundRect(panelX + panelW - 320, panelY + 138, 280, 26, 12, 12);
        g.setColor(new Color(92, 217, 140));
        g.fillRoundRect(panelX + panelW - 320, panelY + 88, 196, 26, 12, 12);
        g.fillRoundRect(panelX + panelW - 320, panelY + 138, 120, 26, 12, 12);

        g.setColor(new Color(255, 214, 92));
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.04)));
        drawCentered(g, "тап по станции — ускорить выпуск", w / 2, (int) (h * 0.90));

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawIsoBox(Graphics2D g, int x, int y, int bw, int bh, Color side, Color top) {
        int d = bw / 3;
        g.setColor(side);
        g.fillRect(x, y, bw, bh);
        Polygon topFace = new Polygon();
        topFace.addPoint(x, y);
        topFace.addPoint(x + d, y - d);
        topFace.addPoint(x + bw + d, y - d);
        topFace.addPoint(x + bw, y);
        g.setColor(top);
        g.fillPolygon(topFace);
        Polygon right = new Polygon();
        right.addPoint(x + bw, y);
        right.addPoint(x + bw + d, y - d);
        right.addPoint(x + bw + d, y - d + bh);
        right.addPoint(x + bw, y + bh);
        g.setColor(side.darker());
        g.fillPolygon(right);
    }

    private static void drawIcon(File out) throws Exception {
        int s = 192;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, SKY, 0, s, FLOOR));
        g.fillRoundRect(0, 0, s, s, 30, 30);
        drawIsoBox(g, 34, 96, 60, 52, STATION, STATION_TOP);
        drawIsoBox(g, 104, 96, 60, 52, STATION, STATION_TOP);
        g.setColor(LAMP);
        g.fillOval(52, 30, 22, 22);
        g.fillOval(122, 30, 22, 22);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }

    private static void drawCentered(Graphics2D g, String s, int cx, int y) {
        g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, y);
    }
}

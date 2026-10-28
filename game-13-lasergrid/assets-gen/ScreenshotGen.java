package lasergrid.gen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Офлайн-генерация screenshot.png и иконки «Луч-88» (векторная графика 80-х).
 * Запуск: java lasergrid.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color BG = new Color(6, 10, 8);
    private static final Color BEAM = new Color(60, 255, 120);
    private static final Color BEAM_DIM = new Color(24, 90, 48);
    private static final Color MIRROR = new Color(80, 220, 255);
    private static final Color TARGET = new Color(255, 90, 90);
    private static final Color TEXT = new Color(140, 255, 170);

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

        int cols = 13, rows = 9;
        int cell = (int) (w * 0.92 / cols);
        int left = (w - cols * cell) / 2;
        int top = (int) (h * 0.22);

        // сетка
        g.setColor(new Color(16, 34, 22));
        g.setStroke(new BasicStroke(2f));
        for (int x = 0; x <= cols; x++) g.drawLine(left + x * cell, top, left + x * cell, top + rows * cell);
        for (int y = 0; y <= rows; y++) g.drawLine(left, top + y * cell, left + cols * cell, top + y * cell);

        // путь луча (яркая змейка) и зеркала
        int[][] beamCells = {{1, 4}, {2, 4}, {3, 4}, {3, 3}, {4, 3}, {5, 3}, {5, 4}, {6, 4}, {7, 4},
                {7, 5}, {8, 5}, {9, 5}, {9, 4}, {10, 4}, {11, 4}};
        g.setColor(BEAM_DIM);
        for (int[] c : beamCells) {
            g.fillRect(left + c[0] * cell + 5, top + c[1] * cell + 5, cell - 10, cell - 10);
        }
        int[][] mirrors = {{3, 4}, {5, 3}, {7, 4}, {9, 5}};
        g.setColor(MIRROR);
        g.setStroke(new BasicStroke(cell * 0.13f, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND));
        for (int i = 0; i < mirrors.length; i++) {
            int cx = left + mirrors[i][0] * cell + cell / 2;
            int cy = top + mirrors[i][1] * cell + cell / 2;
            int d = (int) (cell * 0.33);
            if (i % 2 == 0) g.drawLine(cx - d, cy + d, cx + d, cy - d);
            else g.drawLine(cx - d, cy - d, cx + d, cy + d);
        }
        // цели
        g.setStroke(new BasicStroke(cell * 0.11f));
        int[][] targets = {{5, 3}, {9, 5}, {11, 4}};
        for (int i = 0; i < targets.length; i++) {
            int cx = left + targets[i][0] * cell + cell / 2;
            int cy = top + targets[i][1] * cell + cell / 2;
            g.setColor(i == 2 ? TARGET : BEAM);
            g.drawOval(cx - cell / 3, cy - cell / 3, cell * 2 / 3, cell * 2 / 3);
        }
        // источник
        g.setColor(BEAM);
        g.fillOval(left + cell + cell / 3, top + 4 * cell + cell / 3, cell / 3, cell / 3);

        // HUD
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.05)));
        g.drawString("Уровень 12", (int) (w * 0.04), (int) (w * 0.10));
        String moves = "ходы: 6  par: 4";
        g.drawString(moves, w - g.getFontMetrics().stringWidth(moves) - (int) (w * 0.04), (int) (w * 0.10));

        // заголовок-подпись
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.075)));
        String title = "ЛУЧ-88";
        g.drawString(title, (w - g.getFontMetrics().stringWidth(title)) / 2, (int) (h * 0.12));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.038)));
        String cap = "векторная головоломка: направь луч на все цели";
        g.drawString(cap, (w - g.getFontMetrics().stringWidth(cap)) / 2, (int) (h * 0.78));

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
        g.setColor(BEAM);
        g.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND));
        g.drawLine(24, 96, 96, 96);
        g.setColor(MIRROR);
        g.drawLine(96, 60, 132, 132);
        g.setColor(BEAM);
        g.drawLine(132, 132, 132, 160);
        g.setColor(TARGET);
        g.setStroke(new BasicStroke(10f));
        g.drawOval(112, 152, 40, 40);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

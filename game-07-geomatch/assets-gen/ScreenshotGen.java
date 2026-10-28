package geomatch.gen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Офлайн-генерация screenshot.png и иконки «Гео-Матча» (абстракция, геометрия, Баухаус).
 * Запуск: java geomatch.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color BG = new Color(244, 241, 232);
    private static final Color PANEL = new Color(44, 48, 60);
    private static final Color TEXT = new Color(240, 238, 230);
    private static final Color[] TILE = {
            new Color(231, 76, 60), new Color(46, 134, 193), new Color(241, 196, 15),
            new Color(39, 174, 96), new Color(142, 68, 173), new Color(230, 126, 34)
    };

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

        // Верхняя панель
        g.setColor(PANEL);
        g.fillRect(0, 0, w, (int) (h * 0.28));
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.05)));
        g.drawString("Уровень 12", (int) (w * 0.04), (int) (w * 0.09));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.038)));
        g.drawString("Счёт: 4230 / 5500", (int) (w * 0.04), (int) (w * 0.145));
        g.drawString("Цель: 18 фишек цвета 3 (собрано 11)", (int) (w * 0.04), (int) (w * 0.19));
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.05)));
        String moves = "Ходы: 9";
        g.drawString(moves, w - g.getFontMetrics().stringWidth(moves) - (int) (w * 0.04), (int) (w * 0.09));

        // Доска 8x8
        int boardSize = (int) (w * 0.94);
        int cell = boardSize / 8;
        int left = (w - boardSize) / 2;
        int top = (int) (h * 0.32);
        g.setColor(new Color(226, 219, 202));
        g.fillRoundRect(left - 12, top - 12, boardSize + 24, boardSize + 24, 30, 30);

        int[][] colors = new int[8][8];
        java.util.Random rng = new java.util.Random(42);
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++)
                colors[y][x] = rng.nextInt(6);

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int cx = left + x * cell + cell / 2;
                int cy = top + y * cell + cell / 2;
                drawShape(g, cx, cy, (int) (cell * 0.40), colors[y][x], x == 3 && y == 4);
            }
        }

        g.setColor(new Color(231, 76, 60));
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.045)));
        String cap = "match-3 из абстрактных форм · 40 уровней";
        g.drawString(cap, (w - g.getFontMetrics().stringWidth(cap)) / 2, (int) (h * 0.885));

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static void drawShape(Graphics2D g, int cx, int cy, int r, int color, boolean selected) {
        g.setColor(TILE[color % TILE.length]);
        int sides = 3 + (color % 4);
        Polygon p = new Polygon();
        for (int i = 0; i < sides; i++) {
            double angle = Math.PI * 2 * i / sides - Math.PI / 2 + (selected ? 0.2 : 0);
            p.addPoint((int) (cx + Math.cos(angle) * r), (int) (cy + Math.sin(angle) * r));
        }
        g.fillPolygon(p);
        if (selected) {
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(9f));
            g.drawPolygon(p);
        }
    }

    private static void drawIcon(File out) throws Exception {
        int s = 192;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(BG);
        g.fillRoundRect(0, 0, s, s, 30, 30);
        drawShape(g, 56, 56, 38, 0, false);
        drawShape(g, 136, 56, 38, 2, false);
        drawShape(g, 56, 136, 38, 3, false);
        drawShape(g, 136, 136, 38, 4, false);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

package drone.gen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Офлайн-генерация screenshot.png и иконки «Дрон-Патруля» (киберпанк, автозавод).
 * Запуск: java drone.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color BG = new Color(10, 12, 22);
    private static final Color GRID = new Color(24, 30, 48);
    private static final Color CYAN = new Color(34, 225, 255);
    private static final Color PINK = new Color(255, 42, 140);
    private static final Color TEXT = new Color(220, 232, 255);

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
        g.setColor(GRID);
        g.setStroke(new BasicStroke(2f));
        for (int x = 0; x <= w; x += 120) g.drawLine(x, 0, x, h);
        for (int y = 0; y <= h; y += 120) g.drawLine(0, y, w, y);

        // «цех»: неоновые контуры оборудования
        g.setColor(new Color(34, 225, 255, 60));
        g.setStroke(new BasicStroke(6f));
        g.draw(new Ellipse2D.Double(w * 0.06, h * 0.62, w * 0.3, w * 0.3));
        g.draw(new Ellipse2D.Double(w * 0.62, h * 0.7, w * 0.32, w * 0.32));
        g.setColor(new Color(255, 42, 140, 55));
        g.drawRect((int) (w * 0.12), (int) (h * 0.38), (int) (w * 0.2), (int) (h * 0.12));
        g.drawRect((int) (w * 0.66), (int) (h * 0.3), (int) (w) * 22 / 100, (int) (h * 0.14));

        // враги
        int[][] enemies = {{300, 900}, {760, 1080}, {540, 780}, {860, 700}, {240, 1250}};
        Color[] ec = {new Color(255, 84, 140), new Color(255, 170, 60), new Color(170, 80, 230),
                new Color(120, 200, 255), new Color(255, 84, 140)};
        for (int i = 0; i < enemies.length; i++) {
            g.setColor(ec[i]);
            g.fill(new Ellipse2D.Double(enemies[i][0] - 66, enemies[i][1] - 66, 132, 132));
            g.setColor(new Color(255, 255, 255, 90));
            g.fill(new Ellipse2D.Double(enemies[i][0] - 36, enemies[i][1] - 36, 72, 72));
        }

        // пули
        g.setColor(CYAN);
        for (int i = 0; i < 6; i++) {
            g.fill(new Ellipse2D.Double(520 + i * 46, 1280 - i * 92, 24, 24));
        }

        // дрон
        int dx = 540, dy = 1450;
        Polygon tri = new Polygon();
        tri.addPoint(dx, dy - 70);
        tri.addPoint(dx - 58, dy + 52);
        tri.addPoint(dx + 58, dy + 52);
        g.setColor(CYAN);
        g.fillPolygon(tri);
        g.setColor(PINK);
        g.fillOval(dx - 22, dy - 22, 44, 44);

        // уголки телеметрии
        g.setColor(CYAN);
        g.setStroke(new BasicStroke(8f));
        g.drawLine(30, 30, 120, 30); g.drawLine(30, 30, 30, 120);
        g.drawLine(w - 30, 30, w - 120, 30); g.drawLine(w - 30, 30, w - 30, 120);
        g.drawLine(30, h - 30, 120, h - 30); g.drawLine(30, h - 30, 30, h - 120);
        g.drawLine(w - 30, h - 30, w - 120, h - 30); g.drawLine(w - 30, h - 30, w - 30, h - 120);
        // HUD
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.048)));
        g.drawString("Волна 7", (int) (w * 0.04), (int) (w * 0.10));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.038)));
        g.setColor(new Color(120, 255, 170));
        g.drawString("корпус: 82", (int) (w * 0.04), (int) (w * 0.15));
        g.setColor(new Color(120, 200, 255));
        g.drawString("щит: 50", (int) (w * 0.04), (int) (w * 0.195));
        g.setColor(TEXT);
        String score = "очки: 4820";
        g.drawString(score, w - g.getFontMetrics().stringWidth(score) - (int) (w * 0.04), (int) (w * 0.10));

        // баннер волны
        g.setColor(PINK);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.09)));
        String banner = "ВОЛНА 7";
        g.drawString(banner, (w - g.getFontMetrics().stringWidth(banner)) / 2, (int) (h * 0.28));

        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.036)));
        g.setColor(new Color(150, 170, 200));
        String cap = "дрон следует за пальцем · адаптивные волны · апгрейды между волнами";
        g.drawString(cap, (w - g.getFontMetrics().stringWidth(cap)) / 2, (int) (h * 0.94));

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
        Polygon tri = new Polygon();
        tri.addPoint(96, 34);
        tri.addPoint(38, 148);
        tri.addPoint(154, 148);
        g.setColor(CYAN);
        g.fillPolygon(tri);
        g.setColor(PINK);
        g.fillOval(74, 76, 44, 44);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

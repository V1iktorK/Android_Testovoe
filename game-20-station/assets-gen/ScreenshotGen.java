package station.gen;

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
 * Офлайн-генерация screenshot.png и иконки «Станции-Падение» (космо-индастри).
 * Запуск: java station.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
public final class ScreenshotGen {

    private static final Color BG = new Color(4, 8, 12);
    private static final Color ROOM = new Color(13, 31, 41);
    private static final Color[] KIND_COLORS = {
            new Color(64, 255, 255), new Color(242, 204, 76), new Color(64, 255, 255),
            new Color(140, 217, 140), new Color(191, 64, 217), new Color(153, 166, 191)
    };
    private static final Color TEXT = new Color(64, 255, 255);

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
        g.setColor(BG);
        g.fillRect(0, 0, w, h);

        // «звёзды» за окном станции
        for (int i = 0; i < 60; i++) {
            g.setColor(new Color(255, 255, 255, 60 + (i * 37) % 180));
            g.fillOval((i * 173) % w, (i * 97) % h, 3, 3);
        }

        // HUD
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) (w * 0.028)));
        String hud = "Глубина 2   корпус: 74   оружие: 28   очки: 1840";
        g.drawString(hud, (w - g.getFontMetrics().stringWidth(hud)) / 2, (int) (h * 0.08));

        // Сетка комнат 4x3
        String[] kinds = {"entrance", "combat", "treasure", "shop", "combat", "lab", "combat", "treasure", "boss", "combat", "shop", "lab"};
        int[] powers = {0, 32, 28, 20, 41, 25, 38, 30, 95, 44, 22, 26};
        for (int i = 0; i < 12; i++) {
            int col = i % 4;
            int row = i / 4;
            int x = (int) (col * w / 4.0 + 20);
            int y = (int) (h * 0.18 + row * h * 0.22);
            int rw = (int) (w / 4.0 - 40);
            int rh = (int) (h * 0.18);
            Color kindColor = KIND_COLORS[i % KIND_COLORS.length];
            g.setColor(ROOM);
            g.fill(new Rectangle2D.Double(x, y, rw, rh));
            g.setColor(kindColor);
            g.fill(new Rectangle2D.Double(x, y, rw, 10));
            g.setColor(new Color(0, 0, 0, 90));
            g.fill(new Rectangle2D.Double(x, y + 10, rw, rh - 10));
            g.setColor(TEXT);
            g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.018)));
            g.drawString(kinds[i], x + 18, y + (int) (rh * 0.42));
            g.drawString("угроза " + powers[i], x + 18, y + (int) (rh * 0.72));
        }

        // лог
        g.setColor(new Color(255, 217, 128));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) (w * 0.022)));
        String log = "Бой: нанёс 86, получил 12 урона";
        g.drawString(log, (w - g.getFontMetrics().stringWidth(log)) / 2, (int) (h * 0.93));

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
        g.setColor(new Color(51, 60, 89));
        g.fill(new Rectangle2D.Double(36, 46, 120, 100));
        g.setColor(new Color(102, 191, 229));
        g.fillRect(52, 66, 34, 26);
        g.fillRect(106, 66, 34, 26);
        g.setColor(new Color(217, 76, 76));
        g.fillOval(82, 108, 28, 28);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }
}

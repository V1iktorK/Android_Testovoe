package pathlink.gen

import com.pathlink.game.BoardGenerator
import com.pathlink.game.Tile
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.geom.Line2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Офлайн-генерация screenshot.png и иконки игры (headless AWT).
 * Запуск: java pathlink.gen.ScreenshotGen <screenshot.png> <ширина> <высота>
 */
object ScreenshotGen {

    private val bg = Color(247, 241, 232)
    private val tileFill = Color(255, 248, 237)
    private val pipe = Color(127, 181, 164)
    private val accent = Color(232, 168, 124)
    private val text = Color(74, 68, 81)
    private val star = Color(240, 190, 90)

    @JvmStatic
    fun main(args: Array<String>) {
        val out = File(args.getOrElse(0) { "screenshot.png" })
        val w = args.getOrElse(1) { "1080" }.toInt()
        val h = args.getOrElse(2) { "1920" }.toInt()
        drawScreenshot(w, h, out)
        drawIcon(File("src/res/drawable/ic_launcher.png"))
        println("OK: ${out.absolutePath}")
    }

    private fun drawScreenshot(w: Int, h: Int, out: File) {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = bg
        g.fillRect(0, 0, w, h)

        // Заголовок
        g.color = text
        g.font = Font("SansSerif", Font.BOLD, (w * 0.105f).toInt())
        drawCentered(g, "Соедини пути", w / 2, (h * 0.14f).toInt())
        g.font = Font("SansSerif", Font.PLAIN, (w * 0.042f).toInt())
        drawCentered(g, "Уровень 21 · ходы: 14 · par: 12", w / 2, (h * 0.185f).toInt())

        // Доска 5x5 из генератора
        val board = BoardGenerator.generate(5, 7919L * 21 + 104729L)
        val boardW = (w * 0.86f).toInt()
        val cell = boardW / 5
        val left = (w - boardW) / 2
        val top = (h * 0.26f).toInt()

        g.color = tileFill
        g.fillRoundRect(left - cell / 8, top - cell / 8, boardW + cell / 4, boardW + cell / 4, cell / 5, cell / 5)

        for (y in 0 until 5) {
            for (x in 0 until 5) {
                drawTile(g, left + x * cell, top + y * cell, cell, board.tileAt(x, y))
            }
        }

        // Звёзды внизу
        for (i in 0 until 3) {
            g.color = if (i < 2) star else Color(214, 205, 191)
            g.fillOval(w / 2 + (i - 1) * (w * 0.13f).toInt() - (w * 0.042f).toInt(),
                (h * 0.79f).toInt(), (w * 0.084f).toInt(), (w * 0.084f).toInt())
        }
        g.color = text
        g.font = Font("SansSerif", Font.PLAIN, (w * 0.045f).toInt())
        drawCentered(g, "потяните плитки — и сеть замкнётся", w / 2, (h * 0.87f).toInt())

        g.dispose()
        ImageIO.write(img, "png", out)
    }

    private fun drawTile(g: java.awt.Graphics2D, x: Int, y: Int, size: Int, tile: Tile) {
        val cx = x + size / 2.0
        val cy = y + size / 2.0
        g.color = pipe
        g.stroke = BasicStroke(size * 0.17f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        if (tile.north) g.draw(Line2D.Double(cx, cy, cx, y + size * 0.1))
        if (tile.south) g.draw(Line2D.Double(cx, cy, cx, y + size * 0.9))
        if (tile.west) g.draw(Line2D.Double(cx, cy, x + size * 0.1, cy))
        if (tile.east) g.draw(Line2D.Double(cx, cy, x + size * 0.9, cy))
        val r = (size * 0.13f).toInt()
        g.fillOval(cx.toInt() - r, cy.toInt() - r, r * 2, r * 2)
    }

    private fun drawIcon(out: File) {
        val s = 192
        val img = BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = bg
        g.fillRoundRect(0, 0, s, s, 36, 36)
        g.color = pipe
        g.stroke = BasicStroke(22f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(Line2D.Double(48.0, 96.0, 96.0, 96.0))
        g.draw(Line2D.Double(96.0, 96.0, 96.0, 48.0))
        g.color = accent
        g.draw(Line2D.Double(96.0, 96.0, 144.0, 96.0))
        g.draw(Line2D.Double(96.0, 96.0, 96.0, 144.0))
        g.dispose()
        out.parentFile?.mkdirs()
        ImageIO.write(img, "png", out)
    }

    private fun drawCentered(g: java.awt.Graphics2D, s: String, cx: Int, y: Int) {
        val fm = g.fontMetrics
        g.drawString(s, cx - fm.stringWidth(s) / 2, y)
    }
}

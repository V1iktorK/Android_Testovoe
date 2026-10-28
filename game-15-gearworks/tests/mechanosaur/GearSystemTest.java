package mechanosaur;

import org.junit.Test;
import static org.junit.Assert.*;
import com.mechanosaur.game.GearSystem;
import com.mechanosaur.game.Gear;

/**
 * Тесты механики зубчатых передач: сцепление, направление, передаточные числа.
 */
public class GearSystemTest {

    @Test
    public void adjacent_gears_mesh() {
        GearSystem sys = new GearSystem();
        sys.place(new Gear(2, 2, 1));   // радиус 1
        sys.place(new Gear(4, 2, 1));   // центр в 2 клетках — сцепляется
        assertTrue("Соседние шестерни сцеплены", sys.areMeshed(0, 1));
    }

    @Test
    public void distant_gears_do_not_mesh() {
        GearSystem sys = new GearSystem();
        sys.place(new Gear(2, 2, 1));
        sys.place(new Gear(8, 8, 1));
        assertFalse("Далёкие шестерни не сцеплены", sys.areMeshed(0, 1));
    }

    @Test
    public void rotation_direction_alternates() {
        GearSystem sys = new GearSystem();
        sys.place(new Gear(2, 2, 1));
        sys.place(new Gear(4, 2, 1));
        sys.place(new Gear(6, 2, 1));
        assertEquals("Исходная шестерня крутится вправо", 1, sys.rotationOf(0));
        assertEquals("Сцепленная — влево", -1, sys.rotationOf(1));
        assertEquals("Третья — снова вправо", 1, sys.rotationOf(2));
    }

    @Test
    public void gear_ratio_multiplies_along_chain() {
        GearSystem sys = new GearSystem();
        sys.place(new Gear(2, 2, 2));   // большая ведущая
        sys.place(new Gear(5, 2, 1));   // маленькая ведомая
        double ratio = sys.ratioFrom(0, 1);
        assertEquals("Передаточное число r1/r2", 2.0, ratio, 1e-9);
    }

    @Test
    public void chain_connectivity() {
        GearSystem sys = new GearSystem();
        sys.place(new Gear(2, 2, 1));
        sys.place(new Gear(4, 2, 1));
        sys.place(new Gear(6, 2, 1));
        assertTrue("Цепь от 0 до 2 замкнута", sys.connected(0, 2));
        assertFalse("Цепь до изолированной шестерни разомкнута",
                sys.connected(0, 3));
    }

    @Test
    public void placing_on_top_is_rejected() {
        GearSystem sys = new GearSystem();
        assertTrue(sys.place(new Gear(3, 3, 1)));
        assertFalse("Занятая клетка отклоняется", sys.place(new Gear(3, 3, 1)));
    }

    @Test
    public void output_rpm_with_ratio() {
        GearSystem sys = new GearSystem();
        sys.place(new Gear(2, 2, 2));
        sys.place(new Gear(5, 2, 1));
        double rpm = sys.outputRpm(0, 1, 30.0);
        assertEquals("Выход крутится быстрее в 2 раза", 60.0, rpm, 1e-9);
    }
}

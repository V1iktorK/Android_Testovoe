package com.smoke.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class LogicTest {
    @Test
    public void add_works() {
        assertEquals(5, Logic.add(2, 3));
        assertEquals(0, Logic.add(-1, 1));
    }
}

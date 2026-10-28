package com.smoke.kt

import org.junit.Test
import org.junit.Assert.*

class LogicTest {
    @Test
    fun add_works() {
        assertEquals(5, Logic.add(2, 3))
        assertEquals(0, Logic.add(-1, 1))
    }
}

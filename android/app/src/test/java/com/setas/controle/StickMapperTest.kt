package com.setas.controle

import com.setas.controle.Arrows.DOWN
import com.setas.controle.Arrows.LEFT
import com.setas.controle.Arrows.RIGHT
import com.setas.controle.Arrows.UP
import org.junit.Assert.assertEquals
import org.junit.Test

class StickMapperTest {
    @Test fun stick_insideDeadZone_isZero() = assertEquals(0, StickMapper.map(5f, 5f, 200f))

    @Test fun stick_downRight_isBoth() = assertEquals(DOWN or RIGHT, StickMapper.map(40f, 40f, 200f))

    @Test fun stick_straightUp_isUp() = assertEquals(UP, StickMapper.map(0f, -50f, 200f))

    @Test fun stick_straightLeft_isLeft() = assertEquals(LEFT, StickMapper.map(-50f, 0f, 200f))

    @Test fun stick_upLeft_isBoth() = assertEquals(UP or LEFT, StickMapper.map(-40f, -40f, 200f))
}

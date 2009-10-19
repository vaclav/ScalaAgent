package org.scalaagent

import org.junit.Assert._
import org.junit.Test
import org.junit.Before
import org.scalatest.junit.{ShouldMatchersForJUnit, JUnitSuite}

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Oct 19, 2009
 * Time: 1:44:59 PM
 * To change this template use File | Settings | File Templates.
 */

class AgentSuite extends JUnitSuite with ShouldMatchersForJUnit {
    @Test def testAgent() {
        assertEquals(10, 10)
    }
}
package kdocformatter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UtilitiesTest {
    @Test
    fun testFindSamePosition() {
        fun addCaret(s: String, caret: Int): String {
            return s.substring(0, caret) + "|" + s.substring(caret)
        }

        fun check(newWithCaret: String, oldWithCaret: String) {
            val oldCaretIndex = oldWithCaret.indexOf('|')
            val newCaretIndex = newWithCaret.indexOf('|')
            assertTrue(oldCaretIndex != -1)
            assertTrue(newCaretIndex != -1)
            val old =
                oldWithCaret.substring(0, oldCaretIndex) + oldWithCaret.substring(oldCaretIndex + 1)
            val new =
                newWithCaret.substring(0, newCaretIndex) + newWithCaret.substring(newCaretIndex + 1)
            val newPos = findSamePosition(old, oldCaretIndex, new)

            val actual = new.substring(0, newPos) + "|" + new.substring(newPos)
            assertEquals(newWithCaret, actual)
        }

        // Prefix match
        check("|/** Test\n Different Middle End */", "|/** Test2 End */")
        check("/|** Test\n Different Middle End */", "/|** Test2 End */")
        check("/*|* Test\n Different Middle End */", "/*|* Test2 End */")
        check("/**| Test\n Different Middle End */", "/**| Test2 End */")
        check("/** |Test\n Different Middle End */", "/** |Test2 End */")
        check("/** T|est\n Different Middle End */", "/** T|est2 End */")
        check("/** Te|st\n Different Middle End */", "/** Te|st2 End */")
        check("/** Tes|t\n Different Middle End */", "/** Tes|t2 End */")
        check("/** Test|\n Different Middle End */", "/** Test|2 End */")
        // End match
        check("/** Test\n Different Middle| End */", "/** Test2| End */")
        check("/** Test\n Different Middle E|nd */", "/** Test2 E|nd */")
        check("/** Test\n Different Middle En|d */", "/** Test2 En|d */")
        check("/** Test\n Different Middle End| */", "/** Test2 End| */")
        check("/** Test\n Different Middle End |*/", "/** Test2 End |*/")
        check("/** Test\n Different Middle End *|/", "/** Test2 End *|/")
        check("/** Test\n Different Middle End */|", "/** Test2 End */|")

        check("|/**\nTest End\n*/", "|/** Test End */")
        check("/|**\nTest End\n*/", "/|** Test End */")
        check("/*|*\nTest End\n*/", "/*|* Test End */")
        check("/**|\nTest End\n*/", "/**| Test End */")
        check("/**\n|Test End\n*/", "/** |Test End */")
        check("/**\nT|est End\n*/", "/** T|est End */")
        check("/**\nTe|st End\n*/", "/** Te|st End */")
        check("/**\nTes|t End\n*/", "/** Tes|t End */")
        check("/**\nTest| End\n*/", "/** Test| End */")
        check("/**\nTest |End\n*/", "/** Test |End */")
        check("/**\nTest E|nd\n*/", "/** Test E|nd */")
        check("/**\nTest En|d\n*/", "/** Test En|d */")
        check("/**\nTest End|\n*/", "/** Test End| */")
        check("/**\nTest End\n|*/", "/** Test End |*/")
        check("/**\nTest End\n*|/", "/** Test End *|/")
        check("/**\nTest End\n*/|", "/** Test End */|")

        check("|/** Test End */", "|/** Test2 End */")
        check("/|** Test End */", "/|** Test2 End */")
        check("/*|* Test End */", "/*|* Test2 End */")
        check("/**| Test End */", "/**| Test2 End */")
        check("/** |Test End */", "/** |Test2 End */")
        check("/** T|est End */", "/** T|est2 End */")
        check("/** Te|st End */", "/** Te|st2 End */")
        check("/** Tes|t End */", "/** Tes|t2 End */")
        check("/** Test| End */", "/** Test|2 End */")
        check("/** Test |End */", "/** Test2 |End */")
        check("/** Test E|nd */", "/** Test2 E|nd */")
        check("/** Test En|d */", "/** Test2 En|d */")
        check("/** Test End| */", "/** Test2 End| */")
        check("/** Test End |*/", "/** Test2 End |*/")
        check("/** Test End *|/", "/** Test2 End *|/")
        check("/** Test End */|", "/** Test2 End */|")
    }
}

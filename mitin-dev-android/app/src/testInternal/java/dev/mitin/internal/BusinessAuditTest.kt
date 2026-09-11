package dev.mitin.internal

import org.junit.Assert.*
import org.junit.Test

class BusinessAuditTest {
    @Test fun scoreMatchesWebsiteFixturesAndPriorities() {
        assertEquals(100,calculateBusinessAudit(List(7){100}).score)
        assertEquals(4,calculateBusinessAudit(listOf(0,10,0,10,0,0,0)).score)
        val mixed=calculateBusinessAudit(listOf(55,65,55,75,60,60,55))
        assertEquals(61,mixed.score)
        assertEquals(listOf("website","crm","ai"),mixed.priorities.map{it.key})
        assertEquals(listOf("website","leads","crm"),calculateBusinessAudit(List(7){100}).priorities.map{it.key})
    }
    @Test fun incompleteOrInventedAnswersCannotProduceAResult() {
        for(values in listOf(emptyList(),List(7){-1},List(7){101})) {
            assertTrue(runCatching{calculateBusinessAudit(values)}.isFailure)
        }
    }
}

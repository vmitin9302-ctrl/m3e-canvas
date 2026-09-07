package dev.mitin.demo

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class DemoRepositoryTest {
    private fun repository() = DemoRepository(DemoSnapshot.initial()) {}

    @Test fun budgetAndStageWireValuesRemainCanonical() {
        assertEquals(listOf("under_10k", "10_20k", "20_40k", "40_70k", "70k_plus", "unknown"),
            BudgetRange.entries.map { Json.encodeToString(it).trim('"') })
        assertEquals(listOf("not_started", "in_progress", "waiting_client", "completed"),
            StageStatus.entries.map { Json.encodeToString(it).trim('"') })
    }
    @Test fun submittedBriefIsTheOwnersSameRecord() = runTest {
        val r = repository()
        val brief = Brief(type = "Бот", task = "Вымышленный бот для записи", budget = BudgetRange.UNKNOWN)
        val id = r.submit(brief)
        assertEquals(brief, r.state.value.leads.single { it.id == id }.brief)
        assertFalse(r.state.value.projects.any { it.leadId == id })
    }
    @Test fun conversionIsIdempotentAndBudgetIsNotPrice() = runTest {
        val r = repository()
        val id = r.createProject(DemoRole.OWNER, 1045)
        assertEquals(id, r.createProject(DemoRole.OWNER, 1045))
        assertEquals(2, r.state.value.projects.size)
        val p = r.state.value.projects.single { it.id == id }
        assertNull(p.agreedPrice); assertNull(p.paid); assertNull(p.agreedDeadline)
        assertFalse(p.demoAvailable)
        assertTrue(p.stages.all { it == StageStatus.NOT_STARTED })
    }
    @Test fun simultaneousConversionsCreateOneProject() = runTest {
        val r = repository()
        val ids = List(8) { async { r.createProject(DemoRole.OWNER, 1045) } }.awaitAll()
        assertEquals(1, ids.toSet().size)
        assertEquals(1, r.state.value.projects.count { it.leadId == 1045 })
    }
    @Test fun ownerStageEditVisibleInCommonSnapshotAndOtherProjectUnchanged() = runTest {
        val r = repository()
        val id = r.createProject(DemoRole.OWNER, 1045)
        val sample = r.state.value.projects.first()
        r.changeStage(DemoRole.OWNER, id, 3, StageStatus.WAITING_CLIENT)
        val p = r.state.value.projects.single { it.id == id }
        assertEquals(3, p.currentStage)
        assertEquals(StageStatus.WAITING_CLIENT, p.stages[3])
        assertEquals(sample, r.state.value.projects.first())
    }
    @Test fun clientCannotChangeOwnerDataEvenInDemo() = runTest {
        val r = repository()
        assertTrue(runCatching { r.createProject(DemoRole.CLIENT, 1045) }.isFailure)
        assertTrue(runCatching { r.changeStage(DemoRole.CLIENT, 1, 0, StageStatus.COMPLETED) }.isFailure)
        assertEquals(DemoSnapshot.initial(), r.state.value)
    }
    @Test fun failedStorageDoesNotPublishSuccessOrPartialData() = runTest {
        val r = DemoRepository(DemoSnapshot.initial()) { error("synthetic storage failure") }
        assertTrue(runCatching { r.submit(Brief()) }.isFailure)
        assertTrue(runCatching { r.createProject(DemoRole.OWNER, 1045) }.isFailure)
        assertEquals(DemoSnapshot.initial(), r.state.value)
    }
    @Test fun snapshotRoundTripRetainsBothRolesChanges() = runTest {
        var saved = ""
        val r = DemoRepository(DemoSnapshot.initial()) { saved = Json.encodeToString(it) }
        val id = r.createProject(DemoRole.OWNER, r.submit(Brief(budget = BudgetRange.OVER_70)))
        r.changeStage(DemoRole.OWNER, id, 1, StageStatus.IN_PROGRESS)
        r.addMessage(DemoRole.CLIENT, id, "Вымышленный комментарий")
        val restored = Json.decodeFromString<DemoSnapshot>(saved)
        assertEquals(r.state.value, restored)
        assertNull(restored.projects.last().agreedPrice)
    }
    @Test fun demoFeedbackIsSharedAndDoesNotEnableNewProjectDemo() = runTest {
        val r = repository()
        r.decideDemo(DemoRole.CLIENT, 1, false, "Увеличить заголовок")
        assertEquals("Увеличить заголовок", r.state.value.projects.first().demoFeedback)
        val id = r.createProject(DemoRole.OWNER, 1045)
        assertTrue(runCatching { r.decideDemo(DemoRole.CLIENT, id, true) }.isFailure)
        assertTrue(runCatching { r.decideDemo(DemoRole.OWNER, 1, true) }.isFailure)
    }
    @Test fun malformedBriefAndBlankFeedbackAreRejected() = runTest {
        val r = repository()
        assertTrue(runCatching { r.submit(Brief(task = "  ")) }.isFailure)
        assertTrue(runCatching { r.submit(Brief(type = "unknown")) }.isFailure)
        assertTrue(runCatching { r.submit(Brief(features = "a".repeat(2001))) }.isFailure)
        assertTrue(runCatching { r.decideDemo(DemoRole.CLIENT, 1, false) }.isFailure)
        assertEquals(DemoSnapshot.initial(), r.state.value)
    }
    @Test fun resetRemovesEnteredDemonstrationData() = runTest {
        val r = repository()
        r.submit(Brief(task = "Вымышленная идея"))
        r.addMessage(DemoRole.CLIENT, 1, "Локальное сообщение")
        r.reset()
        assertEquals(DemoSnapshot.initial(), r.state.value)
    }
    @Test fun convertedLeadCannotBeManuallyConvertedAgainByStatus() = runTest {
        val r = repository()
        r.createProject(DemoRole.OWNER, 1045)
        assertTrue(runCatching { r.changeLeadStatus(DemoRole.OWNER, 1045, LeadStatus.NEW) }.isFailure)
    }
}

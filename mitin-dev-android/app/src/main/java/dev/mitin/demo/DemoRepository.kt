package dev.mitin.demo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BudgetRange(val label: String) {
    @SerialName("under_10k") UNDER_10K("до 10 000 ₽"),
    @SerialName("10_20k") FROM_10_20("10–20 тыс."),
    @SerialName("20_40k") FROM_20_40("20–40 тыс."),
    @SerialName("40_70k") FROM_40_70("40–70 тыс."),
    @SerialName("70k_plus") OVER_70("70 тыс.+"),
    @SerialName("unknown") UNKNOWN("пока не знаю")
}

@Serializable
enum class StageStatus(val label: String, val symbol: String) {
    @SerialName("not_started") NOT_STARTED("Не начат", "○"),
    @SerialName("in_progress") IN_PROGRESS("В работе", "●"),
    @SerialName("waiting_client") WAITING_CLIENT("Ожидаем клиента", "◷"),
    @SerialName("completed") COMPLETED("Завершён", "✓")
}

@Serializable enum class DemoRole { CLIENT, OWNER }
@Serializable enum class LeadStatus(val label: String) {
    NEW("Новая"), WORK("В работе"), WAITING("Ожидание"), CLOSED("Закрыта"), CONVERTED("Проект создан")
}
val projectTypes = listOf("Сайт", "Бот", "CRM", "Автоматизация", "AI", "Своя идея")
val stageNames = listOf("ТЗ", "Дизайн", "Разработка", "Проверка", "Запуск", "Сопровождение")

@Serializable data class Brief(
    val type: String = "Сайт",
    val task: String = "Нужен сайт для автосервиса, чтобы клиенты видели услуги и оставляли заявки.",
    val features: String = "Услуги, каталог, форма заявки",
    val budget: BudgetRange = BudgetRange.FROM_20_40,
    val deadline: String = "В течение месяца",
    val integrations: String = "Пока не нужны",
    val comments: String = ""
) {
    fun validate() {
        require(type in projectTypes)
        require(task.isNotBlank() && task.length <= 2000)
        require(features.isNotBlank() && features.length <= 2000)
        require(deadline.length <= 200 && integrations.length <= 1000 && comments.length <= 2000)
    }
}

@Serializable data class DemoLead(
    val id: Int, val brief: Brief, val status: LeadStatus = LeadStatus.NEW
)
@Serializable data class DemoMessage(val role: DemoRole, val text: String)
@Serializable data class DemoProject(
    val id: Int,
    val leadId: Int,
    val name: String,
    // A budget range never sets agreedPrice or paid.
    val agreedPrice: Int? = null,
    val paid: Int? = null,
    val agreedDeadline: String? = null,
    val currentStage: Int = 0,
    val stages: List<StageStatus> = List(6) { StageStatus.NOT_STARTED },
    val demoAvailable: Boolean = false,
    val demoDecision: String = "Ожидается решение",
    val demoFeedback: String = "",
    val messages: List<DemoMessage> = emptyList()
)
@Serializable data class DemoSnapshot(
    val schemaVersion: Int = 1,
    val leads: List<DemoLead>,
    val projects: List<DemoProject>,
    val nextLeadId: Int = 1046,
    val nextProjectId: Int = 2
) {
    companion object {
        fun initial() = DemoSnapshot(
            leads = listOf(DemoLead(1042, Brief(), LeadStatus.CONVERTED), DemoLead(1045, Brief())),
            projects = listOf(DemoProject(
                id = 1, leadId = 1042, name = "Сайт автосервиса", agreedPrice = 30000,
                paid = 15000, agreedDeadline = "18.09.2026", currentStage = 2,
                stages = listOf(StageStatus.COMPLETED, StageStatus.COMPLETED, StageStatus.IN_PROGRESS,
                    StageStatus.NOT_STARTED, StageStatus.NOT_STARTED, StageStatus.NOT_STARTED),
                demoAvailable = true,
                messages = listOf(DemoMessage(DemoRole.OWNER, "Мобильная версия готова к просмотру. Это пример сообщения."))
            ))
        )
    }
}

/** One offline store for both demo roles. No identity/authentication or remote transport. */
class DemoRepository(initial: DemoSnapshot, private val persist: suspend (DemoSnapshot) -> Unit) {
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(initial)
    val state = mutable.asStateFlow()

    private suspend fun <T> change(operation: (DemoSnapshot) -> Pair<DemoSnapshot, T>): T = mutex.withLock {
        val (next, result) = operation(mutable.value)
        persist(next) // Publish success only after local storage succeeded.
        mutable.value = next
        result
    }

    suspend fun submit(brief: Brief): Int = change { current ->
        brief.validate()
        check(current.leads.size < 100) { "Достигнут лимит демоданных. Сбросьте демо в профиле." }
        val id = current.nextLeadId
        current.copy(leads = current.leads + DemoLead(id, brief), nextLeadId = id + 1) to id
    }

    suspend fun createProject(role: DemoRole, leadId: Int): Int = change { current ->
        require(role == DemoRole.OWNER)
        val lead = current.leads.single { it.id == leadId }
        val existing = current.projects.find { it.leadId == leadId }
        if (existing != null) current to existing.id else {
            val id = current.nextProjectId
            val project = DemoProject(id, leadId, "${lead.brief.type} · заявка №$leadId")
            current.copy(
                projects = current.projects + project,
                nextProjectId = id + 1,
                leads = current.leads.map { if (it.id == leadId) it.copy(status = LeadStatus.CONVERTED) else it }
            ) to id
        }
    }

    suspend fun changeLeadStatus(role: DemoRole, leadId: Int, status: LeadStatus) = change { current ->
        require(role == DemoRole.OWNER && status != LeadStatus.CONVERTED)
        val lead = current.leads.single { it.id == leadId }
        require(lead.status != LeadStatus.CONVERTED)
        current.copy(leads = current.leads.map { if (it.id == leadId) it.copy(status = status) else it }) to Unit
    }

    suspend fun changeStage(role: DemoRole, projectId: Int, stage: Int, status: StageStatus) = change { current ->
        require(role == DemoRole.OWNER && stage in stageNames.indices)
        require(current.projects.any { it.id == projectId })
        current.copy(projects = current.projects.map { project ->
            if (project.id != projectId) project else project.copy(
                currentStage = stage,
                stages = project.stages.mapIndexed { i, value -> if (i == stage) status else value }
            )
        }) to Unit
    }

    suspend fun decideDemo(role: DemoRole, projectId: Int, approve: Boolean, comment: String = "") = change { current ->
        require(role == DemoRole.CLIENT)
        val project = current.projects.single { it.id == projectId }
        require(project.demoAvailable)
        require(comment.length <= 2000 && (approve || comment.isNotBlank()))
        current.copy(projects = current.projects.map {
            if (it.id != projectId) it else it.copy(
                demoDecision = if (approve) "Одобрено локально · демо" else "Есть правки · демо",
                demoFeedback = comment
            )
        }) to Unit
    }

    suspend fun addMessage(role: DemoRole, projectId: Int, text: String) = change { current ->
        require(text.isNotBlank() && text.length <= 1000)
        require(current.projects.any { it.id == projectId })
        current.copy(projects = current.projects.map {
            if (it.id != projectId) it else it.copy(messages = (it.messages + DemoMessage(role, text)).takeLast(30))
        }) to Unit
    }

    suspend fun reset() = change { DemoSnapshot.initial() to Unit }
}

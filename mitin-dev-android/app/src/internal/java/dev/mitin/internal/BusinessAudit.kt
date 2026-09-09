package dev.mitin.internal

import kotlin.math.roundToInt

data class AuditDimension(val key: String, val title: String, val weight: Int,
    val answers: List<Pair<Int, String>>, val recommendation: String, val explanation: String)

/** Same weights, answer values and priority ordering as business-audit.js on the site. */
val auditDimensions = listOf(
    AuditDimension("website", "Сайт", 20, listOf(100 to "Современный сайт",55 to "Сайт есть, но устарел",20 to "Только соцсети / карточки",0 to "Сайта нет"),
        "Обновить цифровую витрину", "Сделать понятный мобильный сайт с оффером, кейсами и быстрым CTA."),
    AuditDimension("leads", "Заявки", 20, listOf(100 to "Форма или бот передают заявку в CRM",65 to "Формы и мессенджеры не полностью связаны",35 to "Заявки собираются вручную",10 to "Нет понятного сценария заявки"),
        "Собрать обращения в единый сценарий", "Связать формы и мессенджеры так, чтобы заявка не терялась между каналами."),
    AuditDimension("crm", "CRM", 15, listOf(100 to "Полноценная CRM",55 to "Таблица или заметки",20 to "Клиенты остаются в переписках",0 to "Учёта клиентов нет"),
        "Ввести CRM-воронку", "Хранить контакты, статусы, историю и следующие действия в одном рабочем пространстве."),
    AuditDimension("response", "Скорость ответа", 15, listOf(100 to "Первый ответ сразу",75 to "Обычно до часа",35 to "Через несколько часов",10 to "Иногда на следующий день"),
        "Ускорить первый ответ", "Добавить автоответ, бот или уведомление ответственному сразу после обращения."),
    AuditDimension("analytics", "Аналитика", 10, listOf(100 to "Видна вся воронка до продажи",60 to "Есть базовая аналитика",20 to "Источник определяется примерно",0 to "Аналитики нет"),
        "Настроить сквозную аналитику", "Связать источник, визит, заявку и результат, чтобы понимать рабочие каналы."),
    AuditDimension("automation", "Автоматизация", 10, listOf(100 to "Автоматизировано несколько процессов",60 to "Есть отдельные автоматизации",20 to "Почти всё вручную",0 to "Автоматизации нет"),
        "Убрать повторяющуюся ручную работу", "Автоматизировать уведомления, статусы, напоминания и передачу данных между сервисами."),
    AuditDimension("ai", "AI", 10, listOf(100 to "AI встроен в процессы",55 to "Сотрудники используют AI вручную",15 to "AI только пробовали",0 to "AI пока не используется"),
        "Добавить AI туда, где он окупается", "Начать с квалификации заявок, консультаций, резюме диалогов или подготовки контента."),
)

data class BusinessAuditResult(val score: Int, val priorities: List<AuditDimension>)
fun calculateBusinessAudit(values: List<Int>): BusinessAuditResult {
    require(values.size == auditDimensions.size)
    auditDimensions.forEachIndexed { i, dimension -> require(dimension.answers.any { it.first == values[i] }) }
    val score = auditDimensions.mapIndexed { i, d -> values[i] * d.weight / 100.0 }.sum().roundToInt()
    val ranked = auditDimensions.indices.sortedWith(compareBy<Int> { values[it] }.thenByDescending { auditDimensions[it].weight })
    return BusinessAuditResult(score, ranked.take(3).map { auditDimensions[it] })
}

fun auditVerdict(score: Int): String = when {
    score >= 85 -> "Сильная digital-система. Главный потенциал — связать данные, AI и автоматизации в единый контур и продолжать улучшать конверсию."
    score >= 65 -> "Хорошая цифровая база уже есть, но несколько несвязанных процессов могут тормозить продажи и создавать лишнюю ручную работу."
    score >= 45 -> "База есть, но часть заявок и данных, скорее всего, теряется между сайтом, мессенджерами и ручным учётом."
    else -> "Сейчас есть несколько быстрых точек роста. Даже 2–3 базовые цифровые изменения могут заметно упростить путь клиента и работу с заявками."
}

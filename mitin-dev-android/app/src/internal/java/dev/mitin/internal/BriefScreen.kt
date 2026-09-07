package dev.mitin.internal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import dev.mitin.demo.*

val briefServices = linkedMapOf("site" to "Сайт", "bot" to "Бот", "automation" to "Автоматизация", "ai" to "AI", "app" to "Приложение", "crm" to "CRM", "idea" to "Своя идея")
val briefBudgets = linkedMapOf("under_10k" to "до 10 000 ₽", "10_20k" to "10 000–20 000 ₽", "20_40k" to "20 000–40 000 ₽", "40_70k" to "40 000–70 000 ₽", "70k_plus" to "70 000 ₽ и выше", "unknown" to "Пока не знаю")

@OptIn(ExperimentalLayoutApi::class)
@Composable fun BriefScreen(vm: BriefViewModel) {
    val state=vm.state
    val uri=LocalUriHandler.current
    val focus=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    val dismissInput = { focus.clearFocus(); keyboard?.hide(); Unit }
    SectionIntro("Из идеи в план", "AI-бриф", "Расскажите о задумке. Мы поможем определить главное и собрать понятное ТЗ.")
    if (vm.capabilities?.ai != true) { InfoCard("AI-бриф пока недоступен", "Попробуйте позже. Доступность определяет сервер."); return }
    if(vm.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Note("AI отвечает или сервер проверяет состояние…") }
    vm.error?.let { InfoCard("Не удалось завершить действие",it,tag="brief-error") }
    if(vm.canRetry) SecondaryAction("Повторить", "brief-retry") { vm.retry() }
    when {
        state == null -> {
            Heading("Что хотите создать?")
            vm.portfolio?.let { Note("На основе кейса ${vm.portfolioTitle ?: it}. Это направление, а не обещание копирования.") }
            var service by rememberSaveable { mutableStateOf("site") }
            var budget by rememberSaveable { mutableStateOf("unknown") }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { briefServices.forEach { (key,label) -> FilterChip(selected=service==key,onClick={service=key},label={Text(label)},enabled=!vm.busy,modifier=Modifier.testTag("brief-service-$key")) } }
            Heading("Ориентир бюджета")
            briefBudgets.forEach { (key,label) -> FilterChip(selected=budget==key,onClick={budget=key},label={Text(label)},enabled=!vm.busy,modifier=Modifier.fillMaxWidth().testTag("brief-budget-$key")) }
            Note("Бюджет не является согласованной ценой. При небольшом бюджете обсудим MVP и этапность.")
            PrimaryAction("Начать обсуждение", "brief-start", !vm.busy) { vm.start(service,budget) }
        }
        state.submitted -> {
            Heading("Заявка отправлена")
            Note("Номер заявки: ${state.reference}. Мы свяжемся с вами по указанному контакту.")
            SecondaryAction("Новый бриф", "brief-new") { vm.reset() }
        }
        else -> {
            state.portfolio?.let { Note("Направление кейса: $it") }
            Note("Бюджет: ${briefBudgets[state.budget] ?: state.budget}")
            if(vm.contactStep && state.finalBrief != null) {
                Heading("Проверьте заявку")
                BriefDocument(state.finalBrief,modifier=Modifier.testTag("brief-final"))
                Note("После явной отправки ТЗ и контакт попадут в CRM MITIN DEV для рассмотрения. Цена и сроки ещё не согласованы.")
                if (!vm.submissionAvailable) {
                    InfoCard("Отправка пока недоступна", "Ваше ТЗ готово. Приём заявок ещё не активирован — ничего не отправлено в CRM.", tag="submission-disabled")
                    PrimaryAction("Проверить отправку", "brief-submission-check") { vm.prepare("", "email", "") }
                } else if(state.prepared == null) {
                    var name by rememberSaveable { mutableStateOf("") }
                    var contact by rememberSaveable { mutableStateOf("") }
                    var type by rememberSaveable { mutableStateOf("email") }
                    OutlinedTextField(name,{if(it.length<=120) name=it},label={Text("Ваше имя")},modifier=Modifier.fillMaxWidth().testTag("brief-name"))
                    listOf("email" to "Email", "phone" to "Телефон", "vk" to "VK", "max" to "MAX").forEach { (key,label) ->
                        FilterChip(type==key,{type=key},label={Text(label)},enabled=!vm.busy)
                    }
                    OutlinedTextField(contact,{if(it.length<=180) contact=it},label={Text("Контакт")},modifier=Modifier.fillMaxWidth().testTag("brief-contact"))
                    PrimaryAction("Проверить контакт", "brief-prepare", !vm.busy && name.isNotBlank() && contact.isNotBlank()) { dismissInput(); vm.prepare(name,type,contact) }
                } else {
                    Text("${state.prepared.name}\n${state.prepared.contact}")
                    SecondaryAction("Изменить контакт", "brief-edit-contact") { vm.editContact() }
                    var consent by rememberSaveable(state.proof) { mutableStateOf(false) }
                    Row { Checkbox(consent,{consent=it},enabled=!vm.busy,modifier=Modifier.testTag("brief-consent")); Text("Согласен на обработку данных") }
                    TextButton(onClick={uri.openUri("https://24promtbot.ru/consent.html")}) { Text("Согласие на обработку данных") }
                    PrimaryAction("Отправить заявку", "brief-confirm", !vm.busy && consent) { vm.confirm(consent) }
                }
                SecondaryAction("Продолжить обсуждение", "brief-discuss") { vm.discuss() }
            } else {
                state.messages.forEach { message -> ChatBubble(message) }
                if(state.messages.isEmpty()) Note("Расскажите о задаче: для кого проект и какой результат нужен?")
                var message by rememberSaveable { mutableStateOf("") }
                OutlinedTextField(message,{if(it.length<=4000) message=it},label={Text("Сообщение AI")},modifier=Modifier.fillMaxWidth().testTag("brief-message"))
                PrimaryAction("Отправить сообщение", "brief-send",!vm.busy && message.isNotBlank()) { dismissInput(); vm.message(message); message="" }
                if(state.messages.isNotEmpty()) PrimaryAction("Сформировать итоговое ТЗ", "brief-finalize",!vm.busy) { dismissInput(); vm.finalBrief() }
                if(state.finalBrief != null) {
                    Heading("Итоговое ТЗ")
                    Surface(shape=androidx.compose.foundation.shape.RoundedCornerShape(24.dp), color=MaterialTheme.colorScheme.surfaceContainerLow) {
                        BriefDocument(state.finalBrief,modifier=Modifier.padding(20.dp).testTag("brief-final"))
                    }
                    PrimaryAction("Перейти к отправке заявки", "brief-to-contact",!vm.busy) { dismissInput(); vm.showContact() }
                }
            }
            SecondaryAction("Обновить сессию", "brief-refresh") { vm.retry() }
            SecondaryAction("Начать новый бриф", "brief-reset") { vm.reset() }
        }
    }
}

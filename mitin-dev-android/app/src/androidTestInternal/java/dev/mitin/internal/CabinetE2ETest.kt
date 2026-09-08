package dev.mitin.internal

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

private const val ORIGIN="https://localhost:8443/"
private fun obj(vararg values:Pair<String,String>)=buildJsonObject {for((key,value) in values)put(key,value)}
private fun mailCode(email:String,purpose:String="verify_email"):String = HttpAuthApi.secureClient().newCall(Request.Builder().url(ORIGIN+"test/cabinet-mail")
    .post(obj("email" to email,"purpose" to purpose).toString().toRequestBody("application/json".toMediaType())).build()).execute().use {
        check(it.isSuccessful);Json.parseToJsonElement(it.body.string()).jsonObject.text("token")
    }
private suspend fun register(api:CabinetApi,email:String) {
    api.call("auth/register","POST",body=buildJsonObject {put("name","Синтетический клиент кабинета");put("email",email);put("password",TEST_PASSWORD);put("consent",true)})
    api.call("auth/verify-email","POST",body=obj("token" to withContext(Dispatchers.IO){mailCode(email)}))
}

class CabinetNetworkE2ETest {
    @Test fun realClientOwnerJourneyAndIsolation() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
        val stores=List(3){KeystoreRefreshStore(context,"cabinet_"+UUID.randomUUID().toString().replace("-",""))}
        val managers=stores.map{SessionManager(HttpAuthApi(ORIGIN),it,scope)}
        val (a,b,owner)=managers
        val api=CabinetApi(ORIGIN)
        val email="cabinet-${UUID.randomUUID()}@example.test"
        val emailB="cabinet-${UUID.randomUUID()}@example.test"
        val repos=managers.map{CabinetRepository(it,api)};val (client,other,admin)=repos
        try {
            register(api,email);register(api,emailB)
            a.login(email,Secret(TEST_PASSWORD));b.login(emailB,Secret(TEST_PASSWORD));owner.login("owner@example.com",Secret(TEST_PASSWORD))
            assertEquals("owner",owner.state.value.profile!!.role)
            client.mutate("profile","PATCH",obj("name" to "Клиент Android","phone" to "+70000000000","legal_status" to "self_employed"))
            assertEquals("self_employed",client.read("profile").text("legal_status"))
            val lead=client.mutate("leads","POST",obj("title" to "Android проект","service" to "Сайт","description" to "Синтетическое тестовое задание"))
            val project=admin.mutate("owner/leads/${lead.text("id")}/project","POST",obj("title" to "Проект Android"));val id=project.text("id")
            val path="projects/$id";val own="owner/$path"
            assertTrue(project["agreed_price_amount"] is JsonNull)
            val stage=admin.mutate("$own/stages","POST",buildJsonObject{put("title","Разработка");put("position",0);put("status","in_progress")})
            assertEquals(stage.text("id"),client.read(path).text("current_stage_id"))
            val demo=admin.mutate("$own/demos","POST",obj("title" to "Первая версия"))
            client.mutate("$path/demos/${demo.text("id")}/decision","POST",obj("decision" to "approved"))
            val v2=admin.mutate("$own/demos","POST",obj("title" to "Вторая версия"))
            val versions=client.read("$path/demos",0)["items"]!!.jsonArray
            assertTrue(versions.last().jsonObject["decision"] is JsonNull)
            client.mutate("$path/demos/${v2.text("id")}/decision","POST",obj("decision" to "changes_requested","comment" to "Уточнить заголовок"))
            val message=obj("body" to "Сообщение из Android","client_request_id" to UUID.randomUUID().toString())
            val sent=client.mutate("$path/messages","POST",message)
            assertEquals(sent.text("id"),client.mutate("$path/messages","POST",message).text("id"))
            admin.mutate("$own/read-state","PUT",obj("last_read_message_id" to sent.text("id")))
            admin.mutate("$own/messages","POST",obj("body" to "Ответ владельца","client_request_id" to UUID.randomUUID().toString()))
            val material=admin.mutate("$own/materials","POST",obj("title" to "Тексты"))
            val file=client.upload(id,"android.txt","text/plain","Synthetic Android material".toByteArray(),material.text("id"))
            assertArrayEquals("Synthetic Android material".toByteArray(),client.download(id,file.text("id")))
            admin.mutate(own,"PATCH",buildJsonObject{put("version",1);put("agreed_price_amount",1000000)})
            admin.mutate("$own/payments","POST",buildJsonObject{put("amount_minor",400000);put("type","prepayment");put("status","confirmed")})
            admin.mutate("$own/expenses","POST",buildJsonObject{put("title","Хостинг");put("amount_minor",50000);put("category","infrastructure");put("recurrence","monthly");put("status","confirmed")})
            assertEquals(600000L,client.read("$path/financial-summary").number("balance_amount"))
            val support=client.mutate("$path/support","POST",obj("type" to "question","subject" to "Вопрос","description" to "Когда следующий этап?"))
            admin.mutate("$own/support/${support.text("id")}","PATCH",obj("status" to "in_progress"))
            for(section in listOf("stages","demos","messages","materials","files","payments","expenses","support","events")) {
                assertTrue(client.read("$path/$section",0)["items"]!!.jsonArray.isNotEmpty())
                val error=runCatching{other.read("$path/$section",0)}.exceptionOrNull()
                assertTrue(error is AuthFailure && error.status==404)
            }
            val forbidden=runCatching{client.read("owner/clients",0)}.exceptionOrNull();assertTrue(forbidden is AuthFailure && forbidden.status==403)
            val foreignFile=runCatching{other.download(id,file.text("id"))}.exceptionOrNull();assertTrue(foreignFile is AuthFailure && foreignFile.status==404)
            assertTrue(admin.read("owner/analytics").number("project_count")!!>=1)
            client.publicAction("auth/password-reset/request",obj("email" to email))
            client.publicAction("auth/password-reset/confirm",obj("token" to withContext(Dispatchers.IO){mailCode(email,"password_reset")},"password" to TEST_PASSWORD))
            assertTrue(runCatching{client.read(path)}.isFailure)
            assertNull(a.state.value.profile)
            a.login(email,Secret(TEST_PASSWORD));assertEquals(id,client.read(path).text("id"))
        } finally {for(manager in managers) runCatching{manager.logout()};for(store in stores)store.clear();scope.cancel()}
    }
}

class CabinetUiE2ETest {
    @get:Rule val ui=createAndroidComposeRule<InternalActivity>()
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val manager get()=(ui.activity.application as InternalApplication).manager!!
    private val device get()=UiDevice.getInstance(instrumentation)
    private fun waitFor(tag:String)=ui.waitUntil(60_000){ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()}
    private fun tap(tag:String) {
        waitFor(tag);ui.waitUntil(60_000){ui.onAllNodes(hasTestTag(tag) and isEnabled()).fetchSemanticsNodes().isNotEmpty()}
        val node=ui.onNodeWithTag(tag)
        if(ui.onAllNodes(hasTestTag(tag) and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty())node.performScrollTo()
        node.assertIsDisplayed().performClick();ui.waitForIdle()
    }
    private fun fill(tag:String,value:String) {
        waitFor(tag);val node=ui.onNodeWithTag(tag)
        if(ui.onAllNodes(hasTestTag(tag) and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty())node.performScrollTo()
        node.performTextReplacement(value);ui.waitForIdle()
    }
    private fun hideKeyboard() {device.pressBack();ui.waitForIdle()}
    private fun shot(stage:String) {
        val label=InstrumentationRegistry.getArguments().getString("portfolioCase") ?: "cabinet"
        val file=File(instrumentation.targetContext.getExternalFilesDir(null),"cabinet-$label-$stage.png")
        assertTrue(device.takeScreenshot(file));device.executeShellCommand("mkdir -p /sdcard/Download/mitin-network")
        device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/mitin-network/${file.name}")
    }
    private fun login(email:String) {fill("cabinet-email",email);fill("cabinet-password",TEST_PASSWORD);hideKeyboard();tap("cabinet-auth-submit");waitFor("cabinet-projects")}
    @Test fun registrationProjectMessagesOwnerAndRecreation() {
        runBlocking{manager.logout()}
        waitFor("cabinet-email")
        val email="cabinet-${UUID.randomUUID()}@example.test"
        tap("auth-register");fill("cabinet-email",email);fill("cabinet-name","Синтетический клиент");fill("cabinet-password",TEST_PASSWORD);hideKeyboard();tap("cabinet-consent");tap("cabinet-auth-submit")
        waitFor("cabinet-token")
        fill("cabinet-token",runBlocking(Dispatchers.IO){mailCode(email)});hideKeyboard();tap("cabinet-auth-submit")
        waitFor("cabinet-email");login(email);shot("client-dashboard")
        tap("cabinet-profile");tap("cabinet-add");fill("field-city","Москва");hideKeyboard();tap("cabinet-save")
        ui.waitUntil(60_000){ui.onAllNodesWithTag("cabinet-save").fetchSemanticsNodes().isEmpty()}
        tap("cabinet-back");tap("cabinet-leads");tap("cabinet-add");fill("field-title","Проект из Android");fill("field-service","Сайт");fill("field-description","Длинное описание задачи клиента. ".repeat(15));hideKeyboard();tap("cabinet-save")
        ui.waitUntil(60_000){ui.onAllNodesWithTag("cabinet-save").fetchSemanticsNodes().isEmpty()}
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
        val store=KeystoreRefreshStore(instrumentation.targetContext,"ui_owner_"+UUID.randomUUID().toString().replace("-",""))
        val owner=SessionManager(HttpAuthApi(ORIGIN),store,scope)
        val project=runBlocking {
            owner.login("owner@example.com",Secret(TEST_PASSWORD))
            val client=CabinetRepository(manager,CabinetApi(ORIGIN));val admin=CabinetRepository(owner,CabinetApi(ORIGIN))
            val lead=client.read("leads",0)["items"]!!.jsonArray.first().jsonObject
            val p=admin.mutate("owner/leads/${lead.text("id")}/project","POST",obj("title" to "Проверка интерфейса"))
            admin.mutate("owner/projects/${p.text("id")}/demos","POST",obj("title" to "Демо интерфейса"))
            owner.logout();store.clear();scope.cancel();p
        }
        tap("cabinet-back");tap("cabinet-back");tap("cabinet-projects");tap("open-0");shot("project")
        tap("project-messages");fill("project-message","Сообщение с клавиатурой. ".repeat(10));shot("message-ime");hideKeyboard()
        ui.onNodeWithTag("project-message").assertExists();tap("send-project-message")
        ui.waitUntil(60_000){ui.onAllNodesWithTag("cabinet-item-0").fetchSemanticsNodes().isNotEmpty()}
        ui.activityRule.scenario.recreate();ui.waitForIdle();waitFor("project-message");shot("message-recreated")
        tap("cabinet-back");tap("project-demos");tap("approve-0");tap("cabinet-back");tap("cabinet-back");tap("cabinet-back");tap("cabinet-logout")
        waitFor("cabinet-email");assertNull(manager.state.value.profile)
        login("owner@example.com");waitFor("cabinet-clients");shot("owner-dashboard")
        tap("cabinet-projects")
        // Newest project is first, and the owner list reflects the same DB record.
        tap("open-0");tap("project-payments");tap("cabinet-add");fill("field-amount_minor","1234,56");hideKeyboard();shot("owner-payment-form");tap("cabinet-save")
        ui.waitUntil(60_000){ui.onAllNodesWithTag("cabinet-save").fetchSemanticsNodes().isEmpty()}
        tap("cabinet-back");tap("cabinet-back");tap("cabinet-back");tap("cabinet-logout");waitFor("cabinet-email")
        assertNull(manager.state.value.profile)
    }
}

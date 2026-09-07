package dev.mitin.internal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.mitin.demo.MitinTheme
import kotlinx.coroutines.*
import org.junit.Rule
import org.junit.Test

class PortfolioStateUiTest {
    @get:Rule val ui = createComposeRule()
    @Test fun loadingEmptyErrorRetryLongTextAndMissingImage() {
        var result = CompletableDeferred<List<PortfolioItem>>()
        val item = PortfolioItem("long-case", "Длинное название ".repeat(10), "Описание ".repeat(100), "Подробности ".repeat(200))
        val repo = object : PortfolioRepository {
            override suspend fun list() = result.await()
            override suspend fun detail(slug: String) = item
        }
        val vm = PortfolioViewModel(repo)
        ui.setContent { MitinTheme { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { PortfolioScreen(vm) {} } } }
        ui.onNodeWithTag("portfolio-loading").assertExists()
        result.complete(emptyList())
        ui.waitUntil { ui.onAllNodesWithTag("portfolio-empty").fetchSemanticsNodes().isNotEmpty() }
        result = CompletableDeferred()
        ui.onNodeWithTag("portfolio-refresh").performClick()
        result.completeExceptionally(PortfolioFailure(500))
        ui.waitUntil { ui.onAllNodesWithTag("portfolio-error").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithTag("portfolio-loading").assertDoesNotExist()
        result = CompletableDeferred()
        ui.onNodeWithTag("portfolio-retry").performScrollTo().performClick()
        result.complete(listOf(item))
        ui.waitUntil { ui.onAllNodesWithTag("portfolio-long-case").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithTag("portfolio-image-placeholder").assertExists()
        ui.onNodeWithTag("portfolio-open-long-case").performScrollTo().performClick()
        ui.waitUntil { ui.onAllNodesWithTag("portfolio-detail-title").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithTag("portfolio-detail-title").assertTextEquals(item.title)
        ui.onNodeWithTag("portfolio-discuss").performScrollTo().assertIsDisplayed()
        ui.onNodeWithTag("portfolio-site").assertDoesNotExist()
        ui.onNodeWithTag("portfolio-back").performScrollTo().performClick()
        ui.onNodeWithTag("portfolio-long-case").assertExists()
    }
}

package com.qtekfun.ultimatelauncher.layoutsync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutSyncTest {
    private fun app(p: String, x: Int, y: Int) = Item.App(p, "$p.Main", "personal", Cell(x, y))
    private val dev = Device("phone", "OPPO", "CPH2841", 36, 560, 411, 905)
    private fun snap(grid: Grid, pages: List<Page>, hot: List<Pair<Int, Item>> = emptyList()) =
        Snapshot("2026-10-04T00:00:00Z", dev, Settings(grid), hot, pages)
    private fun state(grid: Grid, installed: List<String>, hot: Int = 5, widgets: Set<String> = emptySet()) =
        DeviceState(installed.associateWith { listOf("$it.Main") }, widgets, grid, hot, false)

    @Test fun jsonRoundTripGivesSameState() {
        val s = snap(Grid(5, 7), listOf(Page(0, listOf(app("a", 0, 0), Item.Folder("F", listOf(Item.App("b", "b.Main")), Cell(1, 0)),
            Item.Widget("w/x.W", Span(4, 2), Cell(0, 2))))), listOf(0 to app("h", 0, 0).copy(cell = null)))
        assertEquals(s, LayoutJson.fromJson(LayoutJson.toJson(s)))
    }

    @Test(expected = UnsupportedSchemaException::class) fun rejectsNewerSchema() {
        LayoutJson.fromJson(LayoutJson.toJson(snap(Grid(4, 6), emptyList())).replace("\"schema\": 1", "\"schema\": 2"))
    }

    @Test fun growingGridKeepsPositions() { // 4x6 -> 5x6
        val pages = listOf(Page(0, listOf(app("a", 3, 5), app("b", 0, 0))))
        val r = GridReflow.reflow(pages, Grid(5, 6))
        assertFalse(r.reflowed); assertEquals(Cell(3, 5), r.pages[0].first { (it as Item.App).pkg == "a" }.cell)
    }

    @Test fun shrinkingGridReflowsInReadingOrderAndSplitsPages() { // 5x6 -> 5x4 con 24 apps
        val items = (0 until 24).map { app("p$it", it % 5, it / 5) }
        val r = GridReflow.reflow(listOf(Page(0, items)), Grid(5, 4))
        assertTrue(r.reflowed); assertEquals(2, r.pages.size)
        assertEquals(20, r.pages[0].size); assertEquals(4, r.pages[1].size)
        assertEquals("p0", (r.pages[0][0] as Item.App).pkg); assertEquals("p23", (r.pages[1].last() as Item.App).pkg)
    }

    @Test fun bigWidgetIsClampedToSmallerGrid() { // widget 6x2 en rejilla de 4 columnas
        val r = GridReflow.reflow(listOf(Page(0, listOf(Item.Widget("w/W", Span(6, 2), Cell(0, 0))))), Grid(4, 5))
        assertEquals(Span(4, 2), (r.pages[0][0] as Item.Widget).span); assertEquals(1, r.clampedSpans)
    }

    @Test fun missingAppsAndWorkProfileAreReported() {
        val s = snap(Grid(5, 7), listOf(Page(0, listOf(app("ok", 0, 0), app("gone", 1, 0), Item.App("w", "w.Main", "work", Cell(2, 0))))))
        val p = ImportPlanner.plan(s, state(Grid(5, 7), listOf("ok")))
        assertEquals(1, p.pages.sumOf { it.size }); assertEquals(2, p.omitted.size)
        assertTrue(p.omitted.any { it.reason.contains("no instalada") }); assertTrue(p.omitted.any { it.reason.contains("perfil de trabajo") })
    }

    @Test fun otherBrandWidgetIsOmittedAndKnownWidgetIsPending() {
        val s = snap(Grid(5, 7), listOf(Page(0, listOf(Item.Widget("com.miui.x/W", Span(2, 2), Cell(0, 0)), Item.Widget("com.keep/W", Span(2, 2), Cell(2, 0))))))
        val p = ImportPlanner.plan(s, state(Grid(5, 7), emptyList(), widgets = setOf("com.keep/W")))
        assertEquals(1, p.pendingWidgets.size); assertEquals(1, p.omitted.size)
    }

    @Test fun hotseatOverflowMovesToHomeAndFolderKeepsInstalledApps() {
        val hot = (0 until 6).map { it to (Item.App("h$it", "h$it.Main") as Item) }
        val s = snap(Grid(5, 7), listOf(Page(0, listOf(Item.Folder("F", listOf(Item.App("a", "a.Main"), Item.App("zz", "zz.Main")), Cell(0, 0))))), hot)
        val p = ImportPlanner.plan(s, state(Grid(5, 7), listOf("a") + (0 until 6).map { "h$it" }, hot = 5))
        assertEquals(5, p.hotseat.size)
        assertTrue(p.pages.flatten().any { it is Item.App && it.pkg == "h5" })
        assertEquals(1, (p.pages.flatten().first { it is Item.Folder } as Item.Folder).items.size)
    }
}

package com.zasko.imageloads.ui.generic

import com.zasko.imageloads.ui.common.DynamicSourceConfig
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.jsoup.Jsoup

class GenericSourceRepositoryTest {

    @Test
    fun extractCategories_handlesTrendszineMenu() {
        val config = DynamicSourceConfig(
            key = "trendszine",
            type = 1,
            title = "Trendszine",
            cover = "",
            baseUrl = "https://trendszine.com/",
            processMethods = JSONObject("""{"list":{}}"""),
        )
        val html = """
            <html>
              <body>
                <ul id="mega-menu-primary">
                  <li><a class="mega-menu-link" href="/">全部</a></li>
                  <li>
                    <a class="mega-menu-link" href="/category/cosplay">Cosplay</a>
                    <ul class="mega-sub-menu">
                      <li><a class="mega-menu-link" href="/category/cosplay/best-coser">精选 Coser</a></li>
                    </ul>
                  </li>
                </ul>
                <main>
                  <article>
                    <div class="post-image">
                      <a href="https://trendszine.com/detail/1">
                        <img src="preview://cover" width="400" height="600" alt="Cover Alt" />
                      </a>
                    </div>
                    <h2 class="entry-title">
                      <a href="https://trendszine.com/detail/1">Sample Title</a>
                    </h2>
                  </article>
                </main>
              </body>
            </html>
        """.trimIndent()

        val result = GenericSourceRepository.extractCategories(
            doc = Jsoup.parse(html),
            config = config,
            categorySelector = "#mega-menu-primary > li",
            categoryLinkSelector = "a.mega-menu-link[href]",
            childrenCategorySelector = "ul.mega-sub-menu a.mega-menu-link[href]",
        )

        assertEquals(listOf("全部", "Cosplay"), result.map { it.title })
        assertEquals("https://trendszine.com", result.first().url)
        assertEquals("https://trendszine.com/category/cosplay", result[1].url)
        assertEquals(listOf("精选 Coser"), result[1].children.map { it.title })
        assertEquals("https://trendszine.com/category/cosplay/best-coser", result[1].children.first().url)
    }
}

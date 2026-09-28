package com.zasko.imageloads.feature.generic

import com.zasko.imageloads.core.ui.common.DynamicSourceConfig
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.jsoup.Jsoup

class GenericSourceRepositoryTest {

    @Test
    fun weibomnListSelectors_extractCoverAndNextPage() {
        val html = """
            <div id="contentContainer">
              <div class="photo-list">
                <a href="/girl1789989317.html">
                  <p style="background-image: url('/images/image/20260921/cover.jpg');"></p>
                </a>
              </div>
            </div>
            <ol class="page-navigator">
              <li><a href="/list2.html">下一页</a></li>
            </ol>
        """.trimIndent()
        val document = Jsoup.parse(html)
        val item = document.selectFirst("#contentContainer .photo-list")
        val finalPageDocument = Jsoup.parse(
            """
                <ol class="page-navigator">
                  <li><a href="/list256.html">上一页</a></li>
                  <li><a href="/list257.html">已经是尾页</a></li>
                </ol>
            """.trimIndent(),
        )

        assertEquals("/girl1789989317.html", item?.selectFirst("a[href]")?.attr("href"))
        assertEquals(
            "/images/image/20260921/cover.jpg",
            item?.selectFirst("p[style*=background-image]")?.readSourceAttribute("style.background-image"),
        )
        assertEquals("/list2.html", document.selectFirst(".page-navigator a:containsOwn(下一页)[href]")?.attr("href"))
        assertEquals(null, finalPageDocument.selectFirst(".page-navigator a:containsOwn(下一页)[href]"))
    }

    @Test
    fun weibomnDetailSelectors_excludeRecommendationImages() {
        val html = """
            <article class="post">
              <div class="post-content indent">
                <p>云在脚下 光在眼前</p>
                <div class="photos">
                  <a href="/images/one.jpg" data-lightbox="img"><img src="/images/one.jpg" /></a>
                  <a href="/images/two.jpg" data-lightbox="img"><img src="/images/two.jpg" /></a>
                </div>
                <blockquote><p>更新时间：2026年09月21日 19时</p></blockquote>
                <h2>猜你喜欢</h2>
                <div class="photos">
                  <a href="/girl123.html"><img src="/images/recommendation.jpg" /></a>
                </div>
              </div>
            </article>
        """.trimIndent()
        val document = Jsoup.parse(html)
        val content = document.selectFirst("article.post .post-content")

        assertEquals("云在脚下 光在眼前", document.selectFirst("article.post .post-content > p:first-child")?.text())
        assertEquals("更新时间：2026年09月21日 19时", document.selectFirst("article.post .post-content blockquote p")?.text())
        assertEquals(
            listOf(
                "/images/one.jpg",
                "/images/two.jpg",
            ),
            content?.select(".photos a[data-lightbox=img][href]")?.map { it.readSourceAttribute("href") },
        )
    }

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

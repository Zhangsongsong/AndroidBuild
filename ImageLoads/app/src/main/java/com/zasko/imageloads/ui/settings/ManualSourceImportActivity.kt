package com.zasko.imageloads.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.base.BaseComposeActivity
import com.zasko.imageloads.components.SourceLocalDataStore
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.ImageLoadsTopBar
import com.zasko.imageloads.ui.common.DynamicSourceStore
import org.json.JSONArray
import org.json.JSONObject

class ManualSourceImportActivity : BaseComposeActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ManualSourceImportActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageLoadsTheme {
                ManualSourceImportScreen(
                    onBack = ::finish,
                    onSave = ::saveSource,
                )
            }
        }
    }

    private fun saveSource(draft: ManualSourceDraft): Boolean {
        val key = with(DynamicSourceStore) {
            draft.key.normalizeSourceKey()
        }
        val title = draft.title.trim()
        val baseUrl = draft.baseUrl.trim()
        if (key.isBlank()) {
            showToast("请填写来源 Key")
            return false
        }
        if (DynamicSourceStore.isBuiltInKey(key = key)) {
            showToast("不能覆盖内置来源")
            return false
        }
        if (title.isBlank()) {
            showToast("请填写来源名称")
            return false
        }
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            showToast("主页地址需要以 http:// 或 https:// 开头")
            return false
        }

        return runCatching {
            val sourceJson = draft.toSourceJson(key = key, title = title, baseUrl = baseUrl)
            val (targetId, normalizedJson) = DynamicSourceStore.normalizeSourceJson(
                rawKey = key,
                sourceJson = sourceJson,
            )
            SourceLocalDataStore.saveSourceJson(targetId = targetId, sourceJson = normalizedJson)
            showToast("已保存来源：$title")
        }.isSuccess.also { success ->
            if (!success) {
                showToast("保存失败")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

private data class ManualSourceDraft(
    val key: String,
    val title: String,
    val baseUrl: String,
    val cover: String,
    val listFirstPage: String,
    val listNextPage: String,
    val listItemSelector: String,
    val listDetailLinkSelector: String,
    val listCoverSelector: String,
    val listCoverAttrs: String,
    val listTitleSelector: String,
    val listTitleAttrs: String,
    val listNextPageSelector: String,
    val listNextPageValue: String,
    val detailTitleSelector: String,
    val detailDateSelector: String,
    val detailTagSelector: String,
    val detailContentSelector: String,
    val detailImageSelector: String,
    val detailImageAttrs: String,
    val detailImageLinkSelector: String,
    val detailImageAttr: String,
    val detailPageContainerSelector: String,
    val detailPageLinkSelector: String,
    val detailNextPageValue: String,
)

@Composable
private fun ManualSourceImportScreen(
    onBack: () -> Unit,
    onSave: (ManualSourceDraft) -> Boolean,
) {
    var key by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var baseUrl by rememberSaveable { mutableStateOf("") }
    var cover by rememberSaveable { mutableStateOf("") }
    var listFirstPage by rememberSaveable { mutableStateOf("{baseUrl}") }
    var listNextPage by rememberSaveable { mutableStateOf("{baseUrl}/page/{page}") }
    var listItemSelector by rememberSaveable { mutableStateOf("article") }
    var listDetailLinkSelector by rememberSaveable { mutableStateOf("a[href]") }
    var listCoverSelector by rememberSaveable { mutableStateOf("img[src], img[data-src], img[data-lazy-src]") }
    var listCoverAttrs by rememberSaveable { mutableStateOf("src, data-src, data-lazy-src") }
    var listTitleSelector by rememberSaveable { mutableStateOf("h2 a, h2, .entry-title a, .entry-title") }
    var listTitleAttrs by rememberSaveable { mutableStateOf("img.alt, a.text") }
    var listNextPageSelector by rememberSaveable { mutableStateOf("") }
    var listNextPageValue by rememberSaveable { mutableStateOf("manual page + 1") }
    var detailTitleSelector by rememberSaveable { mutableStateOf("h1, .entry-title") }
    var detailDateSelector by rememberSaveable { mutableStateOf("") }
    var detailTagSelector by rememberSaveable { mutableStateOf("") }
    var detailContentSelector by rememberSaveable { mutableStateOf("article, .entry-content, body") }
    var detailImageSelector by rememberSaveable { mutableStateOf("img[src], img[data-src], img[data-lazy-src]") }
    var detailImageAttrs by rememberSaveable { mutableStateOf("src, data-src, data-lazy-src") }
    var detailImageLinkSelector by rememberSaveable { mutableStateOf("") }
    var detailImageAttr by rememberSaveable { mutableStateOf("href") }
    var detailPageContainerSelector by rememberSaveable { mutableStateOf("") }
    var detailPageLinkSelector by rememberSaveable { mutableStateOf("a[href]") }
    var detailNextPageValue by rememberSaveable { mutableStateOf("none") }

    fun currentDraft(): ManualSourceDraft {
        return ManualSourceDraft(
            key = key,
            title = title,
            baseUrl = baseUrl,
            cover = cover,
            listFirstPage = listFirstPage,
            listNextPage = listNextPage,
            listItemSelector = listItemSelector,
            listDetailLinkSelector = listDetailLinkSelector,
            listCoverSelector = listCoverSelector,
            listCoverAttrs = listCoverAttrs,
            listTitleSelector = listTitleSelector,
            listTitleAttrs = listTitleAttrs,
            listNextPageSelector = listNextPageSelector,
            listNextPageValue = listNextPageValue,
            detailTitleSelector = detailTitleSelector,
            detailDateSelector = detailDateSelector,
            detailTagSelector = detailTagSelector,
            detailContentSelector = detailContentSelector,
            detailImageSelector = detailImageSelector,
            detailImageAttrs = detailImageAttrs,
            detailImageLinkSelector = detailImageLinkSelector,
            detailImageAttr = detailImageAttr,
            detailPageContainerSelector = detailPageContainerSelector,
            detailPageLinkSelector = detailPageLinkSelector,
            detailNextPageValue = detailNextPageValue,
        )
    }

    fun saveAndBack() {
        if (onSave(currentDraft())) {
            onBack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ImageLoadsTopBar(
                title = "手动添加来源",
                onBack = onBack,
                actions = {
                    TextButton(onClick = ::saveAndBack) {
                        Text(text = "保存")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "保存后会作为动态来源显示在首页；Key 已存在时会覆盖原来源。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            item {
                ManualSectionTitle(text = "来源信息")
            }
            item {
                ManualTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = "来源 Key",
                    placeholder = "例如 my_source",
                    singleLine = true,
                )
            }
            item {
                ManualTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "来源名称",
                    placeholder = "首页显示的名称",
                    singleLine = true,
                )
            }
            item {
                ManualTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = "主页地址",
                    placeholder = "https://example.com/",
                    keyboardType = KeyboardType.Uri,
                    singleLine = true,
                )
            }
            item {
                ManualTextField(
                    value = cover,
                    onValueChange = { cover = it },
                    label = "首页封面 URL",
                    placeholder = "可留空，获取列表后会自动刷新",
                    keyboardType = KeyboardType.Uri,
                    singleLine = true,
                )
            }
            item {
                ManualSectionTitle(text = "封面列表页面")
            }
            item {
                ManualTextField(
                    value = listFirstPage,
                    onValueChange = { listFirstPage = it },
                    label = "第一页链接模板",
                    placeholder = "{baseUrl}",
                    singleLine = false,
                )
            }
            item {
                ManualTextField(
                    value = listNextPage,
                    onValueChange = { listNextPage = it },
                    label = "下一页链接模板",
                    placeholder = "{baseUrl}/page/{page}",
                    singleLine = false,
                )
            }
            item {
                ManualTextField(
                    value = listItemSelector,
                    onValueChange = { listItemSelector = it },
                    label = "列表 item 选择器",
                    placeholder = "article",
                    singleLine = false,
                )
            }
            item {
                ManualTextField(
                    value = listDetailLinkSelector,
                    onValueChange = { listDetailLinkSelector = it },
                    label = "详情链接选择器",
                    placeholder = "a[href]",
                    singleLine = false,
                )
            }
            item {
                ManualTextField(
                    value = listCoverSelector,
                    onValueChange = { listCoverSelector = it },
                    label = "封面图选择器",
                    placeholder = "img[src]",
                    singleLine = false,
                )
            }
            item {
                ManualTextField(
                    value = listCoverAttrs,
                    onValueChange = { listCoverAttrs = it },
                    label = "封面 URL 属性顺序",
                    placeholder = "src, data-src, data-lazy-src",
                    singleLine = false,
                )
            }
            item {
                ManualTextField(
                    value = listTitleSelector,
                    onValueChange = { listTitleSelector = it },
                    label = "标题选择器",
                    placeholder = "h2 a, .entry-title",
                    singleLine = false,
                )
            }
            item {
                ManualTextField(
                    value = listTitleAttrs,
                    onValueChange = { listTitleAttrs = it },
                    label = "标题属性兜底",
                    placeholder = "img.alt, a.text",
                    singleLine = false,
                )
            }
            item {
                ManualSectionTitle(text = "封面列表分页")
            }
            item {
                ManualTextField(
                    value = listNextPageSelector,
                    onValueChange = { listNextPageSelector = it },
                    label = "下一页按钮选择器",
                    placeholder = "可留空，使用链接模板分页",
                    singleLine = false,
                )
            }
            item {
                ManualTextField(
                    value = listNextPageValue,
                    onValueChange = { listNextPageValue = it },
                    label = "下一页判断",
                    placeholder = "manual page + 1 或 none",
                    singleLine = false,
                )
            }
            item {
                ManualSectionTitle(text = "详情页面")
            }
            item {
                ManualTextField(
                    value = detailTitleSelector,
                    onValueChange = { detailTitleSelector = it },
                    label = "详情标题选择器",
                    placeholder = "h1",
                    singleLine = false,
                )
            }
            item {
                ManualTextField(
                    value = detailDateSelector,
                    onValueChange = { detailDateSelector = it },
                    label = "日期选择器",
                    placeholder = "可留空",
                    singleLine = false,
                )
            }
            item {
                ManualTextField(
                    value = detailTagSelector,
                    onValueChange = { detailTagSelector = it },
                    label = "标签选择器",
                    placeholder = "可留空",
                    singleLine = false,
                )
            }
            item {
                ManualTextField(
                    value = detailContentSelector,
                    onValueChange = { detailContentSelector = it },
                    label = "详情内容容器选择器",
                    placeholder = "article, .entry-content, body",
                    singleLine = false,
                )
            }
            item {
                ManualTextField(
                    value = detailImageSelector,
                    onValueChange = { detailImageSelector = it },
                    label = "详情图片选择器",
                    placeholder = "img[src]",
                    singleLine = false,
                )
            }
            item {
                ManualTextField(
                    value = detailImageAttrs,
                    onValueChange = { detailImageAttrs = it },
                    label = "详情图片 URL 属性顺序",
                    placeholder = "src, data-src, data-lazy-src",
                    singleLine = false,
                )
            }
            item {
                ManualTextField(
                    value = detailImageLinkSelector,
                    onValueChange = { detailImageLinkSelector = it },
                    label = "原图链接选择器",
                    placeholder = "可留空，例如 a[data-fancybox][href]",
                    singleLine = false,
                )
            }
            item {
                ManualTextField(
                    value = detailImageAttr,
                    onValueChange = { detailImageAttr = it },
                    label = "原图链接属性",
                    placeholder = "href",
                    singleLine = false,
                )
            }
            item {
                ManualSectionTitle(text = "详情分页")
            }
            item {
                ManualTextField(
                    value = detailPageContainerSelector,
                    onValueChange = { detailPageContainerSelector = it },
                    label = "分页容器选择器",
                    placeholder = "可留空",
                    singleLine = false,
                )
            }
            item {
                ManualTextField(
                    value = detailPageLinkSelector,
                    onValueChange = { detailPageLinkSelector = it },
                    label = "分页链接选择器",
                    placeholder = "a[href]",
                    singleLine = false,
                )
            }
            item {
                ManualTextField(
                    value = detailNextPageValue,
                    onValueChange = { detailNextPageValue = it },
                    label = "详情下一页判断",
                    placeholder = "none 或 next numeric page url",
                    singleLine = false,
                )
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = ::saveAndBack,
                ) {
                    Text(text = "保存并完成")
                }
            }
        }
    }
}

@Composable
private fun ManualSectionTitle(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ManualTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    singleLine: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(text = label)
        },
        placeholder = {
            Text(text = placeholder)
        },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        maxLines = if (singleLine) 1 else 5,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
    )
}

private fun ManualSourceDraft.toSourceJson(
    key: String,
    title: String,
    baseUrl: String,
): JSONObject {
    return JSONObject()
        .put("key", key)
        .put("type", with(DynamicSourceStore) { key.toDynamicSourceType() })
        .put("title", title)
        .put("cover", cover.trim())
        .put("baseUrl", baseUrl)
        .put("processMethods", toProcessMethodsJson(sourceKey = key, baseUrl = baseUrl))
}

private fun ManualSourceDraft.toProcessMethodsJson(sourceKey: String, baseUrl: String): JSONObject {
    return JSONObject()
        .put(
            "parser",
            JSONObject()
                .put("name", "MJson")
                .put("entry", "MJson.parse(html)")
                .put("engine", "Jsoup.parse(html)")
                .put("description", "把 HTML 字符串转换成 Jsoup Document，后续列表、分页、详情都基于 CSS selector 解析"),
        )
        .put("list", toListProcessJson(sourceKey = sourceKey, baseUrl = baseUrl))
        .put("detail", toDetailProcessJson(sourceKey = sourceKey))
}

private fun ManualSourceDraft.toListProcessJson(sourceKey: String, baseUrl: String): JSONObject {
    return JSONObject()
        .put("request", JSONObject().put("homeUrl", baseUrl))
        .put(
            "pageUrl",
            JSONObject()
                .put("firstPage", listFirstPage.trim().ifBlank { "{baseUrl}" })
                .put("nextPage", listNextPage.trim().ifBlank { listFirstPage.trim().ifBlank { "{baseUrl}" } }),
        )
        .put(
            "parse",
            JSONObject()
                .put("itemSelector", listItemSelector.trim())
                .put("detailLinkSelector", listDetailLinkSelector.trim())
                .put("coverSelector", listCoverSelector.trim())
                .put("coverAttrOrder", listCoverAttrs.toStringJsonArray())
                .put("titleSelector", listTitleSelector.trim())
                .put("titleAttrOrder", listTitleAttrs.toStringJsonArray()),
        )
        .put(
            "pagination",
            JSONObject()
                .put("nextPageSelector", listNextPageSelector.trim())
                .put("nextPageValue", listNextPageValue.trim().ifBlank { "none" }),
        )
        .put("cache", JSONObject().put("pageDir", "privateHtml/$sourceKey/{page}"))
}

private fun ManualSourceDraft.toDetailProcessJson(sourceKey: String): JSONObject {
    return JSONObject()
        .put("request", JSONObject().put("detailUrl", "list.item.href"))
        .put(
            "parse",
            JSONObject()
                .put("titleSelector", detailTitleSelector.trim())
                .put("dateSelector", detailDateSelector.trim())
                .put("tagSelector", detailTagSelector.trim())
                .put("contentSelector", detailContentSelector.trim())
                .put("imageSelector", detailImageSelector.trim())
                .put("imageAttrOrder", detailImageAttrs.toStringJsonArray())
                .put("imageLinkSelector", detailImageLinkSelector.trim())
                .put("imageAttr", detailImageAttr.trim().ifBlank { "href" }),
        )
        .put(
            "pagination",
            JSONObject()
                .put("containerSelector", detailPageContainerSelector.trim())
                .put("linkSelector", detailPageLinkSelector.trim().ifBlank { "a[href]" })
                .put("nextPageValue", detailNextPageValue.trim().ifBlank { "none" }),
        )
        .put("cache", JSONObject().put("detailDir", "privateHtml/$sourceKey/detail/{detailCacheFileName}"))
}

private fun String.toStringJsonArray(): JSONArray {
    return JSONArray(
        split(Regex("""[,，\n]"""))
            .map { it.trim() }
            .filter { it.isNotBlank() },
    )
}

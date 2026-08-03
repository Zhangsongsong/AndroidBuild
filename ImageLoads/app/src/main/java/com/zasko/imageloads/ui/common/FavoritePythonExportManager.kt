package com.zasko.imageloads.ui.common

import com.zasko.imageloads.components.HttpHeaderConfigStore
import com.zasko.imageloads.components.HttpHeaderItem
import com.zasko.imageloads.components.SourceLocalDataStore
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.ui.meizi5.Meizi5FavoriteStore
import com.zasko.imageloads.ui.taotu.TaoTuFavoriteStore
import com.zasko.imageloads.ui.trendszine.TrendszineFavoriteStore
import com.zasko.imageloads.utils.Constants
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

object FavoritePythonExportManager {

    private const val VERSION = 1
    private const val KEY_ALL = "all"
    private const val KEY_PARSER = "parser"
    private const val KEY_LIST = "list"
    private const val KEY_DETAIL = "detail"
    private const val KEY_CACHE = "cache"

    fun createExportFileName(source: FavoriteBackupSource): String {
        return "imageloads_favorites_${source.key}.py"
    }

    fun createPython(source: FavoriteBackupSource): String {
        val payload = createPayloadJson(source = source)
        val encodedPayload = Base64.getEncoder()
            .encodeToString(payload.toString().toByteArray(Charsets.UTF_8))
        return PYTHON_TEMPLATE.replace("__PAYLOAD_BASE64__", encodedPayload)
    }

    private fun createPayloadJson(source: FavoriteBackupSource): JSONObject {
        val sources = JSONObject()
        exportSources(source = source).forEach { exportSource ->
            sources.put(exportSource.key, exportSource.toPythonSourceJson())
        }
        return JSONObject()
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("sources", sources)
    }

    private fun exportSources(source: FavoriteBackupSource): List<FavoriteBackupSource> {
        return if (source.key == KEY_ALL) {
            FavoriteBackupManager.sourceOptions.filterNot { it.key == KEY_ALL }
        } else {
            listOf(source)
        }
    }

    private fun FavoriteBackupSource.toPythonSourceJson(): JSONObject {
        val baseUrl = baseUrl()
        return JSONObject()
            .put("type", type)
            .put("key", key)
            .put("title", title)
            .put("baseUrl", baseUrl)
            .put("downloadDir", key)
            .put("headers", HttpHeaderConfigStore.getHeadersForUrl(url = baseUrl).toHeadersJson())
            .put("processMethods", sourceProcessMethods().toPythonMethodsJson())
            .put("favorites", getFavorites(sourceType = type).toFavoritesJson())
    }

    private fun FavoriteBackupSource.baseUrl(): String {
        if (baseUrl.isNotBlank()) {
            return baseUrl
        }
        return when (type) {
            Constants.THEME_TYPE_TRENDSZINE -> "https://trendszine.com/"
            Constants.THEME_TYPE_MEIZI5 -> "https://meizi5.com/"
            Constants.THEME_TYPE_TAOTU -> "https://taotu.org/"
            else -> ""
        }
    }

    private fun getFavorites(sourceType: Int): List<ImageLoadsInfo> {
        return when (sourceType) {
            Constants.THEME_TYPE_TRENDSZINE -> TrendszineFavoriteStore.getFavorites()
            Constants.THEME_TYPE_MEIZI5 -> Meizi5FavoriteStore.getFavorites()
            Constants.THEME_TYPE_TAOTU -> TaoTuFavoriteStore.getFavorites()
            else -> {
                FavoriteBackupManager.sourceOptions.firstOrNull { it.type == sourceType }
                    ?.let { SourceLocalDataStore.getFavorites(targetId = it.key, defaultSourceType = it.type) }
                    .orEmpty()
            }
        }
    }

    private fun FavoriteBackupSource.sourceProcessMethods(): JSONObject {
        return when (type) {
            Constants.THEME_TYPE_TRENDSZINE,
            Constants.THEME_TYPE_MEIZI5,
            Constants.THEME_TYPE_TAOTU,
            -> SourceProcessMethodStore.getOrCacheMethods(sourceType = type)

            else -> SourceLocalDataStore.getProcessMethods(targetId = key) ?: JSONObject()
        }
    }

    private fun List<HttpHeaderItem>.toHeadersJson(): JSONObject {
        val headers = JSONObject()
        forEach { header ->
            val name = header.name.trim()
            val value = header.value.trim()
            if (name.isNotBlank() && value.isNotBlank()) {
                headers.put(name, value)
            }
        }
        return headers
    }

    private fun JSONObject.toPythonMethodsJson(): JSONObject {
        return JSONObject(toString()).apply {
            remove(KEY_PARSER)
            optJSONObject(KEY_LIST)?.remove(KEY_CACHE)
            optJSONObject(KEY_DETAIL)?.remove(KEY_CACHE)
        }
    }

    private fun List<ImageLoadsInfo>.toFavoritesJson(): JSONArray {
        val array = JSONArray()
        distinctBy { it.href.ifBlank { it.url } }.forEach { info ->
            array.put(
                JSONObject()
                    .put("url", info.url)
                    .put("href", info.href)
                    .put("title", info.title)
                    .put("width", info.width)
                    .put("height", info.height),
            )
        }
        return array
    }

    private val PYTHON_TEMPLATE = """
        #!/usr/bin/env python3
        # Generated by ImageLoads. Dependencies: pip install requests beautifulsoup4
        import base64
        import json
        import os
        import re
        import time
        from pathlib import Path
        from urllib.parse import unquote, urljoin, urlparse

        import requests
        from bs4 import BeautifulSoup

        MAX_DETAIL_PAGE_COUNT = 50
        OUTPUT_ROOT = Path("imageloads_favorites")
        PAYLOAD = json.loads(base64.b64decode("__PAYLOAD_BASE64__").decode("utf-8"))


        def safe_select(node, selector):
            if not selector:
                return []
            try:
                return node.select(selector)
            except Exception as exc:
                print(f"selector failed: {selector} {exc}")
                return []


        def safe_select_one(node, selector):
            items = safe_select(node, selector)
            return items[0] if items else None


        def sanitize_filename(value, fallback="image_detail"):
            value = (value or "").strip()
            value = re.sub(r"[\\/:*?\"<>|]", "_", value)
            value = re.sub(r"\s+", " ", value).strip(" .")
            return value or fallback


        def absolute_url(base_url, raw_url):
            raw_url = (raw_url or "").strip()
            if not raw_url:
                return ""
            return urljoin(base_url, raw_url)


        def extract_attr(node, attr_spec):
            attr_spec = str(attr_spec or "").strip()
            if not attr_spec:
                return ""
            if "." in attr_spec and not attr_spec.startswith("data-"):
                selector, attr_name = attr_spec.rsplit(".", 1)
                selector = selector.strip()
                attr_name = attr_name.strip()
                target = node
                node_name = getattr(node, "name", "") or ""
                if selector and selector.lower() != node_name.lower():
                    target = safe_select_one(node, selector)
                return (target.get(attr_name) if target else "") or ""
            return node.get(attr_spec, "") or ""


        def first_attr(node, attr_order):
            for attr_spec in attr_order or []:
                value = extract_attr(node, attr_spec)
                if value:
                    return value
            return ""


        def unique_urls(urls):
            seen = set()
            result = []
            for url in urls:
                normalized = (url or "").strip()
                if normalized and normalized not in seen:
                    seen.add(normalized)
                    result.append(normalized)
            return result


        def request_text(session, url, headers):
            response = session.get(url, headers=headers, timeout=30)
            response.raise_for_status()
            if not response.encoding:
                response.encoding = response.apparent_encoding
            return response.text


        def merged_headers(source, referer=None, accept=None):
            headers = dict(source.get("headers") or {})
            if referer:
                headers["Referer"] = referer
            if accept:
                headers["Accept"] = accept
            return headers


        def page_number_from_url(url):
            path = urlparse(url).path.rstrip("/")
            tail = path.rsplit("/", 1)[-1]
            try:
                return int(tail)
            except Exception:
                return 1


        def page_number_from_link(link):
            text = link.get_text(strip=True)
            if text.isdigit():
                return int(text)
            href_tail = urlparse(link.get("href", "")).path.rstrip("/").rsplit("/", 1)[-1]
            return int(href_tail) if href_tail.isdigit() else None


        def build_detail_page_url(current_url, next_page):
            trimmed = current_url.rstrip("/")
            tail = trimmed.rsplit("/", 1)[-1]
            if tail.isdigit():
                return trimmed.rsplit("/", 1)[0] + f"/{next_page}"
            return trimmed + f"/{next_page}"


        def find_next_detail_url(source, soup, current_url):
            detail = (source.get("processMethods") or {}).get("detail") or {}
            pagination = detail.get("pagination") or {}
            next_rule = str(pagination.get("nextPageValue") or "").strip().lower()
            if next_rule == "none":
                return ""

            container = soup
            container_selector = pagination.get("containerSelector")
            if container_selector:
                container = safe_select_one(soup, container_selector) or soup

            link_selector = pagination.get("linkSelector") or "a[href]"
            links = safe_select(container, link_selector)
            if not links:
                return ""

            current_page = page_number_from_url(current_url)
            candidates = []
            for link in links:
                href = absolute_url(current_url, link.get("href", ""))
                page = page_number_from_link(link)
                if href and page and page > current_page:
                    candidates.append((page, href))
            if candidates:
                return sorted(candidates, key=lambda item: item[0])[0][1]

            if current_page <= len(links):
                return build_detail_page_url(current_url, current_page + 1)
            return ""


        def parse_detail(source, url, html):
            soup = BeautifulSoup(html, "html.parser")
            detail = (source.get("processMethods") or {}).get("detail") or {}
            parse = detail.get("parse") or {}

            title = ""
            title_selector = parse.get("titleSelector")
            title_node = safe_select_one(soup, title_selector) if title_selector else None
            if title_node:
                title = title_node.get_text(strip=True)

            content = soup
            content_selector = parse.get("contentSelector")
            if content_selector:
                content = safe_select_one(soup, content_selector) or soup

            image_urls = []
            image_link_selector = parse.get("imageLinkSelector")
            if image_link_selector:
                image_attr = parse.get("imageAttr") or "href"
                for node in safe_select(content, image_link_selector):
                    image_urls.append(absolute_url(url, extract_attr(node, image_attr)))

            image_selector = parse.get("imageSelector")
            if image_selector:
                attr_order = parse.get("imageAttrOrder") or ["src", "data-src", "data-lazy-src"]
                for node in safe_select(content, image_selector):
                    image_urls.append(absolute_url(url, first_attr(node, attr_order)))
            elif not image_link_selector:
                for node in safe_select(content, "img[src], img[data-src], img[data-lazy-src]"):
                    image_urls.append(absolute_url(url, first_attr(node, ["src", "data-src", "data-lazy-src"])))

            return {
                "url": url,
                "title": title,
                "images": unique_urls(image_urls),
                "nextPageUrl": find_next_detail_url(source, soup, url),
            }


        def collect_detail_images(session, source, favorite):
            detail_url = (favorite.get("href") or "").strip()
            if not detail_url:
                return {"title": favorite.get("title") or "", "images": []}

            visited = set()
            current_url = detail_url
            title = favorite.get("title") or ""
            images = []
            load_count = 0

            while current_url and current_url not in visited and load_count < MAX_DETAIL_PAGE_COUNT:
                visited.add(current_url)
                html = request_text(session, current_url, merged_headers(source, referer=current_url))
                detail = parse_detail(source, current_url, html)
                title = title or detail.get("title") or ""
                images.extend(detail.get("images") or [])
                current_url = detail.get("nextPageUrl") or ""
                load_count += 1
                time.sleep(0.2)

            return {"title": title, "images": unique_urls(images)}


        def image_file_name(image_url, index):
            path_name = unquote(os.path.basename(urlparse(image_url).path))
            fallback = f"image_{index:04d}.jpg"
            return sanitize_filename(path_name, fallback=fallback)


        def download_image(session, source, image_url, detail_url, dest_file):
            if dest_file.exists() and dest_file.stat().st_size > 0:
                return "skip"
            headers = merged_headers(source, referer=detail_url, accept="*/*")
            with session.get(image_url, headers=headers, stream=True, timeout=60) as response:
                response.raise_for_status()
                with dest_file.open("wb") as output:
                    for chunk in response.iter_content(chunk_size=1024 * 64):
                        if chunk:
                            output.write(chunk)
            return "saved"


        def download_favorite(session, source, favorite):
            detail_url = (favorite.get("href") or "").strip()
            detail = collect_detail_images(session, source, favorite)
            title = detail.get("title") or favorite.get("title") or detail_url.rstrip("/").rsplit("/", 1)[-1]
            images = detail.get("images") or []
            item_dir = OUTPUT_ROOT / source.get("downloadDir", source.get("key", "source")) / sanitize_filename(title)
            item_dir.mkdir(parents=True, exist_ok=True)

            saved_count = 0
            skipped_count = 0
            for index, image_url in enumerate(images, start=1):
                dest_file = item_dir / image_file_name(image_url, index)
                try:
                    result = download_image(session, source, image_url, detail_url, dest_file)
                    if result == "saved":
                        saved_count += 1
                    else:
                        skipped_count += 1
                    print(f"  [{index}/{len(images)}] {result}: {dest_file.name}")
                except Exception as exc:
                    print(f"  [{index}/{len(images)}] failed: {image_url} {exc}")
            return saved_count, skipped_count, len(images)


        def main():
            OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
            session = requests.Session()
            total_saved = 0
            total_skipped = 0
            total_images = 0

            for source_key, source in (PAYLOAD.get("sources") or {}).items():
                favorites = source.get("favorites") or []
                print(f"{source.get('title', source_key)} favorites: {len(favorites)}")
                for item_index, favorite in enumerate(favorites, start=1):
                    title = favorite.get("title") or favorite.get("href") or favorite.get("url") or f"item-{item_index}"
                    print(f"[{item_index}/{len(favorites)}] {title}")
                    try:
                        saved, skipped, image_count = download_favorite(session, source, favorite)
                        total_saved += saved
                        total_skipped += skipped
                        total_images += image_count
                    except Exception as exc:
                        print(f"  failed item: {exc}")
                print()

            print(f"done. images={total_images}, saved={total_saved}, skipped={total_skipped}, output={OUTPUT_ROOT.resolve()}")


        if __name__ == "__main__":
            main()
    """.trimIndent() + "\n"
}

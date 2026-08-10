# ImageLoads APK 与使用说明

## APK 信息

- App 名称：ImageLoads
- 包名：`com.zasko.imageloads`
- 当前构建类型：`release`
- 版本号：`1.0`
- versionCode：`1`
- minSdk：`25`
- APK 文件：`ImageLoads/app/build/outputs/apk/release/app-release.apk`
- APK 大小：约 `12.5 MB`
- 构建时间：`2026-08-10 12:17:04 CST`

当前 release 包使用 debug keystore 签名，目的是方便本地安装和运行；它仍然是 release 构建，默认 `debuggable=false`。

## 安装方式

在 `/Users/songzhouzhang/Mine/Github/AndroidBuild` 目录下执行：

```bash
adb install -r ImageLoads/app/build/outputs/apk/release/app-release.apk
```

如果设备上已经安装了不同签名的同包名应用，需要先卸载旧包：

```bash
adb uninstall com.zasko.imageloads
adb install -r ImageLoads/app/build/outputs/apk/release/app-release.apk
```

## 使用文档位置

完整 App 使用说明在：

```text
ImageLoads/APP_USAGE.md
```

界面截图在：

```text
ImageLoads/docs/images/
```

## App 使用摘要

ImageLoads 是一个按“来源”浏览图片列表、查看详情、收藏和下载整组图片的 Android App。当前首页主要展示已经导入或手动添加的动态来源；如果首页为空，需要先导入来源 JSON，或通过“手动添加”创建来源。

### 首次使用

1. 打开 App 后进入首页。
2. 首页为空时，点击左上角菜单。
3. 在“导入数据”中选择“JSON数据”导入来源 JSON，或选择“手动添加”创建来源。
4. 浏览网络内容需要设备联网。
5. 下载图片时按系统提示授予存储权限；Android 11 及以上可能需要授予“管理所有文件”权限。

### 首页功能

- 点击来源封面或“打开”：进入该来源图片列表。
- 点击爱心按钮：进入该来源收藏列表。
- 点击下载按钮：查看已下载图片分组。
- “本地 / 网络”：切换列表和详情数据来源。
- “公共 Header”：控制该来源请求网络时是否合并公共 Header。
- 删除按钮：删除来源定义、收藏记录和本地 HTML 缓存；公共下载目录中的图片不会被删除。

### 导入来源

- “JSON数据”：粘贴来源 JSON，或选择 `.json` 文件后导入。
- “手动添加”：填写来源 Key、名称、主页地址、封面 URL，以及列表页、详情页 CSS 选择器配置。
- 来源 Key 会规范化为小写，只保留字母、数字、下划线和短横线。
- 不能覆盖内置来源 Key：`trendszine`、`meizi5`、`taotu`、`xiuren`。

### 浏览、收藏和下载

- 图片列表为双列瀑布流，接近底部会自动加载下一页。
- 右上角更多菜单支持“打开网页”和“跳转页码”。
- 点击图片进入详情页。
- 详情页可收藏图集、预览大图、下载整组图片。
- 收藏列表支持单条下载和批量下载全部未下载收藏。
- 已下载过的图集会被识别；再次下载时会提示是否覆盖。

### Header 与实验室

- “实验室”用于调试来源处理方法和 Header。
- 测试包的公共 Header 默认包含 User-Agent、Accept、Accept-Language。
- 正式包默认不展示公共 Header 的内置数据。
- 来源 Header 中同名项会覆盖公共 Header。
- 如果列表或详情解析失败，优先检查目标网页是否可访问、Header 是否正确、CSS selector 是否仍匹配页面结构。

## 本地文件位置

下载图片保存到公共存储：

```text
/storage/emulated/0/ImageLoads/download/{sourceKey}/detail/{图集标题}/
```

App 缓存 HTML 保存到应用外部私有目录：

```text
/storage/emulated/0/Android/data/com.zasko.imageloads/files/html/{sourceKey}/
```

删除首页来源不会删除公共下载目录中的图片；在“已下载”页面删除分组会删除对应本地图片文件。

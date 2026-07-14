# 为 Downmatica 添加自定义原理图源

> 本文档面向 **Fabric 模组开发者**，介绍如何为 Downmatica 扩展或集成新的原理图源。

## 1. 引言

Downmatica 提供了灵活的 API，允许开发者通过实现 `SchematicAcquirer` 接口来添加自定义原理图源。
您既可以将新源直接贡献至 Downmatica 本体，也可以开发独立的扩展模组。
本文档将指导您完成从依赖配置到实现、注册的全流程。

## 2. 选择集成方式

您有两种方式将自定义源集成到 Downmatica 中：

| 方式 | 适用场景 |
|-|-|
|**直接并入 Downmatica 本体**|您仅添加一个或多个原理图源，且不修改其他功能。推荐向 [Downmatica GitHub 仓库](https://github.com/aji110905/Downmatica) 提交 Pull Request。|
|**开发独立扩展模组**|您需要修改或增强 Downmatica 的其他行为，或希望保持独立维护。扩展模组可以自由选择版本和发布周期。|

> 💡 如果您认为 Downmatica 本体在功能或接口上有所欠缺，也欢迎通过 Issue 或 PR 提出改进建议。

## 3. 配置 Gradle 依赖

如果选择独立开发扩展模组，需要添加 Gradle 依赖。

### 3.1 添加 Maven 仓库

在 `build.gradle`（或 `settings.gradle` 的 `pluginManagement`/`dependencyResolutionManagement` 块）中添加：

```gradle
repositories {
    maven {
        url = "https://maven.ajitech.top/releases"
    }
    	maven {
        url = "https://maven.ajitech.top/releases"
    }
	maven {
		url = "https://masa.dy.fi/maven/sakura-ryoko"
	}
	maven {
		url = "https://maven.fallenbreath.me/releases"
	}
	maven {
		url = "https://maven.terraformersmc.com/"
	}
	maven {
		url = 'https://maven.fabricmc.net/'
	}
}
```

### 3.2 添加依赖

在 `dependencies` 块中引用 Downmatica，请将 `<minecraft_version>` 替换为您的目标 Minecraft 版本（例如 `1.21.11`）：

```gradle
dependencies {
    //对于 Minecraft 26.1 及以上（未混淆环境），应该使用`implementation`。
    modImplementation "top.ajitech:downmatica:v1.0.0-mc<minecraft_version>"
}
```

## 4. 实现原理图获取器

自定义源的核心是实现 `top.ajitech.downmatica.api.SchematicAcquirer` 接口。该接口是函数式接口，仅包含一个抽象方法：

```java
Collection<Schematic> getSchematics();
```

### 4.1 接口契约

- `getSchematics()` 在每次打开 Downmatica 下载界面时被调用。
- Downmatica 会为每个获取器启动 **虚拟线程** 异步执行，因此该方法可以执行耗时网络操作。
- 返回的 `Schematic` 对象可以通过 `Schematic.builder()` 构建。
- 您还可以通过 `onDetailButtonClicked()` 设置点击“详情”按钮时的回调（如打开网页）。

> 完整的 API 说明请查阅 `top.ajitech.downmatica.api` 包中的 Javadoc。

### 4.2 示例实现

以下示例展示如何从 HTTP API 获取原理图列表并转换为 `Schematic` 对象：

```java
package top.ajitech.downmatica.example;

import top.ajitech.downmatica.Downmatica;
import top.ajitech.downmatica.api.Schematic;
import top.ajitech.downmatica.api.SchematicAcquirer;
import top.ajitech.downmatica.util.CompatibleUtil;
import top.ajitech.downmatica.util.URIBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.dy.masa.malilib.util.StringUtils;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;

public class ExampleSchematicAcquirer implements SchematicAcquirer {

    @Override
    public Collection<Schematic> getSchematics() {
        ArrayList<Schematic> result = new ArrayList<>();
        try {
            // 构建请求 URI
            URI uri = URIBuilder.create("https://example.com/api/schematics").build();
            HttpRequest request = HttpRequest.newBuilder().uri(uri).build();
            HttpResponse<String> response = Downmatica.HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            // 解析 JSON 响应
            JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            for (JsonElement element : jsonObject.get("data").getAsJsonArray()) {
                JsonObject object = element.getAsJsonObject();
                Schematic schematic = Schematic.builder()
                        .source(StringUtils.translate("downmatica.source.example"))  // 源名称（翻译键）
                        .title(object.get("title").getAsString())
                        .author(object.get("author").getAsString())
                        .description(object.get("description").getAsString())
                        .downloadFileInfo(object.get("file_name").getAsString(), 
                                          URI.create(object.get("file_url").getAsString()))
                        .onDetailButtonClicked(() -> CompatibleUtil.openUri(
                                URI.create(object.get("detail_url").getAsString())))
                        .build();
                result.add(schematic);
            }
        } catch (Exception e) {
            Downmatica.LOGGER.error("Failed to get schematics from Example source", e);
        }
        return result;
    }
}
```

### 4.3 注册获取器

在您的模组入口点（`ModInitializer#onInitialize`）中调用 `SchematicAcquirer.register()` 注册实例：

```java
@Override
public void onInitialize() {
    SchematicAcquirer.register(new ExampleSchematicAcquirer());
}
```

> 注册应尽早执行（入口点内），以确保界面打开时获取器已就绪。

## 5. 本地化支持（翻译键）

如果您的源名称需要支持多语言，请为每个语言文件添加对应的翻译条目。

- **翻译键格式**：`downmatica.source.<your_source_id>`，其中 `<your_source_id>` 使用小写字母和下划线（例如 `example`）。
- **至少提供 `en_us` 翻译**。

示例 `assets/downmatica/lang/en_us.json`：

```json
{
  "downmatica.source.example": "Example"
}
```

只有添加进 Downmatica 本体才需要遵守该规则。

## 6. 注意事项

- **异常处理**：`getSchematics()` 内部应妥善捕获异常，避免影响其他获取器。建议使用日志记录错误。
- **兼容性**：需要打开网页推荐使用 `top.ajitech.downmatica.util.CompatibleUtil#openUri()` ，因为在不同的Minecraft 版本中，打开网页的方式有差异。
- **HttpClient**：请使用 `top.ajitech.downmatica.core.HttpClientContainer#get()` 获取 HttpClient 实例，该实例受用户配置。

如果有任何问题，欢迎通过 Issue 或 Pull Request 与我们交流！
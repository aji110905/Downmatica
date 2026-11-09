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

## 4. 分页与搜索模型

Downmatica 的下载界面是**分页**的：每个界面页固定显示 20 条条目。所有已注册源的条目按**注册顺序**拼成一条统一的长表——前一个源的条目排完后接下一个源，一页内不足 20 条时由下一个源补齐。总页数按各源报告的条目总数之和除以 20 计算。

搜索框的关键词会传给每个源（注意：界面传入的关键词已被转为小写）。

您的获取器不需要关心页码和切片——管理器只会问您两个问题："你一共有多少条？"、"还能再给我一批吗？"，并把您交付的批次原样追加到长表上，按需切片显示。数据量小或者后端不分页的源，继承现成的抽象类即可（见 5.1）；数据量大、后端本身分页的源，请直接实现接口做**惰性流式交付**（见 5.2），翻页翻到哪里才请求到哪里，绝不整站拉取。

## 5. 实现原理图获取器

### 5.1 简单方式：继承 `CachedListSchematicAcquirer`

如果您的源"一次性就能拿到全部条目"（一份 JSON 清单之类），直接继承 `top.ajitech.downmatica.api.impl.CachedListSchematicAcquirer`，只需实现一个方法：

```java
protected abstract Collection<Schematic> getSchematics();
```

父类负责其余全部协议逻辑：只调用您一次并缓存、在本地按关键词过滤（来源/标题/作者/描述任一字段包含即命中，不区分大小写）、总数按过滤后条数计算、迭代器状态复位。

> ⚠️ 本地过滤要求条目的各字段在构造时就绪、取值无副作用。描述需要再发一次请求才能拿到的"懒加载条目"**不要**用这个基类——过滤一千个条目就会打爆详情接口。这类源请用 5.2，把关键词交给服务端搜索。

### 5.2 完整方式：实现 `SchematicAcquirer` 接口

接口共四个方法，构成一个**批次迭代器**协议：

```java
public interface SchematicAcquirer {
    void setKeyword(String keyword);   // 关键词变化时调用一次（空串表示不过滤；不会为null）
    int getTotalSchematic();           // 当前关键词下的条目总数，用于计算总页数
    Collection<Schematic> getNext();   // 交付下一批条目（只在 hasNext() 为真时被调用）
    boolean hasNext();                 // 本轮还有没有可交付的批次
}
```

管理器的使用顺序是：

1. 关键词变化时调用一次 `setKeyword`（**必须**把"可交付"状态复位，让 `hasNext` 重新为真；关键词字段要初始化为空串，不能假设 `setKeyword` 一定先被调用）。
2. 每次查询先读 `getTotalSchematic` 算总页数。有关键词时，总数必须是**过滤后**的条数，否则"共 M 页"会虚高。
3. 长表不够填满当前页时，反复"先问 `hasNext`，为真才调 `getNext`"，直到够一页；交付完或明确拿不动了就返回 `false`。

关键契约与红线：

- `hasNext()` **绝对不能恒返回 false**——那样 `getNext()` 永远不会被调用，您的源在界面上会表现为"不存在"。
- `getNext()` 返回**空批次**会被管理器视为"这个源本轮不再交付"并换下一个源（这是防止坏源死循环的守卫）。分页型实现建议这样写状态：`hasNext = nextPage <= totalPages`，拉到失败/拉空时直接把剩余页数清零。
- 后端总数如果是估计值（搜索引擎常见），按它多算出来的尾页会拉到空批次并被自动跳过，可以接受。
- **任何方法都不要向外抛异常**。管理器和界面目前不会替您兜底：一个异常会杀死本次后台加载线程，界面会一直卡在"正在加载..."。请在实现内部捕获并记日志，失败时安静地交出剩余数据（或空批次）。
- 这些方法都在虚拟线程上被调用（界面不冻结），可以执行耗时网络操作；但管理器对全部源的调用持有同一把锁，请不要在实现里长时间阻塞（例如自己再起线程等网络然后 join）。
- 您**不需要**知道当前页码，也不能自己乱序交付：批次顺序就是长表顺序。

`Schematic` 对象通过 `Schematic.builder()` 构建；`onDetailButtonClicked()` 可设置"详情"按钮回调（如打开网页）。`getSource()` 建议填翻译键（见第 6 节）。

### 5.3 示例：分页型源

```java
package top.ajitech.downmatica.example;

import top.ajitech.downmatica.DownmaticaMod;
import top.ajitech.downmatica.api.HttpClientGetter;
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
import java.util.List;

public class ExampleSchematicAcquirer implements SchematicAcquirer {

    private static final int PAGE_SIZE = 24; //每次向后端请求的条目数，取后端允许的上限即可

    private String keyword = "";        //必须初始化为空串
    private String probedKeyword = null; //针对哪个关键词探过总数
    private int totalSchematic = 0;
    private int nextPage = 1;
    private int maxPages = 0;

    @Override
    public void setKeyword(String keyword) {
        this.keyword = keyword;
        probedKeyword = null; //换关键词：总数要重探，页游标回第1页
        nextPage = 1;
        maxPages = 0;
    }

    @Override
    public int getTotalSchematic() {
        ensureProbed();
        return totalSchematic;
    }

    @Override
    public Collection<Schematic> getNext() {
        ensureProbed();
        if (nextPage > maxPages) {
            return List.of();
        }
        try {
            URI uri = URIBuilder.create("https://example.com/api/schematics")
                    .addParam("page", String.valueOf(nextPage))
                    .addParam("pageSize", String.valueOf(PAGE_SIZE))
                    .addParam("q", keyword) //空串表示不过滤，按后端约定也可省略该参数
                    .build();
            HttpResponse<String> response = HttpClientGetter.send(
                    HttpRequest.newBuilder().uri(uri), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                //失败要出声，但对管理器只需交出空批次
                DownmaticaMod.LOGGER.warn("Example request failed with status {} (page {})",
                        response.statusCode(), nextPage);
                maxPages = 0;
                return List.of();
            }
            JsonObject object = JsonParser.parseString(response.body()).getAsJsonObject();
            nextPage++;
            ArrayList<Schematic> result = new ArrayList<>();
            for (JsonElement element : object.get("data").getAsJsonArray()) {
                JsonObject item = element.getAsJsonObject();
                Schematic schematic = Schematic.builder()
                        .source(StringUtils.translate("downmatica.source.example"))
                        .title(item.get("title").getAsString())
                        .author(item.get("author").getAsString())
                        .description(item.get("description").getAsString())
                        .downloadFileInfo(item.get("file_name").getAsString(),
                                URI.create(item.get("file_url").getAsString()))
                        .onDetailButtonClicked(() -> CompatibleUtil.openUri(
                                URI.create(item.get("detail_url").getAsString())))
                        .build();
                result.add(schematic);
            }
            return result;
        } catch (Exception e) {
            DownmaticaMod.LOGGER.error("Failed to get Example page {}", nextPage, e);
            maxPages = 0;
            return List.of();
        }
    }

    @Override
    public boolean hasNext() {
        ensureProbed();
        return nextPage <= maxPages;
    }

    /**
     * 用一个只取1条的请求探总数，每个关键词只探一次。
     */
    private void ensureProbed() {
        if (keyword.equals(probedKeyword)) {
            return;
        }
        try {
            URI uri = URIBuilder.create("https://example.com/api/schematics")
                    .addParam("page", "1")
                    .addParam("pageSize", "1")
                    .addParam("q", keyword)
                    .build();
            HttpResponse<String> response = HttpClientGetter.send(
                    HttpRequest.newBuilder().uri(uri), HttpResponse.BodyHandlers.ofString());
            JsonObject object = JsonParser.parseString(response.body()).getAsJsonObject();
            totalSchematic = Math.max(0, object.get("total").getAsInt());
            maxPages = (totalSchematic + PAGE_SIZE - 1) / PAGE_SIZE;
            probedKeyword = keyword;
        } catch (Exception e) {
            DownmaticaMod.LOGGER.error("Failed to count Example schematics", e);
            totalSchematic = 0;
            maxPages = 0;
            //故意不记probedKeyword：下次查询会重试探数
        }
    }
}
```

### 5.4 注册获取器

在您的模组入口点（`ModInitializer#onInitialize`）中调用 `SchematicAcquirer.register()` 注册实例：

```java
@Override
public void onInitialize() {
    SchematicAcquirer.register(new ExampleSchematicAcquirer());
}
```

> 注册应尽早执行（入口点内），以确保界面打开时获取器已就绪。注册顺序就是长表里的展示顺序。

## 6. 本地化支持（翻译键）

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

## 7. 注意事项

- **异常处理**：接口的每个方法都不允许把异常抛给管理器，详见 5.2 的红线说明。
- **状态复位**：`setKeyword` 是唯一的重置信号，交付状态、缓存、页游标都要在这里处理干净；它只在关键词**变化**时被调用，首次使用前可能从未被调过。
- **兼容性**：需要打开网页推荐使用 `top.ajitech.downmatica.util.CompatibleUtil#openUri()` ，因为在不同的Minecraft 版本中，打开网页的方式有差异。
- **HttpClient**：请使用 `top.ajitech.downmatica.api.HttpClientGetter#send()` 发送请求，该方法受用户配置。

如果有任何问题，欢迎通过 Issue 或 Pull Request 与我们交流！

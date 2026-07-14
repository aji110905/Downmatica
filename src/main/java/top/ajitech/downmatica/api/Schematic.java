package top.ajitech.downmatica.api;

import top.ajitech.downmatica.api.impl.SchematicImpl;
import org.jetbrains.annotations.Nullable;
import top.ajitech.downmatica.util.StringUtil;

import java.net.URI;
import java.util.List;

/**
 * 表示从网络中获取的原理图。
 * <p>
 * 推荐使用通用实现{@link SchematicImpl}。
 */
public interface Schematic {
    /**
     * 返回该原理图的来源。
     * <p>
     * 该值不为有效字符串时将在页面中显示<code>未知</code>。
     * <p>
     * 校验逻辑请查看{@link StringUtil#hasText(String)}。
     * @return 来源
     */
    @Nullable String getSource();

    /**
     * 返回该原理图的标题。
     * <p>
     * 该值不为有效字符串时将在页面中显示<code>未知</code>。
     * <p>
     * 校验逻辑请查看{@link StringUtil#hasText(String)}。
     * @return 标题
     */
    @Nullable String getTitle();

    /**
     * 返回该原理图的作者。
     * <p>
     * 该值不为有效字符串时将在页面中显示<code>未知</code>。
     * <p>
     * 校验逻辑请查看{@link StringUtil#hasText(String)}。
     * @return 作者
     */
    @Nullable String getAuthor();

    /**
     * 返回该原理图的描述。
     * <p>
     * 该值不为有效字符串时将在页面中显示<code>无</code>。
     * <p>
     * 校验逻辑请查看{@link StringUtil#hasText(String)}。
     * @return 描述
     */
    @Nullable String getDescription();

    /**
     * 返回该原理图的下载地址。
     * <p>
     * 该值为<code>null</code>时，<code>下载</code>按钮将不可点击。
     * @return 下载地址
     */
    @Nullable SchematicDownloadInfo getDownloadFileInfo();

    /**
     * 当详情按钮被点击时调用{@link Runnable#run()}
     * <p>
     * 当该值为<code>null</code>时，<code>详情</code>按钮将不可点击。
     * @return 详情按钮点击事件
     */
    @Nullable Runnable onDetailButtonClicked();

    /**
     * 返回该原理图用于搜索的字符串。
     * @return 搜索字符串
     */
    List<String> getSearchStrings();

    static Builder builder() {
        return new Builder();
    }

    class Builder {
        private String source;
        private String title;
        private String author;
        private String description;
        private SchematicDownloadInfo downloadFileInfo;
        private Runnable runnable;

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder downloadFileInfo(String localFileName, URI remoteFileURI) {
            this.downloadFileInfo = SchematicDownloadInfo.of(localFileName, remoteFileURI);
            return this;
        }

        public Builder onDetailButtonClicked(Runnable runnable) {
            this.runnable = runnable;
            return this;
        }

        public Schematic build() {
            return new SchematicImpl(source, title, author, description, downloadFileInfo, runnable);
        }
    }
}

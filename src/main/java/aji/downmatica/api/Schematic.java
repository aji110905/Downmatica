package aji.downmatica.api;

import aji.downmatica.api.impl.SchematicImpl;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;

/**
 * 表示从远程或以其他方式获取的原理图。
 * <p>
 * 推荐使用通用实现{@link SchematicImpl}。
 */
public interface Schematic {
    /**
     * 返回该原理图的来源。
     * <p>
     * 可以是网站的名称，或者任何东西，没有过多校验。
     * <p>
     * 推荐返回翻译后的名称，因为一个{@link SchematicAcquirer}可能会返回多个原理图，但通常都是同一个来源。
     * @return 来源
     */
    @Nullable String getSource();

    /**
     * 返回该原理图的标题。
     * <p>
     * 标题也会用于下载后保存的文件名称，请确保名称符合文件命名规则。
     * <p>
     * 具体校验规则请查看{@link aji.downmatica.util.StringUtil#isValidFileName(String)}。
     * <p>
     * 如果不是合理的文件名称，下载后的文件名将会为为一串随机的uuid。
     * @return 标题
     */
    @Nullable String getTitle();

    /**
     * 获取该原理图的作者。
     * @return 作者
     */
    @Nullable String getAuthor();

    /**
     * 获取该原理图的描述。
     * @return 描述
     */
    @Nullable String getDescription();

    /**
     * 获取该原理图的下载地址。
     * @return 下载地址
     */
    @Nullable URI getDownloadURI();

    /**
     * 获取该原理图的网页地址。
     * @return 网页地址
     */
    @Nullable URI getWebURI();

    /**
     * 获取该原理图用于搜索的字符串。
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
        private URI downloadURI;
        private URI webURI;

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

        public Builder downloadURI(URI downloadURI) {
            this.downloadURI = downloadURI;
            return this;
        }

        public Builder webURI(URI webURI) {
            this.webURI = webURI;
            return this;
        }

        public Schematic build() {
            return new SchematicImpl(source, title, author, description, downloadURI, webURI);
        }
    }
}

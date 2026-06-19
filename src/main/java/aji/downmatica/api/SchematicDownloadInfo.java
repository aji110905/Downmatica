package aji.downmatica.api;

import aji.downmatica.api.impl.SchematicDownloadInfoImpl;
import aji.downmatica.util.FileUtil;

import java.net.URI;

/**
 * 表示原理图下载信息。
 * <p>
 * 推荐使用通用实现{@link SchematicDownloadInfoImpl}。
 */
public interface SchematicDownloadInfo {
    /**
     * 获取本地文件名。
     * @return 本地文件名
     */
    String getLocalFileName();

    /**
     * 获取远程文件地址。
     * @return 远程文件地址
     */
    URI getRemoteFileURI();

    default boolean isValid() {
        return FileUtil.isValidFileName(getLocalFileName()) && getRemoteFileURI() != null;
    }

    static SchematicDownloadInfo of(String localFileName, URI remoteFileURI) {
        return new SchematicDownloadInfoImpl(localFileName, remoteFileURI);
    }
}

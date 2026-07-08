package top.ajitech.downmatica.api.impl;

import top.ajitech.downmatica.api.SchematicDownloadInfo;

import java.net.URI;

public class SchematicDownloadInfoImpl implements SchematicDownloadInfo {
    private final String localFileName;
    private final URI remoteFileURI;

    public SchematicDownloadInfoImpl(String localFileName, URI remoteFileURI) {
        this.localFileName = localFileName;
        this.remoteFileURI = remoteFileURI;
    }

    @Override
    public String getLocalFileName() {
        return localFileName;
    }

    @Override
    public URI getRemoteFileURI() {
        return remoteFileURI;
    }
}

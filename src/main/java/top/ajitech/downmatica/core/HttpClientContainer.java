package top.ajitech.downmatica.core;

import java.net.http.HttpClient;
import java.time.Duration;

public class HttpClientContainer {
    public static final HttpClientContainer INSTANCE = new HttpClientContainer();

    private HttpClient httpClient;
    private int timeout = ConfigHandler.INSTANCE.httpTimeout.getDefaultIntegerValue();
    private boolean enableHttp2 = ConfigHandler.INSTANCE.enableHttp2.getDefaultBooleanValue();
    private HttpClient.Redirect redirectPolicy = ((ConfigHandler.RedirectPolicyConfigOptionListEntry) ConfigHandler.INSTANCE.redirectPolicy.getDefaultOptionListValue()).getRedirectPolicy();

    private HttpClientContainer(){
        createHttpClient();
    }

    public HttpClient get(){
        return httpClient;
    }

    public void setTimeout(int timeout) {
        if (this.timeout != timeout){
            this.timeout = timeout;
            createHttpClient();
        }
    }

    public void setEnableHttp2(boolean enableHttp2) {
        if (this.enableHttp2 != enableHttp2){
            this.enableHttp2 = enableHttp2;
            createHttpClient();
        }
    }

    public void setRedirectPolicy(HttpClient.Redirect redirectPolicy) {
        if (!this.redirectPolicy.equals(redirectPolicy)){
            this.redirectPolicy = redirectPolicy;
            createHttpClient();
        }
    }

    private void createHttpClient(){
        HttpClient.Builder builder = HttpClient.newBuilder()
                .followRedirects(redirectPolicy)
                .version(enableHttp2 ? HttpClient.Version.HTTP_2 : HttpClient.Version.HTTP_1_1);
        if (timeout >= 0) {
            builder.connectTimeout(Duration.ofMillis(timeout));
        }
        httpClient = builder.build();
    }
}

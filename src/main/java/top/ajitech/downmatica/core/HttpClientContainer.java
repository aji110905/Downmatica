package top.ajitech.downmatica.core;

import java.net.http.HttpClient;
import java.time.Duration;

public class HttpClientContainer {
    public static final HttpClientContainer INSTANCE = new HttpClientContainer();

    private HttpClient httpClient;
    private int timeout = ConfigHandler.INSTANCE.httpTimeout.getDefaultIntegerValue();
    private boolean enableHttp2 = ConfigHandler.INSTANCE.enableHttp2.getDefaultBooleanValue();
    private HttpClient.Redirect redirectPolicy = ((RedirectPolicyConfigOptionListEntry) ConfigHandler.INSTANCE.redirectPolicy.getDefaultOptionListValue()).getRedirectPolicy();

    private HttpClientContainer(){
        createHttpClient();
    }

    /**
     * 此方法供内部调用，不推荐直接使用。
     * <p>
     * 未来版本中，该方法的签名或行为可能会在不预先通知的情况下发生变化。
     * <p>
     * 建议使用{@link top.ajitech.downmatica.api.HttpClientGetter#get()}作为替代，
     * 该方法会保持向下兼容性，而此方法不会。
     */
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

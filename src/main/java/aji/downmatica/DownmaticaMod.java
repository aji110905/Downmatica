package aji.downmatica;

import aji.downmatica.network.SDKArchiveSchematicAcquirer;
import aji.downmatica.network.SchematicAcquirers;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.time.Duration;

public class DownmaticaMod implements ModInitializer {
    public static final String MOD_ID = "downmatica";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    @Override
    public void onInitialize() {
        SchematicAcquirers.register(new SDKArchiveSchematicAcquirer());
    }
}

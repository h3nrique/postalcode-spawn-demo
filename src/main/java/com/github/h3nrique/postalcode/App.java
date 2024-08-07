package com.github.h3nrique.postalcode;

import com.github.h3nrique.postalcode.actors.PostalCode;
import com.github.h3nrique.postalcode.actors.PostalCodeGenerator;
import com.github.h3nrique.postalcode.handler.PostalCodeHandler;
import com.github.h3nrique.postalcode.service.PostalCodeService;
import io.eigr.spawn.api.*;

import io.eigr.spawn.api.extensions.DependencyInjector;
import io.eigr.spawn.api.extensions.SimpleDependencyInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.StringJoiner;

public final class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        Config cfg = Config.createDefaultConfig();

        PostalCodeService postalCodeService = new PostalCodeService();
        DependencyInjector dependencyInjector = SimpleDependencyInjector.createInjector();
        dependencyInjector.bind(PostalCodeService.class, postalCodeService);

        waitForProxy(cfg.startupDelaySeconds());

        Spawn spawnSystem = new Spawn.SpawnSystem()
                .create(cfg.spawnSystemName, dependencyInjector)
                .withActor(PostalCodeGenerator.class)
                .withActor(PostalCode.class)
                .withTransportOptions(TransportOpts.builder()
                        .host(cfg.userFunctionHost)
                        .port(Integer.parseInt(cfg.userFunctionPort))
                        .proxyHost(cfg.spawnProxyHost)
                        .proxyPort(Integer.parseInt(cfg.spawnProxyPort))
                        .build())
                .withTerminationGracePeriodSeconds(5)
                .build();

        spawnSystem.start();

        RestServer.create(cfg.host, Integer.parseInt(cfg.port))
                .withRoute("/postalcode", new PostalCodeHandler(spawnSystem))
                .start();

        log.info("Actor running and ready to connection at ports [{}] and [{}]", cfg.userFunctionPort, cfg.port);
    }

    private static void waitForProxy(String startupDelaySeconds) {
        try {
            log.info("Waiting [{}] seconds to start...", startupDelaySeconds);
            Thread.sleep(Long.parseLong(startupDelaySeconds) * 1000);
        } catch (Exception err) {
            // em branco
        }
    }

    public record Config(String startupDelaySeconds, String host, String port, String userFunctionHost,
                         String userFunctionPort, String spawnProxyHost, String spawnProxyPort, String spawnSystemName,
                         String authToken) {

        public static Config createDefaultConfig() {
            String startupDelaySeconds = System.getenv("STARTUP_DELAY_SECONDS") != null ? System.getenv("STARTUP_DELAY_SECONDS") : "10";
            String host = System.getenv("HOST") != null ? System.getenv("HOST") : "0.0.0.0";
            String port = System.getenv("PORT") != null ? System.getenv("PORT") : "8080";

            String userFunctionHost = System.getenv("USER_FUNCTION_HOST") != null ? System.getenv("USER_FUNCTION_HOST") : "localhost";
            String userFunctionPort = System.getenv("USER_FUNCTION_PORT") != null ? System.getenv("USER_FUNCTION_PORT") : "8091";
            String spawnProxyHost = System.getenv("SPAWN_PROXY_HOST") != null ? System.getenv("SPAWN_PROXY_HOST") : "localhost";
            String spawnProxyPort = System.getenv("SPAWN_PROXY_PORT") != null ? System.getenv("SPAWN_PROXY_PORT") : "9001";
            String spawnSystemName = System.getenv("SPAWN_SYSTEM_NAME") != null ? System.getenv("SPAWN_SYSTEM_NAME") : "spawn-system";

            String authToken = System.getenv("AUTH_TOKEN") != null ? System.getenv("AUTH_TOKEN") : "h3nrique@123";

            return new Config(startupDelaySeconds, host, port, userFunctionHost, userFunctionPort, spawnProxyHost,
                    spawnProxyPort, spawnSystemName, authToken);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Config.class.getSimpleName() + "[", "]")
                    .add("DELAY_SECONDS='" + startupDelaySeconds + "'")
                    .add("HOST='" + host + "'")
                    .add("PORT='" + port + "'")
                    .add("AUTH_TOKEN='" + authToken + "'")
                    .add("USER_FUNCTION_HOST='" + userFunctionHost + "'")
                    .add("USER_FUNCTION_PORT='" + userFunctionPort + "'")
                    .add("SPAWN_PROXY_HOST='" + spawnProxyHost + "'")
                    .add("SPAWN_PROXY_PORT='" + spawnProxyPort + "'")
                    .add("SPAWN_SYSTEM_NAME='" + spawnSystemName + "'")
                    .add("AUTH_TOKEN='" + authToken + "'")
                    .toString();
        }
    }
}
package br.com.fabricads.poc.spawn;

import br.com.fabricads.poc.spawn.actors.*;
import br.com.fabricads.poc.spawn.handler.PostalCodeHandler;
import br.com.fabricads.poc.spawn.service.PostalCodeService;
import io.eigr.spawn.api.*;

import br.com.fabricads.poc.proto.Common;
import io.eigr.spawn.api.extensions.DependencyInjector;
import io.eigr.spawn.api.extensions.SimpleDependencyInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {

        String startupDelaySeconds = System.getenv("STARTUP_DELAY_SECONDS") != null ? System.getenv("STARTUP_DELAY_SECONDS") : "10";
        String host = System.getenv("HOST") != null ? System.getenv("HOST") : "0.0.0.0";
        String port = System.getenv("PORT") != null ? System.getenv("PORT") : "8080";
        String userFunctionHost = System.getenv("USER_FUNCTION_HOST") != null ? System.getenv("USER_FUNCTION_HOST") : "localhost";
        String userFunctionPort = System.getenv("USER_FUNCTION_PORT") != null ? System.getenv("USER_FUNCTION_PORT") : "8091";
        String spawnProxyHost = System.getenv("SPAWN_PROXY_HOST") != null ? System.getenv("SPAWN_PROXY_HOST") : "localhost";
        String spawnProxyPort = System.getenv("SPAWN_PROXY_PORT") != null ? System.getenv("SPAWN_PROXY_PORT") : "9001";
        String spawnSystemName = System.getenv("SPAWN_SYSTEM_NAME") != null ? System.getenv("SPAWN_SYSTEM_NAME") : "spawn-system";

        log.debug("HOST               :: {}", host);
        log.debug("PORT               :: {}", port);
        log.debug("USER_FUNCTION_HOST :: {}", userFunctionHost);
        log.debug("USER_FUNCTION_PORT :: {}", userFunctionPort);
        log.debug("SPAWN_PROXY_HOST   :: {}", spawnProxyHost);
        log.debug("SPAWN_PROXY_PORT   :: {}", spawnProxyPort);
        log.debug("SPAWN_SYSTEM_NAME  :: {}", spawnSystemName);

        try {
                log.info("Waiting [{}] seconds to start...", startupDelaySeconds);
                Thread.sleep(Long.valueOf(startupDelaySeconds) * 1000);
        } catch (Exception err) {

        }
        TransportOpts opts = TransportOpts.builder()
                .host(userFunctionHost)
                .port(Integer.parseInt(userFunctionPort))
                .proxyHost(spawnProxyHost)
                .proxyPort(Integer.parseInt(spawnProxyPort))
                .build();

        PostalCodeService postalCodeService = new PostalCodeService();
        DependencyInjector dependencyInjector = SimpleDependencyInjector.createInjector();
        dependencyInjector.bind(PostalCodeService.class, postalCodeService);

        Spawn spawnSystem = new Spawn.SpawnSystem()
                .create(spawnSystemName)
                .withActor(PostalCodeGenerator.class)
                .withActor(PostalCode.class, dependencyInjector, arg -> new PostalCode((DependencyInjector) arg))
                .withTransportOptions(opts)
                .withTerminationGracePeriodSeconds(5)
                .build();

        spawnSystem.start();

        // RestServer.create(host, Integer.parseInt(port))
        //         .withRoute("/postalcode", new PostalCodeHandler(spawnSystem))
        //         .start();

        // ActorRef postalCodeGenerator = spawnSystem.createActorRef( ActorIdentity.of(spawnSystemName, "postal_code_generator"));

        // Common.Generator msg = Common.Generator.newBuilder()
        //         .setCountryName("Brasil")
        //         .setBeginRangePostalCode("60192000")
        //         .setEndRangePostalCode("60192200")
        //         .build();

        // postalCodeGenerator.invokeAsync("generatePostalCodes", msg);
        log.info("Actor running and ready to connection at ports [{}] and [{}]", userFunctionPort, port);
    }
}
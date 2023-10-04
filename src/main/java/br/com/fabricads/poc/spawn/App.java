package br.com.fabricads.poc.spawn;

import br.com.fabricads.poc.spawn.actors.*;
import io.eigr.spawn.api.*;

import br.com.fabricads.poc.proto.Common;
import br.com.fabricads.poc.spawn.util.RequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {

        String host = System.getenv("HOST") != null ? System.getenv("HOST") : "localhost";
        String port = System.getenv("PORT") != null ? System.getenv("PORT") : "8091";
        String proxyHost = System.getenv("PROXY_HOST") != null ? System.getenv("PROXY_HOST") : "localhost";
        String proxyPort = System.getenv("PROXY_PORT") != null ? System.getenv("PROXY_PORT") : "9003";
        String spawnSystemName = System.getenv("SPAWN_SYSTEM_NAME") != null ? System.getenv("SPAWN_SYSTEM_NAME") : "spawn-system";

        log.debug("HOST       :: {}", host);
        log.debug("PORT       :: {}", port);
        log.debug("PROXY_HOST :: {}", proxyHost);
        log.debug("PROXY_PORT :: {}", proxyPort);

        TransportOpts opts = TransportOpts.builder()
                .host(host)
                .port(Integer.parseInt(port))
                .proxyHost(proxyHost)
                .proxyPort(Integer.parseInt(proxyPort))
                .build();

        Spawn spawnSystem = new Spawn.SpawnSystem()
                .create(spawnSystemName)
                .withActor(PostalCodeGenerator.class)
                .withActor(PostalCode.class, new RequestService(), arg -> new PostalCode((RequestService) arg))
                .withTransportOptions(opts)
                .withTerminationGracePeriodSeconds(5)
                .build();

        spawnSystem.start();

        ActorRef postalCodeGenerator = spawnSystem.createActorRef( ActorIdentity.of(spawnSystemName, "postal_code_generator"));

        Common.Generator msg = Common.Generator.newBuilder()
                .setCountryName("Brasil")
                .setBeginRangePostalCode("60192000")
                .setEndRangePostalCode("60192200")
                .build();

        postalCodeGenerator.invokeAsync("generatePostalCodes", msg);
    }
}
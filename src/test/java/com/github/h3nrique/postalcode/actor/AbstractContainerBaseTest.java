package com.github.h3nrique.postalcode.actor;

import com.github.h3nrique.postalcode.App;
import com.github.h3nrique.postalcode.actors.PostalCode;
import com.github.h3nrique.postalcode.actors.PostalCodeGenerator;
import com.github.h3nrique.postalcode.service.PostalCodeService;
import io.eigr.spawn.api.Spawn;
import io.eigr.spawn.api.TransportOpts;
import io.eigr.spawn.api.exceptions.SpawnException;
import io.eigr.spawn.api.extensions.DependencyInjector;
import io.eigr.spawn.api.extensions.SimpleDependencyInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

abstract class AbstractContainerBaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractContainerBaseTest.class);

    private static final String SPAWN_PROXY_IMAGE_NAME = "eigr/spawn-proxy:1.4.2-rc.2";
    static final GenericContainer<?> SPAWN_CONTAINER;
    static final Spawn spawnSystem;
    static final App.Config cfg;

    static {
        Testcontainers.exposeHostPorts(8091);

        SPAWN_CONTAINER = new GenericContainer<>(DockerImageName.parse(SPAWN_PROXY_IMAGE_NAME))
                .waitingFor(new LogMessageWaitStrategy()
                        .withRegEx(".*Proxy Application started successfully.*"))
                .withEnv("TZ", "America/Sao_Paulo")
                .withEnv("SPAWN_PROXY_LOGGER_LEVEL", "DEBUG")
                .withEnv("SPAWN_STATESTORE_KEY", "3Jnb0hZiHIzHTOih7t2cTEPEpY98Tu1wvQkPfq/XwqE=")
                .withEnv("PROXY_ACTOR_SYSTEM_NAME", "spawn-system-test")
                .withEnv("PROXY_DATABASE_TYPE", "native")
                .withEnv("PROXY_DATABASE_DATA_DIR", "mnesia_data")
                .withEnv("NODE_COOKIE", "cookie-9ce3712b0c3ee21b582c30f942c0d4da-HLuZyQzy+nt0p0r/PVVFTp2tqfLom5igrdmwkYSuO+Q=")
                .withEnv("POD_NAMESPACE", "spawn-h3nrique")
                .withEnv("POD_IP", "spawn")
                .withEnv("PROXY_HTTP_PORT", "9001")
                .withEnv("USER_FUNCTION_PORT", "8091")
                .withEnv("USER_FUNCTION_HOST", "host.testcontainers.internal")
                .withExtraHost("host.testcontainers.internal", "host-gateway")
                .withExposedPorts(9001)
                .withAccessToHost(true);
        SPAWN_CONTAINER.start();

        cfg = new App.Config("5", "0.0.0.0", "8080", "localhost", "8091", SPAWN_CONTAINER.getHost(),
                "9001", "spawn-system-test", "example");
        DependencyInjector dependencyInjector = SimpleDependencyInjector.createInjector();
        dependencyInjector.bind(App.Config.class, cfg);
        dependencyInjector.bind(PostalCodeService.class, new PostalCodeService());

        try {
            spawnSystem = new Spawn.SpawnSystem()
                    .create(cfg.spawnSystemName(), dependencyInjector)
                    .withActor(PostalCode.class)
                    .withActor(PostalCodeGenerator.class)
                    .withTerminationGracePeriodSeconds(10)
                    .withTransportOptions(TransportOpts.builder()
                            .host(SPAWN_CONTAINER.getHost())
                            .port(Integer.parseInt(cfg.userFunctionPort()))
                            .proxyPort(SPAWN_CONTAINER.getMappedPort(9001))
                            .build())
                    .build();
            spawnSystem.start();
        } catch (SpawnException e) {
            throw new RuntimeException(e);
        }
        LOG.info("Spawn system started");
    }
}

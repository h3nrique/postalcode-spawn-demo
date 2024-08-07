package com.github.h3nrique.postalcode.actor;

import com.github.h3nrique.postalcode.proto.Postalcode;
import io.eigr.functions.protocol.Protocol;
import io.eigr.spawn.api.ActorIdentity;
import io.eigr.spawn.api.ActorRef;
import io.eigr.spawn.api.exceptions.SpawnException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TJActorTest extends AbstractContainerBaseTest {

    private static final Logger log = LoggerFactory.getLogger(TJActorTest.class);

    @Test
    public void findCep() throws SpawnException {
        String cep = "03694090";
        Postalcode.CreateRequest createRequest = Postalcode.CreateRequest.newBuilder()
                .setPostalCode(cep)
                .build();
        ActorIdentity actorIdentity = ActorIdentity.of(cfg.spawnSystemName(), cep, "PostalCode", true);
        ActorRef actorRef = spawnSystem.createActorRef(actorIdentity);
        actorRef.invoke("OnCreate", createRequest, Protocol.Noop.class);
        actorRef.invoke("Get", Postalcode.PostalCodeState.class)
                .ifPresent(resp -> {
                    assertNotNull(resp);
                    log.debug("PostalCode Get :: [{}]", resp);
                });
    }
}

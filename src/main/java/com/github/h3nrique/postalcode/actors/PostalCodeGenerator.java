package com.github.h3nrique.postalcode.actors;

import com.github.h3nrique.postalcode.proto.Common;
import com.github.h3nrique.postalcode.proto.Postalcode;
import io.eigr.spawn.api.ActorIdentity;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatelessActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;
import io.eigr.spawn.api.exceptions.ActorCreationException;
import io.eigr.spawn.api.exceptions.ActorInvocationException;
import io.eigr.spawn.internal.ActionBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.*;

public final class PostalCodeGenerator implements StatelessActor {

    private static final Logger log = LoggerFactory.getLogger(PostalCodeGenerator.class);

    @Override
    public ActorBehavior configure(BehaviorCtx context) {
        return new NamedActorBehavior(
                action("Generate", ActionBindings.of(Common.Generator.class, this::generate))
        );
    }

    public Value generate(ActorContext<?> context, Common.Generator msg) {
        log.debug("Received invocation. Message: '{}'. Context: '{}'.", msg, context);
        final String spawnSystemName = System.getenv("SPAWN_SYSTEM_NAME") != null ? System.getenv("SPAWN_SYSTEM_NAME") : "spawn-system";

        try {
            Integer.parseInt(msg.getBeginRangePostalCode());
            Integer.parseInt(msg.getEndRangePostalCode());
        } catch (NumberFormatException err) {
            log.error("Invalid range");
            return Value.at().noReply();
        }

        IntStream.rangeClosed(Integer.parseInt(msg.getBeginRangePostalCode()), Integer.parseInt(msg.getEndRangePostalCode()))
                .sequential()
                .filter(i -> i % 10 == 0)
                .peek(i -> {
                    try {
                        log.debug("Waiting 10 seconds to generate new 'postal_code' actor.");
                        Thread.sleep(10000);
                    } catch (InterruptedException err) {
                        Thread.currentThread().interrupt();
                    }
                })
                .forEach(current -> {
                    try {
                        String postalCode = "00000000".concat(String.valueOf(current));
                        postalCode = postalCode.substring(postalCode.length() - 8);
                        ActorIdentity actorIdentity = ActorIdentity.of(spawnSystemName, postalCode, "postal_code");
                        context.getSpawnSystem()
                                .createActorRef(actorIdentity)
                                .invokeAsync("OnCreate", Postalcode.CreateRequest.newBuilder()
                                        .setPostalCode(postalCode)
                                        .build()
                                );
                    } catch (ActorCreationException | ActorInvocationException err) {
                        throw new RuntimeException(err);
                    }
                });

        return Value.at().noReply();
    }
}
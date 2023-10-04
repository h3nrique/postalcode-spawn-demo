package br.com.fabricads.poc.spawn.actors;

import br.com.fabricads.poc.proto.Common;
import br.com.fabricads.poc.proto.Postalcode;
import io.eigr.spawn.api.ActorIdentity;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.annotations.Action;
import io.eigr.spawn.api.actors.annotations.stateless.StatelessNamedActor;
import io.eigr.spawn.api.exceptions.ActorCreationException;
import io.eigr.spawn.api.exceptions.ActorInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

@StatelessNamedActor(name = "postal_code_generator")
public class PostalCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(PostalCodeGenerator.class);

    @Action(name = "generatePostalCodes")
    public Value generate(Common.Generator msg, ActorContext<?> context) {
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
                                .invokeAsync("onCreate", Postalcode.CreateRequest.newBuilder()
                                        .setCode(postalCode)
                                        .setCountry(msg.getCountryName())
                                        .build()
                                );
                    } catch (ActorCreationException | ActorInvocationException err) {
                        throw new RuntimeException(err);
                    }
                });

        return Value.at().noReply();
    }
}
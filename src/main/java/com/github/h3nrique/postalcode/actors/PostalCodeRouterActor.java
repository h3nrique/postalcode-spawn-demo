package com.github.h3nrique.postalcode.actors;

import com.github.h3nrique.postalcode.proto.*;
import io.eigr.spawn.api.ActorIdentity;
import io.eigr.spawn.api.ActorRef;
import io.eigr.spawn.api.Spawn;
import io.eigr.spawn.api.actors.ActionBindings;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatelessActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;
import io.eigr.spawn.api.actors.workflows.Forward;
import io.eigr.spawn.api.exceptions.ActorCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.*;

public final class PostalCodeRouterActor implements StatelessActor {

    private static final Logger log = LoggerFactory.getLogger(PostalCodeRouterActor.class);

    @Override
    public ActorBehavior configure(BehaviorCtx context) {
        return new NamedActorBehavior(
                name("PostalCodeRouter"),
                action("Create", ActionBindings.of(CreateRequest.class, this::create))
        );
    }

    public Value create(ActorContext<?> context, CreateRequest msg) {
        log.debug("Received invocation. Message: '{}'. Context: '{}'.", msg, context);
        try {
            Spawn spawn = context.getSpawnSystem();
            ActorRef actorRef = spawn
                    .createActorRef(ActorIdentity.of(spawn.getSystem(), msg.getPostalCode(), "PostalCode", true));

            return Value.at()
                    .flow(Forward.to(actorRef, "Get"))
                    .reply();

        } catch (ActorCreationException e) {
            throw new RuntimeException(e);
        }
    }
}
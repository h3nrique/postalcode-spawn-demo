package com.github.h3nrique.postalcode.actors;

import com.github.h3nrique.postalcode.proto.Common;
import com.github.h3nrique.postalcode.proto.Postalcode;
import com.github.h3nrique.postalcode.service.PostalCodeService;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatefulActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;
import io.eigr.spawn.internal.ActionBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.*;

public final class PostalCode implements StatefulActor<Postalcode.PostalCodeState> {

    private static final Logger log = LoggerFactory.getLogger(PostalCode.class);

    private PostalCodeService postalCodeService;

    @Override
    public ActorBehavior configure(BehaviorCtx context) {
        this.postalCodeService = context.getInjector().getInstance(PostalCodeService.class);
        return new NamedActorBehavior(
                action("OnCreate", ActionBindings.of(Postalcode.CreateRequest.class, this::onCreatePostalCode))
        );
    }

    public Value onCreatePostalCode(ActorContext<Postalcode.PostalCodeState> context, Postalcode.CreateRequest msg) {
        log.debug("Received invocation. Message: '{}'. Context: '{}'.", msg, context);

        Postalcode.PostalCodeState.Builder builder = Postalcode.PostalCodeState.newBuilder();
        Map<String, String> postalCode = postalCodeService.find(msg.getPostalCode());
        if(!postalCode.isEmpty()) {
            if(context.getState().isPresent()) {
                log.trace("State is present and value is '{}'.", context.getState().get());
            } else {
                log.trace("State not present.");
            }
            Postalcode.PostalCodeState state = builder.setCode(msg.getPostalCode())
                    .setCity(postalCode.get("localidade"))
                    .setState(postalCode.get("uf"))
                    .setStreet(postalCode.get("logradouro"))
                    .setCountry(postalCode.get("pais"))
                    .setStatus(Common.PostalCodeStatus.FOUND)
                    .build();
            return Value.at()
                    .state(builder.build())
                    .noReply();
        }
        return Value.at()
                .state(builder
                        .setStatus(Common.PostalCodeStatus.UNKNOWN)
                        .build())
                .noReply();
    }
}
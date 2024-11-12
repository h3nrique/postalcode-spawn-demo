package com.github.h3nrique.postalcode.actors;

import com.github.h3nrique.postalcode.proto.*;
import com.github.h3nrique.postalcode.service.PostalCodeService;
import com.google.type.PostalAddress;
import io.eigr.spawn.api.actors.ActionBindings;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatefulActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.*;

public final class PostalCodeActor implements StatefulActor<PostalCodeStateProto.PostalCodeState> {

    private static final Logger log = LoggerFactory.getLogger(PostalCodeActor.class);

    private PostalCodeService postalCodeService;

    @Override
    public ActorBehavior configure(BehaviorCtx context) {
        this.postalCodeService = context.getInjector().getInstance(PostalCodeService.class);
        return new NamedActorBehavior(
                name("PostalCode"),
                action("OnCreate", ActionBindings.of(CreateRequestProto.CreateRequest.class, this::onCreate))
        );
    }

    public Value onCreate(ActorContext<PostalCodeStateProto.PostalCodeState> context, CreateRequestProto.CreateRequest msg) {
        log.debug("Received invocation. Message: '{}'. Context: '{}'.", msg, context);

        PostalCodeStateProto.PostalCodeState.Builder builder = PostalCodeStateProto.PostalCodeState.newBuilder();
        return postalCodeService.find(msg.getPostalCode())
                .map(postalCode -> {
                    if(context.getState().isPresent()) {
                        log.trace("State is present and value is '{}'.", context.getState().get());
                    } else {
                        log.trace("State not present.");
                    }
                    PostalCodeStateProto.PostalCodeState state = builder.setPostalAddress(PostalAddress.newBuilder()
                                    .addAddressLines(postalCode.get("logradouro"))
                                    .addAddressLines(postalCode.get("complemento"))
                                    .addAddressLines(postalCode.get("bairro"))
                                    .addAddressLines(postalCode.get("localidade"))
                                    .addAddressLines(postalCode.get("uf"))
                                    .build())
                            .setStatus(PostalCodeStatusProto.PostalCodeStatus.FOUND)
                            .build();
                    return Value.at()
                            .state(state)
                            .noReply();
                })
                .orElse(Value.at()
                        .state(builder
                                .setStatus(PostalCodeStatusProto.PostalCodeStatus.NOT_FOUND)
                                .build())
                        .noReply());
    }
}
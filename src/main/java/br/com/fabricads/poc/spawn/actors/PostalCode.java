package br.com.fabricads.poc.spawn.actors;

import br.com.fabricads.poc.proto.Common;
import br.com.fabricads.poc.proto.Postalcode;
import br.com.fabricads.poc.spawn.service.PostalCodeService;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.annotations.Action;
import io.eigr.spawn.api.actors.annotations.stateful.StatefulUnNamedActor;
import io.eigr.spawn.api.extensions.DependencyInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@StatefulUnNamedActor(name = "postal_code", stateType = Postalcode.PostalCodeState.class)
public final class PostalCode {

    private static final Logger log = LoggerFactory.getLogger(PostalCode.class);

    private final PostalCodeService postalCodeService;

    public PostalCode(DependencyInjector dependencyInjector) {
        this.postalCodeService = dependencyInjector.getInstance(PostalCodeService.class);
    }

    @Action(name = "onCreate")
    public Value onCreatePostalCode(Postalcode.CreateRequest msg, ActorContext<Postalcode.PostalCodeState> context) {
        log.debug("Received invocation. Message: '{}'. Context: '{}'.", msg, context);

        Postalcode.PostalCodeState.Builder builder = Postalcode.PostalCodeState.newBuilder();
        Map<String, String> postalCode = postalCodeService.find(msg.getPostalCode());
        if(!postalCode.isEmpty()) {
            if(context.getState().isPresent()) {
                log.trace("State is present and value is '{}'.", context.getState().get());
            } else {
                log.trace("State not present.");
            }
            builder.setCode(msg.getPostalCode())
                    .setCity(postalCode.get("localidade"))
                    .setState(postalCode.get("uf"))
                    .setStreet(postalCode.get("logradouro"))
                    .setCountry(msg.getCountry())
                    .setStatus(Common.PostalCodeStatus.FOUND);
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
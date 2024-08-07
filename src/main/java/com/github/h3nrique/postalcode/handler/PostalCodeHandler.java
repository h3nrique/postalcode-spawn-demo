package com.github.h3nrique.postalcode.handler;

import com.github.h3nrique.postalcode.proto.Postalcode;
import com.github.h3nrique.postalcode.RestServer;
import com.google.protobuf.util.JsonFormat;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.eigr.spawn.api.ActorIdentity;
import io.eigr.spawn.api.ActorRef;
import io.eigr.spawn.api.Spawn;
import io.eigr.spawn.api.exceptions.SpawnException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class PostalCodeHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(RestServer.class);
    private static final Pattern getPostalCodeEndpointPattern = Pattern.compile("/postalcode/([0-9]{8})");
    private static final Pattern listPostalCodeEndpointPattern = Pattern.compile("/postalcode");
    private static final Pattern postalCodePattern = Pattern.compile("^[0-9]{8}$");
    private static final Map<String, String> defaultHeaders = new HashMap<>() {{
        put("Access-Control-Allow-Methods", "*");
        put("Access-Control-Allow-Origin", "*");
    }};

    private final Spawn spawn;

    public PostalCodeHandler(Spawn spawn) {
        this.spawn = spawn;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        log.debug("Received '{}' request.", exchange.getRequestMethod());
        switch (exchange.getRequestMethod()) {
            case "POST":
                postRequest(exchange);
                break;
            case "GET":
                getRequest(exchange);
                break;
            case "OPTIONS":
                optionsRequest(exchange);
                break;
            default:
                otherwiseRequest(exchange);
                break;
        }
    }

    private void getRequest(HttpExchange exchange) throws IOException {
        try (OutputStream out = exchange.getResponseBody()) {
            String path = exchange.getRequestURI().getPath();
            Matcher getPostalCodeMatcher = getPostalCodeEndpointPattern.matcher(path);
            Matcher listPostalCodeMatcher = listPostalCodeEndpointPattern.matcher(path);
            if (getPostalCodeMatcher.matches()) {
                String postalCode = getPostalCodeMatcher.group(1);
                ActorRef actorRef = spawn
                        .createActorRef(ActorIdentity.of(spawn.getSystem(), postalCode, "PostalCode"));
                Optional<Postalcode.PostalCodeState> actorState = actorRef.invoke("get",
                        Postalcode.PostalCodeState.class);
                if (actorState.isPresent()) {
                    byte[] bytes = JsonFormat.printer().print(actorState.get()).getBytes();
                    defaultHeaders.forEach((key, value) -> exchange.getResponseHeaders().set(key, value));
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, bytes.length);
                    out.write(bytes);
                } else {
                    byte[] bytes = "{\"error\": \"PostalCode not found\"}".getBytes();
                    defaultHeaders.forEach((key, value) -> exchange.getResponseHeaders().set(key, value));
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(404, bytes.length);
                    out.write(bytes);
                }
            } else if (listPostalCodeMatcher.matches()) {
                byte[] bytes = "{\"error\": \"Not Implemented Yet\"}".getBytes();
                defaultHeaders.forEach((key, value) -> exchange.getResponseHeaders().set(key, value));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                out.write(bytes);
            } else {
                badRequest(exchange, out);
            }
        } catch (IOException | SpawnException err) {
            log.error("Internal Error.", err);
            internalError(exchange);
        }
    }

    private void postRequest(HttpExchange exchange) throws IOException {
        try (OutputStream out = exchange.getResponseBody(); InputStream in = exchange.getRequestBody()) {
            String requestBody = new BufferedReader(new InputStreamReader(in))
                    .lines()
                    .collect(Collectors.joining("\n"));
            log.debug("requestBody :: {}", requestBody);
            Postalcode.CreateRequest.Builder postalCodeRequestBuilder = Postalcode.CreateRequest.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(requestBody, postalCodeRequestBuilder);
            Matcher postalCodeMatcher = postalCodePattern.matcher(postalCodeRequestBuilder.getPostalCode());
            if (postalCodeMatcher.matches()) {
                ActorRef actorRef = spawn
                        .createActorRef(ActorIdentity.of(spawn.getSystem(), postalCodeMatcher.group(), "PostalCode"));
                actorRef.invokeAsync("OnCreate", postalCodeRequestBuilder.build());
                defaultHeaders.forEach((key, value) -> exchange.getResponseHeaders().set(key, value));
                exchange.sendResponseHeaders(201, 0);
            } else {
                badRequest(exchange, out);
            }
        } catch (SpawnException | IOException err) {
            log.error("Internal Error.", err);
            internalError(exchange);
        }
    }

    private void optionsRequest(HttpExchange exchange) throws IOException {
        try (OutputStream out = exchange.getResponseBody()) {
            defaultHeaders.forEach((key, value) -> exchange.getResponseHeaders().set(key, value));
            exchange.sendResponseHeaders(200, 0);
            out.write("".getBytes());
        } catch (IOException err) {
            log.error("Internal Error.", err);
            internalError(exchange);
        }
    }

    private void otherwiseRequest(HttpExchange exchange) throws IOException {
        try (OutputStream out = exchange.getResponseBody()) {
            byte[] bytes = "{\"error\": \"Invalid request\"}".getBytes();
            defaultHeaders.forEach((key, value) -> exchange.getResponseHeaders().set(key, value));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, bytes.length);
            out.write(bytes);
        } catch (IOException err) {
            log.error("Internal Error.", err);
            internalError(exchange);
        }
    }

    private static void internalError(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(500, 0);
    }

    private static void badRequest(HttpExchange exchange, OutputStream out) throws IOException {
        byte[] bytes = "{\"error\": \"Bad Request\"}".getBytes();
        defaultHeaders.forEach((key, value) -> exchange.getResponseHeaders().set(key, value));
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(400, bytes.length);
        out.write(bytes);
    }
}

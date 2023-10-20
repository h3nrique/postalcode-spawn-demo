package br.com.fabricads.poc.spawn.handler;

import br.com.fabricads.poc.proto.Postalcode;
import br.com.fabricads.poc.spawn.RestServer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.eigr.spawn.api.ActorIdentity;
import io.eigr.spawn.api.ActorRef;
import io.eigr.spawn.api.Spawn;
import io.eigr.spawn.api.exceptions.SpawnException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class PostalCodeHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(RestServer.class);
    private static final Pattern getPostalCodePattern = Pattern.compile("/postalcode/([0-9]{8})");
    private static final Pattern listPostalCodePattern = Pattern.compile("/postalcode");

    private final Spawn spawn;

    public PostalCodeHandler(Spawn spawn) {
        this.spawn = spawn;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        log.info("Received request.");
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
            Matcher getPostalCodeMatcher = getPostalCodePattern.matcher(path);
            Matcher listPostalCodeMatcher = listPostalCodePattern.matcher(path);
            if (getPostalCodeMatcher.matches()) {
                String postalCode = getPostalCodeMatcher.group(1);
                ActorRef actorRef = spawn
                        .createActorRef(ActorIdentity.of(spawn.getSystem(), postalCode, "postal_code"));
                Optional<Postalcode.PostalCodeState> actorState = actorRef.invoke("get",
                        Postalcode.PostalCodeState.class);
                if (actorState.isPresent()) {
                    byte[] bytes = actorState.get().toString().getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, (long) bytes.length);
                    out.write(bytes);
                } else {
                    byte[] bytes = "{\"error\": \"PostalCode not found\"}".getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(404, bytes.length);
                    out.write(bytes);
                }
            } else if (listPostalCodeMatcher.matches()) {
                byte[] bytes = "{\"error\": \"Not Implemented Yet\"}".getBytes();
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
            log.info("requestBody :: {}", requestBody);
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, String> map = new Gson().fromJson(requestBody, type);
            Pattern postalCodePattern = Pattern.compile("^[0-9]{8}$");
            Matcher postalCodeMatcher = postalCodePattern.matcher(map.getOrDefault("postalCode", ""));
            if (postalCodeMatcher.matches()) {
                ActorRef actorRef = spawn
                        .createActorRef(ActorIdentity.of(spawn.getSystem(), postalCodeMatcher.group(), "postal_code"));
                actorRef.invokeAsync("onCreate", Postalcode.CreateRequest.newBuilder()
                        .setCode(postalCodeMatcher.group())
                        .setCountry("Brasil")
                        .build());
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
            byte[] bytes = "{\"error\": \"Not Implemented Yet\"}".getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            out.write(bytes);
        } catch (IOException err) {
            log.error("Internal Error.", err);
            internalError(exchange);
        }

    }

    private void otherwiseRequest(HttpExchange exchange) throws IOException {
        try (OutputStream out = exchange.getResponseBody()) {
            byte[] bytes = "{\"error\": \"Invalid request\"}".getBytes();
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
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(400, bytes.length);
        out.write(bytes);
    }
}

# Postalcode Spawn Demo

Java demo project using [Spawn](https://github.com/eigr/spawn) to find postal codes at [ViaCEP](https://viacep.com.br).

## Locally start `Spawn Proxy` and some dependencies

```bash
docker network create spawn-demo
docker-compose up mariadb nats spawn
```

## Start Java `ActorHost` application

```bash
docker-compose up app
```

## Test

```bash
# Request PostalCode
curl -v -H 'Content-Type: application/json' -d '{ "postalCode": "03694090", "country": "Brasil" }' 'http://localhost:8080/postalcode'

# Get PostalCode info
curl -v 'http://localhost:8080/postalcode/03694090'
```


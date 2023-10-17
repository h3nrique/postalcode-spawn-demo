# postalcode-spawn-demo

Java demo project using [Spawn](https://github.com/eigr/spawn) to find postal codes at [ViaCEP](https://viacep.com.br).

# Start Spawn
```bash
docker network create spawn-demo
docker-compose up mariadb nats spawn
```

# Start Actor
```bash
docker-compose up app
```

# Test
```bash
# Request PostalCode
curl -v -H 'Content-Type: application/json' -d '{ "postalCode": "03694090" }' 'http://localhost:8080/postalcode'

# Get PostalCode info
curl -v 'http://localhost:8080/postalcode/03694090'
```


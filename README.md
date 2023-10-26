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

## To deploy on Openshift with Openshift Pipeline (Tekton)
```bash
oc create ns spawn
oc process openshift//mysql-persistent -e MYSQL_USER=admin -e MYSQL_PASSWORD=admin -e MYSQL_ROOT_PASSWORD=mypassword -e MYSQL_DATABASE=eigr | oc create -n spawn-demo -f -
oc create secret generic mysql-connection-secret -n eigr-functions --from-literal=database=eigr --from-literal=host='mysql.spawn.svc.cluster.local' --from-literal=port='3306' --from-literal=username='admin' --from-literal=password='admin' --from-literal=encryptionKey=$(openssl rand -base64 32)
tee actorsystem.yaml << END
apiVersion: spawn-eigr.io/v1
kind: ActorSystem
metadata:
  name: spawn-demo
spec:
#  selector:
#    app: spawn-demo
  statestore:
    type: MySql
    credentialsSecretRef: mysql-connection-secret
    pool:
      size: "10"
END
oc create -f actorsystem.yaml -n spawn
oc create -f openshift-pipeline.yaml -n spawn
# If you want to expose actor service with route command
oc create route edge postalcode --service=postalcode-spawn-demo --port=8080
```

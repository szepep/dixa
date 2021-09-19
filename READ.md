# Dixa Backend Engineer test

### Requirements

- java 11
- gradle
- docker
- docker-compose

### Project structure

```
.
├── proto - definitin of proto and gRPC
├── proxy-service - REST proxy 
├── prime-number-server - gRPC prime number generator
├── docker-compose.yaml
```

### Build

Run `./gradlew build` in the root repository.

### Run

To package and deploy the application you need to run

- `./gradlew bootBuildImage`
- `docker-compose up`

You can access the proxy e.g. on http://localhost:8080/prime/10.
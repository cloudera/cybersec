# Parser Chains

## With the Mock API
This scenario is good for developing the UI against backend services that do not exist yet.

Make sure you have the latest Node (LTS).

In order to run the application on your local computer, you need to run two services in parallel: the client code and the mock service (which provides mock data via http).

### Running the Mock API

```bash
cd frontend/parser-chains-api
npm ci
npm start
```

### Running the client application with a development server
The following spins up the client application using a development server. The dev server uses the proxy configuration set in proxy.conf.dev.json

```bash
cd frontend/parser-chains-client
npm ci
npm run start:dev
```

## With REST services
If you would like to develop the UI using services that currently exist, run the following:

```bash
docker-compose up
cd frontend/parser-chains-client
npm ci
npm run start:dev -- --port=4201
// because port 4200 is being used by docker
// serve the app on another port for development
```

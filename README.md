# Java starter for Wodby

A production-oriented HTTP application for the [Wodby Java service](https://github.com/wodby/service-java)
and [Java stack](https://github.com/wodby/stack-java).

It runs on Java 21 or later with no framework and no runtime dependencies, and demonstrates:

- routes on the JDK's built-in HTTP server (`jdk.httpserver`), with a virtual thread per request
- an HTML page and static assets bundled into the jar
- JSON, health, not-found and method-not-allowed responses
- request logging, graceful shutdown on SIGTERM, JUnit tests and Wodby CI

## Local development

```shell
./mvnw test
./mvnw package
java -jar target/app.jar
```

Open <http://localhost:8080>. Useful endpoints are:

- `/` — the landing page, with the running Java version
- `/assets/styles.css` — a static resource from the jar
- `/api/status` — a JSON response
- `/healthz` — the health endpoint the post-deployment check calls

Set `PORT` to use a different local port. On Wodby the application must listen on port 8080.

## Project structure

`Main.java` starts the server and stops it on shutdown; `App.java` holds the routes. Files under
`src/main/resources/web` are packaged into the jar, so the runtime image needs only `app.jar`.

The stack's PostgreSQL, Valkey and mail services are disabled by default, and the starter does not use them.
When you enable and link one, its connection settings arrive as environment variables, such as `DB_HOST`,
`DB_PORT`, `DB_NAME`, `DB_USERNAME` and `DB_PASSWORD` for a database. Add the JDBC driver or client you need to
`pom.xml` and read the variables at startup; the [Java service guidance](https://github.com/wodby/service-java/blob/main/GUIDANCE.md)
lists them all.

## Development workspace

In a Wodby development workspace, the checkout is built with the Maven wrapper and the application restarts with
the new build whenever a file changes, usually within a few seconds. A build that fails leaves the previous build
running; its errors are in the service's logs.

ARG WODBY_BASE_IMAGE
FROM ${WODBY_BASE_IMAGE} AS builder

# Download dependencies in their own layer, so a source change reuses them.
ARG COPY_FROM
COPY --chown=wodby:wodby ${COPY_FROM}/pom.xml ${COPY_FROM}/mvnw /usr/src/app/
COPY --chown=wodby:wodby ${COPY_FROM}/.mvn /usr/src/app/.mvn
RUN sh ./mvnw -B -ntp -q dependency:go-offline

COPY --chown=wodby:wodby ${COPY_FROM} /usr/src/app
RUN java-build /home/wodby/app.jar

FROM ${WODBY_BASE_IMAGE}
COPY --from=builder --chown=wodby:wodby /home/wodby/app.jar /home/wodby/app.jar

CMD ["java-app"]

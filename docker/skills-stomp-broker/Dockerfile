FROM rabbitmq:3.8-management-alpine

ADD join.sh /
COPY enabled_plugins /etc/rabbitmq/enabled_plugins

RUN apk add --no-cache bind-tools

RUN sed -i 's/exec "$@"/\
    sh -c "while ! nc -z localhost 15672; do sleep 0.1; done; sleep 3; .\/join.sh" \&\
    \nexec "$@"/' /usr/local/bin/docker-entrypoint.sh

ARG BUILD_DATE
ARG VERSION

LABEL classification="U" \
    description="RabbitMQ STOMP broker used to support Skills as a Service web socket connections in a clustered environment" \
    maintainer="Skills as a Service Team" \
    run="docker run --name skills-stomp-broker -d -p 15672:15672 -p 61613:61613 <registry location>/skills-stomp-broker" \
    source="Skills as a Service platform" \
    version="$VERSION" \
    buildTime="$BUILD_DATE"
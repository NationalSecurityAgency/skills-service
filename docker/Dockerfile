FROM eclipse-temurin:24-jdk-alpine-3.21

# install netcat so start script can use nc command to wait for DB to come up
RUN apk update && apk upgrade \
&& apk add --no-cache \
netcat-openbsd \
wget \
procps \
bash \
&& rm -rf /var/cache/apk/*

ARG BUILD_DATE
ARG VERSION
ARG VCS_REF=unspecified

LABEL org.label-schema.build-date=$BUILD_DATE
LABEL org.label-schema.license=Apache-2.0
LABEL org.label-schema.name=SkillTree
LABEL org.label-schema.schema-version=$VERSION
LABEL org.label-schema.url=https://github.com/NationalSecurityAgency/skills-service
LABEL org.label-schema.usage=https://code.nsa.gov/skills-docs/
LABEL org.label-schema.vcs-ref=$VCS_REF
LABEL org.label-schema.vendor=SkillTree
LABEL org.label-schema.vcs-url=https://github.com/NationalSecurityAgency/skills-service
LABEL org.label-schema.vendor=SkillTree
LABEL org.label-schema.version=7.8.0

LABEL org.opencontainers.image.created=$BUILD_DATE
LABEL org.opencontainers.image.documentation=https://code.nsa.gov/skills-docs/
LABEL org.opencontainers.image.licenses=Apache-2.0
LABEL org.opencontainers.image.revision=$VCS_REF
LABEL org.opencontainers.image.source=https://github.com/NationalSecurityAgency/skills-service
LABEL org.opencontainers.image.title=SkillTree
LABEL org.opencontainers.image.url=https://github.com/NationalSecurityAgency/skills-service
LABEL org.opencontainers.image.vendor=SkillTree
LABEL org.opencontainers.image.version=$VERSION

VOLUME /tmp

EXPOSE 80
EXPOSE 8443
EXPOSE 8808

RUN mkdir /data

COPY skills-service.jar skills.jar
COPY startup.sh startup.sh

ENTRYPOINT ["bash", "/startup.sh"]

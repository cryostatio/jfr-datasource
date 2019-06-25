FROM registry.access.redhat.com/ubi8/ubi

ENV QUARKUS_HOME=/home/quarkus

USER root

LABEL io.openshift.s2i.scripts-url="image:///usr/local/s2i" \
      io.k8s.description="jfr-datasource S2I Runtime Image" \
      io.k8s.display-name="jfr-datasource S2I Runtime" \
      io.openshift.tags="runtime,java,quarkus,native"

RUN mkdir -p /usr/local/s2i
COPY s2i/* /usr/local/s2i/
RUN chmod 755 /usr/local/s2i/*

RUN adduser -u 1001 -g 0 quarkus --home-dir ${QUARKUS_HOME} && \
     chown -R 1001:0 ${QUARKUS_HOME} && \
     chmod -R ug+rwX ${QUARKUS_HOME}

USER 1001

WORKDIR /home/quarkus

EXPOSE 8080

FROM quay.io/quarkus/ubi-quarkus-native-s2i:19.2.1

USER root

LABEL io.openshift.s2i.scripts-url="image:///usr/local/s2i" \
      io.openshift.s2i.destination="/tmp" \
      io.k8s.description="jfr-datasource S2I Builder" \
      io.k8s.display-name="jfr-datasource S2I Builder" \
      io.openshift.tags="builder,java,quarkus,native"


COPY s2i/* /usr/local/s2i/
RUN chmod 755 /usr/local/s2i/*

USER 1001
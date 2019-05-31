FROM quay.io/quarkus/centos-quarkus-native-s2i:latest

USER root

RUN yum install -y git

COPY s2i/assemble /usr/local/s2i/assemble
RUN chmod 755 /usr/local/s2i/assemble

USER 1001

RUN git clone https://github.com/JDKMissionControl/jmc && pushd jmc/core && mvn clean install && popd && rm -rf jmc

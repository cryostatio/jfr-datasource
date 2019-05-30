FROM quarkus/centos-quarkus-native-s2i:latest

USER root

RUN yum install -y git
RUN git clone https://github.com/JDKMissionControl/jmc && pushd jmc/core && mvn clean install && popd && rm -rf jmc

USER 1001
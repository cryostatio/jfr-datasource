FROM quay.io/quarkus/centos-quarkus-native-s2i:latest

USER root

RUN yum install -y git

USER 1001

RUN git clone https://github.com/JDKMissionControl/jmc && pushd jmc/core && mvn clean install && popd && rm -rf jmc

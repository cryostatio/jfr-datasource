# Deploying pre-built images on OpenShift

```
oc new-app https://quay.io/repository/rh-jmc-team/jfr-datasource
oc expose svc/jfr-datasource

oc new-app grafana/grafana:6.2.2 -e GF_INSTALL_PLUGINS=grafana-simple-json-datasource
oc expose svc/grafana
```

# Adding datasource and dashboard to the Grafana instance

Visit the grafana instance and add a simple-json-datasource with URL pointing to the jfr-datasource instance. For a pre-made dashboard that can be imported into Grafana, see [Dashboards](../dashboards)




aconcagua
=========

About metrics:
--------------

Running in docker:
------------------

* See config files section

```shell
docker network create --driver bridge metrics

docker run \
  --name=prometheus \
  -p 9090:9090 \
  --network metrics \
  -v ~/code/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus

docker run \
  --name=grafana \
  -p 3000:3000 \
  --network metrics \
  grafana/grafana:latest

```

* Head over `localhost:3000` in your browser to open up grafana
* User `admin` password `admin`
* Add datasource
   because we created a user defined network in docker we now can connect
   to prometheus by its container name and port: `prometheus:9090`
* Save and test (should show a green popup)
* Create dashboard
* Add panel
* In the `metrics` text box start typing `total_requests`
* Repeat

Running on your host machine:
-----------------------------

```shell
prometheus --config.file="prometheus.yml"

grafana-server \
  --config=/usr/local/etc/grafana/grafana.ini \
  --homepath /usr/local/share/grafana \
  --packaging=brew cfg:default.paths.logs=/usr/local/var/log/grafana \
  cfg:default.paths.data=/usr/local/var/lib/grafana \
  cfg:default.paths.plugins=/usr/local/var/lib/grafana/plugins
```

Config files
------------
`prometheus.yml` config file

```yml
global:
  scrape_interval: 15s # By default, scrape targets every 15 seconds.

  # Attach these labels to any time series or alerts when communicating with
  # external systems (federation, remote storage, Alertmanager).
  external_labels:
    monitor: 'codelab-monitor'

# A scrape configuration containing exactly one endpoint to scrape:
# Here it's Prometheus itself.
scrape_configs:
  # The job name is added as a label `job=<job_name>` to any timeseries scraped from this config.
  - job_name: 'aconcagua'

    # Override the global default and scrape targets from this job every 5 seconds.
    scrape_interval: 1s

    static_configs:
      #      - targets: [ 'localhost:8080' ] # if prometheus and your app runs on docker
      - targets: [ 'host.docker.internal:8080' ] # if prometheus runs on docker but your app doesn't

```

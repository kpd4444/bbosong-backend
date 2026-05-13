# Bbosong Performance Test Plan

## Goal

Measure and visualize the clothes image analysis API under baseline, load, monitoring, scale-out, and verification phases.

## Tooling

| Tool | Role |
|---|---|
| k6 | Load test runner |
| Spring Actuator | Application metrics endpoint |
| Prometheus | Metrics collection |
| Grafana | Dashboard visualization |
| Docker Compose | Local app and monitoring stack |

## Phase 1: Baseline

- Scenario: upload one clothing image and wait for analysis result.
- Tool: k6, Spring Actuator.
- Target: average latency under 5s, P95 under 15s.
- Script: `k6/phase1-baseline.js`.
- Keep the same script and image for Phase 5.

## Phase 2: Load Test

- Scenario: gradually increase virtual users.
- Default stages: `50 -> 100 -> 200 -> 300 -> 500`.
- Tool: k6.
- Script: `k6/phase2-load.js`.
- Observe whether OpenAI latency, timeout, DB connection, CPU, or memory fails first.

## Phase 2-2: Stress Test

- Scenario: push beyond expected load.
- Default stages: `500 -> 800 -> 1000`.
- Tool: k6.
- Script: `k6/phase2-stress.js`.
- This phase is for finding the breaking point, not for pass/fail validation.

## Phase 3: Monitoring

- Scenario: run Phase 2 while watching Grafana.
- Tools: Prometheus and Grafana.
- Dashboard: `Bbosong Performance Overview`.
- Key panels:
  - HTTP RPS
  - HTTP P95 latency
  - 5xx error ratio
  - CPU usage
  - JVM memory
  - Hikari active/pending connections

## Phase 4: Solution Application

- Scenario: scale logic server replicas after identifying bottleneck.
- Target demo command:

```bash
docker service scale bbosong_logic=3
```

The current repository is a single Spring Boot application. A true security-server and logic-server split requires separate service boundaries. Until that split exists, Docker Swarm scale-out should be treated as application replica scale-out.

## Phase 5: Verification

- Scenario: rerun the same condition as Phase 1 or Phase 2 after improvement.
- Tool: k6.
- Script: `k6/phase5-verify.js`.
- Compare:
  - before/after P95 latency
  - error rate
  - response field regression such as `material` and `washingMethod`

## Local Run

Start app and monitoring:

```bash
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

If `.env` is not present, create it first because the existing app compose file requires DB, JWT, and OpenAI values.

The default Prometheus config is for Docker Compose and scrapes:

```text
app:8080
```

Open:

```text
Prometheus: http://localhost:9090
Grafana:    http://localhost:3000
```

Grafana default login:

```text
admin / admin
```

Run baseline:

```bash
docker compose -f docker-compose.monitoring.yml run --rm k6 run k6/phase1-baseline.js
```

Run load test:

```bash
docker compose -f docker-compose.monitoring.yml run --rm k6 run k6/phase2-load.js
```

Run stress test:

```bash
docker compose -f docker-compose.monitoring.yml run --rm k6 run k6/phase2-stress.js
```

Run verification:

```bash
docker compose -f docker-compose.monitoring.yml run --rm k6 run k6/phase5-verify.js
```

## Swarm Demo

Deploy stack:

```bash
docker stack deploy -c docker-stack.yml bbosong
```

The Swarm stack uses `monitoring/prometheus/prometheus-swarm.yml` and scrapes:

```text
bbosong_logic:8080
```

Scale the logic service:

```bash
docker service scale bbosong_bbosong_logic=3
```

Before running k6, place a real clothing image at:

```text
tests/fixtures/clothes.jpg
```

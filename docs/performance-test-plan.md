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
- Target: average latency under 15s, P95 under 20s.
- Script: `k6/baseline.js`.
- Keep the same script and image for Phase 5.

## Phase 2: Load Test

- Scenario: gradually increase virtual users.
- Default stages: `1 -> 2 -> 3 -> 4`.
- Tool: k6.
- Script: `k6/load.js`.
- Observe whether OpenAI latency, timeout, DB connection, CPU, or memory fails first.

## Phase 2-2: Stress Test

- Scenario: push beyond expected load.
- Default stages: `1 -> 3 -> 5 -> 10`.
- Tool: k6.
- Script: `k6/stress.js`.
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
docker service scale bbosong_bbosong_logic=3
```

The current repository is a single Spring Boot application. A true security-server and logic-server split requires separate service boundaries. Until that split exists, Docker Swarm scale-out should be treated as application replica scale-out.

## Phase 5: Verification

- Scenario: rerun the same condition as Phase 1 or Phase 2 after improvement.
- Tool: k6.
- Script: `k6/verify.js`.
- Compare:
  - before/after P95 latency
  - error rate
  - response field regression such as `material` and `washingMethod`

## Local Run

Start app and monitoring:

```bash
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

If `.env` is not present, create it first because the existing app compose file requires DB, JWT, OpenAI, and Grafana credential values.

The k6 compose service uses the Docker Compose service DNS name:

```text
BASE_URL=http://app:8080
```

The default Prometheus config is for Docker Compose and scrapes:

```text
host.docker.internal:8080
```

Open:

```text
Prometheus: http://localhost:9090
Grafana:    http://localhost:3000
```

Run baseline:

```bash
docker compose run --rm k6 run baseline.js
```

Run load test:

```bash
docker compose run --rm k6 run load.js
```

Run stress test:

```bash
docker compose run --rm k6 run stress.js
```

Run verification:

```bash
docker compose run --rm k6 run verify.js
```

Run accuracy smoke test:

```bash
docker compose run --rm -e K6_EXPECTED_PATH=tests/fixtures/expected-reliable.json -e K6_ACCURACY_ITERATIONS=1 k6 run accuracy.js
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

The default k6 image is:

```text
k6/tests/fixtures/black_jacket_001.png
```

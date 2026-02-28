# Environment

- TEMPLATEPORT defaults to 8104.
- Application listens on ${TEMPLATEPORT} (and defaults to 8104) behind /api/v1.
- Dockerfile exposes 8104; docker-compose maps host 8104 to container 8104.
- Tests run via Testcontainers, so they ignore this port directly but still rely on server.port=8104 when the app boots locally.

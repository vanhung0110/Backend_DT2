Docker & Deployment (quick start)

Prerequisites
- Docker and Docker Compose installed
- (Optional) set env vars for `VOICE_AGORA_APP_ID` and `VOICE_AGORA_APP_CERTIFICATE` before starting if you want real Agora tokens

Local development (with MySQL container)
1) Build and start services:
   docker compose up --build

   - App will be available at http://localhost:8080
   - MySQL will be available at port 3306 and the DB `backend_vnchat` will be created

2) Environment variables
   - You can set values via env or pass them into docker-compose. The most important are:
     - `VOICE_AGORA_APP_ID` and `VOICE_AGORA_APP_CERTIFICATE` for Agora production tokens
     - `APP_JWT_SECRET` to override the default JWT secret

Notes / Production
- The compose file is for local development. For production use:
  - Use an external managed database (RDS, Azure DB, etc.) and provide connection via `SPRING_DATASOURCE_URL`/username/password
  - Ensure `APP_JWT_SECRET` is sufficiently long and secrets are stored in a secret manager
  - Consider multi-stage deployments, health checks, resource limits, and a CI pipeline to build and publish images

Migrations
- Flyway migrations are included under `src/main/resources/db/migration`. The container will run migrations at startup (if `spring.flyway.enabled=true`).

If you want, I can:
- Add a small `Dockerfile` label & metadata, push image to a registry, and open a draft PR with these artifacts.
- Add Kubernetes manifests (Deployment + Service + Secret + PersistentVolumeClaim) for production.

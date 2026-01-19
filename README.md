# hungdt2 — Auth PoC

Run:
- Configure DB in `src/main/resources/application.properties` or via environment variables (spring.datasource.*). Project currently uses Flyway to run `V1__create_users.sql`.
- Set JWT secret in `application.properties` or env var `APP_JWT_SECRET` (property `app.jwt.secret`).
- Build & run:

Windows (PowerShell):
.
```
.\mvnw.cmd -DskipTests package
.\mvnw.cmd spring-boot:run
```

APIs (examples):

Register:
```
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"namnguyen","email":"nam@gmail.com","phone":"0909","password":"P@ssw0rd123","displayName":"Nam Nguyen"}'
```

Expected 201 response shape:
```
{ "data": { "id": 1, "username": "namnguyen", "email":"nam@gmail.com", "phone":"0909", "displayName":"Nam Nguyen", "isActive": true } }
```

Login:
```
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"namnguyen","password":"P@ssw0rd123"}'
```
Expected 200 response shape:
```
{ "data": { "accessToken": "jwt...", "tokenType":"Bearer", "expiresIn": 3600 } }
```

Me (use token):
```
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/users/me
```
Expected 200 response shape:
```
{ "data": { "id": 1, "username": "namnguyen", "email":"nam@gmail.com", "phone":"0909", "displayName":"Nam Nguyen" } }
```

Notes:
- Configure DB via `application.properties` or env variables (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).
- Set `app.jwt.secret` to a secure 32+ byte secret in production.

# Todo List

Aplicacao com frontend Angular e backend Spring Boot.

## Local sem Docker

Backend local com H2 em arquivo:

```powershell
cd todo_back-end
$env:SPRING_PROFILES_ACTIVE='local'
.\gradlew.bat bootRun
```

Frontend:

```powershell
cd todo_front-end/todo-list
npm install
npm start -- --host 127.0.0.1 --port 4200
```

URLs:

- Frontend: `http://127.0.0.1:4200`
- Backend: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`

## Docker

Configure `todo_back-end/.env` usando `todo_back-end/.env.example` como base.

```powershell
cd todo_back-end
docker compose up --build
```

O Compose sobe:

- `todo-db`: PostgreSQL
- `todo-backend`: Spring Boot
- `todo-frontend`: Angular

## Autenticacao

O login usa JWT com refresh token.

- O backend ainda retorna `token` e `refreshToken` no corpo para clientes API.
- O navegador usa cookies `HttpOnly` (`todo_access_token` e `todo_refresh_token`).
- O frontend guarda apenas metadados nao sensiveis da sessao e perfil.

Endpoints principais:

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `POST /auth/forgot-password`
- `POST /auth/reset-password`
- `GET /profile`
- `PUT /profile/password`
- `PUT /profile/photo`
- `GET /task`
- `POST /task`
- `PUT /task/{id}`
- `PUT /task/concluir/{id}`
- `DELETE /task/{id}`

## Recuperacao de senha por e-mail

Para envio real de e-mail, configure SMTP no `todo_back-end/.env`:

```properties
SPRING_MAIL_HOST=smtp.example.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-user
SPRING_MAIL_PASSWORD=your-password
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
APP_MAIL_FROM=no-reply@example.com
```

Sem SMTP, o backend registra o link de reset no log para desenvolvimento.

## Fotos de perfil

O frontend envia a imagem em data URL, mas o backend grava arquivo em disco e salva apenas a URL no banco.

- Local: `todo_back-end/.data/profile-photos`
- Docker: volume `profile_photos`
- URL publica: `/uploads/profile-photos/...`

## Validacao

Backend:

```powershell
cd todo_back-end
.\gradlew.bat test --no-daemon --console=plain
```

Frontend:

```powershell
cd todo_front-end/todo-list
npm run build
npm test -- --watch=false --browsers=ChromeHeadless
```

Smoke E2E com frontend e backend rodando:

```powershell
cd todo_front-end/todo-list
npm run test:e2e
```

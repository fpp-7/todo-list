# Todo List

Monorepo com frontend Angular e backend Spring Boot para autenticacao com JWT/refresh token, reset de senha, foto de perfil e gestao de tarefas.

## Estrutura

- `todo_front-end/todo-list`: aplicacao Angular.
- `todo_back-end`: API Spring Boot, Flyway, H2 para desenvolvimento local e PostgreSQL para Docker/producao.

## Requisitos

- Java 21
- Node.js 20+ e npm
- Docker Desktop, se quiser subir tudo com containers

## Configuracao de ambiente

O backend concentra as variaveis de ambiente em `todo_back-end/.env`.

```powershell
cd todo_back-end
Copy-Item .env.example .env
```

Variaveis mais importantes:

- `JWT_SECRET`: obrigatoria; use um valor forte e aleatorio.
- `APP_FRONTEND_URL`: URL base usada em CORS, cookies e links de reset.
- `SECURE_COOKIES` e `COOKIE_SAME_SITE`: ajuste para o ambiente em que a aplicacao roda.
- `APP_LOG_DIR`, `ROOT_LOG_LEVEL`, `APP_LOG_LEVEL`, `SQL_LOG_LEVEL`: controlam onde os logs vao e qual verbosidade sera usada.
- `SPRING_MAIL_*` e `APP_MAIL_FROM`: opcionais; habilitam envio real de e-mail para reset de senha.

Sem SMTP configurado, o backend continua funcionando e registra o link de reset no log.

## Rodando sem Docker

Backend com profile local e banco H2 em arquivo:

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

URLs locais:

- Frontend: `http://127.0.0.1:4200`
- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Rodando com Docker

```powershell
cd todo_back-end
docker compose up --build
```

O compose sobe:

- `todo-db`: PostgreSQL 15
- `todo-backend`: API Spring Boot
- `todo-frontend`: aplicacao Angular

Volumes usados:

- `postgres_data`: dados do PostgreSQL
- `profile_photos`: fotos de perfil persistidas pelo backend
- `backend_logs`: arquivos de log do backend dentro do container

## Logs

O backend usa `logback-spring.xml` com saida em console e arquivo.

- Local sem Docker: `todo_back-end/.logs/todo-backend.log`
- Docker: `/app/.logs/todo-backend.log` dentro do container, persistido no volume `backend_logs`
- Rotacao: diaria, com rollover por tamanho e historico de 14 dias

Niveis padrao por profile:

- `local`: aplicacao em `DEBUG`, SQL em `DEBUG`
- `prod`: aplicacao em `INFO`, SQL em `WARN`
- `test`: aplicacao e root em `WARN`

Voce pode sobrescrever com:

- `ROOT_LOG_LEVEL`
- `APP_LOG_LEVEL`
- `SQL_LOG_LEVEL`
- `APP_LOG_DIR`

## Autenticacao e sessao

O login usa access token + refresh token.

- O backend ainda retorna `token` e `refreshToken` no corpo para clientes API.
- O navegador usa cookies `HttpOnly` (`todo_access_token` e `todo_refresh_token`).
- O frontend guarda apenas metadados nao sensiveis da sessao e do perfil.

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

## API de tarefas

Endpoints principais:

- `GET /task`
- `POST /task`
- `PUT /task/{id}`
- `PUT /task/concluir/{id}`
- `DELETE /task/{id}`

### `GET /task` sem query params

Mantem compatibilidade com o comportamento antigo e retorna um array simples:

```json
[
  {
    "id": 10,
    "name": "Planejar sprint",
    "description": "Fechar escopo",
    "category": "Produto",
    "priority": "Alta",
    "dueDate": "2026-04-20",
    "done": false
  }
]
```

### `GET /task` com paginação e filtros

Se a requisicao incluir `page`, `size` ou qualquer filtro, a resposta passa a ser paginada.

Parametros aceitos:

- `page`: indice zero-based; padrao `0`
- `size`: tamanho da pagina; padrao `10`, maximo `100`
- `query`: busca parcial em nome, descricao e categoria
- `category`: filtro parcial por categoria
- `priority`: filtro exato por prioridade
- `done`: `true` ou `false`
- `status`: `hoje`, `em_andamento`, `planejada` ou `concluida`
- `dueDateFrom`: data inicial no formato `yyyy-MM-dd`
- `dueDateTo`: data final no formato `yyyy-MM-dd`

Ordenacao padrao: `id DESC`.

Exemplo:

```http
GET /task?page=0&size=5&status=planejada&query=planejar&priority=Alta
```

Resposta:

```json
{
  "items": [
    {
      "id": 42,
      "name": "Planejar release",
      "description": "Consolidar checklist",
      "category": "Produto",
      "priority": "Alta",
      "dueDate": "2026-04-22",
      "done": false
    }
  ],
  "page": 0,
  "size": 5,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

## Fotos de perfil

O frontend envia a imagem em data URL, mas o backend grava o arquivo em disco e salva apenas a URL publica no banco.

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

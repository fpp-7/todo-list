# 📝 Todo List

> Monorepo com **frontend Angular 20** e **backend Spring Boot 3** — autenticação JWT com refresh token, recuperação de senha por e-mail, foto de perfil e gerenciamento completo de tarefas.

---

## 📁 Estrutura do Projeto

```
todo-list-main/
├── todo_back-end/          # API REST — Spring Boot 3 + Java 21
│   ├── src/
│   │   └── main/
│   │       ├── java/       # Código-fonte Java (controllers, services, models…)
│   │       └── resources/  # Configurações e migrações Flyway
│   ├── Dockerfile
│   ├── docker-compose.yml  # Orquestração completa (DB + Mailpit + Backend + Frontend)
│   └── .env.example        # Modelo de variáveis de ambiente
│
└── todo_front-end/
    └── todo-list/          # SPA Angular 20
        ├── src/app/
        │   ├── components/ # task-form, task-list
        │   ├── pages/      # login-page, register-page, reset-password-page
        │   └── core/       # auth, api, tasks, profile, theme, toast
        └── Dockerfile
```

---

## ✅ Pré-requisitos

| Ferramenta | Versão mínima | Usado para |
|---|---|---|
| Java | 21 | Rodar o backend localmente |
| Node.js + npm | 20+ | Rodar o frontend localmente |
| Docker Desktop | qualquer atual | Subir tudo com containers |

---

## ⚙️ Configuração do Ambiente

O backend centraliza todas as variáveis de ambiente em `todo_back-end/.env`.

```powershell
cd todo_back-end
Copy-Item .env.example .env
```

Edite o `.env` e configure ao menos o `JWT_SECRET`. As demais variáveis têm valores padrão razoáveis para desenvolvimento local.

### Variáveis de Ambiente

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `JWT_SECRET` | ✅ Sim | — | Chave secreta para assinar os JWTs. Use um valor longo e aleatório. |
| `JWT_EXPIRATION_MINUTES` | Não | `15` | Tempo de vida do access token em minutos. |
| `REFRESH_TOKEN_EXPIRATION_DAYS` | Não | `7` | Tempo de vida do refresh token em dias. |
| `PASSWORD_RESET_EXPIRATION_MINUTES` | Não | `30` | Validade do link de redefinição de senha. |
| `APP_FRONTEND_URL` | Não | `http://localhost:4200` | URL base usada em CORS, cookies e links de reset. |
| `SECURE_COOKIES` | Não | `false` | Marcar cookies como `Secure` (usar `true` em HTTPS). |
| `COOKIE_SAME_SITE` | Não | `Lax` | Política SameSite dos cookies (`Lax`, `Strict` ou `None`). |
| `PROFILE_PHOTO_MAX_BYTES` | Não | `2097152` | Tamanho máximo de foto de perfil em bytes (padrão: 2 MB). |
| `APP_LOG_DIR` | Não | `.logs` | Diretório onde os arquivos de log serão gravados. |
| `ROOT_LOG_LEVEL` | Não | `INFO` | Nível de log raiz. |
| `APP_LOG_LEVEL` | Não | `INFO` | Nível de log da aplicação. |
| `SQL_LOG_LEVEL` | Não | `WARN` | Nível de log do Hibernate/SQL. |
| `SPRING_MAIL_HOST` | Não | — | Host SMTP para envio de e-mails de reset. |
| `SPRING_MAIL_PORT` | Não | — | Porta SMTP. |
| `APP_MAIL_FROM` | Não | `no-reply@todo.local` | Endereço remetente dos e-mails. |

> **Dica:** Com Docker, o projeto já sobe um **Mailpit** local para capturar e-mails de recuperação de senha sem depender de um SMTP real. Para usar o Mailpit fora do Docker, defina `SPRING_MAIL_HOST=127.0.0.1` e `SPRING_MAIL_PORT=1025`.

---

## 🚀 Rodando sem Docker

### Backend (perfil `local` com banco H2 em arquivo)

```powershell
cd todo_back-end
$env:SPRING_PROFILES_ACTIVE = 'local'
.\gradlew.bat bootRun
```

Para testar recuperação de senha sem SMTP real, suba apenas o Mailpit:

```powershell
# Em outro terminal, dentro de todo_back-end/
docker compose up todo-mailpit
```

### Frontend

```powershell
cd todo_front-end/todo-list
npm install
npm start -- --host 127.0.0.1 --port 4200
```

### URLs locais (sem Docker)

| Serviço | URL |
|---|---|
| Frontend | `http://127.0.0.1:4200` |
| Backend / API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Mailpit (inbox) | `http://localhost:8025` |

---

## 🐳 Rodando com Docker

```powershell
cd todo_back-end
docker compose up --build
```

O `docker-compose.yml` orquestra quatro serviços:

| Container | Imagem | Porta | Descrição |
|---|---|---|---|
| `todo-db` | `postgres:15` | `5432` | Banco de dados PostgreSQL |
| `todo-mailpit` | `axllent/mailpit` | `1025` / `8025` | SMTP local + interface web de e-mails |
| `todo-backend` | Build local | `8080` | API Spring Boot |
| `todo-frontend` | Build local | `4200` | SPA Angular |

### Volumes Persistidos

| Volume | Conteúdo |
|---|---|
| `postgres_data` | Dados do PostgreSQL |
| `profile_photos` | Fotos de perfil enviadas pelos usuários |
| `backend_logs` | Arquivos de log do backend |

---

## 🔐 Autenticação e Sessão

O sistema usa **access token + refresh token** com rotação automática.

- O backend retorna `token` e `refreshToken` no corpo da resposta (útil para clientes de API).
- O **navegador** recebe e envia os tokens via cookies `HttpOnly` (`todo_access_token` e `todo_refresh_token`), sem expô-los ao JavaScript.
- O frontend armazena apenas metadados não sensíveis da sessão (nome de exibição, foto de perfil etc.).

### Endpoints de Autenticação

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/auth/register` | Cria uma nova conta |
| `POST` | `/auth/login` | Autentica e retorna tokens |
| `POST` | `/auth/refresh` | Renova o access token com o refresh token |
| `POST` | `/auth/logout` | Revoga o refresh token e limpa os cookies |
| `POST` | `/auth/forgot-password` | Solicita e-mail de redefinição de senha |
| `POST` | `/auth/reset-password` | Confirma redefinição com token recebido por e-mail |

### Endpoints de Perfil

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/profile` | Retorna dados do perfil do usuário autenticado |
| `PUT` | `/profile/password` | Altera a senha |
| `PUT` | `/profile/photo` | Atualiza a foto de perfil |

---

## ✅ API de Tarefas

Todos os endpoints requerem autenticação. As tarefas são isoladas por usuário.

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/task` | Lista tarefas (simples ou paginada, veja abaixo) |
| `POST` | `/task` | Cria uma nova tarefa |
| `PUT` | `/task/{id}` | Atualiza uma tarefa existente |
| `PUT` | `/task/concluir/{id}` | Marca uma tarefa como concluída |
| `DELETE` | `/task/{id}` | Remove uma tarefa |

### `GET /task` — Modo Simples

Sem parâmetros de query, retorna um array simples de tarefas:

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

### `GET /task` — Modo Paginado com Filtros

Se incluir `page`, `size` ou qualquer filtro, a resposta será paginada.

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `page` | `integer` | Índice zero-based (padrão: `0`) |
| `size` | `integer` | Itens por página (padrão: `10`, máximo: `100`) |
| `query` | `string` | Busca parcial em nome, descrição e categoria |
| `category` | `string` | Filtro parcial por categoria |
| `priority` | `string` | Filtro exato por prioridade (`Alta`, `Média`, `Baixa`) |
| `done` | `boolean` | Filtra por status de conclusão (`true` ou `false`) |
| `status` | `string` | `hoje`, `em_andamento`, `planejada` ou `concluida` |
| `dueDateFrom` | `date` | Data de vencimento inicial (`yyyy-MM-dd`) |
| `dueDateTo` | `date` | Data de vencimento final (`yyyy-MM-dd`) |

**Exemplo de requisição:**

```http
GET /task?page=0&size=5&status=planejada&query=planejar&priority=Alta
```

**Exemplo de resposta:**

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

---

## 🖼️ Fotos de Perfil

O frontend envia a imagem como data URL. O backend converte, grava o arquivo em disco e salva apenas a URL pública no banco de dados.

| Ambiente | Localização |
|---|---|
| Local (sem Docker) | `todo_back-end/.data/profile-photos/` |
| Docker | Volume `profile_photos` → `/app/uploads/profile-photos/` |
| URL pública | `/uploads/profile-photos/<filename>` |

---

## 📋 Logs

O backend usa `logback-spring.xml` com saída simultânea em console e arquivo, com rotação diária e por tamanho (histórico de 14 dias).

| Ambiente | Caminho do log |
|---|---|
| Local (sem Docker) | `todo_back-end/.logs/todo-backend.log` |
| Docker | `/app/.logs/todo-backend.log` (volume `backend_logs`) |

### Níveis padrão por perfil

| Perfil | `APP_LOG_LEVEL` | `SQL_LOG_LEVEL` |
|---|---|---|
| `local` | `DEBUG` | `DEBUG` |
| `prod` | `INFO` | `WARN` |
| `test` | `WARN` | `WARN` |

---

## 🧪 Testes

### Backend

```powershell
cd todo_back-end
.\gradlew.bat test --no-daemon --console=plain
```

### Frontend — Unitários

```powershell
cd todo_front-end/todo-list
npm run build
npm test -- --watch=false --browsers=ChromeHeadless
```

### Frontend — Smoke E2E (requer frontend e backend rodando)

```powershell
cd todo_front-end/todo-list
npm run test:e2e
```

---

## 🛠️ Tecnologias

### Backend
- **Java 21** + **Spring Boot 3.5**
- **Spring Security** — autenticação stateless com JWT (biblioteca `com.auth0:java-jwt`)
- **Spring Data JPA** + **Hibernate**
- **Flyway** — migrações de banco de dados versionadas
- **H2** — banco em memória/arquivo para desenvolvimento e testes
- **PostgreSQL 15** — banco em produção/Docker
- **Lombok** — redução de boilerplate
- **springdoc-openapi** — documentação Swagger UI automática
- **Mailpit** — servidor SMTP fake para testes de e-mail

### Frontend
- **Angular 20** com componentes standalone
- **TypeScript 5.9**
- **RxJS 7.8**
- Lazy loading de rotas com guards de autenticação (`authGuard` / `publicOnlyGuard`)
- Interceptor HTTP para renovação automática de tokens

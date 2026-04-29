# ⚙️ Backend — Todo List API

API REST construída com **Spring Boot 3.5** e **Java 21**, responsável por autenticação, gerenciamento de tarefas, perfil de usuário e recuperação de senha por e-mail.

---

## 🗂️ Estrutura de Pacotes

```
src/main/java/br/com/project/to_do/
├── ToDoItApplication.java        # Classe principal (entry point)
├── controller/
│   ├── AuthenticationController  # Endpoints /auth/*
│   ├── ProfileController         # Endpoints /profile/*
│   └── TaskController            # Endpoints /task/*
├── service/
│   ├── TokenService              # Geração e validação de JWT
│   ├── RefreshTokenService       # Rotação de refresh tokens
│   ├── AuthCookieService         # Gerenciamento de cookies HttpOnly
│   ├── PasswordResetService      # Fluxo de redefinição de senha
│   └── TaskService               # Lógica de negócio de tarefas
├── model/
│   ├── Member                    # Entidade de usuário (implementa UserDetails)
│   ├── Task                      # Entidade de tarefa
│   ├── RefreshToken              # Entidade de refresh token persistido
│   └── PasswordResetToken        # Entidade de token de reset de senha
├── repository/                   # Interfaces Spring Data JPA
├── dto/                          # Records de request/response
├── exception/                    # Exceções de negócio personalizadas
└── infra/
    ├── SecurityConfigurations    # Configuração do Spring Security + CORS + Swagger
    ├── SecurityFilter            # Filtro JWT (extrai token de cookie ou header)
    ├── GlobalExceptionHandler    # Handler global de erros (@ControllerAdvice)
    └── StaticResourceConfig      # Serve fotos de perfil como recurso estático
```

```
src/main/resources/
├── application.properties            # Configuração base (lê variáveis de ambiente)
├── application-local.properties      # Perfil local: H2, DEBUG, DevTools
├── application-prod.properties       # Perfil prod: PostgreSQL, INFO
├── application-test.properties       # Perfil test: H2 em memória
├── logback-spring.xml                # Configuração de logging (console + arquivo)
└── db/migration/
    ├── V1__init_schema.sql           # Cria tabelas member e task
    └── V2__auth_tokens.sql           # Cria tabelas refresh_token e password_reset_token
```

---

## 🔧 Configuração e Execução

> Consulte o **[README raiz](../README.md)** para instruções completas de configuração de ambiente (`.env`), execução local, execução com Docker e detalhes de todos os endpoints.

### Execução rápida (local)

```powershell
# Na pasta todo_back-end/
Copy-Item .env.example .env
# Edite .env e defina JWT_SECRET

$env:SPRING_PROFILES_ACTIVE = 'local'
.\gradlew.bat bootRun
```

O perfil `local` usa **H2** em arquivo (sem necessidade de PostgreSQL) e ativa `spring-boot-devtools` para live reload.

---

## 🏗️ Build e Testes

```powershell
# Compilar e testar
.\gradlew.bat test --no-daemon --console=plain

# Gerar JAR executável
.\gradlew.bat bootJar --no-daemon --console=plain
```

---

## 🐳 Docker

O `Dockerfile` usa **multi-stage build**:

1. **Stage `build`** — `eclipse-temurin:21-jdk` compila o projeto com Gradle e gera o JAR.
2. **Stage final** — `eclipse-temurin:21-jre` copia apenas o JAR e expõe a porta `8080`.

Para subir tudo (backend + frontend + banco + mailpit):

```powershell
docker compose up --build
```

---

## 🔑 Segurança

- **Autenticação stateless** com Spring Security — sem sessão HTTP no servidor.
- Tokens JWT assinados com `com.auth0:java-jwt` e validados em cada requisição pelo `SecurityFilter`.
- Refresh tokens persistidos no banco e invalidados no logout.
- Senhas armazenadas com `BCryptPasswordEncoder`.
- CORS configurável via variável `APP_CORS_ALLOWED_ORIGINS`.
- Rotas públicas: `/auth/**`, `/uploads/profile-photos/**`, `/swagger-ui/**`, `/v3/api-docs/**`.

---

## 📖 Documentação da API

Com o backend rodando, acesse:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

---

## 📦 Dependências Principais

| Dependência | Versão | Finalidade |
|---|---|---|
| Spring Boot | 3.5.13 | Framework principal |
| Spring Security | (via BOM) | Autenticação e autorização |
| Spring Data JPA + Hibernate | (via BOM) | Persistência |
| Flyway | (via BOM) | Migrações de banco |
| com.auth0:java-jwt | 4.4.0 | Geração e validação de JWT |
| springdoc-openapi-starter-webmvc-ui | 2.8.9 | Swagger UI automático |
| Lombok | (via BOM) | Redução de boilerplate |
| H2 | (via BOM) | Banco em memória/arquivo (dev e testes) |
| PostgreSQL | (via BOM) | Banco de dados de produção |
| Spring Boot Starter Mail | (via BOM) | Envio de e-mails de reset de senha |

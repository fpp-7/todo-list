# 🖥️ Frontend — Todo List

SPA (Single Page Application) construída com **Angular 20**, responsável pela interface de autenticação, gerenciamento de tarefas e perfil do usuário.

---

## 🗂️ Estrutura do Projeto

```
src/
├── index.html              # HTML raiz da aplicação
├── main.ts                 # Ponto de entrada do Angular
├── styles.css              # Estilos globais
└── app/
    ├── app.ts              # Componente raiz
    ├── app.routes.ts       # Definição de rotas (lazy loading)
    ├── app.config.ts       # Configuração global (providers, interceptors)
    │
    ├── pages/              # Telas carregadas por lazy loading
    │   ├── login-page/         # Tela de login
    │   ├── register-page/      # Tela de cadastro
    │   └── reset-password-page/ # Tela de redefinição de senha
    │
    ├── components/         # Componentes reutilizáveis
    │   ├── task-list/          # Lista principal de tarefas (tela principal)
    │   └── task-form/          # Formulário de criação/edição de tarefa
    │
    └── core/               # Serviços e infraestrutura
        ├── auth/
        │   ├── auth.guard.ts        # Guards: authGuard / publicOnlyGuard
        │   ├── auth.interceptor.ts  # Interceptor HTTP (renova token automaticamente)
        │   ├── auth.dtos.ts         # Tipos de request/response de autenticação
        │   ├── session.service.ts   # Gerencia o estado da sessão do usuário
        │   └── access-api.service.ts # Chamadas HTTP de autenticação
        ├── api/            # Cliente HTTP base
        ├── tasks/          # Serviço e tipos de tarefas
        ├── profile/        # Serviço de perfil (foto, senha)
        ├── theme/          # Gerenciamento de tema (claro/escuro)
        └── toast/          # Sistema de notificações toast
```

---

## 🔀 Rotas

| Rota | Componente | Proteção |
|---|---|---|
| `/` | Redireciona para `/login` | — |
| `/login` | `LoginPage` | `publicOnlyGuard` (redireciona se já autenticado) |
| `/register` | `RegisterPage` | `publicOnlyGuard` |
| `/reset-password` | `ResetPasswordPage` | `publicOnlyGuard` |
| `/tasks` | `TaskList` | `authGuard` (requer autenticação) |
| `/dashboard` | Redireciona para `/tasks` | — |
| `/**` | Redireciona para `/login` | — |

---

## 🛡️ Autenticação no Frontend

- Os tokens são armazenados em **cookies `HttpOnly`** gerenciados pelo backend — o JavaScript não tem acesso direto a eles.
- O `SessionService` mantém em memória apenas metadados não sensíveis (nome de exibição, URL da foto de perfil).
- O `AuthInterceptor` detecta respostas `401` e tenta renovar o access token via `/auth/refresh` automaticamente, repetindo a requisição original após o refresh bem-sucedido.
- Os guards `authGuard` e `publicOnlyGuard` protegem as rotas, redirecionando o usuário conforme o estado da sessão.

---

## 🚀 Executando Localmente

### Pré-requisitos

- Node.js 20+
- npm

### Instalação e Início

```powershell
cd todo_front-end/todo-list
npm install
npm start -- --host 127.0.0.1 --port 4200
```

A aplicação estará disponível em `http://127.0.0.1:4200`.

> O backend precisa estar rodando em `http://localhost:8080` para a aplicação funcionar corretamente.

---

## 🐳 Docker

O `Dockerfile` usa a imagem `node:22`:

1. Copia o projeto e instala as dependências com `npm install`.
2. Instala o Angular CLI globalmente.
3. Expõe a porta `4200` e inicia com `ng serve --host 0.0.0.0`.

O container é iniciado automaticamente pelo `docker compose up` (a partir de `todo_back-end/`).

---

## 🏗️ Build de Produção

```powershell
cd todo_front-end/todo-list
npm run build
```

Os artefatos de produção serão gerados em `dist/`, otimizados para performance.

---

## 🧪 Testes

### Unitários (Karma + Jasmine)

```powershell
npm test -- --watch=false --browsers=ChromeHeadless
```

### Smoke E2E

Requer frontend (`http://127.0.0.1:4200`) e backend (`http://localhost:8080`) rodando:

```powershell
npm run test:e2e
```

O script E2E está em `scripts/e2e-smoke.mjs`.

---

## 📦 Dependências Principais

| Pacote | Versão | Finalidade |
|---|---|---|
| `@angular/core` | ^20.3.18 | Framework principal |
| `@angular/router` | ^20.3.18 | Roteamento SPA com lazy loading |
| `@angular/forms` | ^20.3.18 | Formulários reativos |
| `rxjs` | ~7.8.0 | Programação reativa |
| `zone.js` | ~0.15.0 | Detecção de mudanças Angular |
| `@angular/cli` | ^20.3.3 | Tooling de desenvolvimento |
| `typescript` | ~5.9.2 | Tipagem estática |
| `karma` + `jasmine` | ~6.4 / ~5.9 | Testes unitários |

---

## 🔧 Scripts npm

| Script | Comando | Descrição |
|---|---|---|
| `npm start` | `ng serve` | Inicia o servidor de desenvolvimento |
| `npm run build` | `ng build` | Build de produção |
| `npm test` | `ng test` | Executa testes unitários |
| `npm run test:e2e` | `node scripts/e2e-smoke.mjs` | Executa smoke tests E2E |
| `npm run watch` | `ng build --watch` | Build em modo watch (desenvolvimento) |

---

## 🌐 Configuração do Backend

O endereço da API é configurado em `src/environments/`:

- `environment.ts` — ambiente de desenvolvimento (aponta para `http://localhost:8080`)
- `environment.prod.ts` — ambiente de produção

---

> Para instruções completas de execução com Docker e descrição de todos os endpoints da API, consulte o **[README raiz](../../README.md)**.

# todo-list

## Contrato atual de integração `task`

O frontend Angular já está configurado para consumir o backend Spring em `http://localhost:8080`.

### Endpoints esperados

- `GET /task`
- `POST /task`
- `PUT /task/{id}`
- `PUT /task/concluir/{id}`
- `DELETE /task/{id}`

### Payload esperado pelo frontend

```json
{
  "id": 1,
  "name": "Nova tarefa",
  "description": "Detalhes da tarefa",
  "category": "Pessoal",
  "priority": "Média",
  "dueDate": "2026-03-18",
  "done": false
}
```

### Regras que o frontend já assume

- `priority` aceita `Baixa`, `Média` e `Alta`.
- `dueDate` pode ser `null`.
- `PUT /task/concluir/{id}` retorna a tarefa atualizada com `done: true`.
- `DELETE /task/{id}` pode responder sem corpo.

## Contrato atual de integração `auth`

O frontend da tela de login já está preparado para abrir modal e consumir os endpoints abaixo no backend Spring.

### Endpoints esperados

- `POST /auth/esqueci-senha`
- `POST /auth/solicitar-convite`

### Payload esperado para recuperar senha

```json
{
  "email": "voce@empresa.com"
}
```

### Payload esperado para solicitar convite

```json
{
  "name": "Seu nome",
  "email": "voce@empresa.com",
  "company": "Equipe ou empresa"
}
```

### Resposta esperada pelo frontend

```json
{
  "status": "RECEBIDO",
  "message": "Solicitação registrada com sucesso."
}
```

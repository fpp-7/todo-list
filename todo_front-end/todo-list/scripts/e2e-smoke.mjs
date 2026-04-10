const apiUrl = process.env.E2E_API_URL ?? 'http://localhost:8080';
const webUrl = process.env.E2E_WEB_URL ?? 'http://127.0.0.1:4200';
const email = `e2e.${Date.now()}@example.com`;
const password = 'senha123';

await assertFrontendIsReachable();
await assertAuthAndTaskFlow();

console.log('E2E smoke passed');

async function assertFrontendIsReachable() {
  const response = await fetch(webUrl);
  assert(response.ok, `Frontend unavailable at ${webUrl}: ${response.status}`);

  const html = await response.text();
  assert(html.includes('app-root'), 'Frontend index did not include Angular root element.');
}

async function assertAuthAndTaskFlow() {
  await post('/auth/register', {
    firstName: 'E2E',
    lastName: 'Smoke',
    login: email,
    password,
  });

  const loginResponse = await post('/auth/login', {
    login: email,
    password,
  });
  const cookies = extractCookies(loginResponse);
  const loginBody = await loginResponse.json();

  assert(Boolean(loginBody.token), 'Login did not return an access token.');
  assert(Boolean(loginBody.refreshToken), 'Login did not return a refresh token.');
  assert(cookies.includes('todo_access_token='), 'Login did not set access cookie.');
  assert(cookies.includes('todo_refresh_token='), 'Login did not set refresh cookie.');

  const profileResponse = await get('/profile', cookies);
  const profile = await profileResponse.json();
  assert(profile.displayName === 'E2E Smoke', `Unexpected display name: ${profile.displayName}`);

  const taskResponse = await post(
    '/task',
    {
      name: 'Tarefa E2E',
      description: 'Criada pelo smoke test',
      category: 'QA',
      priority: 'Alta',
      dueDate: null,
      done: false,
    },
    cookies,
  );
  const task = await taskResponse.json();
  assert(task.name === 'Tarefa E2E', `Unexpected task name: ${task.name}`);

  const tasksResponse = await get('/task', cookies);
  const tasks = await tasksResponse.json();
  assert(
    Array.isArray(tasks) && tasks.some((currentTask) => currentTask.id === task.id),
    'Created task was not returned by task list.',
  );
}

async function get(path, cookieHeader = '') {
  const response = await fetch(`${apiUrl}${path}`, {
    headers: cookieHeader ? { Cookie: cookieHeader } : {},
  });
  await assertOk(response, `GET ${path}`);
  return response;
}

async function post(path, body, cookieHeader = '') {
  const response = await fetch(`${apiUrl}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(cookieHeader ? { Cookie: cookieHeader } : {}),
    },
    body: JSON.stringify(body),
  });
  await assertOk(response, `POST ${path}`);
  return response;
}

async function assertOk(response, label) {
  if (!response.ok) {
    throw new Error(`${label} failed with ${response.status}: ${await response.text()}`);
  }
}

function extractCookies(response) {
  const setCookies =
    typeof response.headers.getSetCookie === 'function'
      ? response.headers.getSetCookie()
      : splitSetCookieHeader(response.headers.get('set-cookie') ?? '');

  return setCookies.map((cookie) => cookie.split(';')[0]).join('; ');
}

function splitSetCookieHeader(header) {
  if (!header) {
    return [];
  }

  return header.split(/,(?=\s*[^;]+=)/);
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

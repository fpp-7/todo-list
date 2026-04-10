const DEFAULT_BACKEND_UNAVAILABLE_MESSAGE =
  'Backend indisponivel. Verifique se a API esta rodando em http://localhost:8080.';

type ApiErrorBody = {
  readonly message?: unknown;
  readonly fieldErrors?: unknown;
};

export function extractApiErrorMessage(
  error: unknown,
  backendUnavailableMessage = DEFAULT_BACKEND_UNAVAILABLE_MESSAGE,
): string | null {
  if (isHttpStatusZero(error)) {
    return backendUnavailableMessage;
  }

  const errorBody = getErrorBody(error);

  if (!errorBody) {
    return null;
  }

  if (typeof errorBody.message === 'string') {
    return errorBody.message;
  }

  if (!Array.isArray(errorBody.fieldErrors)) {
    return null;
  }

  const [firstFieldError] = errorBody.fieldErrors;

  if (!isObject(firstFieldError)) {
    return null;
  }

  const fieldMessage = firstFieldError['message'];

  return typeof fieldMessage === 'string' ? fieldMessage : null;
}

function isHttpStatusZero(error: unknown): boolean {
  return isObject(error) && error['status'] === 0;
}

function getErrorBody(error: unknown): ApiErrorBody | null {
  if (!isObject(error)) {
    return null;
  }

  const errorBody = error['error'];

  return isObject(errorBody) ? (errorBody as ApiErrorBody) : null;
}

function isObject(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object';
}

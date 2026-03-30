import { HttpErrorResponse } from '@angular/common/http';

interface ErrorPayload {
  message?: string;
}

export function readHttpErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    const payload = error.error as ErrorPayload | undefined;
    return payload?.message ?? fallback;
  }

  return fallback;
}

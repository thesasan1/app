/**
 * RFC 9457 Problem Details body, extended with a stable machine-readable `code`
 * (API_SPECIFICATION.md §1, "Standard error codes").
 */
export interface ProblemDetails {
  type: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  code: ErrorCode;
  errors?: FieldError[];
  requestId?: string;
}

export interface FieldError {
  field: string;
  message: string;
}

export type ErrorCode =
  | 'VALIDATION_FAILED'
  | 'UNAUTHENTICATED'
  | 'TOKEN_EXPIRED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'VERSION_CONFLICT'
  | 'PAYLOAD_TOO_LARGE'
  | 'UNPROCESSABLE'
  | 'UPGRADE_REQUIRED'
  | 'RATE_LIMITED'
  | 'AI_UNAVAILABLE'
  | 'INTERNAL_ERROR';

export const PROBLEM_CONTENT_TYPE = 'application/problem+json';

const STATUS_CODES: Record<number, ErrorCode> = {
  400: 'VALIDATION_FAILED',
  401: 'UNAUTHENTICATED',
  403: 'FORBIDDEN',
  404: 'NOT_FOUND',
  409: 'VERSION_CONFLICT',
  413: 'PAYLOAD_TOO_LARGE',
  422: 'UNPROCESSABLE',
  426: 'UPGRADE_REQUIRED',
  429: 'RATE_LIMITED',
  503: 'AI_UNAVAILABLE',
};

export function defaultCodeForStatus(status: number): ErrorCode {
  return STATUS_CODES[status] ?? 'INTERNAL_ERROR';
}

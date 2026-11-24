import { ArgumentsHost, Catch, ExceptionFilter, HttpException, HttpStatus, Logger } from '@nestjs/common';
import { STATUS_CODES } from 'node:http';
import type { Request, Response } from 'express';
import { AppException } from '../errors/app-exception';
import { FieldError, PROBLEM_CONTENT_TYPE, ProblemDetails, defaultCodeForStatus } from '../errors/problem-details';

/**
 * Maps every thrown error to an RFC 9457 response. Unknown errors become a generic 500 and
 * their details are logged, never returned, so internals do not leak to clients.
 */
@Catch()
export class ProblemDetailsFilter implements ExceptionFilter {
  private readonly logger = new Logger(ProblemDetailsFilter.name);

  catch(exception: unknown, host: ArgumentsHost): void {
    const http = host.switchToHttp();
    const request = http.getRequest<Request & { id?: string | number }>();
    const response = http.getResponse<Response>();

    const problem = this.toProblem(exception, request.originalUrl ?? request.url);
    if (request.id !== undefined) problem.requestId = String(request.id);

    if (problem.status >= 500) {
      this.logger.error({ err: exception, requestId: problem.requestId }, 'Unhandled error');
    }

    response.status(problem.status).type(PROBLEM_CONTENT_TYPE).json(problem);
  }

  private toProblem(exception: unknown, instance: string): ProblemDetails {
    if (exception instanceof AppException) {
      return {
        type: 'about:blank',
        title: STATUS_CODES[exception.getStatus()] ?? 'Error',
        status: exception.getStatus(),
        detail: exception.detail,
        instance,
        code: exception.code,
        ...(exception.fieldErrors ? { errors: exception.fieldErrors } : {}),
      };
    }

    if (exception instanceof HttpException) {
      const status = exception.getStatus();
      const body = exception.getResponse();
      const errors = status === Number(HttpStatus.BAD_REQUEST) ? extractValidationErrors(body) : undefined;
      return {
        type: 'about:blank',
        title: STATUS_CODES[status] ?? 'Error',
        status,
        detail: errors ? 'Request validation failed.' : extractMessage(body, exception.message),
        instance,
        code: defaultCodeForStatus(status),
        ...(errors ? { errors } : {}),
      };
    }

    const clientError = asBodyParserClientError(exception);
    if (clientError) {
      return {
        type: 'about:blank',
        title: STATUS_CODES[clientError.status] ?? 'Error',
        status: clientError.status,
        detail: clientError.detail,
        instance,
        code: clientError.status === 413 ? 'PAYLOAD_TOO_LARGE' : defaultCodeForStatus(clientError.status),
      };
    }

    return {
      type: 'about:blank',
      title: 'Internal Server Error',
      status: HttpStatus.INTERNAL_SERVER_ERROR,
      detail: 'An unexpected error occurred.',
      instance,
      code: 'INTERNAL_ERROR',
    };
  }
}

function extractMessage(body: string | object, fallback: string): string {
  if (typeof body === 'string') return body;
  const message = (body as { message?: unknown }).message;
  return typeof message === 'string' ? message : fallback;
}

/** ValidationPipe produces `{ message: string[] }`; the pipe factory below emits "field: message". */
function extractValidationErrors(body: string | object): FieldError[] | undefined {
  if (typeof body !== 'object') return undefined;
  const message = (body as { message?: unknown }).message;
  if (!Array.isArray(message)) return undefined;
  return message.map((entry: unknown) => {
    const text = String(entry);
    const separator = text.indexOf(': ');
    return separator > 0
      ? { field: text.slice(0, separator), message: text.slice(separator + 2) }
      : { field: '', message: text };
  });
}

/**
 * Express body-parser rejects malformed or oversized bodies with plain errors marked
 * `expose: true` and a 4xx `status`. Those are client errors, not server faults.
 */
function asBodyParserClientError(exception: unknown): { status: number; detail: string } | undefined {
  if (typeof exception !== 'object' || exception === null) return undefined;
  const { status, expose, type } = exception as { status?: unknown; expose?: unknown; type?: unknown };
  if (expose !== true || typeof status !== 'number' || status < 400 || status >= 500) return undefined;
  const details: Record<string, string> = {
    'entity.too.large': 'Request body exceeds the size limit.',
    'entity.parse.failed': 'Request body is not valid JSON.',
  };
  return { status, detail: (typeof type === 'string' && details[type]) || 'The request could not be processed.' };
}

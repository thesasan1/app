import { HttpException } from '@nestjs/common';
import { ErrorCode, FieldError } from './problem-details';

/** Domain-level HTTP error carrying a stable error code for clients. */
export class AppException extends HttpException {
  constructor(
    status: number,
    public readonly code: ErrorCode,
    public readonly detail?: string,
    public readonly fieldErrors?: FieldError[],
  ) {
    super(detail ?? code, status);
  }
}

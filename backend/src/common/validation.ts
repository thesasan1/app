import { BadRequestException, ValidationError, ValidationPipe } from '@nestjs/common';

/** Flattens nested class-validator errors into "path: message" strings. */
export function flattenValidationErrors(errors: ValidationError[], parentPath = ''): string[] {
  return errors.flatMap((error) => {
    const path = parentPath ? `${parentPath}.${error.property}` : error.property;
    const own = Object.values(error.constraints ?? {}).map((message) => `${path}: ${message}`);
    return [...own, ...flattenValidationErrors(error.children ?? [], path)];
  });
}

/**
 * Global validation (API_SPECIFICATION.md §1): unknown properties are rejected, payloads are
 * transformed into DTO instances, and failures become 400 VALIDATION_FAILED problem details.
 */
export function createValidationPipe(): ValidationPipe {
  return new ValidationPipe({
    whitelist: true,
    forbidNonWhitelisted: true,
    transform: true,
    exceptionFactory: (errors) => new BadRequestException(flattenValidationErrors(errors)),
  });
}

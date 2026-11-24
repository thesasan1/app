import { ValidationError } from '@nestjs/common';
import { flattenValidationErrors } from './validation';

function error(
  property: string,
  constraints?: Record<string, string>,
  children: ValidationError[] = [],
): ValidationError {
  return { property, constraints, children };
}

describe('flattenValidationErrors', () => {
  it('prefixes each message with its property path', () => {
    const result = flattenValidationErrors([error('title', { isNotEmpty: 'title should not be empty' })]);
    expect(result).toEqual(['title: title should not be empty']);
  });

  it('walks nested children with dotted paths', () => {
    const nested = error('reminder', undefined, [error('offsetMinutes', { isInt: 'must be an integer' })]);
    expect(flattenValidationErrors([nested])).toEqual(['reminder.offsetMinutes: must be an integer']);
  });
});

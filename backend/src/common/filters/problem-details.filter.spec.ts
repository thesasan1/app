import { ArgumentsHost, BadRequestException, NotFoundException } from '@nestjs/common';
import { AppException } from '../errors/app-exception';
import { ProblemDetailsFilter } from './problem-details.filter';

function createHost(request: Record<string, unknown> = {}) {
  const response = {
    status: jest.fn().mockReturnThis(),
    type: jest.fn().mockReturnThis(),
    json: jest.fn().mockReturnThis(),
  };
  const host = {
    switchToHttp: () => ({
      getRequest: () => ({ url: '/v1/things', originalUrl: '/v1/things', ...request }),
      getResponse: () => response,
    }),
  } as unknown as ArgumentsHost;
  return { host, response };
}

describe('ProblemDetailsFilter', () => {
  const filter = new ProblemDetailsFilter();

  it('maps HttpException to problem details with a default code', () => {
    const { host, response } = createHost({ id: 'req-12345678' });
    filter.catch(new NotFoundException(), host);
    expect(response.status).toHaveBeenCalledWith(404);
    expect(response.type).toHaveBeenCalledWith('application/problem+json');
    expect(response.json).toHaveBeenCalledWith(
      expect.objectContaining({
        status: 404,
        code: 'NOT_FOUND',
        title: 'Not Found',
        instance: '/v1/things',
        requestId: 'req-12345678',
      }),
    );
  });

  it('turns validation messages into field errors', () => {
    const { host, response } = createHost();
    filter.catch(new BadRequestException(['title: title should not be empty', 'loose message']), host);
    expect(response.json).toHaveBeenCalledWith(
      expect.objectContaining({
        code: 'VALIDATION_FAILED',
        errors: [
          { field: 'title', message: 'title should not be empty' },
          { field: '', message: 'loose message' },
        ],
      }),
    );
  });

  it('keeps the code and detail of AppException', () => {
    const { host, response } = createHost();
    filter.catch(new AppException(409, 'VERSION_CONFLICT', 'Version 3 is outdated.'), host);
    expect(response.json).toHaveBeenCalledWith(
      expect.objectContaining({ status: 409, code: 'VERSION_CONFLICT', detail: 'Version 3 is outdated.' }),
    );
  });

  it('maps body-parser size errors to 413', () => {
    const { host, response } = createHost();
    const tooLarge = Object.assign(new Error('request entity too large'), {
      status: 413,
      expose: true,
      type: 'entity.too.large',
    });
    filter.catch(tooLarge, host);
    expect(response.status).toHaveBeenCalledWith(413);
    expect(response.json).toHaveBeenCalledWith(expect.objectContaining({ code: 'PAYLOAD_TOO_LARGE' }));
  });

  it('hides internals of unknown errors', () => {
    const { host, response } = createHost();
    jest.spyOn(filter['logger'], 'error').mockImplementation(() => undefined);
    filter.catch(new Error('connection string postgres://secret'), host);
    const body = response.json.mock.calls[0][0];
    expect(body).toEqual(
      expect.objectContaining({ status: 500, code: 'INTERNAL_ERROR', detail: 'An unexpected error occurred.' }),
    );
    expect(JSON.stringify(body)).not.toContain('secret');
  });
});

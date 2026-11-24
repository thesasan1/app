import 'reflect-metadata';
import { Body, Controller, Module, Post } from '@nestjs/common';
import { NestExpressApplication } from '@nestjs/platform-express';
import { Test } from '@nestjs/testing';
import { IsNotEmpty, IsString, MaxLength } from 'class-validator';
import request from 'supertest';
import { AppModule } from '../src/app.module';
import { configureApp } from '../src/app.setup';
import { loadEnv } from '../src/config/env';

class EchoDto {
  @IsString()
  @IsNotEmpty()
  @MaxLength(10)
  title!: string;
}

/** Test-only controller exercising the global validation pipe through the real HTTP stack. */
@Controller('echo')
class EchoController {
  @Post()
  echo(@Body() body: EchoDto): EchoDto {
    return body;
  }
}

@Module({ controllers: [EchoController] })
class EchoModule {}

describe('HTTP application (e2e)', () => {
  let app: NestExpressApplication;

  beforeAll(async () => {
    process.env.LOG_LEVEL = 'silent';
    process.env.NODE_ENV = 'test';
    const moduleRef = await Test.createTestingModule({ imports: [AppModule, EchoModule] }).compile();
    app = moduleRef.createNestApplication<NestExpressApplication>();
    configureApp(app, loadEnv());
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  it('GET /v1/health/live returns ok', async () => {
    const response = await request(app.getHttpServer()).get('/v1/health/live').expect(200);
    expect(response.body).toEqual({ status: 'ok' });
  });

  it('GET /v1/health/ready returns ok with checks', async () => {
    const response = await request(app.getHttpServer()).get('/v1/health/ready').expect(200);
    expect(response.body).toEqual({ status: 'ok', checks: {} });
  });

  it('serves routes only under the version prefix', async () => {
    await request(app.getHttpServer()).get('/health/live').expect(404);
  });

  it('returns RFC 9457 problem details for unknown routes', async () => {
    const response = await request(app.getHttpServer()).get('/v1/nope').expect(404);
    expect(response.headers['content-type']).toMatch(/^application\/problem\+json/);
    expect(response.body).toEqual(expect.objectContaining({ status: 404, code: 'NOT_FOUND', instance: '/v1/nope' }));
  });

  it('echoes and propagates a valid x-request-id', async () => {
    const response = await request(app.getHttpServer())
      .get('/v1/nope')
      .set('x-request-id', 'abc12345-test')
      .expect(404);
    expect(response.headers['x-request-id']).toBe('abc12345-test');
    expect(response.body.requestId).toBe('abc12345-test');
  });

  it('replaces a malformed x-request-id with a generated one', async () => {
    const response = await request(app.getHttpServer())
      .get('/v1/health/live')
      .set('x-request-id', 'bad id!')
      .expect(200);
    expect(response.headers['x-request-id']).toMatch(/^[0-9a-f-]{36}$/);
  });

  it('rejects unknown properties and invalid fields with field errors', async () => {
    const response = await request(app.getHttpServer()).post('/v1/echo').send({ title: '', extra: true }).expect(400);
    expect(response.body.code).toBe('VALIDATION_FAILED');
    const fields = response.body.errors.map((error: { field: string }) => error.field);
    expect(fields).toEqual(expect.arrayContaining(['title', 'extra']));
  });

  it('accepts a valid payload', async () => {
    await request(app.getHttpServer()).post('/v1/echo').send({ title: 'Hello' }).expect(201, { title: 'Hello' });
  });

  it('rejects JSON bodies over 1 MB', async () => {
    const response = await request(app.getHttpServer())
      .post('/v1/echo')
      .set('content-type', 'application/json')
      .send(JSON.stringify({ title: 'x'.repeat(1024 * 1024 + 10) }))
      .expect(413);
    expect(response.body.code).toBe('PAYLOAD_TOO_LARGE');
  });

  it('rejects malformed JSON with 400 problem details', async () => {
    const response = await request(app.getHttpServer())
      .post('/v1/echo')
      .set('content-type', 'application/json')
      .send('{"title":')
      .expect(400);
    expect(response.body).toEqual(expect.objectContaining({ status: 400, code: 'VALIDATION_FAILED' }));
  });

  it('sets security headers and hides the framework', async () => {
    const response = await request(app.getHttpServer()).get('/v1/health/live');
    expect(response.headers['x-content-type-options']).toBe('nosniff');
    expect(response.headers['x-powered-by']).toBeUndefined();
  });

  it('serves the OpenAPI document outside production', async () => {
    const response = await request(app.getHttpServer()).get('/v1/docs-json').expect(200);
    expect(response.body.paths).toHaveProperty('/v1/health/live');
  });
});

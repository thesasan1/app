import { VersioningType } from '@nestjs/common';
import { NestExpressApplication } from '@nestjs/platform-express';
import { DocumentBuilder, OpenAPIObject, SwaggerModule } from '@nestjs/swagger';
import helmet from 'helmet';
import { ProblemDetailsFilter } from './common/filters/problem-details.filter';
import { createValidationPipe } from './common/validation';
import { Env } from './config/env';

export const API_VERSION = '1';

/** Shared HTTP configuration for the server and for e2e tests, so tests exercise the real setup. */
export function configureApp(app: NestExpressApplication, env: Env): void {
  app.use(helmet());
  app.disable('x-powered-by');
  app.useBodyParser('json', { limit: '1mb' });
  app.enableVersioning({ type: VersioningType.URI, defaultVersion: API_VERSION });
  app.useGlobalPipes(createValidationPipe());
  app.useGlobalFilters(new ProblemDetailsFilter());

  const origins = env.CORS_ORIGINS.split(',')
    .map((origin) => origin.trim())
    .filter(Boolean);
  if (origins.length > 0) app.enableCors({ origin: origins });

  if (env.NODE_ENV !== 'production') {
    SwaggerModule.setup(`v${API_VERSION}/docs`, app, () => createOpenApiDocument(app));
  }
}

export function createOpenApiDocument(app: NestExpressApplication): OpenAPIObject {
  const config = new DocumentBuilder()
    .setTitle('Personal OS API')
    .setDescription('REST API for the AI-powered Personal Operating System. See docs/API_SPECIFICATION.md.')
    .setVersion(API_VERSION)
    .addBearerAuth()
    .build();
  return SwaggerModule.createDocument(app, config);
}

/**
 * Writes the OpenAPI document to openapi/openapi.v1.json, or with --check fails when the
 * committed file is out of date (API_SPECIFICATION.md §4, contract drift check).
 */
import 'reflect-metadata';
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { NestFactory } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import { AppModule } from '../src/app.module';
import { configureApp, createOpenApiDocument } from '../src/app.setup';
import { loadEnv } from '../src/config/env';

async function main(): Promise<void> {
  const env = loadEnv({ ...process.env, NODE_ENV: 'test', LOG_LEVEL: 'silent' });
  const app = await NestFactory.create<NestExpressApplication>(AppModule, { logger: false });
  configureApp(app, env);
  await app.init();
  const document = `${JSON.stringify(createOpenApiDocument(app), null, 2)}\n`;
  await app.close();

  const target = join(__dirname, '..', 'openapi', 'openapi.v1.json');
  if (process.argv.includes('--check')) {
    const committed = readFileSync(target, 'utf8');
    if (committed !== document) {
      console.error('openapi/openapi.v1.json is out of date. Run `npm run openapi` and commit the result.');
      process.exit(1);
    }
    console.log('OpenAPI document is up to date.');
    return;
  }
  writeFileSync(target, document);
  console.log(`Wrote ${target}`);
}

main().catch((error: unknown) => {
  console.error(error);
  process.exit(1);
});

import 'reflect-metadata';
import { NestFactory } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import { Logger } from 'nestjs-pino';
import { AppModule } from './app.module';
import { configureApp } from './app.setup';
import { loadEnv } from './config/env';

async function bootstrap(): Promise<void> {
  const env = loadEnv();
  const app = await NestFactory.create<NestExpressApplication>(AppModule, { bufferLogs: true });
  app.useLogger(app.get(Logger));
  app.enableShutdownHooks();
  configureApp(app, env);
  await app.listen(env.PORT);
}

bootstrap().catch((error: unknown) => {
  // Logger may not exist yet (e.g. invalid environment), so fall back to stderr.
  console.error(error instanceof Error ? error.message : error);
  process.exit(1);
});

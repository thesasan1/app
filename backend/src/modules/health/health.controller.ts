import { Controller, Get } from '@nestjs/common';
import { ApiOkResponse, ApiTags } from '@nestjs/swagger';
import { HealthStatusDto } from './health.dto';

/**
 * Liveness and readiness probes. Unauthenticated by design and never expose internals.
 * Readiness gains PostgreSQL and Redis checks when those modules are introduced.
 */
@ApiTags('health')
@Controller('health')
export class HealthController {
  @Get('live')
  @ApiOkResponse({ type: HealthStatusDto })
  live(): HealthStatusDto {
    return { status: 'ok' };
  }

  @Get('ready')
  @ApiOkResponse({ type: HealthStatusDto })
  ready(): HealthStatusDto {
    return { status: 'ok', checks: {} };
  }
}

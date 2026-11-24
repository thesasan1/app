import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class HealthStatusDto {
  @ApiProperty({ enum: ['ok'] })
  status!: 'ok';

  @ApiPropertyOptional({
    description: 'Per-dependency results, keyed by dependency name.',
    type: 'object',
    additionalProperties: { type: 'string', enum: ['up', 'down'] },
  })
  checks?: Record<string, 'up' | 'down'>;
}

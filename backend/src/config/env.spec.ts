import { InvalidEnvironmentError, loadEnv } from './env';

describe('loadEnv', () => {
  it('applies defaults for an empty environment', () => {
    expect(loadEnv({})).toEqual({ NODE_ENV: 'development', PORT: 3000, LOG_LEVEL: 'info', CORS_ORIGINS: '' });
  });

  it('coerces PORT from a string', () => {
    expect(loadEnv({ PORT: '8080' }).PORT).toBe(8080);
  });

  it.each([['0'], ['70000'], ['abc']])('rejects invalid PORT %s', (port) => {
    expect(() => loadEnv({ PORT: port })).toThrow(InvalidEnvironmentError);
  });

  it('rejects an unknown NODE_ENV and names the variable', () => {
    try {
      loadEnv({ NODE_ENV: 'staging-ish' });
      fail('expected loadEnv to throw');
    } catch (error) {
      expect(error).toBeInstanceOf(InvalidEnvironmentError);
      expect((error as InvalidEnvironmentError).issues[0]).toMatch(/^NODE_ENV:/);
    }
  });
});

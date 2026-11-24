// @ts-check
import eslint from '@eslint/js';
import prettier from 'eslint-config-prettier';
import tseslint from 'typescript-eslint';

export default tseslint.config(
    { ignores: ['dist/', 'coverage/', 'node_modules/', 'openapi/'] },
    eslint.configs.recommended,
    ...tseslint.configs.recommendedTypeChecked,
    {
        languageOptions: {
            parserOptions: {
                projectService: { allowDefaultProject: ['*.mjs'] },
                tsconfigRootDir: import.meta.dirname,
            },
        },
        rules: {
            '@typescript-eslint/no-floating-promises': 'error',
            '@typescript-eslint/explicit-function-return-type': ['warn', { allowExpressions: true }],
            'no-console': ['error', { allow: ['error', 'log'] }],
        },
    },
    {
        files: ['**/*.spec.ts', '**/*.e2e-spec.ts'],
        rules: {
            '@typescript-eslint/no-unsafe-assignment': 'off',
            '@typescript-eslint/no-unsafe-member-access': 'off',
            '@typescript-eslint/no-unsafe-argument': 'off',
            '@typescript-eslint/unbound-method': 'off',
            '@typescript-eslint/no-unsafe-call': 'off',
            '@typescript-eslint/explicit-function-return-type': 'off',
            '@typescript-eslint/dot-notation': 'off',
        },
    },
    prettier,
);

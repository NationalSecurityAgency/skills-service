// https://eslint.org/docs/user-guide/configuring

module.exports = {
  root: true,

  parserOptions: {
    parser: 'babel-eslint',
  },

  env: {
    browser: true,
    node: true,
  },

  // https://github.com/vuejs/eslint-plugin-vue#priority-a-essential-error-prevention
  // consider switching to `plugin:vue/strongly-recommended` or `plugin:vue/recommended` for stricter rules.
  extends: ['plugin:vue/essential', '@vue/airbnb'],

  // required to lint *.vue files
  plugins: [
    'vue',
  ],

  settings: {
    'import/resolver': {
      webpack: {
        config: require.resolve('@vue/cli-service/webpack.config.js'),
      },
    },
  },

  // check if imports actually resolve
  // add your custom rules here
  rules: {
    'import/extensions': [
      'error',
      'always',
      {
        js: 'never',
        vue: 'never',
      },
    ],
    'no-param-reassign': [
      'error',
      {
        props: true,
        ignorePropertyModificationsFor: [
          'state',
          'acc',
          'e',
        ],
      },
    ],
    'import/no-extraneous-dependencies': [
      'error',
      {
        optionalDependencies: [
          'test/unit/index.js',
        ],
      },
    ],
    'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'off',
    indent: 'off',
    'vue/script-indent': [
      'error',
      2,
      {
        baseIndent: 1,
        switchCase: 0,
        ignores: [],
      },
    ],
    'vue/max-attributes-per-line': [
      2,
      {
        singleline: 5,
        multiline: {
          max: 5,
          allowFirstLine: true,
        },
      },
    ],
    'max-len': [
      'error',
      {
        code: 300,
      },
    ],
    'no-console': process.env.NODE_ENV === 'production' ? 'error' : 'off',
  },

  extends: [
    'plugin:vue/essential',
    '@vue/airbnb',
  ],
};

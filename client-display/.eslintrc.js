module.exports = {
  root: true,
  env: {
    node: true,
  },
  extends: [
    'plugin:vue/essential',
    '@vue/airbnb',
  ],
  rules: {
    'no-console': process.env.NODE_ENV === 'production' ? 'error' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'off',
    'indent': ['error', 2],
    'max-len': ['error', { 'code': 300 }],
    'no-underscore-dangle': 0,
    'no-new': 0,
    'vue/script-indent': 0, // I have to disable this for now. It makes your newline then blocks look real dumb
  },
  'overrides': [
    {
      'files': ['*.vue'],
      'rules': {
        'indent': 'off'
      }
    }
  ],
  parserOptions: {
    parser: 'babel-eslint',
  },
};

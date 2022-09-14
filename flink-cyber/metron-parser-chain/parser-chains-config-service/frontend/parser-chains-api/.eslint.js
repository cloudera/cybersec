module.exports = {
    'env': {
      'node': true,
      'es6': true
    },
    'extends': [
      'standard'
    ],
    'globals': {
      'Atomics': 'readonly',
      'SharedArrayBuffer': 'readonly'
    },
    'parserOptions': {
      'ecmaVersion': 2018,
      'sourceType': 'module'
    },
    'rules': {
      'semi': [1, 'always'],
      'space-before-function-paren': [1, {"anonymous": "always", "named": "never", "asyncArrow": "always"}]

    }
  }

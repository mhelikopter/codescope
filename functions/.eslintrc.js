module.exports = {
  root: true,
  env: {
    es6: true,
    node: true,
  },
  extends: [
    "eslint:recommended",
    "plugin:import/errors",
    "plugin:import/warnings",
    "plugin:import/typescript",
    "google",
    "plugin:@typescript-eslint/recommended",
  ],
  parser: "@typescript-eslint/parser",
  parserOptions: {
    project: ["tsconfig.json", "tsconfig.dev.json"],
    sourceType: "module",
  },
  ignorePatterns: [
    "/lib/**/*", // Ignore built files.
    "/generated/**/*", // Ignore generated files.
  ],
  plugins: [
    "@typescript-eslint",
    "import",
  ],
  rules: {
    "quotes": ["error", "double"],
    "import/no-unresolved": 0,
    "indent": ["error", 2],
    // Allow long lines for embedded prompt template literals and strings;
    // breaking these up would change the prompt text sent to Gemini.
    "max-len": [
      "error",
      {
        "code": 100,
        "ignoreStrings": true,
        "ignoreTemplateLiterals": true,
        "ignoreUrls": true,
        "ignoreRegExpLiterals": true,
      },
    ],
    // Cloud Functions writes logs via console.*; logs go to Cloud Logging.
    "no-console": "off",
    // Allow underscore-prefixed unused vars/args for intentional placeholders.
    "no-unused-vars": "off",
    "@typescript-eslint/no-unused-vars": [
      "error",
      {
        "argsIgnorePattern": "^_",
        "varsIgnorePattern": "^_",
        "caughtErrorsIgnorePattern": "^_",
      },
    ],
  },
};

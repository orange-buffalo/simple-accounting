module.exports = {
  root: true,
  env: { node: true },
  extends: [
    "plugin:vue/recommended",
    "@vue/airbnb"],
  rules: {
    "import/no-useless-path-segments": "off",
    "import/extensions": "off",
    "no-console": process.env.NODE_ENV === "production" ? "error" : "off",
    "no-debugger": process.env.NODE_ENV === "production" ? "error" : "off"
  },
  parserOptions: { parser: "babel-eslint" }
};

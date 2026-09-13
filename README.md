# Simple Accounting

![Docker Image Version (latest semver)](https://img.shields.io/docker/v/orangebuffalo/simple-accounting?style=flat-square&logo=docker&label=orangebuffalo%2Fsimple-accounting)

Simple Accounting is a basic application for managing your small business finances. It is free to use and open source.

It has the following features:
* Tracking invoices, incomes and expenses.
* Managing customers, categories and taxes.
* Generating reports.
* Storing related documents in the cloud (Google Drive).

## Getting Started

Simple Accounting can be deployed to any container environment (e.g. Docker or Kubernetes) via
`orangebuffalo/simple-accounting` Docker image. See [Deployment docs](./docs/Deployment.md) for more details
on possible configurations and persistence options.

For a quick look into Simple Accounting, you can run the demo locally:
1. `docker run --rm -e SA_DEMO_ENABLED=true -p 9393:9393 orangebuffalo/simple-accounting`
2. Open http://localhost:9393 in your browser.
3. Login with `Fry / password` for regular user.
4. Login with `Hermes / password` for admin user.

## Development

If you like to contribute to Simple Accounting, be it a bug report, improvement idea or
a pull request, please refer to [Contributing](./docs/CONTRIBUTING.md) and [Development](./docs/Development.md) docs.

## Icons

Simple Accounting uses its own icon set, located in `frontend/src/icons/svg`. Icons are designed on a 24x24 grid
in a monoline style and are named `<name>-<variant>.svg`, where the variant states the context the icon is drawn for:
* `simple` - reduced to the essential shapes, for the pages, where icons are rendered small (13-20px);
* `detailed` - richer drawing, for the places where icons are rendered large: navigation menu, dashboard cards
  and document panels. A detailed variant is optional: when it is missing, the simple icon is used instead.

See `SaIcon` component (`variant` property) for the usage details.

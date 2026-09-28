# Deploying Simple Accounting

Simple Accounting can be deployed to any container environment (e.g. Docker or Kubernetes) via
`orangebuffalo/simple-accounting` Docker image.

## Configuration

### Public URL

Simple Accounting needs to know its publicly accessible URL to generate correct links (e.g. document download URLs).
This URL must be the address that **end users** use to reach the application — including the protocol, host, and port
(if non-standard). It must be reachable from the user's browser.

For example, if the application is behind a reverse proxy at `https://accounting.example.com`, configure:

* `SA_PUBLIC_URL` = `https://accounting.example.com`

If running locally on the default port, use `http://localhost:9393`.

### Reverse proxy

If Simple Accounting is deployed behind a reverse proxy (e.g. Nginx, Traefik, HAProxy), you need to ensure the proxy
supports **WebSocket connections** at the `/api/graphql/subscriptions` path. This path is used by the application
for real-time push notifications via the `graphql-transport-ws` subprotocol.

For Nginx, add the following to the relevant `location` block:

```nginx
location /api/graphql/subscriptions {
    proxy_pass http://backend;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
}
```

Most modern reverse proxies and cloud load balancers support WebSocket upgrade out of the box, but it may need
to be enabled explicitly.

### Database

Simple Accounting uses PostgreSQL. Create an empty database and a user with permission to create tables in it.
Port `9393` should be exposed to access the UI. Mount `/data` if you use local document storage or are upgrading
from a file-based H2 installation.

The schema is created and updated automatically on startup. Configure:

* `SA_DATABASE_HOST` - PostgreSQL host (default: `localhost`).
* `SA_DATABASE_PORT` - PostgreSQL port (default: `5432`).
* `SA_DATABASE_NAME` - database name (default: `simple-accounting`).
* `SA_DATABASE_USERNAME` - PostgreSQL user (default: `sa`).
* `SA_DATABASE_PASSWORD` - PostgreSQL password (default: empty; configure a password for production).

#### Upgrading from H2

Stop the old application, back up `/data/db/simple-accounting.mv.db`, and keep the `/data` mount when starting
the new version. On first start the application creates the PostgreSQL schema and, if it finds the H2 file,
copies all application data to the empty PostgreSQL database. It migrates a temporary copy of the H2 file first;
the original file remains unchanged. Keep using the original H2 username and password in `SA_DATABASE_USERNAME`
and `SA_DATABASE_PASSWORD` for this first start (the PostgreSQL user must use those same credentials).
If the credentials differ, set `SA_DATABASE_LEGACY_H2_USERNAME` and `SA_DATABASE_LEGACY_H2_PASSWORD`
instead to access the old file.

If the H2 database lives elsewhere, set `SA_DATABASE_LEGACY_H2_PATH` to its path **without** `.mv.db`.
The import refuses to overwrite a PostgreSQL database containing application data and is marked complete only
after the entire copy commits. Keep the H2 backup until you have verified the migrated data. Subsequent starts
use PostgreSQL; the H2 file is not removed automatically.

### Google Drive integration

Simple Accounting can use Google Drive to store related documents. To enable this feature, you need to provide
the following environment parameters:

* `SA_DOCUMENTS_STORAGE_GOOGLE_DRIVE_CLIENT_ID` - Google Drive client ID.
* `SA_DOCUMENTS_STORAGE_GOOGLE_DRIVE_CLIENT_SECRET` - Google Drive client secret.

#### Setting up Google Drive client

Please refer to the [Google Drive docs](https://developers.google.com/identity/protocols/oauth2) for more details on
how to create a project and obtain client ID and secret.

Use these URLs (based on your host) as the authorized redirect URIs:

* `/api/v1/auth/storage/google-drive/callback`
* `/api/v1/auth/oauth2/callback`
* `/oauth-callback`

Enable Google Drive API for you project.

### Local file system storage

As a simpler alternative to Google Drive, Simple Accounting can store uploaded documents directly on the host file
system. This is well-suited for self-hosted deployments without third-party document storage.

The documents are organised under a configurable base directory, with one sub-directory per workspace. Make sure the
base directory is on a persistent volume (for example under `/data`).

The feature is disabled by default. Enable it and set the base directory with these environment parameters:

* `SA_DOCUMENTS_STORAGE_LOCAL_FS_ENABLED` = `true` — enable local file system storage (default: `false`).
* `SA_DOCUMENTS_STORAGE_LOCAL_FS_BASE_DIRECTORY` — absolute path to the directory where documents will
  be stored (default: `/data/documents-storage/local`).

### Backups

Use `pg_dump` and your PostgreSQL server's backup/restore procedures.

## Administration

On the first start, the application will create a default admin user. Check the following logs to get the credentials:

```
Application database does not contain any admin users. Created a new user with login 'admin' and password '...'. It is highly recommended to change the generated password.
```

Please use these credentials to login and change the password. It is possible to rename the admin user using the 
standard user editing functionality.

Admin user can then create other admins and/or regular users.

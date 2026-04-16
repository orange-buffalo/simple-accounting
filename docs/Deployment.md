# Deploying Simple Accounting

Simple Accounting can be deployed to any container environment (e.g. Docker or Kubernetes) via
`orangebuffalo/simple-accounting` Docker image.

## Configuration

### Public URL

Simple Accounting needs to know its publicly accessible URL to generate correct links (e.g. document download URLs).
This URL must be the address that **end users** use to reach the application — including the protocol, host, and port
(if non-standard). It must be reachable from the user's browser.

For example, if the application is behind a reverse proxy at `https://accounting.example.com`, configure:

* `SIMPLEACCOUNTING_PUBLIC_URL` = `https://accounting.example.com`

If running locally on the default port, use `http://localhost:9393`.

### Database

Simple Accounting uses H2 database with file-based persistence. The database file is stored in `/data` directory.
Mount it to the container to preserve between container restarts / upgrades. Port `9393` should be exposed
to access the UI.

We use automatic schema migration, so the database will be updated to the latest version on application start.

We highly recommend providing the following environment parameters to change the default credentials for the database:

* `SPRING_DATASOURCE_PASSWORD` - database password.
* `SPRING_DATASOURCE_USERNAME` - database username.

### Google Drive integration

Simple Accounting can Google Drive to store related documents. To enable this feature, you need to provide
the following environment parameters:

* `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE-DRIVE_CLIENT-ID` - Google Drive client ID.
* `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE-DRIVE_CLIENT-SECRET` - Google Drive client secret.

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
system. This is well-suited for self-hosted deployments where all data should remain on a single server without
requiring any third-party service.

The documents are organised under a configurable base directory, with one sub-directory per workspace. Make sure the
base directory is on a persistent volume (for example under `/data`, which is already mounted for the database).

The feature is disabled by default. Enable it and set the base directory with these environment parameters:

* `SIMPLEACCOUNTING_DOCUMENTS_STORAGE_LOCAL_FS_ENABLED` = `true` — enable local file system storage (default: `false`).
* `SIMPLEACCOUNTING_DOCUMENTS_STORAGE_LOCAL_FS_BASE_DIRECTORY` — absolute path to the directory where documents will
  be stored. There is no default; this property must be set when the feature is enabled.

### Enabling backups

Simple Accounting can automatically backup the database to Dropbox. To enable this feature, you need to provide
the following environment parameters:

* `SIMPLEACCOUNTING_BACKUP_ENABLED` = `true` - enable backups.
* `SIMPLEACCOUNTING_BACKUP_DROPBOX_ACTIVE` = `true` - enable Dropbox backup.
* `SIMPLEACCOUNTING_BACKUP_DROPBOX_ACCESSTOKEN` - Dropbox access token.
* `SIMPLEACCOUNTING_BACKUP_DROPBOX_REFRESHTOKEN` - Dropbox refresh token.
* `SIMPLEACCOUNTING_BACKUP_DROPBOX_CLIENTID` - Dropbox client ID.
* `SIMPLEACCOUNTING_BACKUP_DROPBOX_CLIENTSECRET` - Dropbox client secret.

Refer to [Dropbox docs](https://www.dropbox.com/developers/reference/developer-guide) for how to setup the app
and get the credentials. You can then use
[Postman](https://learning.postman.com/docs/sending-requests/authorization/oauth-20/#specifying-an-authorization-code)
to obtain the initial access and refresh token.

## Administration

On the first start, the application will create a default admin user. Check the following logs to get the credentials:

```
Application database does not contain any admin users. Created a new user with login 'admin' and password '...'. It is highly recommended to change the generated password.
```

Please use these credentials to login and change the password. It is possible to rename the admin user using the 
standard user editing functionality.

Admin user can then create other admins and/or regular users.

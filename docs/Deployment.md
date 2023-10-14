# Deploying Simple Accounting

Simple Accounting can be deployed to any container environment (e.g. Docker or Kubernetes) via
`orangebuffalo/simple-accounting` Docker image.

## Configuration

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

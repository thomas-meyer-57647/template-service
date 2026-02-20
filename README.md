# template-service

## Environment Variables

The service supports these environment variables:

- `TEMPLATE_DB_HOST` (default: `localhost`)
- `TEMPLATE_DB_PORT` (default: `3306`)
- `TEMPLATE_DB_NAME` (default: `template`)
- `TEMPLATE_DB_USER` (default: `root`)
- `TEMPLATE_DB_PASSWORD` (default: empty)
- `TEMPLATEPORT` (default: `8080`)

## IntelliJ Run Configuration

Example `Environment variables`:

`TEMPLATE_DB_HOST=localhost;TEMPLATE_DB_PORT=3306;TEMPLATE_DB_NAME=template;TEMPLATE_DB_USER=root;TEMPLATE_DB_PASSWORD=;TEMPLATEPORT=8080`

`Include system environment variables` can stay enabled because variable names are service-specific.

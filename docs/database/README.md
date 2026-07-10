# Database model

The editable database model is maintained in `schema.dbml`.

The SVG file is a visual export used for quick reading.

![Database ERD](erd.svg)

`schema.dbml` represents the current database model from the service migrations. Runtime database changes are managed through each service's Flyway migrations.

Validate the DBML and regenerate the visual export from the repository root:

```powershell
npx --yes --package @dbml/cli dbml2sql docs/database/schema.dbml --mysql -o $env:TEMP/banking-schema.sql
npx --yes --package @softwaretechnik/dbml-renderer dbml-renderer -i docs/database/schema.dbml -o docs/database/erd.svg
```

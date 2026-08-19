# 11 — Troubleshooting

Real error messages and their fixes, grouped by where they surface.

---

## Backend will not start

### `UnsupportedClassVersionError` / "class file has wrong version"

```
java.lang.UnsupportedClassVersionError: org/springframework/boot/SpringApplication
has been compiled by a more recent version of the Java Runtime (class file version 61.0),
this version of the Java Runtime only recognizes class file versions up to 52.0
```

**Cause.** You ran `mvnw` directly, and the `java` on your PATH is **Java 8**. Class file version
52 is Java 8; Spring Boot 3 needs 61 (Java 17) or higher.

**Fix.** Use the launcher, which finds and pins a suitable JDK:

```powershell
cd backend
.\run.ps1
```

Or set it yourself for the session:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
.\mvnw.cmd spring-boot:run
```

JDK 17, 21 and 25 are all installed on this machine under `C:\Program Files\Eclipse Adoptium\`.

---

### `APP_JWT_SECRET is not set`

```
============================================================
 APP_JWT_SECRET is not set.

 Create backend/.env (copy from .env.example) and set:
   APP_JWT_SECRET=<at least 32 random characters>
============================================================
```

**Cause.** `backend/.env` does not exist, or the value is blank. This is deliberate — the
application refuses to fall back to a default signing key.

**Fix.**

```powershell
cd backend
Copy-Item .env.example .env
```

Then set a secret of at least 32 characters:

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Max 256 }))
```

---

### `APP_JWT_SECRET must be at least 32 characters`

HS256 needs a 256-bit key. Generate a longer one with the command above.

---

### `Failed to configure a DataSource: 'url' attribute is not specified`

**Cause.** `.env` was not loaded, so no database settings reached the app.

**Fix.** Start with `.\run.ps1`, which loads `.env`. If you are launching from an IDE, either set
the environment variables in the run configuration or start the app from the terminal instead.

---

## Database connection problems

### `Access denied for user 'ecomuser'@'localhost' (using password: YES)`

**Cause.** Wrong `DB_PASSWORD`, or the user was never created.

**Fix.** Verify the credentials work directly:

```powershell
mysql -u ecomuser -p ecommerce_db
```

If that fails, create the user:

```sql
CREATE USER 'ecomuser'@'localhost' IDENTIFIED BY 'your-password';
GRANT ALL PRIVILEGES ON ecommerce_db.* TO 'ecomuser'@'localhost';
FLUSH PRIVILEGES;
```

---

### `Unknown database 'ecommerce_db'`

**Cause.** The schema was never created. Hibernate creates *tables*, not the database itself.

**Fix.**

```sql
CREATE DATABASE ecommerce_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

### `Communications link failure` / `Connection refused`

**Cause.** MySQL is not running, or is not on port 3306.

**Fix.**

```powershell
Get-Service | Where-Object { $_.Name -match 'mysql' }
Start-Service MySQL80        # name may differ by version
```

Check the port:

```powershell
Test-NetConnection -ComputerName localhost -Port 3306
```

If MySQL runs on a different port, update `DB_URL` in `.env`.

---

### `Public Key Retrieval is not allowed`

**Cause.** MySQL 8's `caching_sha2_password` plugin over an unencrypted connection.

**Fix.** The supplied `DB_URL` already includes `allowPublicKeyRetrieval=true`. If you replaced it,
put that parameter back:

```
jdbc:mysql://localhost:3306/ecommerce_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

---

### `Table 'ecommerce_db.users' doesn't exist`

**Cause.** Hibernate did not create the schema — usually because `ddl-auto` was changed, or the
database user lacks DDL privileges.

**Fix.** Confirm `spring.jpa.hibernate.ddl-auto: update` in `application.yml`, and that the user has
`ALL PRIVILEGES`. Alternatively create the tables manually:

```powershell
mysql -u root -p ecommerce_db < ..\database\schema.sql
```

---

## Startup succeeded but something is missing

### No admin account exists

```
------------------------------------------------------------
 ADMIN_PASSWORD is not set, so no administrator was created.
------------------------------------------------------------
```

**Cause.** `ADMIN_PASSWORD` is blank. By design there is no default admin password.

**Fix.** Set `ADMIN_PASSWORD` in `.env` and restart. The seeder creates the account on the next
start, because it only skips when the email already exists.

---

### No products appear

**Cause.** Product seeding only runs when the `products` table is empty — so a deliberately deleted
product is not resurrected on restart.

**Check:**

```sql
SELECT COUNT(*) FROM products;
SELECT COUNT(*) FROM categories;
```

**Fix.** To re-seed from scratch:

```sql
DELETE FROM products;
```

Then restart. (Delete any orders referencing them first, or the foreign key will refuse.)

---

### `Port 8080 was already in use`

```powershell
netstat -ano | Select-String ":8080"
Stop-Process -Id <PID>
```

Or run on another port — set `SERVER_PORT=8081` in `.env`, and update the Vite proxy target in
`frontend/vite.config.js` to match.

---

## Maven wrapper problems

### `mvnw.cmd` is not recognised

Run it with the explicit path prefix from inside `backend/`:

```powershell
.\mvnw.cmd -v
```

---

### The wrapper hangs on first run

It is downloading Maven 3.9.9 (~9 MB) into `~\.m2\wrapper\dists`. This happens once. Behind a
proxy, configure `~\.m2\settings.xml`.

---

### `Could not find or load main class org.apache.maven.wrapper.MavenWrapperMain`

**Cause.** `.mvn/wrapper/maven-wrapper.jar` is missing or corrupt.

**Fix.** Re-download it:

```powershell
cd backend
Invoke-WebRequest -Uri "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar" -OutFile ".mvn\wrapper\maven-wrapper.jar"
```

---

## Frontend problems

### Every request fails; console shows `ERR_CONNECTION_REFUSED`

The UI shows: *"Cannot reach the server. Is the backend running on port 8080?"*

**Fix.** Start the backend. Confirm with:

```powershell
Invoke-RestMethod http://localhost:8080/api/products?size=1
```

---

### Requests return 404 with an HTML body

**Cause.** The Vite proxy is not forwarding `/api`, usually because the backend port changed.

**Fix.** Check `frontend/vite.config.js`:

```js
proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } }
```

Restart the dev server after editing — proxy config is not hot-reloaded.

---

### CORS error in the browser console

```
Access to XMLHttpRequest at 'http://localhost:8080/api/products' from origin
'http://localhost:5173' has been blocked by CORS policy
```

**Cause.** You are calling the backend directly instead of through the proxy, and the origin is not
in the allow-list.

**Fix.** Either use the relative `/api` path so the proxy handles it (the default), or add your
origin:

```bash
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

Restart the backend afterwards.

---

### Signed out unexpectedly

**Cause.** The token expired (24 hours by default), or the account was disabled by an admin.

The axios interceptor clears the session on a 401 and redirects to login. Note that a **403** does
*not* sign you out — that means you are signed in but lack the role.

**Fix.** Sign in again. To lengthen the session, raise `APP_JWT_EXPIRATION_MS`.

---

### Admin pages are empty or bounce to home

**Cause.** The account lacks `ROLE_ADMIN`.

**Check:**

```sql
SELECT u.email, r.name FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id;
```

**Fix.** Sign in as the seeded admin. To promote an existing account:

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'someone@example.com' AND r.name = 'ROLE_ADMIN';
```

They must sign in again to get a token carrying the new role.

---

### `npm install` fails

Ensure Node 18+ (`node -v`). If it still fails:

```powershell
Remove-Item -Recurse -Force node_modules, package-lock.json
npm install
```

---

## Application behaviour

### "Insufficient stock" when the product page shows stock available

**Cause.** Someone bought the remaining units between you loading the page and checking out. Stock
is re-validated inside the checkout transaction — that is the check that counts.

**Fix.** Refresh, and reduce the quantity. The cart page flags lines whose stock has become
insufficient.

---

### "You can only review products from an order that has been delivered to you"

**Cause.** Reviews require a `DELIVERED` order containing that product. Placed-but-undelivered does
not qualify.

**Fix.** As an admin, advance the order all the way to `DELIVERED`, then review as the customer.

---

### "Cannot change status from 'Order Placed' to 'Delivered'"

Working as intended. The message lists the legal next steps. Advance one stage at a time.

---

### A deleted product still appears in old orders

Working as intended. Products referenced by orders are deactivated, not deleted, so historical
invoices keep their line items. The delete response says so explicitly.

---

### Cancelling an order says it is no longer possible

Cancellation is only allowed before `DISPATCHED`. Once goods have physically left, returning them to
the stock count would make inventory wrong.

---

## WhatsApp notifications

Full debugging workflow in [09 — WhatsApp Notifications](09-whatsapp-notifications.md#debugging).
The short version:

| Symptom | Check |
|---|---|
| No message received | Startup log — is the sender Twilio or logging? |
| Rows are `SKIPPED` | `WHATSAPP_ENABLED=true` and all three credentials set, then restart |
| Rows are `FAILED` | Read `error_message`. Code `63015` usually means the recipient has not joined the Twilio sandbox |
| No rows at all | The event never fired — check for `Failed to process notification` in the log |
| Messages arrive late | Expected. Delivery is asynchronous, after commit, by design |

---

## Useful diagnostic commands

```powershell
# Is the backend up?
Invoke-RestMethod http://localhost:8080/api/products?size=1

# Full end-to-end verification
cd backend; .\smoke-test.ps1 -AdminPassword '<your password>'

# Interactive API docs
start http://localhost:8080/swagger-ui.html
```

```sql
-- Schema sanity check
USE ecommerce_db;
SHOW TABLES;                                          -- expect 12
SELECT COUNT(*) FROM products;
SELECT status, COUNT(*) FROM orders GROUP BY status;
SELECT status, COUNT(*) FROM notifications GROUP BY status;

-- Who has which role
SELECT u.email, u.enabled, r.name FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id;
```

To see the SQL Hibernate is running, set in `application.yml`:

```yaml
spring:
  jpa:
    show-sql: true
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE   # includes bound parameter values
```

---

## Complete reset

Wipes all data and re-seeds from scratch:

```sql
DROP DATABASE ecommerce_db;
CREATE DATABASE ecommerce_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Then restart the backend. Hibernate recreates the tables and `DataSeeder` repopulates roles, the
admin account, the six categories and the starter catalogue.

---

**Next:** [12 — Extending](12-extending.md).

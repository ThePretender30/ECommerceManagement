# 01 — Getting Started

Everything needed to go from a fresh clone to a running application. Follow it in order; each
section assumes the previous one succeeded.

---

## 1. Prerequisites

| Requirement | Why | Check it |
|---|---|---|
| **JDK 17 or newer** | Spring Boot 3 will not run on Java 8 | `java -version` |
| **MySQL 8** | The only database this project supports | `mysql --version` |
| **Node.js 18+** | Vite 8 and React 19 | `node -v` |
| **Maven** | *Not required* — the project ships a Maven Wrapper | — |

### About Java on this machine

The `java` on your PATH is **Java 8**. Spring Boot 3.5 requires 17 or newer, so running `mvnw`
directly will fail with `UnsupportedClassVersionError` or a "class file version" error.

JDK 17, 21 and 25 are already installed under `C:\Program Files\Eclipse Adoptium\`. **`run.ps1`
finds the newest suitable one and pins `JAVA_HOME` for you** — that is the whole reason the script
exists. Use it rather than invoking `mvnw` yourself.

If you ever need to do it manually:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
.\mvnw.cmd spring-boot:run
```

### Installing MySQL, if you have not

```powershell
winget install Oracle.MySQL
```

or download the MySQL Installer from https://dev.mysql.com/downloads/installer/. During setup,
choose "Developer Default" and remember the root password you set.

---

## 2. Create the database

Open a MySQL prompt (`mysql -u root -p`) and run:

```sql
CREATE DATABASE ecommerce_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'ecomuser'@'localhost' IDENTIFIED BY 'choose-a-password-here';
GRANT ALL PRIVILEGES ON ecommerce_db.* TO 'ecomuser'@'localhost';
FLUSH PRIVILEGES;
```

You do **not** need to create any tables. Hibernate creates them on first startup
(`spring.jpa.hibernate.ddl-auto=update`). The file `database/schema.sql` documents the same schema
in readable, commented SQL — use it as a reference, or to provision a database manually where the
app should not have DDL rights.

> **Using the `root` account instead?** Set `DB_USERNAME=root` and the root password in the next
> step. Creating a dedicated user is better practice, but either works.

---

## 3. Configure the backend

```powershell
cd backend
Copy-Item .env.example .env
```

Now open `backend/.env` and fill in the three values the application will not start without.

### Required

| Variable | What to set it to |
|---|---|
| `DB_PASSWORD` | The password you chose for `ecomuser` (or root) |
| `APP_JWT_SECRET` | **At least 32 characters** of random text. This signs every token. |
| `ADMIN_PASSWORD` | The password for the administrator account created on first run |

Generate a JWT secret:

```powershell
# PowerShell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Max 256 }))
```

```bash
# Git Bash
openssl rand -base64 48
```

### Optional

| Variable | Default | Notes |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/ecommerce_db?...` | Change only if MySQL is elsewhere |
| `DB_USERNAME` | `ecomuser` | |
| `ADMIN_EMAIL` | `admin@ecommerce.local` | The seeded admin's sign-in email |
| `ADMIN_PHONE` | `+919999999999` | Must be E.164 format |
| `APP_JWT_EXPIRATION_MS` | `86400000` (24 h) | Token lifetime |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:4173` | Comma-separated |
| `SERVER_PORT` | `8080` | |
| `WHATSAPP_ENABLED` | `false` | See [09](09-whatsapp-notifications.md) |
| `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` / `TWILIO_WHATSAPP_FROM` | empty | Only needed for real WhatsApp delivery |

> **Nothing is hard-coded.** `.env` is git-ignored, and the application deliberately refuses to
> start rather than fall back to a default JWT secret or admin password. A well-known default
> credential is worse than a startup failure.

---

## 4. Run the backend

```powershell
cd backend
.\run.ps1
```

`run.ps1` pins `JAVA_HOME` to a JDK 17+, loads `.env` into the process environment, checks the
required variables are present, then starts Spring Boot. The first run downloads Maven 3.9.9 and
the project dependencies, so give it a few minutes.

You are looking for:

```
Started EcommerceApplication in 4.２s
```

On the **first** run you will also see the seeding log:

```
Seeding role ROLE_CUSTOMER
Seeding role ROLE_ADMIN
Seeded administrator account: admin@ecommerce.local
Seeded category Books
... (six categories)
Seeded 42 starter products
```

Seeding is idempotent — it checks before inserting, so restarting never duplicates data or
resurrects a product an admin deleted.

You will also see, unless you configured Twilio:

```
WhatsApp sending is INACTIVE - app.whatsapp.enabled is false
Messages will be logged and stored with status SKIPPED.
```

That is expected and everything still works. See [09](09-whatsapp-notifications.md).

### Confirm the database was populated

```sql
USE ecommerce_db;
SHOW TABLES;                       -- expect 12 tables
SELECT COUNT(*) FROM products;     -- expect ~42
SELECT name, slug FROM categories; -- expect the six departments
SELECT email FROM users;           -- expect the admin account
```

---

## 5. Run the frontend

In a **second terminal**:

```powershell
cd frontend
npm install      # first time only
npm run dev
```

Open http://localhost:5173.

The Vite dev server proxies `/api` to `http://localhost:8080`, so the browser only ever talks to one
origin and CORS never comes into play during development. That proxy is configured in
`frontend/vite.config.js`.

---

## 6. First-run walkthrough

A five-minute tour that exercises the whole system.

### As a customer

1. **Browse.** The home page shows the six categories plus featured, popular and new-arrival rows —
   all live queries against the seeded catalogue.
2. **Register.** Click *Sign up*. The phone number must be in international format
   (e.g. `+919876543210`) because it is the WhatsApp destination.
3. **Filter.** Go to a category, then narrow by price and rating and change the sort order. Notice
   the URL updating — filters live in the query string, so the result is shareable and the back
   button works.
4. **Add to cart**, then open the cart. Change a quantity; the totals come back from the server.
5. **Checkout.** Enter a delivery address and place the order.
6. **Watch the backend log.** You will see the WhatsApp message that was composed:
   ```
   [WhatsApp - NOT SENT]
     To      : +919876543210
     Message : Hi Aisha, thank you for shopping with us! Your order #ORD-... has been placed...
   ```
7. **Track it.** *My Orders* → the order → a progress timeline with the first step marked current.

### As an administrator

8. **Sign out**, then sign in with `ADMIN_EMAIL` / `ADMIN_PASSWORD`. You land on `/admin`.
9. **Dashboard.** Revenue, order counts by status, best sellers and low stock — all real aggregates.
10. **Orders** → open the order you just placed → advance the status. Each change writes a history
    entry and fires a notification. Try selecting a status: only legal next steps are offered.
11. **Notifications.** The audit log shows every message, including the skipped ones.
12. **Mark the order `DELIVERED`**, then go back to the storefront as the customer — you can now
    review that product. Try reviewing something you never bought; the backend refuses.

---

## 7. Verify with the automated test

```powershell
cd backend
.\smoke-test.ps1 -AdminPassword '<your ADMIN_PASSWORD>'
```

It drives the real API through the whole journey and asserts each step, including that a customer
token gets 403 from admin endpoints and that an illegal status jump is rejected. It prints a
pass/fail line per step and exits non-zero on failure.

---

## Common first-run problems

| Symptom | Cause and fix |
|---|---|
| `UnsupportedClassVersionError` | You ran `mvnw` directly with Java 8 on PATH. Use `.\run.ps1`. |
| `APP_JWT_SECRET is not set` | `.env` missing or the value is blank. See step 3. |
| `Access denied for user 'ecomuser'@'localhost'` | Wrong `DB_PASSWORD`, or the user was never created. See step 2. |
| `Unknown database 'ecommerce_db'` | You skipped the `CREATE DATABASE` in step 2. |
| `Communications link failure` | MySQL is not running. Start the service. |
| Frontend loads but every request fails | The backend is not running, or is on a different port. |
| `Port 8080 was already in use` | Something else has the port. Set `SERVER_PORT=8081` in `.env`. |

The full list, with exact error text, is in [11 — Troubleshooting](11-troubleshooting.md).

---

**Next:** [02 — Architecture](02-architecture.md) to understand how the pieces fit together.

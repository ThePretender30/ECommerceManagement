<#
.SYNOPSIS
    Starts the E-Commerce Spring Boot backend.

.DESCRIPTION
    Does the two things that are easy to get wrong on this machine:

      1. Pins JAVA_HOME to a JDK 17+ installation. The Java on PATH here is
         Java 8, which cannot run Spring Boot 3 - starting with `mvnw` directly
         fails with "class file version" or "UnsupportedClassVersionError".

      2. Loads backend/.env into the process environment, so no secret has to
         live in application.yml or be typed on the command line.

.EXAMPLE
    .\run.ps1
    .\run.ps1 -Package        # build a runnable jar instead of starting
#>

param(
    [switch]$Package,
    [switch]$SkipEnvCheck
)

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

# ---------------------------------------------------------------------------
# 1. Locate a JDK 17 or newer
# ---------------------------------------------------------------------------
function Find-Jdk {
    # Honour an already-correct JAVA_HOME first.

    # Otherwise pick the newest installed JDK >= 17.
    $candidates = @()
    foreach ($root in @("$env:ProgramFiles\Eclipse Adoptium",
                        "$env:ProgramFiles\Java",
                        "$env:ProgramFiles\Microsoft")) {
        if (Test-Path $root) {
            $candidates += Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
                           Where-Object { $_.Name -match 'jdk-?(\d+)' -and [int]$Matches[1] -ge 17 }
        }
    }

    $best = $candidates |
            Where-Object { Test-Path "$($_.FullName)\bin\java.exe" } |
            Sort-Object Name -Descending |
            Select-Object -First 1

    if ($best) { return $best.FullName }
    return $null
}

$jdk = Find-Jdk
if (-not $jdk) {
    Write-Host ""
    Write-Host "  No JDK 17 or newer was found." -ForegroundColor Red
    Write-Host "  Spring Boot 3 requires Java 17+. Install one with:" -ForegroundColor Yellow
    Write-Host "      winget install EclipseAdoptium.Temurin.21.JDK" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

$env:JAVA_HOME = $jdk
Write-Host "  JAVA_HOME -> $jdk" -ForegroundColor DarkGray

# ---------------------------------------------------------------------------
# 2. Load .env
# ---------------------------------------------------------------------------
$envFile = Join-Path $PSScriptRoot '.env'

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        # Skip blanks and comments.
        if ($line -and -not $line.StartsWith('#')) {
            $split = $line.IndexOf('=')
            if ($split -gt 0) {
                $key = $line.Substring(0, $split).Trim()
                $value = $line.Substring($split + 1).Trim()
                # Strip surrounding quotes if present.
                if ($value.Length -ge 2 -and
                    (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                     ($value.StartsWith("'") -and $value.EndsWith("'")))) {
                    $value = $value.Substring(1, $value.Length - 2)
                }
                Set-Item -Path "Env:$key" -Value $value
            }
        }
    }
    Write-Host "  Loaded environment from .env" -ForegroundColor DarkGray
}
else {
    Write-Host ""
    Write-Host "  backend\.env not found." -ForegroundColor Yellow
    Write-Host "  Create it from the template:" -ForegroundColor Yellow
    Write-Host "      Copy-Item .env.example .env" -ForegroundColor Cyan
    Write-Host "  then set DB_PASSWORD, APP_JWT_SECRET and ADMIN_PASSWORD." -ForegroundColor Yellow
    Write-Host ""
}

# ---------------------------------------------------------------------------
# 3. Fail early with a clear message rather than a stack trace
# ---------------------------------------------------------------------------
if (-not $SkipEnvCheck) {
    $missing = @()
    if (-not $env:DB_PASSWORD)   { $missing += 'DB_PASSWORD' }
    if (-not $env:APP_JWT_SECRET) { $missing += 'APP_JWT_SECRET' }

    if ($missing.Count -gt 0) {
        Write-Host ""
        Write-Host "  Missing required setting(s): $($missing -join ', ')" -ForegroundColor Red
        Write-Host "  Set them in backend\.env and run again." -ForegroundColor Yellow
        Write-Host ""
        exit 1
    }

    if ($env:APP_JWT_SECRET.Length -lt 32) {
        Write-Host ""
        Write-Host "  APP_JWT_SECRET must be at least 32 characters (currently $($env:APP_JWT_SECRET.Length))." -ForegroundColor Red
        Write-Host ""
        exit 1
    }
}

# ---------------------------------------------------------------------------
# 4. Go
# ---------------------------------------------------------------------------
if ($Package) {
    Write-Host "  Building jar..." -ForegroundColor Cyan
    & .\mvnw.cmd -B clean package
}
else {
    Write-Host "  Starting backend on http://localhost:$(if ($env:SERVER_PORT) { $env:SERVER_PORT } else { '8080' })" -ForegroundColor Green
    Write-Host "  Swagger UI: http://localhost:8080/swagger-ui.html" -ForegroundColor DarkGray
    Write-Host ""
    & .\mvnw.cmd -B spring-boot:run
}

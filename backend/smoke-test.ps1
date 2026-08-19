<#
.SYNOPSIS
    End-to-end API smoke test for the E-Commerce backend.

.DESCRIPTION
    Drives the real REST API through a complete customer and admin journey and
    asserts the outcome of each step. Nothing is mocked - this talks to the
    running Spring Boot app and therefore to MySQL.

    Covers:
      1.  Health / reachability
      2.  Public browsing works without a token
      3.  Registration and login
      4.  Authorisation: customer token is rejected by admin endpoints (403)
      5.  Authorisation: no token is rejected by protected endpoints (401)
      6.  Cart: add, update quantity, stock validation
      7.  Address creation
      8.  Checkout: order placed, stock decremented, cart emptied
      9.  Order tracking timeline
      10. Admin: status transitions, including rejection of an illegal jump
      11. Notification audit rows are written
      12. Review eligibility before and after delivery
      13. Cancellation restores stock

.PARAMETER BaseUrl
    Backend base URL. Defaults to http://localhost:8080.

.PARAMETER AdminEmail / AdminPassword
    Credentials of the seeded administrator (from backend/.env).

.EXAMPLE
    .\smoke-test.ps1 -AdminPassword 'your-admin-password'
#>

param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$AdminEmail = 'admin@ecommerce.local',
    [Parameter(Mandatory = $true)][string]$AdminPassword
)

$ErrorActionPreference = 'Stop'
$script:passed = 0
$script:failed = 0

# ---------------------------------------------------------------------------
#  Tiny assertion helpers
# ---------------------------------------------------------------------------
function Test-Step {
    param([string]$Name, [scriptblock]$Body)
    try {
        & $Body
        Write-Host "  PASS  $Name" -ForegroundColor Green
        $script:passed++
    }
    catch {
        Write-Host "  FAIL  $Name" -ForegroundColor Red
        Write-Host "        $($_.Exception.Message)" -ForegroundColor DarkRed
        $script:failed++
    }
}

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}

function Assert-Equal {
    param($Expected, $Actual, [string]$Message)
    if ($Expected -ne $Actual) { throw "$Message (expected '$Expected', got '$Actual')" }
}

<# Invokes the API. When -ExpectStatus is given, asserts that exact HTTP
   failure code instead of treating it as an error - which is how the
   authorisation checks below verify 401 and 403. #>
function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        $Body,
        [string]$Token,
        [int]$ExpectStatus
    )

    $headers = @{ 'Content-Type' = 'application/json' }
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }

    $params = @{
        Uri         = "$BaseUrl$Path"
        Method      = $Method
        Headers     = $headers
        ErrorAction = 'Stop'
    }
    if ($null -ne $Body) {
        $params['Body'] = ($Body | ConvertTo-Json -Depth 10 -Compress)
    }

    try {
        $response = Invoke-RestMethod @params
        if ($ExpectStatus) {
            throw "Expected HTTP $ExpectStatus but the request succeeded."
        }
        return $response
    }
    catch [System.Net.WebException], [Microsoft.PowerShell.Commands.HttpResponseException] {
        $status = $null
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
        }
        if ($ExpectStatus) {
            if ($status -ne $ExpectStatus) {
                throw "Expected HTTP $ExpectStatus but got $status."
            }
            return $null
        }
        throw "HTTP $status on $Method $Path - $($_.Exception.Message)"
    }
}

Write-Host ""
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host " E-Commerce API smoke test" -ForegroundColor Cyan
Write-Host " Target: $BaseUrl" -ForegroundColor DarkGray
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

# ---------------------------------------------------------------------------
#  1. Reachability
# ---------------------------------------------------------------------------
Write-Host "[1] Connectivity" -ForegroundColor Yellow
Test-Step "Backend is reachable and serving products" {
    $products = Invoke-Api -Method GET -Path '/api/products?size=1'
    Assert-True ($null -ne $products) "No response from /api/products"
}

# ---------------------------------------------------------------------------
#  2. Public browsing (no token)
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "[2] Public browsing without authentication" -ForegroundColor Yellow

$script:categories = $null
Test-Step "Six seeded categories are present" {
    $script:categories = Invoke-Api -Method GET -Path '/api/categories'
    Assert-True ($script:categories.Count -ge 6) "Expected at least 6 categories, found $($script:categories.Count)"
}

Test-Step "Category filtering returns products" {
    $result = Invoke-Api -Method GET -Path '/api/products?category=electronics&size=5'
    Assert-True ($result.totalElements -gt 0) "No products in the electronics category"
}

Test-Step "Keyword search returns results" {
    $result = Invoke-Api -Method GET -Path '/api/products?q=coffee'
    Assert-True ($result.totalElements -gt 0) "Search for 'coffee' returned nothing"
}

Test-Step "Sorting by price ascending is ordered correctly" {
    $result = Invoke-Api -Method GET -Path '/api/products?sort=price_asc&size=10'
    $prices = $result.content | ForEach-Object { [decimal]$_.price }
    for ($i = 1; $i -lt $prices.Count; $i++) {
        Assert-True ($prices[$i] -ge $prices[$i - 1]) "Prices are not in ascending order"
    }
}

Test-Step "Price filtering respects the upper bound" {
    $result = Invoke-Api -Method GET -Path '/api/products?maxPrice=500&size=20'
    foreach ($p in $result.content) {
        Assert-True ([decimal]$p.price -le 500) "Product '$($p.name)' priced $($p.price) exceeds maxPrice=500"
    }
}

# ---------------------------------------------------------------------------
#  3. Registration and login
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "[3] Registration and login" -ForegroundColor Yellow

$stamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$customerEmail = "smoketest+$stamp@example.com"
$customerPassword = 'SmokeTest123!'
$script:customerToken = $null

Test-Step "A new customer can register" {
    $auth = Invoke-Api -Method POST -Path '/api/auth/register' -Body @{
        fullName    = 'Smoke Test User'
        email       = $customerEmail
        password    = $customerPassword
        phoneNumber = '+919876500000'
    }
    Assert-True ($auth.token.Length -gt 20) "No JWT returned from registration"
    Assert-Equal 'ROLE_CUSTOMER' $auth.user.roles[0] "New account did not receive ROLE_CUSTOMER"
    $script:customerToken = $auth.token
}

Test-Step "Registering the same email again is rejected (409)" {
    Invoke-Api -Method POST -Path '/api/auth/register' -ExpectStatus 409 -Body @{
        fullName    = 'Duplicate'
        email       = $customerEmail
        password    = $customerPassword
        phoneNumber = '+919876500001'
    }
}

Test-Step "A wrong password is rejected (401)" {
    Invoke-Api -Method POST -Path '/api/auth/login' -ExpectStatus 401 -Body @{
        email    = $customerEmail
        password = 'definitely-wrong'
    }
}

Test-Step "The customer can sign in and reach /api/auth/me" {
    $auth = Invoke-Api -Method POST -Path '/api/auth/login' -Body @{
        email = $customerEmail; password = $customerPassword
    }
    $script:customerToken = $auth.token
    $me = Invoke-Api -Method GET -Path '/api/auth/me' -Token $script:customerToken
    Assert-Equal $customerEmail $me.email "/api/auth/me returned the wrong account"
}

$script:adminToken = $null
Test-Step "The administrator can sign in" {
    $auth = Invoke-Api -Method POST -Path '/api/auth/login' -Body @{
        email = $AdminEmail; password = $AdminPassword
    }
    Assert-True ($auth.user.roles -contains 'ROLE_ADMIN') "Seeded account does not hold ROLE_ADMIN"
    $script:adminToken = $auth.token
}

# ---------------------------------------------------------------------------
#  4. Authorisation boundaries
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "[4] Authorisation" -ForegroundColor Yellow

Test-Step "Protected endpoint without a token returns 401" {
    Invoke-Api -Method GET -Path '/api/cart' -ExpectStatus 401
}

Test-Step "Admin endpoint with a customer token returns 403" {
    Invoke-Api -Method GET -Path '/api/admin/stats' -Token $script:customerToken -ExpectStatus 403
}

Test-Step "Admin product creation with a customer token returns 403" {
    Invoke-Api -Method POST -Path '/api/admin/products' -Token $script:customerToken -ExpectStatus 403 -Body @{
        name = 'Should never be created'; price = 1; categoryId = 1; stock = 1
    }
}

Test-Step "Admin endpoint with an admin token succeeds" {
    $stats = Invoke-Api -Method GET -Path '/api/admin/stats' -Token $script:adminToken
    Assert-True ($null -ne $stats.totalOrders) "Stats response is missing totalOrders"
}

# ---------------------------------------------------------------------------
#  5. Cart
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "[5] Shopping cart" -ForegroundColor Yellow

$script:product = $null
Test-Step "Pick an in-stock product to buy" {
    $result = Invoke-Api -Method GET -Path '/api/products?inStock=true&size=20&sort=newest'
    $script:product = $result.content | Where-Object { $_.stock -ge 5 } | Select-Object -First 1
    Assert-True ($null -ne $script:product) "No product with at least 5 units in stock"
}

$script:initialStock = 0
Test-Step "Add the product to the cart" {
    $script:initialStock = [int]$script:product.stock
    $cart = Invoke-Api -Method POST -Path '/api/cart/items' -Token $script:customerToken -Body @{
        productId = $script:product.id; quantity = 2
    }
    Assert-Equal 1 $cart.items.Count "Cart should contain exactly one line"
    Assert-Equal 2 $cart.items[0].quantity "Cart quantity should be 2"
}

Test-Step "Adding the same product again increments rather than duplicating" {
    $cart = Invoke-Api -Method POST -Path '/api/cart/items' -Token $script:customerToken -Body @{
        productId = $script:product.id; quantity = 1
    }
    Assert-Equal 1 $cart.items.Count "A duplicate cart line was created"
    Assert-Equal 3 $cart.items[0].quantity "Quantity should have incremented to 3"
}

Test-Step "The cart subtotal is computed server-side and is correct" {
    $cart = Invoke-Api -Method GET -Path '/api/cart' -Token $script:customerToken
    $expected = [decimal]$script:product.price * 3
    Assert-Equal $expected ([decimal]$cart.subtotal) "Subtotal mismatch"
}

Test-Step "Ordering more than the available stock is rejected (409)" {
    $itemId = (Invoke-Api -Method GET -Path '/api/cart' -Token $script:customerToken).items[0].id
    Invoke-Api -Method PUT -Path "/api/cart/items/$itemId" -Token $script:customerToken -ExpectStatus 409 -Body @{
        quantity = $script:initialStock + 100
    }
}

Test-Step "Quantity can be reduced back to 2" {
    $itemId = (Invoke-Api -Method GET -Path '/api/cart' -Token $script:customerToken).items[0].id
    $cart = Invoke-Api -Method PUT -Path "/api/cart/items/$itemId" -Token $script:customerToken -Body @{ quantity = 2 }
    Assert-Equal 2 $cart.items[0].quantity "Quantity was not updated to 2"
}

# ---------------------------------------------------------------------------
#  6. Address + checkout
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "[6] Checkout" -ForegroundColor Yellow

$script:addressId = $null
Test-Step "Create a delivery address" {
    $address = Invoke-Api -Method POST -Path '/api/addresses' -Token $script:customerToken -Body @{
        fullName   = 'Smoke Test User'
        phone      = '+919876500000'
        line1      = '42 Test Street'
        city       = 'Mumbai'
        state      = 'Maharashtra'
        postalCode = '400001'
        country    = 'India'
    }
    Assert-True $address.isDefault "The first saved address should become the default"
    $script:addressId = $address.id
}

$script:orderId = $null
Test-Step "Place the order" {
    $order = Invoke-Api -Method POST -Path '/api/orders' -Token $script:customerToken -Body @{
        addressId = $script:addressId
    }
    Assert-Equal 'ORDER_PLACED' $order.status "New order should start at ORDER_PLACED"
    Assert-True ($order.orderNumber -like 'ORD-*') "Unexpected order number format: $($order.orderNumber)"
    $expectedTotal = [decimal]$script:product.price * 2
    Assert-Equal $expectedTotal ([decimal]$order.totalAmount) "Order total mismatch"
    $script:orderId = $order.id
}

Test-Step "Stock was decremented by the ordered quantity" {
    $refreshed = Invoke-Api -Method GET -Path "/api/products/$($script:product.id)"
    Assert-Equal ($script:initialStock - 2) ([int]$refreshed.stock) "Stock was not decremented correctly"
}

Test-Step "The cart was emptied by checkout" {
    $cart = Invoke-Api -Method GET -Path '/api/cart' -Token $script:customerToken
    Assert-Equal 0 $cart.items.Count "Cart should be empty after checkout"
}

Test-Step "Checking out an empty cart is rejected (400)" {
    Invoke-Api -Method POST -Path '/api/orders' -Token $script:customerToken -ExpectStatus 400 -Body @{
        addressId = $script:addressId
    }
}

Test-Step "The order appears in the customer's history" {
    $orders = Invoke-Api -Method GET -Path '/api/orders' -Token $script:customerToken
    Assert-True ($orders.totalElements -ge 1) "Order history is empty"
}

Test-Step "Order tracking returns a timeline with the first step complete" {
    $tracking = Invoke-Api -Method GET -Path "/api/orders/$($script:orderId)/tracking" -Token $script:customerToken
    Assert-Equal 6 $tracking.progressSteps.Count "Expected six happy-path tracking steps"
    Assert-Equal 'current' $tracking.progressSteps[0].state "ORDER_PLACED should be the current step"
}

# ---------------------------------------------------------------------------
#  7. Review eligibility before delivery
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "[7] Review eligibility" -ForegroundColor Yellow

Test-Step "Reviewing before delivery is rejected (400)" {
    Invoke-Api -Method POST -Path "/api/products/$($script:product.id)/reviews" -Token $script:customerToken -ExpectStatus 400 -Body @{
        rating = 5; reviewText = 'Too early to review.'
    }
}

# ---------------------------------------------------------------------------
#  8. Admin status transitions
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "[8] Admin order management" -ForegroundColor Yellow

Test-Step "An illegal status jump is rejected (400)" {
    # ORDER_PLACED -> DELIVERED skips four stages and must be refused.
    Invoke-Api -Method PUT -Path "/api/admin/orders/$($script:orderId)/status" -Token $script:adminToken -ExpectStatus 400 -Body @{
        status = 'DELIVERED'
    }
}

Test-Step "The order advances through every legal stage to DELIVERED" {
    foreach ($stage in @('ORDER_CONFIRMED', 'PROCESSING', 'DISPATCHED', 'OUT_FOR_DELIVERY', 'DELIVERED')) {
        $updated = Invoke-Api -Method PUT -Path "/api/admin/orders/$($script:orderId)/status" -Token $script:adminToken -Body @{
            status = $stage; note = "Advanced to $stage by smoke test."
        }
        Assert-Equal $stage $updated.status "Order did not move to $stage"
    }
}

Test-Step "The status history recorded all six stages" {
    $order = Invoke-Api -Method GET -Path "/api/orders/$($script:orderId)" -Token $script:customerToken
    Assert-Equal 6 $order.statusHistory.Count "Expected six status-history entries"
}

# ---------------------------------------------------------------------------
#  9. Notifications
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "[9] WhatsApp notification pipeline" -ForegroundColor Yellow

Test-Step "A notification row exists for every status change" {
    # Notifications are dispatched asynchronously after commit, so allow a moment.
    Start-Sleep -Milliseconds 1500
    $notifications = Invoke-Api -Method GET -Path "/api/admin/orders/$($script:orderId)/notifications" -Token $script:adminToken
    Assert-True ($notifications.Count -ge 6) "Expected at least 6 notification records, found $($notifications.Count)"

    $statuses = $notifications | ForEach-Object { $_.status } | Sort-Object -Unique
    Write-Host "        outcomes: $($statuses -join ', ')" -ForegroundColor DarkGray
    foreach ($s in $statuses) {
        Assert-True ($s -in @('SENT', 'FAILED', 'SKIPPED')) "Unexpected notification status '$s'"
    }
}

# ---------------------------------------------------------------------------
#  10. Review after delivery
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "[10] Reviews after delivery" -ForegroundColor Yellow

Test-Step "A delivered order makes the customer eligible to review" {
    $summary = Invoke-Api -Method GET -Path "/api/products/$($script:product.id)/reviews/summary" -Token $script:customerToken
    Assert-True $summary.canReview "Customer should be eligible to review after delivery"
}

Test-Step "The review is accepted and updates the product rating" {
    Invoke-Api -Method POST -Path "/api/products/$($script:product.id)/reviews" -Token $script:customerToken -Body @{
        rating = 4; reviewText = 'Solid product, arrived quickly.'
    } | Out-Null

    $refreshed = Invoke-Api -Method GET -Path "/api/products/$($script:product.id)"
    Assert-True ([decimal]$refreshed.averageRating -gt 0) "Product average rating was not recalculated"
    Assert-True ([int]$refreshed.reviewCount -ge 1) "Product review count was not updated"
}

# ---------------------------------------------------------------------------
#  11. Cancellation restores stock
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "[11] Cancellation" -ForegroundColor Yellow

Test-Step "A second order can be cancelled and its stock restored" {
    $before = [int](Invoke-Api -Method GET -Path "/api/products/$($script:product.id)").stock

    Invoke-Api -Method POST -Path '/api/cart/items' -Token $script:customerToken -Body @{
        productId = $script:product.id; quantity = 1
    } | Out-Null

    $order = Invoke-Api -Method POST -Path '/api/orders' -Token $script:customerToken -Body @{
        addressId = $script:addressId
    }

    $afterOrder = [int](Invoke-Api -Method GET -Path "/api/products/$($script:product.id)").stock
    Assert-Equal ($before - 1) $afterOrder "Stock was not decremented by the second order"

    $cancelled = Invoke-Api -Method PUT -Path "/api/orders/$($order.id)/cancel" -Token $script:customerToken
    Assert-Equal 'CANCELLED' $cancelled.status "Order was not cancelled"

    $afterCancel = [int](Invoke-Api -Method GET -Path "/api/products/$($script:product.id)").stock
    Assert-Equal $before $afterCancel "Stock was not restored after cancellation"
}

Test-Step "A delivered order can no longer be cancelled (400)" {
    Invoke-Api -Method PUT -Path "/api/orders/$($script:orderId)/cancel" -Token $script:customerToken -ExpectStatus 400
}

# ---------------------------------------------------------------------------
#  Summary
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host " Passed: $script:passed   Failed: $script:failed" -ForegroundColor $(if ($script:failed -eq 0) { 'Green' } else { 'Red' })
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

if ($script:failed -gt 0) { exit 1 }
exit 0

# API smoke / regression for the Hardmetal export backend.
# PowerShell 5.1 compatible: uses curl.exe plus temp files because
# System.Net.Http is not loaded and -SkipHttpErrorCheck does not exist in 5.1.
#
# Usage: powershell -ExecutionPolicy Bypass -File scripts\api-smoke.ps1 [-Base http://127.0.0.1:8081/api/v1]
param(
    [string]$Base = "http://127.0.0.1:8081/api/v1",
    [string]$AdminUser = "admin",
    [string]$AdminPass = "Admin@2026",
    [string]$SalesUser = "sales1",
    [string]$SalesPass = "Admin@2026"
)

$ErrorActionPreference = "Stop"
$tmp = Join-Path (Split-Path -Parent $PSScriptRoot) ".tmp"
if (-not (Test-Path $tmp)) { New-Item -ItemType Directory -Path $tmp | Out-Null }
$bodyFile = Join-Path $tmp "smoke-req.json"
$respFile = Join-Path $tmp "smoke-resp.json"
$utf8 = New-Object System.Text.UTF8Encoding($false)
$results = New-Object System.Collections.ArrayList
$rootBase = $Base -replace "/api/v1/?$", ""

# 读取仓库根目录的 .env（模板 .env.example），已存在的环境变量优先，不做覆盖。
$repoRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $repoRoot ".env"
if (Test-Path $envFile) {
    foreach ($line in Get-Content $envFile -Encoding UTF8) {
        $t = $line.Trim()
        if (-not $t -or $t.StartsWith("#")) { continue }
        $i = $t.IndexOf("=")
        if ($i -lt 1) { continue }
        $k = $t.Substring(0, $i).Trim()
        $v = $t.Substring($i + 1).Trim()
        if (-not (Test-Path "Env:$k")) { Set-Item -Path "Env:$k" -Value $v }
    }
}

# Fixed test code; wipe leftovers from previous runs so the suite is repeatable.
$testCode = "SMOKE-0001"
$psql = if ($env:PSQL_EXE) { $env:PSQL_EXE }
        elseif (Get-Command psql -ErrorAction SilentlyContinue) { "psql" }
        else { "D:\pgsql\18.6\pgsql\bin\psql.exe" }
$dbUser = if ($env:DB_USER) { $env:DB_USER } else { "hardmetal" }
if (Test-Path $psql) {
    $env:PGPASSWORD = $env:DB_PASSWORD
    & $psql -h 127.0.0.1 -p 5432 -U $dbUser -d trade -t -c "delete from customer_contact where customer_id in (select id from customer where customer_code like 'SMOKE-%');" 2>$null | Out-Null
    & $psql -h 127.0.0.1 -p 5432 -U $dbUser -d trade -t -c "delete from customer where customer_code like 'SMOKE-%';" 2>$null | Out-Null
}

function Call-Api($method, $path, $body, $token) {
    $url = if ($path -match '^https?://') { $path } else { "$Base$path" }
    $args = @("-s", "-X", $method, $url, "-H", "Accept: application/json",
        "-o", $respFile, "-w", "%{http_code}", "--max-time", "20")
    if ($null -ne $body) {
        [IO.File]::WriteAllText($bodyFile, ($body | ConvertTo-Json -Depth 6 -Compress), $utf8)
        $args += @("-H", "Content-Type: application/json; charset=utf-8", "--data-binary", "@$bodyFile")
    }
    if ($token) { $args += @("-H", "Authorization: Bearer $token") }
    $status = (& curl.exe @args) -join ""
    $text = ""
    if (Test-Path $respFile) { $text = [IO.File]::ReadAllText($respFile, [Text.Encoding]::UTF8) }
    $json = $null
    try { $json = $text | ConvertFrom-Json } catch { }
    [pscustomobject]@{
        Status = [int]$status
        Code   = $json.code
        Msg    = $json.msg
        Data   = $json.data
        Raw    = $text
    }
}

function Check($name, $condition, $detail) {
    $tag = if ($condition) { "PASS" } else { "FAIL" }
    [void]$results.Add("$tag  $name  $detail")
}

# ---------- anonymous ----------
$h = Call-Api "GET" "/health" $null $null
Check "health" ($h.Status -eq 200 -and $h.Code -eq 0 -and $h.Data.status -eq "UP") "status=$($h.Status) code=$($h.Code)"

$act = Call-Api "GET" "$rootBase/actuator/health" $null $null
$actJson = $act.Raw | ConvertFrom-Json
Check "actuator health public" ($act.Status -eq 200 -and $actJson.status -eq "UP" -and $actJson.components.redis.status -eq "UP") "status=$($act.Status) overall=$($actJson.status) redis=$($actJson.components.redis.status)"

$wrong = Call-Api "POST" "/auth/login" @{ username = $AdminUser; password = "wrong-password" } $null
Check "login wrong password" ($wrong.Status -eq 401 -and $wrong.Code -eq 40103) "status=$($wrong.Status) code=$($wrong.Code)"

$noAuth = Call-Api "GET" "/auth/me" $null $null
Check "me without token" ($noAuth.Status -eq 401 -and $noAuth.Code -eq 40101) "status=$($noAuth.Status) code=$($noAuth.Code)"

# ---------- login / refresh ----------
$login = Call-Api "POST" "/auth/login" @{ username = $AdminUser; password = $AdminPass } $null
$access = $login.Data.accessToken
$refresh = $login.Data.refreshToken
Check "login returns contract fields" ($login.Status -eq 200 -and $access -and $refresh -and $login.Data.expiresIn -eq 7200) "status=$($login.Status) expiresIn=$($login.Data.expiresIn) keys=$($login.Data.PSObject.Properties.Name -join ',')"
Check "login user+perms" ($login.Data.user.roleCode -eq "ADMIN" -and @($login.Data.user.perms).Count -ge 12) "role=$($login.Data.user.roleCode) perms=$(@($login.Data.user.perms).Count) item(s)"

$refreshed = Call-Api "POST" "/auth/refresh" @{ refreshToken = $refresh } $null
$access2 = $refreshed.Data.accessToken
$refresh2 = $refreshed.Data.refreshToken
Check "refresh rotates tokens" ($refreshed.Status -eq 200 -and $access2 -and $access2 -ne $access -and $refreshed.Data.expiresIn -eq 7200) "status=$($refreshed.Status) rotated=$($access2 -ne $access)"

$reuse = Call-Api "POST" "/auth/refresh" @{ refreshToken = $refresh } $null
Check "refresh token is one-shot" ($reuse.Status -eq 401 -and $reuse.Code -eq 40102) "status=$($reuse.Status) code=$($reuse.Code)"

$me = Call-Api "GET" "/auth/me" $null $access2
Check "me with access token" ($me.Status -eq 200 -and $me.Data.username -eq $AdminUser) "status=$($me.Status) user=$($me.Data.username)"

$asBearer = Call-Api "GET" "/auth/me" $null $refresh2
Check "refresh token rejected as bearer" ($asBearer.Status -eq 401) "status=$($asBearer.Status) code=$($asBearer.Code)"

# ---------- routing / errors ----------
$notFound = Call-Api "GET" "/nope" $null $access2
Check "404 unified body" ($notFound.Status -eq 404 -and $notFound.Code -eq 40401) "status=$($notFound.Status) code=$($notFound.Code)"

$method = Call-Api "PATCH" "/customers/1" @{} $access2
Check "405 unified body" ($method.Status -eq 405) "status=$($method.Status) code=$($method.Code)"

# ---------- customer vertical ----------
$list0 = Call-Api "GET" "/customers?page=1&size=20" $null $access2
Check "customer list" ($list0.Status -eq 200 -and $null -ne $list0.Data.items) "status=$($list0.Status) total=$($list0.Data.total)"

$payload = @{
    code             = $testCode
    nameCn           = "Muller Hartmetall GmbH"
    nameEn           = "Hartmetall Mueller GmbH"
    customerType     = "end_user"
    level            = "A"
    countryCode      = "DE"
    currency         = "EUR"
    machiningMaterials = @("stainless", "superalloy")
    machiningOps     = @("turning", "milling")
    machineBrands    = @("DMG Mori", "Hermle")
    benchmarkBrand   = "Sandvik"
    paymentTerms     = "30% T/T in advance"
    creditDays       = 30
}
$created = Call-Api "POST" "/customers" $payload $access2
$cid = $created.Data.id
Check "customer create" ($created.Status -eq 200 -and $cid -gt 0 -and $created.Data.code -eq $testCode) "status=$($created.Status) id=$cid msg=$($created.Msg)"

$dup = Call-Api "POST" "/customers" $payload $access2
Check "duplicate code rejected" ($dup.Status -eq 409 -and $dup.Code -eq 40903) "status=$($dup.Status) code=$($dup.Code)"

$invalid = Call-Api "POST" "/customers" @{ code = "X-0001"; customerType = "end_user"; countryCode = "DE" } $access2
Check "missing nameCn rejected" ($invalid.Status -eq 400 -and $invalid.Code -eq 40001) "status=$($invalid.Status) code=$($invalid.Code) msg=$($invalid.Msg)"

if ($cid -gt 0) {
    $detail = Call-Api "GET" "/customers/$cid" $null $access2
    $mats = @($detail.Data.machiningMaterials)
    $brands = @($detail.Data.machineBrands)
    $ops = @($detail.Data.machiningOps)
    Check "customer detail arrays" ($detail.Status -eq 200 -and $mats -contains "stainless" -and $brands -contains "DMG Mori" -and $ops -contains "turning") "status=$($detail.Status) materials=$($mats -join '|') ops=$($ops -join '|') brands=$($brands -join '|')"
    Check "sanctions starts unreviewed" ($detail.Data.sanctionsStatus -eq "unreviewed") "sanctionsStatus=$($detail.Data.sanctionsStatus)"

    $updated = Call-Api "PUT" "/customers/$cid" @{ level = "B" } $access2
    $detail2 = Call-Api "GET" "/customers/$cid" $null $access2
    Check "partial update keeps other fields" ($updated.Status -eq 200 -and $detail2.Data.level -eq "B" -and $detail2.Data.nameEn -eq "Hartmetall Mueller GmbH") "status=$($updated.Status) level=$($detail2.Data.level)"

    $timeline = Call-Api "GET" "/customers/$cid/timeline" $null $access2
    Check "timeline empty ok" ($timeline.Status -eq 200 -and @($timeline.Data.items).Count -eq 0) "status=$($timeline.Status) items=$(@($timeline.Data.items).Count)"

    $contact = Call-Api "POST" "/customers/$cid/contacts" @{ name = "Thomas Mueller"; position = "purchasing"; email = "t.mueller@example.de"; isDecisionMaker = $true } $access2
    $contactId = $contact.Data.id
    Check "contact add" ($contact.Status -eq 200 -and $contactId -gt 0 -and $contact.Data.isDecisionMaker -eq $true) "status=$($contact.Status) id=$contactId"

    $contacts = Call-Api "GET" "/customers/$cid/contacts" $null $access2
    Check "contact list" ($contacts.Status -eq 200 -and @($contacts.Data).Count -eq 1) "status=$($contacts.Status) count=$(@($contacts.Data).Count)"

    $removed = Call-Api "DELETE" "/customers/$cid/contacts/$contactId" $null $access2
    Check "contact delete" ($removed.Status -eq 200 -and $removed.Code -eq 0) "status=$($removed.Status)"

    $contact2 = Call-Api "POST" "/customers/$cid/contacts" @{ name = "Second Contact" } $access2
    $contact2Id = $contact2.Data.id

    $salesLogin = Call-Api "POST" "/auth/login" @{ username = $SalesUser; password = $SalesPass } $null
    $salesToken = $salesLogin.Data.accessToken
    Check "sales login" ($salesLogin.Status -eq 200 -and $salesToken -and $salesLogin.Data.user.roleCode -eq "SALES") "status=$($salesLogin.Status) role=$($salesLogin.Data.user.roleCode)"
    Check "sales has no cost:read" (@($salesLogin.Data.user.perms) -notcontains "cost:read") "perms=$(@($salesLogin.Data.user.perms) -join ',')"

    $salesList = Call-Api "GET" "/customers" $null $salesToken
    Check "sales sees only own customers" ($salesList.Status -eq 200 -and $salesList.Data.total -eq 0) "status=$($salesList.Status) total=$($salesList.Data.total)"

    $forbidden = Call-Api "GET" "/customers/$cid" $null $salesToken
    Check "sales blocked on foreign customer" ($forbidden.Status -eq 403 -and $forbidden.Code -eq 40301) "status=$($forbidden.Status) code=$($forbidden.Code)"

    $salesDelete = Call-Api "DELETE" "/customers/$cid" $null $salesToken
    Check "sales cannot delete customer" ($salesDelete.Status -eq 403 -and $salesDelete.Code -eq 40301) "status=$($salesDelete.Status) code=$($salesDelete.Code)"

    $salesOwner = Call-Api "POST" "/customers" @{ code = "SMOKE-OWNER"; nameCn = "Owner Test"; customerType = "end_user"; countryCode = "DE"; ownerId = 1 } $salesToken
    Check "sales cannot reassign owner" ($salesOwner.Status -eq 403 -and $salesOwner.Code -eq 40301) "status=$($salesOwner.Status) code=$($salesOwner.Code)"

    $salesContactDelete = Call-Api "DELETE" "/customers/$cid/contacts/$contact2Id" $null $salesToken
    Check "sales cannot delete foreign contact" ($salesContactDelete.Status -eq 403 -and $salesContactDelete.Code -eq 40301) "status=$($salesContactDelete.Status) code=$($salesContactDelete.Code)"

    $badCredit = Call-Api "POST" "/customers" @{ code = "SMOKE-BAD"; nameCn = "Bad"; customerType = "end_user"; countryCode = "DE"; creditLimit = -1 } $access2
    Check "creditLimit negative rejected" ($badCredit.Status -eq 400 -and $badCredit.Code -eq 40001) "status=$($badCredit.Status) msg=$($badCredit.Msg)"

    $badBrands = Call-Api "POST" "/customers" @{ code = "SMOKE-BAD"; nameCn = "Bad"; customerType = "end_user"; countryCode = "DE"; machineBrands = @(1..21 | ForEach-Object { "B$_" }) } $access2
    Check "machineBrands over limit rejected" ($badBrands.Status -eq 400 -and $badBrands.Code -eq 40001) "status=$($badBrands.Status) msg=$($badBrands.Msg)"

    $badMaterial = Call-Api "POST" "/customers" @{ code = "SMOKE-BAD"; nameCn = "Bad"; customerType = "end_user"; countryCode = "DE"; machiningMaterials = @("bogus") } $access2
    Check "unknown machining material rejected" ($badMaterial.Status -eq 400 -and $badMaterial.Code -eq 40001) "status=$($badMaterial.Status) msg=$($badMaterial.Msg)"

    $badLen = Call-Api "POST" "/customers" @{ code = "SMOKE-BAD"; nameCn = ("x" * 129); customerType = "end_user"; countryCode = "DE" } $access2
    Check "nameCn length rejected" ($badLen.Status -eq 400 -and $badLen.Code -eq 40001) "status=$($badLen.Status) msg=$($badLen.Msg)"

    if (Test-Path $psql) {
        & $psql -h 127.0.0.1 -p 5432 -U $dbUser -d trade -v ON_ERROR_STOP=1 -t -c "insert into sales_order(order_no, customer_id, currency_code) values ('SO-SMOKE-1', $cid, 'USD');" | Out-Null
        $blocked = Call-Api "DELETE" "/customers/$cid" $null $access2
        Check "delete blocked by open order" ($blocked.Status -eq 409 -and $blocked.Code -eq 40904) "status=$($blocked.Status) code=$($blocked.Code)"
        & $psql -h 127.0.0.1 -p 5432 -U $dbUser -d trade -t -c "delete from sales_order where order_no = 'SO-SMOKE-1';" | Out-Null
    }

    $deleted = Call-Api "DELETE" "/customers/$cid" $null $access2
    Check "customer delete" ($deleted.Status -eq 200 -and $deleted.Code -eq 0) "status=$($deleted.Status)"

    $gone = Call-Api "GET" "/customers/$cid" $null $access2
    Check "deleted customer gone" ($gone.Status -eq 404 -and $gone.Code -eq 40401) "status=$($gone.Status) code=$($gone.Code)"
}

# ---------- system / master data (12.2 - 12.7) ----------
$hs = Call-Api "GET" "/hs-codes?q=8113" $null $access2
Check "hs list by q" ($hs.Status -eq 200 -and @($hs.Data.items).Count -ge 1 -and @($hs.Data.items[0].declarationElements).Count -ge 1) "status=$($hs.Status) items=$(@($hs.Data.items).Count)"

$hsPage = Call-Api "GET" "/hs-codes?chapter=8207&page=1&size=2" $null $access2
Check "hs chapter paging" ($hsPage.Status -eq 200 -and @($hsPage.Data.items).Count -eq 2 -and $hsPage.Data.total -ge 2) "status=$($hsPage.Status) items=$(@($hsPage.Data.items).Count) total=$($hsPage.Data.total)"

$hsBad = Call-Api "POST" "/hs-codes" @{ code = "123"; nameCn = "x" } $access2
Check "hs bad code 40001" ($hsBad.Status -eq 400 -and $hsBad.Code -eq 40001) "status=$($hsBad.Status) code=$($hsBad.Code)"

$hsForbidden = Call-Api "POST" "/hs-codes" @{ code = "8207888888"; nameCn = "x" } $salesToken
Check "sales cannot write hs" ($hsForbidden.Status -eq 403 -and $hsForbidden.Code -eq 40301) "status=$($hsForbidden.Status) code=$($hsForbidden.Code)"

$ports = Call-Api "GET" "/ports?q=Hamburg" $null $access2
Check "port search" ($ports.Status -eq 200 -and @($ports.Data.items).Count -eq 1 -and $ports.Data.items[0].code -eq "DEHAM") "status=$($ports.Status) items=$(@($ports.Data.items).Count)"

$portBad = Call-Api "GET" "/ports?type=dryport" $null $access2
Check "port bad type 40001" ($portBad.Status -eq 400 -and $portBad.Code -eq 40001) "status=$($portBad.Status) code=$($portBad.Code)"

$fx = Call-Api "GET" "/currencies" $null $access2
Check "currency list" ($fx.Status -eq 200 -and $fx.Data.base -eq "CNY" -and @($fx.Data.items).Count -ge 5 -and @($fx.Data.items | Where-Object { $_.currency -eq "CNY" }).Count -eq 0) "status=$($fx.Status) items=$(@($fx.Data.items).Count)"

$fxBad = Call-Api "GET" "/currencies?base=USD" $null $access2
Check "currency base USD 40001" ($fxBad.Status -eq 400 -and $fxBad.Code -eq 40001) "status=$($fxBad.Status) code=$($fxBad.Code)"

$mat = Call-Api "GET" "/raw-material-prices" $null $access2
Check "material price list" ($mat.Status -eq 200 -and @($mat.Data.items).Count -ge 5) "status=$($mat.Status) items=$(@($mat.Data.items).Count)"

$matCreated = Call-Api "POST" "/raw-material-prices" @{ material = "smoke_test"; priceCnyKg = "100.0000"; date = "2026-11-13"; source = "manual" } $access2
$matDup = Call-Api "POST" "/raw-material-prices" @{ material = "smoke_test"; priceCnyKg = "101.0000"; date = "2026-11-13"; source = "manual" } $access2
Check "material dup 40903" ($matCreated.Status -eq 200 -and $matDup.Status -eq 409 -and $matDup.Code -eq 40903) "first=$($matCreated.Status) dup=$($matDup.Status) code=$($matDup.Code)"
if (Test-Path $psql) {
    & $psql -h 127.0.0.1 -p 5432 -U $dbUser -d trade -t -c "delete from raw_material_price where material_type = 'smoke_test';" | Out-Null
}
# ---------- logout (last, revokes the token used above) ----------
$logout = Call-Api "POST" "/auth/logout" @{ refreshToken = $refresh2 } $access2
$afterLogout = Call-Api "GET" "/auth/me" $null $access2
$refreshAfterLogout = Call-Api "POST" "/auth/refresh" @{ refreshToken = $refresh2 } $null
Check "logout revokes access and refresh" ($logout.Status -eq 200 -and $afterLogout.Status -eq 401 -and $refreshAfterLogout.Status -eq 401) "logout=$($logout.Status) me=$($afterLogout.Status) refresh=$($refreshAfterLogout.Status)"

$pass = @($results | Where-Object { $_ -like "PASS*" }).Count
$fail = @($results | Where-Object { $_ -like "FAIL*" }).Count
$results | ForEach-Object { $_ }
""
"===== $pass passed, $fail failed ====="
if ($fail -gt 0) { exit 1 }
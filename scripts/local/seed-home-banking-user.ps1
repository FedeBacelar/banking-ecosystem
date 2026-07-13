[CmdletBinding()]
param(
    [string] $KeycloakBaseUrl = "http://localhost:8090",
    [string] $Realm = "banking-ecosystem",
    [string] $ClientId = "onboarding-orchestrator",
    [string] $ClientSecret = "local-onboarding-secret",
    [string] $CustomerServiceUrl = "http://localhost:8080",
    [string] $AccountServiceUrl = "http://localhost:8081",
    [string] $IdentityServiceUrl = "http://localhost:8082"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$Username = 'home-banking-user'

function Join-Endpoint {
    param([string] $BaseUrl, [string] $Path)

    return $BaseUrl.TrimEnd('/') + '/' + $Path.TrimStart('/')
}

function Get-HttpStatusCode {
    param([System.Management.Automation.ErrorRecord] $ErrorRecord)

    $response = $ErrorRecord.Exception.Response
    if ($null -eq $response) {
        return $null
    }

    if ($response.StatusCode -is [int]) {
        return [int] $response.StatusCode
    }

    if ($null -ne $response.StatusCode.value__) {
        return [int] $response.StatusCode.value__
    }

    return [int] $response.StatusCode
}

function Invoke-NervaApi {
    param(
        [ValidateSet('GET', 'POST', 'PATCH')]
        [string] $Method,
        [string] $Uri,
        [hashtable] $Headers,
        [object] $Body,
        [switch] $AllowNotFound
    )

    $request = @{
        Method      = $Method
        Uri         = $Uri
        Headers     = $Headers
        ErrorAction = 'Stop'
    }

    if ($null -ne $Body) {
        $request.ContentType = 'application/json'
        $request.Body = $Body | ConvertTo-Json -Depth 8
    }

    try {
        return Invoke-RestMethod @request
    }
    catch {
        if ($AllowNotFound -and (Get-HttpStatusCode -ErrorRecord $_) -eq 404) {
            return $null
        }

        throw
    }
}

Write-Host "Obteniendo credenciales locales de servicio..."
$tokenEndpoint = Join-Endpoint $KeycloakBaseUrl "/realms/$Realm/protocol/openid-connect/token"
$tokenResponse = Invoke-RestMethod -Method Post -Uri $tokenEndpoint -ContentType 'application/x-www-form-urlencoded' -Body @{
    grant_type    = 'client_credentials'
    client_id     = $ClientId
    client_secret = $ClientSecret
}

$headers = @{ Authorization = "Bearer $($tokenResponse.access_token)" }

Write-Host "Resolviendo el usuario '$Username' en Keycloak..."
$encodedUsername = [Uri]::EscapeDataString($Username)
$usersEndpoint = Join-Endpoint $KeycloakBaseUrl "/admin/realms/$Realm/users?username=$encodedUsername&exact=true"
$users = Invoke-NervaApi -Method GET -Uri $usersEndpoint -Headers $headers
$matchingUsers = @($users | ForEach-Object { $_ } | Where-Object { $_.username -eq $Username })

if ($matchingUsers.Count -ne 1) {
    throw "Se esperaba exactamente un usuario Keycloak llamado '$Username' y se encontraron $($matchingUsers.Count)."
}

$keycloakSubject = [string] $matchingUsers[0].id
if ([string]::IsNullOrWhiteSpace($keycloakSubject)) {
    throw "Keycloak no devolvio un identificador para '$Username'."
}

Write-Host "Resolviendo el cliente bancario de desarrollo..."
$documentNumber = '99999991'
$expectedFirstName = 'Usuario'
$expectedLastName = 'Local'
$expectedBirthDate = '1990-01-01'
$expectedEmail = 'home-banking-user@local.dev'
$customerLookup = Join-Endpoint $CustomerServiceUrl "/customers/by-document?type=DNI&number=$documentNumber&country=AR"
$customer = Invoke-NervaApi -Method GET -Uri $customerLookup -Headers $headers -AllowNotFound

if ($null -eq $customer) {
    $customerBody = @{
        firstName              = $expectedFirstName
        middleName             = $null
        lastName               = $expectedLastName
        birthDate              = $expectedBirthDate
        nationality            = 'AR'
        documentType           = 'DNI'
        documentNumber         = $documentNumber
        issuingCountry         = 'AR'
        documentExpirationDate = '2035-12-31'
        contactPoints          = @(
            @{ type = 'EMAIL'; value = $expectedEmail; verified = $true },
            @{ type = 'PHONE'; value = '+541100000001'; verified = $true }
        )
        addresses              = @(
            @{
                type         = 'LEGAL'
                street       = 'Calle Local'
                streetNumber = '100'
                city         = 'Buenos Aires'
                province     = 'Ciudad Autonoma de Buenos Aires'
                postalCode   = 'C1000AAA'
                country      = 'AR'
            }
        )
    }
    $customerHeaders = $headers.Clone()
    $customerHeaders['Idempotency-Key'] = 'local-seed:home-banking-user:customer'
    $customer = Invoke-NervaApi -Method POST -Uri (Join-Endpoint $CustomerServiceUrl '/customers/natural-persons') -Headers $customerHeaders -Body $customerBody
    Write-Host "Cliente creado: $($customer.customerId)"
}
else {
    Write-Host "Cliente existente: $($customer.customerId)"
}

if (
    $customer.documentNumber -ne $documentNumber -or
    $customer.documentType -ne 'DNI' -or
    $customer.issuingCountry -ne 'AR' -or
    $customer.firstName -ne $expectedFirstName -or
    $customer.lastName -ne $expectedLastName -or
    [string] $customer.birthDate -ne $expectedBirthDate
) {
    throw "El cliente existente no coincide con la identidad local esperada."
}

$matchingEmails = @(
    $customer.contactPoints |
        ForEach-Object { $_ } |
        Where-Object { $_.type -eq 'EMAIL' -and $_.value -eq $expectedEmail }
)
if ($matchingEmails.Count -ne 1) {
    throw "El cliente existente no tiene el correo local esperado."
}

if ($customer.kycStatus -ne 'APPROVED') {
    if ($customer.kycStatus -ne 'PENDING_REVIEW') {
        throw "No se puede aprobar automaticamente un cliente con KYC '$($customer.kycStatus)'."
    }

    $customer = Invoke-NervaApi -Method PATCH -Uri (Join-Endpoint $CustomerServiceUrl "/customers/$($customer.customerId)/kyc/approve") -Headers $headers -Body @{
        reasonCode = 'LOCAL_DEVELOPMENT_SEED'
        changedBy  = 'seed-home-banking-user'
    }
    Write-Host "Cliente aprobado para el entorno local."
}

if ($customer.status -ne 'ACTIVE') {
    throw "El cliente local no quedo activo. Estado actual: '$($customer.status)'."
}

Write-Host "Resolviendo la caja de ahorro local..."
$accountsEndpoint = Join-Endpoint $AccountServiceUrl "/accounts/customer/$($customer.customerId)"
$accounts = Invoke-NervaApi -Method GET -Uri $accountsEndpoint -Headers $headers
$matchingAccounts = @(
    $accounts |
        ForEach-Object { $_ } |
        Where-Object { $_.type -eq 'SAVINGS' -and $_.currency -eq 'ARS' }
)

if ($matchingAccounts.Count -gt 1) {
    throw "El cliente local tiene mas de una caja de ahorro en pesos."
}

if ($matchingAccounts.Count -eq 0) {
    $accountHeaders = $headers.Clone()
    $accountHeaders['Idempotency-Key'] = 'local-seed:home-banking-user:account'
    $account = Invoke-NervaApi -Method POST -Uri (Join-Endpoint $AccountServiceUrl '/accounts') -Headers $accountHeaders -Body @{
        customerId = $customer.customerId
        type       = 'SAVINGS'
        currency   = 'ARS'
        alias      = 'usuario.local.nerva'
    }
    Write-Host "Cuenta creada: $($account.accountId)"
}
else {
    $account = $matchingAccounts[0]
    Write-Host "Cuenta existente: $($account.accountId)"
}

if ($account.status -eq 'PENDING_ACTIVATION') {
    $account = Invoke-NervaApi -Method PATCH -Uri (Join-Endpoint $AccountServiceUrl "/accounts/$($account.accountId)/activate") -Headers $headers -Body @{
        reason    = 'Preparacion idempotente del entorno local'
        changedBy = 'seed-home-banking-user'
    }
    Write-Host "Cuenta activada para el entorno local."
}
elseif ($account.status -ne 'ACTIVE') {
    throw "La cuenta local no puede utilizarse con estado '$($account.status)'."
}

Write-Host "Resolviendo el vinculo entre Keycloak y el cliente..."
$encodedSubject = [Uri]::EscapeDataString($keycloakSubject)
$identityLookup = Join-Endpoint $IdentityServiceUrl "/identity-links/providers/KEYCLOAK/subjects/$encodedSubject"
$linksByCustomer = Invoke-NervaApi -Method GET -Uri (Join-Endpoint $IdentityServiceUrl "/identity-links/customers/$($customer.customerId)") -Headers $headers
$keycloakLinks = @($linksByCustomer | ForEach-Object { $_ } | Where-Object { $_.provider -eq 'KEYCLOAK' })
$conflictingLinks = @($keycloakLinks | Where-Object { $_.providerSubject -ne $keycloakSubject })

if ($conflictingLinks.Count -gt 0) {
    throw "El cliente local ya esta vinculado a otro usuario Keycloak."
}

$matchingLinks = @($keycloakLinks | Where-Object { $_.providerSubject -eq $keycloakSubject })
if ($matchingLinks.Count -gt 1) {
    throw "Existe mas de un vinculo entre el cliente local y el usuario Keycloak."
}

if ($matchingLinks.Count -eq 1) {
    $identityLink = $matchingLinks[0]
    Write-Host "Vinculo de identidad existente: $($identityLink.id)"
}
else {
    $identityLink = Invoke-NervaApi -Method GET -Uri $identityLookup -Headers $headers -AllowNotFound
    if ($null -ne $identityLink -and [string] $identityLink.customerId -ne [string] $customer.customerId) {
        throw "El usuario Keycloak esta vinculado a un cliente diferente del seed local."
    }

    if ($null -eq $identityLink) {
        $identityLink = Invoke-NervaApi -Method POST -Uri (Join-Endpoint $IdentityServiceUrl '/identity-links') -Headers $headers -Body @{
            customerId      = $customer.customerId
            provider        = 'KEYCLOAK'
            providerSubject = $keycloakSubject
        }
        Write-Host "Vinculo de identidad creado: $($identityLink.id)"
    }
}

if ($identityLink.status -eq 'PENDING_VERIFICATION') {
    $identityLink = Invoke-NervaApi -Method PATCH -Uri (Join-Endpoint $IdentityServiceUrl "/identity-links/$($identityLink.id)/activate") -Headers $headers
    Write-Host "Vinculo de identidad activado."
}
elseif ($identityLink.status -ne 'ACTIVE') {
    throw "El vinculo de identidad local no puede utilizarse con estado '$($identityLink.status)'."
}

Write-Host "Entorno local listo: '$Username' -> cliente $($customer.customerId) -> cuenta $($account.accountId)."

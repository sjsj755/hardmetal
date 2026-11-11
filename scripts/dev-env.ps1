# Configure the current PowerShell session with JDK 25 and Maven 3.9.16.
# Dot-source it so the variables persist in your shell:
#   . .\scripts\dev-env.ps1
# Running it directly only affects the child process and has no lasting effect.

$jdkHome = 'D:\java\java-25'
$mavenHome = 'D:\java\develop\maven\apache-maven-3.9.16'

if (-not (Test-Path (Join-Path $jdkHome 'bin\java.exe'))) {
    throw "JDK 25 not found at $jdkHome"
}
if (-not (Test-Path (Join-Path $mavenHome 'bin\mvn.cmd'))) {
    throw "Maven not found at $mavenHome"
}

$env:JAVA_HOME = $jdkHome

$prepend = @(
    (Join-Path $jdkHome 'bin')
    (Join-Path $mavenHome 'bin')
)

$rest = @($env:Path -split ';' | Where-Object { $_ -and ($prepend -notcontains $_) })
$env:Path = ($prepend + $rest) -join ';'

Write-Host "JAVA_HOME = $env:JAVA_HOME"

& {
    # java -version and mvn -v print to stderr; keep them from becoming
    # terminating errors when the caller runs with $ErrorActionPreference = 'Stop'.
    $ErrorActionPreference = 'Continue'
    & (Join-Path $jdkHome 'bin\java.exe') -version 2>&1 |
        Select-Object -First 1 | ForEach-Object { Write-Host $_ }
    & (Join-Path $mavenHome 'bin\mvn.cmd') -v 2>&1 |
        Select-Object -First 1 | ForEach-Object { Write-Host $_ }
}
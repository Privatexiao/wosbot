$ErrorActionPreference = "Stop"

Write-Host "=== 1. Installing Java JDK 21 via winget ==="
winget install Microsoft.OpenJDK.21 --silent --accept-package-agreements --accept-source-agreements

$jdk21 = Get-ChildItem "C:\Program Files\Microsoft\", "C:\Program Files\Eclipse Adoptium\", "C:\Program Files\Java\" -Recurse -Filter "javac.exe" -ErrorAction SilentlyContinue | Where-Object { $_.FullName -match '21' } | Select-Object -First 1

if (-not $jdk21) {
    $jdk21 = Get-ChildItem "C:\Program Files\Microsoft\", "C:\Program Files\Eclipse Adoptium\", "C:\Program Files\Java\" -Recurse -Filter "javac.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
}

if ($jdk21) {
    $binDir = $jdk21.DirectoryName
    Write-Host "Found JDK 21 at: $binDir"
    $env:JAVA_HOME = Split-Path $binDir -Parent
    $env:PATH = "$binDir;$env:PATH"
} else {
    Write-Error "JDK 21 could not be located after installation."
}

java -version

Write-Host "`n=== 2. Checking / Setting up Apache Maven ==="
$toolsDir = Join-Path (Get-Location) "tools\maven"
$mvnBin = Join-Path $toolsDir "apache-maven-3.9.6\bin"

if (-not (Test-Path $mvnBin)) {
    Write-Host "Downloading Apache Maven 3.9.6..."
    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    $zipPath = Join-Path $toolsDir "maven.zip"
    Invoke-WebRequest -Uri "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip" -OutFile $zipPath
    Write-Host "Extracting Maven..."
    Expand-Archive -Path $zipPath -DestinationPath $toolsDir -Force
    Remove-Item $zipPath -Force
}

$env:PATH = "$mvnBin;$env:PATH"
mvn -version

Write-Host "`n=== 3. Building Frostguard Project ==="
mvn clean install package "-DskipTests" "-Dmaven.test.skip=true"

Write-Host "=== Build Completed Successfully! ==="

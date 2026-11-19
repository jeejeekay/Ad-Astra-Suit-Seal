# Unpacks sealing-project.zip (Ad Astra Space Suit Sealing, from Claude) into ..\AdAstraSpaceSuitSealing, builds it
# and installs the jar into the test pack. Run from the Quicreate folder:
#   powershell -ExecutionPolicy Bypass -File .\build-sealing.ps1
# The Gradle output is saved to sealing-build-log.txt here, so Claude can read it if the build fails.

param(
    [string]$Instance = "C:\Users\Owner\AppData\Roaming\PrismLauncher\instances\Quicreate Skyblock RFA  0.9.0\minecraft"
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$zip = Join-Path $PSScriptRoot 'sealing-project.zip'
$dest = Join-Path (Split-Path $PSScriptRoot -Parent) 'AdAstraSpaceSuitSealing'
$log = Join-Path $PSScriptRoot 'sealing-build-log.txt'

if (Test-Path $zip) {
    Write-Host "1/5 Unpacking into $dest ..." -ForegroundColor Cyan
    New-Item -ItemType Directory -Force -Path $dest | Out-Null
    # Files the update removes are listed in _delete.txt inside the zip (paths from the project folder).
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zf = [System.IO.Compression.ZipFile]::OpenRead($zip)
    $del = $zf.Entries | Where-Object { $_.FullName -eq '_delete.txt' } | Select-Object -First 1
    if ($del) {
        $reader = New-Object System.IO.StreamReader($del.Open())
        $lines = $reader.ReadToEnd() -split "`r?`n" | Where-Object { $_.Trim() -ne '' }
        $reader.Close()
        foreach ($l in $lines) {
            $p = Join-Path $dest ($l.Trim() -replace '/', '\')
            if (Test-Path $p) { Remove-Item -Recurse -Force $p }
        }
    }
    $zf.Dispose()
    Expand-Archive -Path $zip -DestinationPath $dest -Force
    $d = Join-Path $dest '_delete.txt'
    if (Test-Path $d) { Remove-Item -Force $d }
} elseif (-not (Test-Path (Join-Path $dest 'build.gradle'))) {
    throw "sealing-project.zip not found and $dest has no project"
}

Write-Host "2/5 Copying Ad Astra, Botarium and Resourceful Lib into libs (compile only)..." -ForegroundColor Cyan
$libs = Join-Path $dest 'libs'
New-Item -ItemType Directory -Force -Path $libs | Out-Null
foreach ($pattern in 'ad_astra-forge-1.20.1-*.jar', 'botarium-forge-1.20.1-*.jar', 'resourcefullib-forge-1.20.1-*.jar') {
    $src = Get-ChildItem (Join-Path $PSScriptRoot 'libs') -Filter $pattern -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $src) { throw "No $pattern in Quicreate\libs" }
    Copy-Item $src.FullName $libs -Force
}

if (-not ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe") -and ((Split-Path $env:JAVA_HOME -Leaf) -match '17'))) {
    $jdk = Get-ChildItem "C:\Program Files\Eclipse Adoptium", "C:\Program Files\Java", "C:\Program Files\Microsoft", "C:\Program Files\Zulu" -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '17' -and (Test-Path "$($_.FullName)\bin\javac.exe") } |
        Select-Object -First 1
    if (-not $jdk) { throw "No Java 17 JDK found. Install one with:  winget install EclipseAdoptium.Temurin.17.JDK" }
    $env:JAVA_HOME = $jdk.FullName
}
Write-Host "Java: $env:JAVA_HOME"

Write-Host "3/5 Building (the first build downloads Minecraft and takes a few minutes)..." -ForegroundColor Cyan
Push-Location $dest
cmd /c "gradlew.bat build --console=plain > `"$log`" 2>&1"
$code = $LASTEXITCODE
Pop-Location
if ($code -ne 0) {
    Write-Host ""
    Write-Host "BUILD FAILED. The errors:" -ForegroundColor Red
    Select-String -Path $log -Pattern 'error:|FAILURE|What went wrong' -Context 0,3 | Select-Object -First 15 | ForEach-Object { $_.ToString() }
    Write-Host "The full output is in sealing-build-log.txt. Nothing was installed." -ForegroundColor Red
    exit 1
}

Write-Host "4/5 Finding the jar..." -ForegroundColor Cyan
$jar = Get-ChildItem (Join-Path $dest "build\libs\adastra_sealing-*.jar") |
    Where-Object { $_.Name -notmatch 'sources|slim|dev' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { throw "No jar was built. Send sealing-build-log.txt to Claude." }

Write-Host "5/5 Installing..." -ForegroundColor Cyan
$mods = Join-Path $Instance "mods"
Get-ChildItem $mods -Filter "adastra_sealing-*.jar" | Remove-Item
Copy-Item $jar.FullName $mods
Write-Host "Installed $($jar.Name) into the pack." -ForegroundColor Green
if (Test-Path $zip) { Remove-Item -Force $zip }

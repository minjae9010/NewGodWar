[CmdletBinding()]
param(
    [string[]] $Versions = @(
        "1.12.2",
        "1.16.5",
        "1.17.1",
        "1.18.2",
        "1.19.4",
        "1.20.6",
        "1.21.11",
        "26.1.2"
    ),
    [string] $PluginJar = "",
    [string] $WorkDir = ".paper-smoke",
    [string] $JavaExecutable = "java",
    [int] $TimeoutSeconds = 150,
    [switch] $SkipBuild,
    [switch] $AllSupportedVersions,
    [switch] $LatestVersion,
    [switch] $LatestStableVersion,
    [switch] $KeepWorkDirs
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$WorkRoot = Join-Path $RootDir $WorkDir
$FillApiBase = "https://fill.papermc.io/v3/projects/paper"
$LegacyApiBase = "https://api.papermc.io/v2/projects/paper"
$PaperUserAgent = "NewGodWar compatibility smoke test (https://github.com/minjae9010/NewGodWar)"
$IsWindowsPlatform = [System.IO.Path]::DirectorySeparatorChar -eq "\"

function Write-Step {
    param([string] $Message)
    Write-Host ""
    Write-Host "==> $Message"
}

function Get-SupportedPaperVersions {
    return @(
        "1.12",
        "1.12.1",
        "1.12.2",
        "1.13",
        "1.13.1",
        "1.13.2",
        "1.14",
        "1.14.1",
        "1.14.2",
        "1.14.3",
        "1.14.4",
        "1.15",
        "1.15.1",
        "1.15.2",
        "1.16.1",
        "1.16.2",
        "1.16.3",
        "1.16.4",
        "1.16.5",
        "1.17",
        "1.17.1",
        "1.18",
        "1.18.1",
        "1.18.2",
        "1.19",
        "1.19.1",
        "1.19.2",
        "1.19.3",
        "1.19.4",
        "1.20",
        "1.20.1",
        "1.20.2",
        "1.20.4",
        "1.20.5",
        "1.20.6",
        "1.21",
        "1.21.1",
        "1.21.3",
        "1.21.4",
        "1.21.5",
        "1.21.6",
        "1.21.7",
        "1.21.8",
        "1.21.9",
        "1.21.10",
        "1.21.11",
        "26.1.1",
        "26.1.2"
    )
}

function Get-LatestSupportedPaperVersion {
    return (Get-SupportedPaperVersions | Sort-Object { [version] $_ } | Select-Object -Last 1)
}

function Invoke-GradleBuild {
    $gradle = if ($IsWindowsPlatform) {
        Join-Path $RootDir "gradlew.bat"
    } else {
        Join-Path $RootDir "gradlew"
    }

    Write-Step "Building NewGodWar"
    & $gradle "--no-daemon" "clean" "build"
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE."
    }
}

function Resolve-PluginJar {
    if (-not [string]::IsNullOrWhiteSpace($PluginJar)) {
        return (Resolve-Path $PluginJar).Path
    }

    $candidates = @(
        Join-Path (Join-Path $RootDir "build") "libs"
        Join-Path (Join-Path $RootDir "plugin") (Join-Path "build" "libs")
    )

    foreach ($directory in $candidates) {
        if (Test-Path $directory) {
            $jar = Get-ChildItem -Path $directory -Filter "NewGodWar-*.jar" |
                Sort-Object LastWriteTime -Descending |
                Select-Object -First 1
            if ($null -ne $jar) {
                return $jar.FullName
            }
        }
    }

    throw "Could not find NewGodWar jar. Run without -SkipBuild or pass -PluginJar."
}

function Invoke-PaperApi {
    param([string] $Uri)

    Invoke-RestMethod -Uri $Uri -Headers @{
        "User-Agent" = $PaperUserAgent
    }
}

function Invoke-PaperDownload {
    param(
        [string] $Uri,
        [string] $OutFile
    )

    Invoke-WebRequest -Uri $Uri -OutFile $OutFile -Headers @{
        "User-Agent" = $PaperUserAgent
    }
}

function Get-FreeTcpPort {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
    try {
        $listener.Start()
        return $listener.LocalEndpoint.Port
    } finally {
        $listener.Stop()
    }
}

function Get-LogText {
    param([string[]] $Paths)

    $parts = foreach ($path in $Paths) {
        if (Test-Path $path) {
            Get-Content -LiteralPath $path -Raw -ErrorAction SilentlyContinue
        }
    }

    return ($parts -join "`n")
}

function Get-PaperJar {
    param(
        [string] $MinecraftVersion,
        [string] $VersionDir
    )

    $fillBuildsUri = "$FillApiBase/versions/$MinecraftVersion/builds"
    $fillBuilds = Invoke-PaperApi -Uri $fillBuildsUri
    if ($null -ne $fillBuilds -and $fillBuilds.Count -gt 0) {
        $latestBuild = $fillBuilds |
            Where-Object { $_.channel -eq "STABLE" -and $null -ne $_.downloads."server:default" } |
            Sort-Object id |
            Select-Object -Last 1
        if ($null -eq $latestBuild) {
            $latestBuild = $fillBuilds |
                Where-Object { $null -ne $_.downloads."server:default" } |
                Sort-Object id |
                Select-Object -Last 1
        }
        if ($null -ne $latestBuild) {
            $download = $latestBuild.downloads."server:default"
            $jarPath = Join-Path $VersionDir $download.name
            if (-not (Test-Path $jarPath)) {
                Write-Host "Downloading Paper $MinecraftVersion build $($latestBuild.id) from Fill API"
                Invoke-PaperDownload -Uri $download.url -OutFile $jarPath
            }

            return $jarPath
        }
    }

    $buildsUri = "$LegacyApiBase/versions/$MinecraftVersion/builds"
    $builds = Invoke-PaperApi -Uri $buildsUri
    if ($null -eq $builds.builds -or $builds.builds.Count -eq 0) {
        throw "No Paper builds found for Minecraft $MinecraftVersion."
    }
    $latestBuild = $builds.builds | Sort-Object build | Select-Object -Last 1
    $buildNumber = $latestBuild.build
    $downloadName = $null
    if ($null -ne $latestBuild.downloads -and $null -ne $latestBuild.downloads.application) {
        $downloadName = $latestBuild.downloads.application.name
    }
    if ([string]::IsNullOrWhiteSpace($downloadName)) {
        $downloadName = "paper-$MinecraftVersion-$buildNumber.jar"
    }

    $jarPath = Join-Path $VersionDir $downloadName
    if (-not (Test-Path $jarPath)) {
        $downloadUri = "$LegacyApiBase/versions/$MinecraftVersion/builds/$buildNumber/downloads/$downloadName"
        Write-Host "Downloading Paper $MinecraftVersion build $buildNumber"
        Invoke-PaperDownload -Uri $downloadUri -OutFile $jarPath
    }

    return $jarPath
}

function Stop-ServerProcess {
    param([System.Diagnostics.Process] $Process)

    if ($null -eq $Process -or $Process.HasExited) {
        return
    }

    try {
        $Process.Kill($true)
    } catch {
        $Process.Kill()
    }

    $Process.WaitForExit(15000) | Out-Null
}

function Test-PaperVersion {
    param(
        [string] $MinecraftVersion,
        [string] $JarToTest
    )

    $safeVersion = $MinecraftVersion -replace "[^0-9A-Za-z_.-]", "_"
    $versionDir = Join-Path $WorkRoot $safeVersion
    if ((Test-Path $versionDir) -and -not $KeepWorkDirs) {
        Remove-Item -LiteralPath $versionDir -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $versionDir | Out-Null

    $pluginsDir = Join-Path $versionDir "plugins"
    New-Item -ItemType Directory -Force -Path $pluginsDir | Out-Null
    Copy-Item -LiteralPath $JarToTest -Destination (Join-Path $pluginsDir "NewGodWar.jar") -Force

    Set-Content -LiteralPath (Join-Path $versionDir "eula.txt") -Encoding UTF8 -Value @(
        "# Local NewGodWar compatibility smoke test"
        "# https://aka.ms/MinecraftEULA"
        "eula=true"
    )
    $serverPort = Get-FreeTcpPort
    Set-Content -LiteralPath (Join-Path $versionDir "server.properties") -Encoding UTF8 -Value @(
        "online-mode=false"
        "server-ip=127.0.0.1"
        "server-port=$serverPort"
        "spawn-protection=0"
        "view-distance=3"
        "simulation-distance=3"
        "motd=NewGodWar compatibility smoke test"
    )

    $paperJar = Get-PaperJar -MinecraftVersion $MinecraftVersion -VersionDir $versionDir
    $logPath = Join-Path $versionDir "server.log"
    $latestLogPath = Join-Path (Join-Path $versionDir "logs") "latest.log"
    if (Test-Path $logPath) {
        Remove-Item -LiteralPath $logPath -Force
    }

    Write-Step "Starting Paper $MinecraftVersion"

    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = $JavaExecutable
    $psi.Arguments = "-Xmx1G -jar `"$paperJar`" nogui"
    $psi.WorkingDirectory = $versionDir
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $false
    $psi.RedirectStandardOutput = $false
    $psi.RedirectStandardError = $false
    $psi.CreateNoWindow = $true

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $psi

    try {
        if (-not $process.Start()) {
            throw "Failed to start Java process."
        }

        $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
        $enabled = $false
        $ready = $false
        $failure = $null

        while ((Get-Date) -lt $deadline) {
            Start-Sleep -Milliseconds 500

            $logText = Get-LogText -Paths @($logPath, $latestLogPath)

            if ($logText -match "NewGodWar enabled") {
                $enabled = $true
            }
            if ($logText -match "Done \(") {
                $ready = $true
            }
            if ($logText -match "Error occurred while enabling NewGodWar|Could not load 'plugins[\\/]+NewGodWar\.jar'|UnsupportedClassVersionError|NoClassDefFoundError|ClassNotFoundException|ExceptionInInitializerError") {
                $failure = "Plugin load error detected."
                break
            }
            if ($process.HasExited) {
                $failure = "Server process exited before the plugin finished loading."
                break
            }
            if ($enabled -and $ready) {
                break
            }
        }

        if (-not $enabled -or -not $ready) {
            if ($null -eq $failure) {
                $failure = "Timed out waiting for Paper $MinecraftVersion to finish startup."
            }
            throw "$failure Logs: $logPath, $latestLogPath"
        }

        Write-Host "PASS Paper $MinecraftVersion loaded NewGodWar successfully."
    } finally {
        Stop-ServerProcess -Process $process
    }
}

if (-not $SkipBuild) {
    Invoke-GradleBuild
}

if ($AllSupportedVersions) {
    $Versions = Get-SupportedPaperVersions
}
if ($LatestVersion -or $LatestStableVersion) {
    $Versions = @(Get-LatestSupportedPaperVersion)
}

$resolvedJar = Resolve-PluginJar
Write-Host "Testing plugin jar: $resolvedJar"
New-Item -ItemType Directory -Force -Path $WorkRoot | Out-Null

$failures = @()
foreach ($version in $Versions) {
    try {
        Test-PaperVersion -MinecraftVersion $version -JarToTest $resolvedJar
    } catch {
        $failures += [PSCustomObject]@{
            Version = $version
            Error = $_.Exception.Message
        }
        Write-Error "FAIL Paper $version - $($_.Exception.Message)" -ErrorAction Continue
    }
}

if ($failures.Count -gt 0) {
    Write-Host ""
    Write-Host "Compatibility failures:"
    $failures | Format-Table -AutoSize | Out-String | Write-Host
    exit 1
}

Write-Host ""
Write-Host "All requested Paper versions passed."

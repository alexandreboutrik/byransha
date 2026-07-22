#!/opt/homebrew/bin/pwsh

$ErrorActionPreference = "Stop"

$BDir = Join-Path $HOME ".byransha"
$BinDir = Join-Path $BDir "bin"

function Get-RemoteFile {
    param (
        [string]$FileName,
        [string]$DestinationDir
    )

    $BaseUrl = "https://webusers.i3s.unice.fr/~hogie/software/byransha/downloads/bin"
    $Url      = "$BaseUrl/$FileName"
    $FilePath = Join-Path $DestinationDir $FileName

    Write-Host "Downloading $Url..."
    Invoke-WebRequest -Uri $Url -OutFile $FilePath
}

# 1. Helper function to handle downloads and setup
function Download-Artifacts {
    if (Test-Path $BinDir) {
        Remove-Item -Path $BinDir -Recurse -Force
    }
   
    New-Item -ItemType Directory -Force -Path $BinDir | Out-Null

    Write-Host "Initializing/Updating application components..."

    # Udpdate this script
	Get-RemoteFile -FileName "byransha.jar"  -DestinationDir $BinDir
	Get-RemoteFile -FileName "run.ps1"       -DestinationDir $BinDir

    # Determine OS platform target
    if ($IsWindows -or $env:OS -like "*Windows*") { 
        $OS = "windows"
        
        # Gestion du bug OneDrive
        $DesktopPath = [Environment]::GetFolderPath("Desktop")
        if ([string]::IsNullOrEmpty($DesktopPath) -or -not (Test-Path $DesktopPath)) {
            $DesktopPath = Join-Path $HOME "Desktop"
        }
 
         # Define Windows Shortcut attributes
        $ScriptPath   = $PSCommandPath
        $ShortcutPath = Join-Path $DesktopPath "Launch App.lnk"
        $IconPath     = Join-Path $BinDir "byransha.ico"
        Get-RemoteFile -FileName "byransha.ico" -DestinationDir $BinDir
        try {
            Write-Host "Création du raccourci sur le Bureau ($ShortcutPath)..." -ForegroundColor Cyan
            $WshShell = New-Object -ComObject WScript.Shell
             # Configure target to launch PowerShell invisibly running your file
            $Shortcut = $WshShell.CreateShortcut($ShortcutPath)
            $Shortcut.TargetPath = "powershell.exe"
            if (-not [string]::IsNullOrEmpty($ScriptPath)) {
                $Shortcut.Arguments = "-ExecutionPolicy Bypass -WindowStyle Hidden -File `"$ScriptPath`""
            } else {
                $Shortcut.Arguments = "-ExecutionPolicy Bypass -WindowStyle Hidden -File `"$PSScriptRoot\run.ps1`""
            }
            $Shortcut.IconLocation = "$IconPath,0"
            $Shortcut.Description = "Launch Byransha"
              # Save shortcut to desktop
            $Shortcut.Save()
            Write-Host "Raccourci crée" -ForegroundColor Green
        }
        catch {
            Write-Warning "Impossible de créer le raccourci"
        }
    }
    elseif ($IsMacOS) { 
        $OS = "mac-aarch64" 
		Get-RemoteFile -FileName "byransha.png"  -DestinationDir $BinDir
    }
    else { 
        $OS = "linux-x64" 
    }

    # Download JRE archive/zip (Target: JDK 26 Architecture)
    $JreUrl = "https://webusers.i3s.unice.fr/~hogie/software/byransha/downloads/bin/jvm/$OS.zip"
    Write-Host "Downloading $JreUrl..."
    $JrePath = Join-Path $BinDir "jre-$OS.zip"
    Invoke-WebRequest -Uri $JreUrl -OutFile $JrePath
    
    # Extract runtime environment components
    Expand-Archive -Path $JrePath -DestinationPath $BinDir
}

# 2. Bootstrapping evaluation
if (!(Test-Path $BinDir)) {
    Download-Artifacts
}

# 3. Determine execution binary path format based on platform profile
$JavaExe = Join-Path $BinDir "jvm/bin/java.exe"
if (-not ($IsWindows -or $env:OS -like "*Windows*")) {
    $JavaExe = Join-Path $BinDir "jvm/bin/java"
}
$JarPath = Join-Path $BinDir "byransha.jar"

# 4. Main Execution Lifecycle Loop
while ($true) {
    Write-Host "Starting Java Application..."
    & $JavaExe -cp $JarPath byransha.Main

    # If the application requests an asset refresh via explicit exit code 46
    if ($LASTEXITCODE -eq 46) {
        Download-Artifacts
    }
    else {
        break
    }
}
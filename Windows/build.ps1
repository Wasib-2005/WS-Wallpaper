param(
    [switch]$PortableOnly
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$outputRoot = Join-Path $repoRoot "dist"
$bundleDir = Join-Path $outputRoot "WSWallpaper"
$workRoot = Join-Path $PSScriptRoot "build-output"
$iconPath = Join-Path $bundleDir "ws-wallpaper.ico"

if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
    throw "Python 3 is required to build the Windows app."
}

python -m pip install --disable-pip-version-check --upgrade pyinstaller pillow
if ($LASTEXITCODE -ne 0) {
    throw "Could not install the PyInstaller build dependencies."
}

New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null
if (Test-Path $bundleDir) {
    Remove-Item -Recurse -Force $bundleDir
}
New-Item -ItemType Directory -Force -Path $bundleDir | Out-Null

$sourceIcon = Join-Path $repoRoot "Linux\ws-wallpaper.png"
python (Join-Path $PSScriptRoot "make_icon.py") $sourceIcon $iconPath
if ($LASTEXITCODE -ne 0) {
    throw "Could not create the Windows application icon."
}

function Invoke-AppBuild {
    param(
        [string]$Name,
        [string]$EntryPoint,
        [string[]]$ExtraArguments
    )

    $arguments = @(
        "-m", "PyInstaller", "--clean", "--noconfirm", "--onefile",
        "--name", $Name, "--distpath", $bundleDir,
        "--workpath", (Join-Path $workRoot $Name),
        "--specpath", (Join-Path $workRoot "specs")
    ) + $ExtraArguments + @($EntryPoint)

    & python @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "PyInstaller failed while building $Name."
    }
}

New-Item -ItemType Directory -Force -Path (Join-Path $workRoot "specs") | Out-Null
$guiScript = Join-Path $PSScriptRoot "gui_entry.py"
$cliScript = Join-Path $PSScriptRoot "cli_entry.py"
$startupScript = Join-Path $PSScriptRoot "startup.py"
$guiData = (Join-Path $repoRoot "Linux\wallpaper-manager") + ";."
$cliData = (Join-Path $repoRoot "Linux\wallpaper") + ";."
$iconData = $sourceIcon + ";."
$guiHiddenImports = @(
    "--hidden-import=tkinter",
    "--hidden-import=tkinter.filedialog",
    "--hidden-import=tkinter.messagebox",
    "--hidden-import=tkinter.ttk",
    "--hidden-import=PIL",
    "--hidden-import=PIL.Image",
    "--hidden-import=PIL.ImageTk"
)
$cliHiddenImports = @(
    "--hidden-import=argparse",
    "--hidden-import=collections",
    "--hidden-import=csv",
    "--hidden-import=ctypes",
    "--hidden-import=json",
    "--hidden-import=os",
    "--hidden-import=platform",
    "--hidden-import=random",
    "--hidden-import=signal",
    "--hidden-import=subprocess",
    "--hidden-import=sys",
    "--hidden-import=time",
    "--hidden-import=urllib.error",
    "--hidden-import=urllib.parse",
    "--hidden-import=urllib.request"
)

Invoke-AppBuild "ws-wallpaper" $guiScript (@(
    "--windowed", "--icon", $iconPath, "--add-data", $guiData, "--add-data", $iconData
) + $guiHiddenImports)
Invoke-AppBuild "wallpaper" $cliScript (@(
    "--console", "--add-data", $cliData
) + $cliHiddenImports)
Invoke-AppBuild "wallpaper-startup" $startupScript @(
    "--windowed"
)

$portableArchive = Join-Path $outputRoot "WS-Wallpaper-Windows-portable.zip"
if (Test-Path $portableArchive) {
    Remove-Item -Force $portableArchive
}
Compress-Archive -Path (Join-Path $bundleDir "*") -DestinationPath $portableArchive

if (-not $PortableOnly) {
    $compiler = Get-Command ISCC.exe -ErrorAction SilentlyContinue
    if ($compiler) {
        $compilerPath = $compiler.Source
    } else {
        $compilerPath = Join-Path ${env:ProgramFiles(x86)} "Inno Setup 6\ISCC.exe"
        if (-not (Test-Path $compilerPath)) {
            throw "Inno Setup 6 is required to build the installer. Install it or pass -PortableOnly."
        }
    }

    Push-Location $repoRoot
    try {
        & $compilerPath "Windows\installer.iss"
        if ($LASTEXITCODE -ne 0) {
            throw "Inno Setup failed to build the installer."
        }
    } finally {
        Pop-Location
    }
}

Write-Host "Windows app files: $bundleDir"
Write-Host "Portable archive: $portableArchive"
if (-not $PortableOnly) {
    Write-Host "Installer: $(Join-Path $outputRoot 'WS-Wallpaper-Setup.exe')"
}

#ifndef SourceDir
  #define SourceDir "..\dist\WSWallpaper"
#endif

[Setup]
AppId={{D64A7B9E-02CF-4AD9-9A52-2CDCB9046D5B}
AppName=WS Wallpaper
AppVersion=1.0.0.0
AppPublisher=WS Wallpaper
DefaultDirName={localappdata}\Programs\WS Wallpaper
DefaultGroupName=WS Wallpaper
UninstallDisplayIcon={app}\ws-wallpaper.exe
SetupIconFile={#SourceDir}\ws-wallpaper.ico
OutputDir=..\dist
OutputBaseFilename=WS-Wallpaper-Setup
PrivilegesRequired=lowest
ArchitecturesInstallIn64BitMode=x64compatible
DisableProgramGroupPage=yes
UsePreviousAppDir=yes
WizardStyle=modern

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional shortcuts:"; Flags: unchecked
Name: "startup"; Description: "Start wallpaper rotation when I sign in"; GroupDescription: "Background service:"

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\WS Wallpaper"; Filename: "{app}\ws-wallpaper.exe"
Name: "{group}\Uninstall WS Wallpaper"; Filename: "{uninstallexe}"
Name: "{autodesktop}\WS Wallpaper"; Filename: "{app}\ws-wallpaper.exe"; Tasks: desktopicon

[Registry]
Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueType: string; ValueName: "WSWallpaper"; ValueData: """{app}\wallpaper-startup.exe"""; Tasks: startup; Flags: uninsdeletevalue
Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueType: none; ValueName: "WSWallpaper"; Tasks: not startup

[Run]
Filename: "{app}\ws-wallpaper.exe"; Description: "Launch WS Wallpaper"; Flags: postinstall skipifsilent

[UninstallRun]
Filename: "{app}\wallpaper.exe"; Parameters: "--stop"; Flags: runhidden waituntilterminated skipifdoesntexist

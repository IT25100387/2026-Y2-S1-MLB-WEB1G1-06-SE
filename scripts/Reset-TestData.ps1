$ErrorActionPreference = 'Stop'
$workspacePath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$listeners = @(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)
if ($listeners.Count -gt 0) { throw 'Stop the backend before resetting test data.' }
$backupPath = Join-Path $workspacePath ('data/database-backups/' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $backupPath | Out-Null
foreach ($relativeDirectory in @('data','backend/data')) {
    $databaseDirectory = [IO.Path]::GetFullPath((Join-Path $workspacePath $relativeDirectory))
    if (-not $databaseDirectory.StartsWith($workspacePath + [IO.Path]::DirectorySeparatorChar)) { throw 'Invalid database path' }
    if (-not (Test-Path -LiteralPath $databaseDirectory)) { continue }
    foreach ($file in Get-ChildItem -LiteralPath $databaseDirectory -File) {
        if ($file.Name -notin @('fuelstationdb.mv.db','fuelstationdb.trace.db','fuelstationdb.lock.db')) { continue }
        $targetPath = [IO.Path]::GetFullPath($file.FullName)
        if ([IO.Path]::GetDirectoryName($targetPath) -ne $databaseDirectory) { throw 'Invalid database file path' }
        $backupName = $relativeDirectory.Replace('/','-') + '-' + $file.Name
        Copy-Item -LiteralPath $targetPath -Destination (Join-Path $backupPath $backupName)
        Remove-Item -LiteralPath $targetPath
    }
}
Write-Output ('Previous databases backed up to ' + $backupPath)
Write-Output 'Test data removed. Start the backend with DEMO_SEED=true to create valid sample flows.'

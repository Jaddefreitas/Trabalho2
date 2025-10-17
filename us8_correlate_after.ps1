$seg = Get-Content us8_segment_after_clearfix.log
$errors = @()
for ($i=0; $i -lt $seg.Length; $i++) {
    if ($seg[$i] -match '^Line\s+(\d+),\s+file tests/us8.txt:\s*(.*)') {
        $lineno = $matches[1]
        $errText = $matches[2].Trim()
        $cmdLine = ''
        for ($j=$i+1; $j -lt [Math]::Min($seg.Length, $i+10); $j++) {
            if ($seg[$j] -match '^\s*$') { continue }
            if ($seg[$j] -match '^Command producing error:\s*(.*)') { $cmdLine = $matches[1].Trim(); break }
            if ($seg[$j] -match '<.*>') { $cmdLine = $seg[$j].Trim(); break }
        }
        $errors += [PSCustomObject]@{ idx=$i; testLine=[int]$lineno; err=$errText; cmd=$cmdLine }
    }
}

function Find-PriorEvents($startIdx, $seg, $maxEvents) {
    $events = @()
    for ($k=$startIdx-1; $k -ge 0 -and $events.Count -lt $maxEvents; $k--) {
        $line = $seg[$k]
        if ($line -match '^TRACE_CMD_SEQ') { $events += [PSCustomObject]@{ idx=$k; text=$line; type='SEQ' } ; continue }
        if ($line -match '^TRACE_ADD_EMP') { $events += [PSCustomObject]@{ idx=$k; text=$line; type='ADD' } ; continue }
        if ($line -match '^TRACE_REMOVE_EMP') { $events += [PSCustomObject]@{ idx=$k; text=$line; type='REMOVE' } ; continue }
        if ($line -match '^TRACE_CLEAR_DATABASE') { $events += [PSCustomObject]@{ idx=$k; text=$line; type='CLEAR' } ; continue }
        if ($line -match '^TRACE_EXEC_CMD') { $events += [PSCustomObject]@{ idx=$k; text=$line; type='EXEC' } ; continue }
    }
    return $events
}

$out = @()
foreach ($e in $errors) {
    $cmd = $e.cmd
    $empIds = @()
    if ($cmd -match 'EMP-\d+') { $empIds = ([regex]::Matches($cmd, 'EMP-\d+') | ForEach-Object { $_.Value }) }
    $prior = Find-PriorEvents $e.idx $seg 10
    $out += [PSCustomObject]@{
        testLine = $e.testLine;
        error = $e.err;
        command = $e.cmd;
        empIds = ($empIds -join ', ');
        priorEvents = ($prior | ForEach-Object { "[$($_.idx)] $($_.type): $($_.text)" } ) -join " | ";
    }
}

$out | Format-Table -AutoSize | Out-String | Out-File us8_correlation_report_after_clearfix.txt -Encoding utf8
Write-Output ('WROTE ' + (Get-Item us8_correlation_report_after_clearfix.txt).FullName)

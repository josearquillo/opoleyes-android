$json = Get-Content 'app\src\main\assets\data.json' -Raw
$data = $json | ConvertFrom-Json
$counts = @{}
foreach($td in $data) {
    foreach($q in $td.questions) {
        $n = ($q.opciones.PSObject.Properties | Measure-Object).Count
        if(-not $counts.ContainsKey($n)) { $counts[$n] = 0 }
        $counts[$n]++
    }
}
$counts.GetEnumerator() | Sort-Object Name | ForEach-Object { Write-Host ('{0} options: {1} questions' -f $_.Key, $_.Value) }

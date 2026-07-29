$json = Get-Content 'app\src\main\assets\data.json' -Raw
$data = $json | ConvertFrom-Json
$mismatches = 0
foreach($td in $data) {
    $am = @{}
    foreach($a in $td.answers) { $am[$a.id] = $a.correct }
    foreach($q in $td.questions) {
        $correct = $am[$q.id]
        if($correct -and $q.opciones.PSObject.Properties.Name -notcontains $correct) {
            Write-Host "MISMATCH: test=$($td.test.id) qid=$($q.id) correct=$correct options=$($q.opciones.PSObject.Properties.Name -join ',')"
            $mismatches++
        }
    }
}
Write-Host "Total mismatches: $mismatches"

param([int]$Repeats = 5, [switch]$Profile)
$ErrorActionPreference = 'Stop'
$projectDir = Split-Path $PSScriptRoot -Parent
$resultsDir = Join-Path $PSScriptRoot 'results'
$buildDir = Join-Path $PSScriptRoot 'build'
New-Item -ItemType Directory -Force $resultsDir,$buildDir | Out-Null
$sources = @(Get-ChildItem (Join-Path $projectDir 'src/main/java/caroai/*.java') | ForEach-Object FullName)
$harness = Join-Path $PSScriptRoot 'Benchmark.java'
$baselineSources = @($sources | Where-Object { (Split-Path $_ -Leaf) -notin @('AI.java','Difficulty.java','SearchState.java','TranspositionTable.java') })
$baselineSources += @(Get-ChildItem (Join-Path $PSScriptRoot 'baseline/*.java') | ForEach-Object FullName)
$baselineClasses = Join-Path $buildDir 'baseline'
$finalClasses = Join-Path $buildDir 'final'
New-Item -ItemType Directory -Force $baselineClasses,$finalClasses | Out-Null
& javac -encoding UTF-8 -d $baselineClasses @baselineSources $harness
if ($LASTEXITCODE -ne 0) { throw 'Baseline compilation failed' }
& javac -encoding UTF-8 -d $finalClasses @sources $harness
if ($LASTEXITCODE -ne 0) { throw 'Final compilation failed' }
# Run sequentially: concurrent JVM benchmarks compete for the same CPU.
& java -Xms256m -Xmx512m -cp $baselineClasses caroai.Benchmark $Repeats | Set-Content -Encoding utf8 (Join-Path $resultsDir 'baseline.csv')
if ($LASTEXITCODE -ne 0) { throw 'Baseline benchmark failed' }
& java -Xms256m -Xmx512m -cp $finalClasses caroai.Benchmark $Repeats 6 | Set-Content -Encoding utf8 (Join-Path $resultsDir 'depth6.csv')
if ($LASTEXITCODE -ne 0) { throw 'Depth-six benchmark failed' }
& java -Xms256m -Xmx512m -cp $finalClasses caroai.Benchmark $Repeats | Set-Content -Encoding utf8 (Join-Path $resultsDir 'final.csv')
if ($LASTEXITCODE -ne 0) { throw 'Final benchmark failed' }
& javac -encoding UTF-8 -d $buildDir (Join-Path $PSScriptRoot 'PairedBenchmark.java')
if ($LASTEXITCODE -ne 0) { throw 'Paired harness compilation failed' }
& java -Xms256m -Xmx512m -cp $buildDir PairedBenchmark $baselineClasses $finalClasses $Repeats | Set-Content -Encoding utf8 (Join-Path $resultsDir 'paired.csv')
if ($LASTEXITCODE -ne 0) { throw 'Paired benchmark failed' }
& javac -encoding UTF-8 -cp $finalClasses -d $finalClasses (Join-Path $PSScriptRoot 'LatencyProbe.java')
if ($LASTEXITCODE -ne 0) { throw 'Latency probe compilation failed' }
& java -Xms256m -Xmx512m -cp $finalClasses caroai.LatencyProbe | Set-Content -Encoding utf8 (Join-Path $resultsDir 'latency.csv')
if ($LASTEXITCODE -ne 0) { throw 'Latency check failed' }
if ($Profile) {
    foreach ($variant in @('baseline','final')) {
        $classes = if ($variant -eq 'baseline') { $baselineClasses } else { $finalClasses }
        $recording = Join-Path $resultsDir "$variant.jfr"
        & java -Xms256m -Xmx512m "-XX:StartFlightRecording=filename=$recording,settings=profile,dumponexit=true" -cp $classes caroai.Benchmark $Repeats 6 HARD | Set-Content -Encoding utf8 (Join-Path $resultsDir "$variant-profile.log")
        if ($LASTEXITCODE -ne 0) { throw "Profile failed: $variant" }
        & jfr summary $recording | Set-Content -Encoding utf8 (Join-Path $resultsDir "$variant-jfr-summary.txt")
        & jfr view hot-methods $recording | Set-Content -Encoding utf8 (Join-Path $resultsDir "$variant-hot-methods.txt")
    }
}
Write-Output "Saved benchmark data to $resultsDir"

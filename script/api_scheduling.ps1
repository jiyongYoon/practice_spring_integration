# 스크립트가 위치한 경로 계산
$configPath = Join-Path $PSScriptRoot "env.conf"

# 1. env.conf 파일이 있으면 읽어서 환경변수에 설정
if (Test-Path $configPath) {
    Write-Host "📂 설정 파일($configPath)을 로드합니다..." -ForegroundColor Gray
    Get-Content $configPath | ForEach-Object {
        $line = $_.Trim()
        # 빈 줄이나 주석(#)이 아닌 경우만 처리
        if ($line -and -not $line.StartsWith("#")) {
            $key, $value = $line.Split('=', 2)
            if ($key -and $value) {
                [System.Environment]::SetEnvironmentVariable($key.Trim(), $value.Trim())
            }
        }
    }
}

# 2. 환경변수 로드
$apiUrl = $env:API_URL
$interval = $env:INTERVAL_SECONDS

# 필수 값 체크
if (-not $apiUrl -or -not $interval) {
    Write-Error "설정 오류: API_URL 또는 INTERVAL_SECONDS를 찾을 수 없습니다. env.conf 파일이나 시스템 환경변수를 확인하세요."
    exit
}

Write-Host "------------------------------------------" -ForegroundColor Cyan
Write-Host "🚀 스케줄러 시작 (중단: Ctrl + C)"
Write-Host "📍 URL: $apiUrl"
Write-Host "⏱️ 간격: $interval 초"
Write-Host "------------------------------------------"

# 3. 루프 실행
try {
    while ($true) {
        $now = Get-Date -Format "HH:mm:ss"
        try {
            # 에러 방지를 위해 -ErrorAction Stop 추가
            $response = Invoke-RestMethod -Uri $apiUrl -Method Get -ErrorAction Stop
            Write-Host "[$now] ✅ OK" -ForegroundColor Green
        }
        catch {
            Write-Host "[$now] ❌ Error: $($_.Exception.Message)" -ForegroundColor Red
        }
        Start-Sleep -Seconds $interval
    }
}
finally {
    Write-Host "`n스케줄러를 종료합니다." -ForegroundColor Yellow
}
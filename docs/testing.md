# Testing NewGodWar Compatibility

이 프로젝트는 빌드, 실제 서버 기동, 기능 회귀 검사로 호환성을 확인합니다.

1. Gradle 빌드가 Java 8 바이트코드로 성공하는지 확인합니다.
2. Paper 서버를 실제로 켜서 `NewGodWar enabled` 로그가 출력되고 서버가 정상 기동되는지 확인합니다.
3. 최신 지원 Paper에서 코어·능력·명령어 회귀 검사와 실제 프로세스 재시작·강제 종료 후 복구 검사를 실행합니다.

## 로컬 스모크 테스트

PowerShell에서 실행합니다.

Gradle 빌드는 CI와 같은 JDK 21을 사용하도록 `JAVA_HOME`을 지정합니다. `-JavaExecutable`은 테스트 서버용 Java만 선택하므로 빌드용 JDK와 별도로 설정하세요. 여러 Java 버전이 필요한 서버들은 아래 설명대로 `-Versions`로 나누어 실행합니다.

```powershell
.\scripts\Test-PaperMatrix.ps1
```

Windows에서 스크립트 실행 정책에 막히면 다음처럼 현재 실행에만 우회 옵션을 줄 수 있습니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Test-PaperMatrix.ps1
```

특정 버전만 확인하려면 다음처럼 실행합니다.

```powershell
.\scripts\Test-PaperMatrix.ps1 -Versions 1.12.2,1.21.11,26.3
```

지원 목록 전체를 확인하려면 다음처럼 실행합니다.

```powershell
.\scripts\Test-PaperMatrix.ps1 -AllSupportedVersions
```

지원 목록에서 가장 최신 버전만 확인하려면 다음처럼 실행합니다.

```powershell
.\scripts\Test-PaperMatrix.ps1 -LatestVersion
```

로컬 전체 검사는 지정한 `-JavaExecutable` 하나로 모든 서버를 실행합니다. Java 요구 버전이 갈리는 구간은 GitHub Actions 매트릭스가 Java 8/16/17/21/25로 나눠 확인합니다.

로컬 테스트는 기본적으로 `PATH`의 `java`를 사용합니다. 구버전 서버를 로컬에서 확인할 때는 해당 버전에 맞는 Java 실행 파일을 직접 지정할 수 있습니다.

```powershell
.\scripts\Test-PaperMatrix.ps1 -Versions 1.12.2 -JavaExecutable "C:\Program Files\Eclipse Adoptium\jdk-8\bin\java.exe"
```

이미 빌드된 jar를 테스트하려면 다음처럼 실행합니다.

```powershell
.\scripts\Test-PaperMatrix.ps1 -SkipBuild -PluginJar .\build\libs\NewGodWar-0.3.5.jar -Versions 26.3
```

테스트 서버 파일은 `.paper-smoke/` 아래에 생성되며 git에는 포함되지 않습니다.

스크립트는 PaperMC Fill API를 우선 사용하고, 오래된 Paperclip이 더 이상 유효하지 않은 Mojang S3 URL을 호출하지 않도록 Mojang version manifest에서 원본 server jar를 미리 받아 `cache/mojang_<version>.jar`에 넣습니다.

같은 Minecraft 버전의 Paper STABLE 빌드를 우선 사용하며, 없으면 다운로드 가능한 최신 빌드를 사용합니다. Minecraft 26.3은 정식 버전이지만 2026-09-19 기준 Paper 26.3은 ALPHA 채널입니다. 검사에 사용한 빌드와 결과는 각 릴리즈 노트에 기록합니다.

## 기능 회귀 검사

`build`는 운영 플러그인과 별도로 `plugin/build/<종류>-regression/`에 테스트 전용 JAR를 생성합니다. 다음 검사는 격리된 테스트 서버에서 실행하며, 테스트 JAR를 운영 서버에 설치하지 않습니다.

```powershell
.\scripts\Test-PaperMatrix.ps1 -SkipBuild -LatestVersion -ProbeJar plugin/build/core-regression/CoreRegressionProbe.jar -WorkDir .paper-smoke/core-regression
.\scripts\Test-PaperMatrix.ps1 -SkipBuild -LatestVersion -ProbeJar plugin/build/ability-regression/AbilityRegressionProbe.jar -ProbeSuccessMarker "ABILITY REGRESSION PASS" -WorkDir .paper-smoke/ability-regression
.\scripts\Test-PaperMatrix.ps1 -SkipBuild -LatestVersion -ProbeJar plugin/build/command-regression/CommandRegressionProbe.jar -ProbeSuccessMarker "COMMAND REGRESSION PASS" -WorkDir .paper-smoke/command-regression
```

코어 검사는 심장 파괴·승패·카운트다운·팀 채팅·게임과 테스트 월드 복원을, 능력 검사는 팻말·대상 검증·재료·쿨타임·아이템 지급·효과음을, 명령어 검사는 권한·별칭·자동완성·도움말·설정 GUI 이동을 확인합니다.

코어 검사에는 실제 서버 인벤토리를 이용한 이름·설명 있는 재료 차감, 재료 부족 시 무차감, 기본 아이템의 정확한 수량, 도박 보상의 부분·전체 초과 수량 드롭, 사망 시 방어구·보조 손의 단일 드롭, 오프라인 참가자 종료 정리가 포함됩니다. 복구 검사에서는 종료 정리 대기 기록이 실제 프로세스 재시작을 거쳐 적용되고, 완료한 정리는 강제 종료 후에도 다시 실행되지 않는지 확인합니다.

게임 복구 검사는 기본 스모크 테스트의 Paper 캐시를 사용합니다. 프로젝트 버전에 맞는 배포 JAR를 자동 선택하며, 필요하면 두 번째 인수로 플러그인 JAR, 세 번째 인수로 복구 테스트 JAR 경로를 지정할 수 있습니다.

```powershell
.\scripts\Test-PaperMatrix.ps1 -SkipBuild -Versions 26.3
python scripts/Test-GameRecovery.py 26.3
```

정상 재시작, 주기 저장 후 강제 종료, 게임 종료 시 원본 맵 복원, 준비 중 재시작, 맵 복원 도중 중단, 손상된 세션 보존의 여섯 단계를 확인합니다. 다인전 플레이 및 클라이언트 화면의 시각 검증은 이 자동 검사에 포함되지 않습니다.

## 로컬 다인 부하 검사

Paper 26.3 캐시와 배포 JAR가 준비된 환경에서 다음 명령을 실행합니다. 부하 도구는 별도 테스트 JAR이며 운영 플러그인에 포함되지 않습니다.

```powershell
.\gradlew.bat build :plugin:loadRegressionJar
python scripts/Test-Load.py --players 16,32,64 --seconds 60
```

외부에 공개되지 않는 `127.0.0.1` 전용 임시 서버에서 실제 TCP 클라이언트를 16명, 32명, 64명으로 늘립니다. 각 단계에서 게임을 시작하고 이동·블레이즈 막대기 좌/우클릭·공격·명령어를 전송합니다. 마지막 단계는 64명 URF 설정으로 반복합니다. 클라이언트는 Paper 26.3 프로토콜 777 전용이며, 실행 중인 서버에서 패킷 번호를 읽습니다. Python 표준 라이브러리만 사용합니다.

각 단계는 10초 준비 후 지정된 시간만큼 측정하며, 능력 사용은 측정 시작 시 활성화합니다. 봇은 기본 4명씩 별도 Python 프로세스에서 실행해 수신 패킷 처리가 다른 봇의 동작을 지연시키는 영향을 줄입니다. `--clients-per-worker 4`로 이 값을 명시할 수 있습니다. 틱 처리 시간 평균·p95·p99·최대, TPS, 명령 왕복 응답 시간, 힙·GC, 엔티티·예약 작업 수, 서버가 받은 이벤트와 재료 차감량을 기록합니다. 봇 프로세스 CPU와 서버 명령 처리 전후의 지연도 기록해 부하 생성기 병목을 구분합니다. 초기 기준은 TPS 19 이상, 틱 p95 50ms 미만, 명령 응답 p95 200ms 미만입니다. 단계마다 절반의 플레이어를 종료 전에 접속 해제한 뒤 다시 접속시켜 인벤토리·팀·능력·정리 대기 기록과 능력 오브젝트 잔여량을 검사합니다. 결과와 서버 로그는 `.paper-smoke/load-26.3-*/`에 저장됩니다.

측정 환경은 서버 힙 3GiB, 시야·시뮬레이션 거리 4, 작은 평지 전장, 8종 능력 조합입니다. 테스트 도구가 체력을 높이고 매초 회복시키며 재료를 보충해 지속 부하를 유지합니다. 실제 클라이언트 렌더링, 외부 네트워크 지연, 광범위한 청크 탐험, 모든 능력·애드온 조합 및 장시간 운영 용량을 보장하는 검사는 아닙니다.

## GitHub Actions

`.github/workflows/compatibility.yml` 워크플로가 브랜치 push, pull request, 수동 실행에서 동작합니다.

기본 매트릭스는 지원 목록 전체의 Paper 버전을 대상으로 합니다.
별도 `Latest supported Paper / Java 25` job은 지원 목록의 가장 최신 버전, 현재 `26.3`, 서버 기동을 한 번 더 확인합니다.

| Minecraft | Java |
| --- | --- |
| 1.12 ~ 1.16.5 | 8 |
| 1.17 ~ 1.17.1 | 16 |
| 1.18 ~ 1.20.4 | 17 |
| 1.20.5 ~ 1.21.x | 21 |
| 26.1.1 ~ 26.3 | 25 |

일부 버전만 빠르게 확인해야 하면 로컬 스크립트의 `-Versions`에 원하는 Paper 버전을 넘겨 실행하면 됩니다.

## 자동 릴리즈

`master`에 새 버전의 `build.gradle`을 push하거나 `v*` 형식의 태그를 push하면 `.github/workflows/release.yml` 워크플로가 실행됩니다.

1. 플러그인 jar를 빌드합니다.
2. 최신 지원 Paper 서버 기동 스모크 테스트를 통과해야 합니다.
3. 최신 지원 Paper의 코어·능력·명령어 검사와 Paper 26.3의 재시작·강제 종료 복구 검사를 모두 통과해야 합니다.
4. 테스트가 성공하면 해당 태그의 GitHub Release를 만들고 `NewGodWar-*.jar`를 첨부합니다. 릴리즈 본문은 `docs/releases/<태그>.md`를 사용합니다.

전체 Paper 매트릭스는 별도 호환성 신호로 계속 실행하지만, 구버전 개별 실패가 릴리즈 생성을 막지는 않습니다.

예시는 다음과 같습니다.

```bash
git tag v0.1.0
git push origin v0.1.0
```

GitHub Actions 화면에서 `Release` 워크플로를 수동 실행하고 태그를 입력해도 같은 릴리즈 작업을 다시 실행할 수 있습니다.

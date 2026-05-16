# Testing NewGodWar Compatibility

이 프로젝트는 두 단계로 호환성을 확인합니다.

1. Gradle 빌드가 Java 8 바이트코드로 성공하는지 확인합니다.
2. Paper 서버를 실제로 켜서 `NewGodWar enabled` 로그가 출력되고 서버가 정상 기동되는지 확인합니다.

## 로컬 스모크 테스트

PowerShell에서 실행합니다.

```powershell
.\scripts\Test-PaperMatrix.ps1
```

Windows에서 스크립트 실행 정책에 막히면 다음처럼 현재 실행에만 우회 옵션을 줄 수 있습니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Test-PaperMatrix.ps1
```

특정 버전만 확인하려면 다음처럼 실행합니다.

```powershell
.\scripts\Test-PaperMatrix.ps1 -Versions 1.12.2,1.20.6,1.21.1
```

로컬 테스트는 기본적으로 `PATH`의 `java`를 사용합니다. 구버전 서버를 로컬에서 확인할 때는 해당 버전에 맞는 Java 실행 파일을 직접 지정할 수 있습니다.

```powershell
.\scripts\Test-PaperMatrix.ps1 -Versions 1.12.2 -JavaExecutable "C:\Program Files\Eclipse Adoptium\jdk-8\bin\java.exe"
```

이미 빌드된 jar를 테스트하려면 다음처럼 실행합니다.

```powershell
.\scripts\Test-PaperMatrix.ps1 -SkipBuild -PluginJar .\build\libs\NewGodWar-0.1.0-SNAPSHOT.jar -Versions 1.21.1
```

테스트 서버 파일은 `.paper-smoke/` 아래에 생성되며 git에는 포함되지 않습니다.

## GitHub Actions

`.github/workflows/compatibility.yml` 워크플로가 push, pull request, 수동 실행에서 동작합니다.

기본 매트릭스는 대표 Paper 버전들을 대상으로 합니다.

| Minecraft | Java |
| --- | --- |
| 1.12.2 | 8 |
| 1.16.5 | 11 |
| 1.17.1 | 17 |
| 1.18.2 | 17 |
| 1.19.4 | 17 |
| 1.20.6 | 21 |
| 1.21.1 | 21 |

더 촘촘하게 확인해야 하면 워크플로의 `matrix.include`에 버전을 추가하거나, 로컬 스크립트의 `-Versions`에 원하는 Paper 버전을 넘겨 실행하면 됩니다.

# 시작하기

이 문서는 NewGodWar를 빌드하거나 서버에 설치한 뒤 첫 게임을 시작하기 위한 최소 절차를 정리합니다.

## 준비물

- Paper 또는 Spigot 계열 서버
- 서버 버전에 맞는 Java
- 서버 운영 권한 또는 OP 권한
- 플러그인 jar 파일 `NewGodWar-<version>.jar`

권장 Java는 서버 버전에 따라 달라집니다.

| 서버 버전대 | 권장 Java |
| --- | --- |
| 1.12.x ~ 1.16.5 | Java 8 |
| 1.17.x | Java 16 |
| 1.18.x ~ 1.20.4 | Java 17 |
| 1.20.5 ~ 1.21.x | Java 21 |
| 26.1.x | Java 25 |

## 다운로드 또는 빌드

릴리즈 jar를 사용하는 경우 GitHub Release의 Assets에서 `NewGodWar-<version>.jar`를 받아 서버의 `plugins/` 폴더에 넣습니다.

직접 빌드하려면 저장소 루트에서 다음 명령을 실행합니다.

```powershell
.\gradlew.bat clean build
```

Linux/macOS에서는 다음처럼 실행합니다.

```bash
./gradlew clean build
```

최종 플러그인 jar는 `build/libs/NewGodWar-<version>.jar`에 복사됩니다. 모듈 산출물은 `plugin/build/libs/`에서도 확인할 수 있습니다.

## 설치

1. 서버를 종료합니다.
2. `NewGodWar-<version>.jar`를 서버의 `plugins/` 폴더에 넣습니다.
3. 서버를 시작해 `plugins/NewGodWar/config.yml`이 생성되는지 확인합니다.
4. 콘솔 또는 로그에서 `NewGodWar enabled` 메시지를 확인합니다.
5. 필요한 설정을 바꾼 뒤 `/godwar reload` 또는 서버 재시작으로 반영합니다.

## 첫 게임 준비

관리자는 OP 또는 `newgodwar.admin` 권한이 있어야 합니다.

1. `/godwar settings` 또는 `/godwar gui`로 설정 GUI를 엽니다.
2. 사용할 팀이 켜져 있는지 확인합니다.
3. 각 팀 스폰 위치에 서서 `/godwar setspawn <team>`을 실행합니다.
4. 각 팀의 다이아몬드 블록 심장을 바라보고 `/godwar settemple <team>`을 실행합니다.
5. 플레이어를 `/godwar autoteam`으로 자동 배정하거나 `/godwar join <team> <player>`로 수동 배정합니다.
6. `/godwar start`로 게임 시작 준비를 시작합니다.
7. 플레이어는 능력 배정 후 `/t yes`로 확정하거나 `/t no`로 재추첨합니다.
8. 카운트다운이 끝나면 전원이 팀 스폰으로 이동하고 게임이 시작됩니다.

## 빠른 테스트

혼자 능력을 확인하려면 서버에 접속한 뒤 다음 명령을 사용할 수 있습니다.

```text
/godwar test [ability]
```

`[ability]`를 생략하면 무작위 능력이 배정됩니다. 예를 들어 `zeus`를 직접 테스트하려면 `/godwar test zeus`를 사용합니다.

## 기본 조합법

막대기 3개를 세로, 가로, 또는 대각선으로 배치하면 블레이즈 막대기 1개를 제작할 수 있습니다. 블레이즈 막대기는 대부분의 일반/고급 능력 발동 아이템입니다.

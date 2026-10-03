# 버전별 리소스팩

[v0.3.6 다운로드](https://github.com/minjae9010/NewGodWar/releases/tag/v0.3.6)에서 플러그인 JAR와 버전별 ZIP, SHA-1을 제공합니다. 팩은 이 GitHub 저장소의 Release 주소를 사용합니다.

## 자동 연결

```yaml
abilities:
  effects:
    resource-pack:
      enabled: true
      url: 'auto'
      sha1: ''
```

기본 설정으로 서버 버전에 맞는 ZIP과 SHA-1을 플러그인에 포함된 목록에서 선택합니다. 기존 설정의 빈 URL도 자동 선택으로 처리합니다. 전송을 끄려면 `enabled: false`로 설정합니다. 이전의 26.3 전용 URL을 직접 입력했다면 `auto`로 바꾸고 재시작하세요.

주소는 `https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-<구간>.zip` 형식입니다. 특정 릴리즈에 고정하므로 최신 릴리즈가 바뀌어도 설치한 JAR의 해시와 맞습니다. 직접 수정한 팩은 `url`에 다운로드 주소, `sha1`에 해당 ZIP의 40자리 SHA-1을 입력할 수 있습니다.

## 지원 범위

- 1.12~1.13: 버전에 맞는 보관용 팩을 제공하지만, 개별 CustomModelData가 없어 자동 제안하지 않습니다. 메뉴·음식은 일반 아이템, 능력은 파티클로 표시합니다.
- 1.14~1.19.3: 전용 메뉴 아이콘·식신 음식. 능력 공간 연출은 파티클로 대체합니다.
- 1.19.4~1.21.3: 위 기능에 ItemDisplay 능력 문양을 추가합니다. 정수 CustomModelData와 구형 모델 overrides를 사용합니다.
- 1.21.4~26.3: 새 item definition 방식과 버전에 맞는 아틀라스를 사용합니다.

접속 시 다운로드를 수락해야 전용 메뉴·능력 문양이 켜집니다. 거절·실패 시 기본 표시를 유지합니다. 1.20.3 이상은 팩 UUID로 응답을 구분합니다. 그 이전 서버 API는 응답에 팩 식별자가 없으므로 다른 플러그인이나 `server.properties`에서 별도 팩을 동시에 전송하지 마세요.

선택 기준은 **서버 버전**입니다. ViaVersion 같은 프로토콜 변환기를 통한 서로 다른 클라이언트 버전의 동시 접속에는 자동 개별 선택을 제공하지 않습니다. 미등록 버전과 Pre-release/RC는 자동 선택하지 않습니다.

## 직접 다운로드

같은 리소스 형식을 쓰는 버전들은 ZIP 하나를 공유합니다. 각 ZIP 옆의 SHA-1과 [전체 버전 목록](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-manifest.tsv)을 제공합니다.

| Minecraft | 형식 | ZIP | SHA-1 |
| --- | --- | --- | --- |
| 1.12, 1.12.1, 1.12.2 | 3 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.12.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.12.zip.sha1) |
| 1.13, 1.13.1, 1.13.2 | 4 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.13.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.13.zip.sha1) |
| 1.14, 1.14.1, 1.14.2, 1.14.3, 1.14.4 | 4 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.14.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.14.zip.sha1) |
| 1.15, 1.15.1, 1.15.2, 1.16, 1.16.1 | 5 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.15-1.16.1.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.15-1.16.1.zip.sha1) |
| 1.16.2, 1.16.3, 1.16.4, 1.16.5 | 6 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.16.2-1.16.5.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.16.2-1.16.5.zip.sha1) |
| 1.17, 1.17.1 | 7 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.17.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.17.zip.sha1) |
| 1.18, 1.18.1, 1.18.2 | 8 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.18.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.18.zip.sha1) |
| 1.19, 1.19.1, 1.19.2 | 9 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.19-1.19.2.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.19-1.19.2.zip.sha1) |
| 1.19.3 | 12 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.19.3.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.19.3.zip.sha1) |
| 1.19.4 | 13 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.19.4.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.19.4.zip.sha1) |
| 1.20, 1.20.1 | 15 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.20-1.20.1.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.20-1.20.1.zip.sha1) |
| 1.20.2 | 18 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.20.2.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.20.2.zip.sha1) |
| 1.20.3, 1.20.4 | 22 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.20.3-1.20.4.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.20.3-1.20.4.zip.sha1) |
| 1.20.5, 1.20.6 | 32 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.20.5-1.20.6.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.20.5-1.20.6.zip.sha1) |
| 1.21, 1.21.1 | 34 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.21-1.21.1.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.21-1.21.1.zip.sha1) |
| 1.21.2, 1.21.3 | 42 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.21.2-1.21.3.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.21.2-1.21.3.zip.sha1) |
| 1.21.4 | 46 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.21.4.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.21.4.zip.sha1) |
| 1.21.5 | 55 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.21.5.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.21.5.zip.sha1) |
| 1.21.6 | 63 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.21.6.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.21.6.zip.sha1) |
| 1.21.7, 1.21.8 | 64 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.21.7-1.21.8.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.21.7-1.21.8.zip.sha1) |
| 1.21.9, 1.21.10 | 69.0 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.21.9-1.21.10.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.21.9-1.21.10.zip.sha1) |
| 1.21.11 | 75.0 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.21.11.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-1.21.11.zip.sha1) |
| 26.1, 26.1.1, 26.1.2 | 84.0 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-26.1.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-26.1.zip.sha1) |
| 26.2 | 88.0 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-26.2.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-26.2.zip.sha1) |
| 26.3 | 97.1 | [다운로드](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-26.3.zip) | [확인](https://github.com/minjae9010/NewGodWar/releases/download/v0.3.6/NewGodWar-Art-26.3.zip.sha1) |

## 빌드·검증

JDK 21로 `./gradlew clean build`를 실행하면 `build/libs`에 ZIP 25종과 `.sha1`, 버전 목록이 생성됩니다. `./gradlew resourcePack`으로 팩만 만들 수도 있습니다. 원본 그림은 `scripts/effect-art`의 Java2D 코드, 호환 구간은 `pack-versions.tsv`입니다.

`python scripts/effect-art/verify_versions.py`는 전체 팩의 형식·모델·텍스처·메뉴 31종·음식 6종·기본 아이템 복귀·SHA-1과 배포 JAR의 버전 목록을 검사합니다. 실제 클라이언트 화면 검증 범위는 [테스트 안내](../testing.md)를 참고하세요.

형식 변경 근거: [1.21.4 아이템 모델](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-4), [1.21.9 팩 메타데이터](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-9), [1.21.11](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11), [26.1](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-1), [26.2](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-2), [26.3](https://feedback.minecraft.net/hc/en-us/articles/48913133328013-Minecraft-Java-Edition-26-3). 1.21.9·1.21.11·26.1.2의 아틀라스 경계는 Mojang 공식 클라이언트 JAR에서도 확인했습니다.

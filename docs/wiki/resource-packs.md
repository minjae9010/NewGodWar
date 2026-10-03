# 버전별 리소스팩

[리소스팩 전용 공간](https://github.com/minjae9010/NewGodWar/tree/codex/resource-packs)에 버전별 ZIP과 SHA-1을 보관합니다. 플러그인 릴리즈와 독립된 `codex/resource-packs` 브랜치이며, 팩을 한 번 업로드한 후 여러 플러그인 버전에서 재사용합니다.

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

주소는 `https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/<SHA-1>/NewGodWar-Art-<구간>.zip` 형식입니다. 플러그인 릴리즈 번호를 사용하지 않습니다. 팩 내용이 바뀌는 경우에만 새 해시 경로에 추가하며 기존 경로는 유지합니다. `url: auto`는 설치한 JAR에 기록된 파일과 해시를 선택합니다. 직접 수정한 팩은 `url`과 `sha1`을 함께 지정할 수 있습니다.

일반 `build`와 릴리즈는 ZIP을 생성하거나 업로드하지 않습니다. 이미 게시한 목록 두 개(`art-packs.properties`, `art-models.properties`)만 플러그인에 포함합니다. v0.3.6에 실수로 첨부했던 파일은 해당 버전 사용자의 기존 주소 유지를 위해 남기지만, v0.3.7부터는 별도 공간을 사용하며 릴리즈에는 플러그인 JAR만 첨부합니다.

## 지원 범위

- 1.12~1.13: 버전에 맞는 보관용 팩을 제공하지만, 개별 CustomModelData가 없어 자동 제안하지 않습니다. 메뉴·음식은 일반 아이템, 능력은 파티클로 표시합니다.
- 1.14~1.19.3: 전용 메뉴 아이콘·식신 음식. 능력 공간 연출은 파티클로 대체합니다.
- 1.19.4~1.21.3: 위 기능에 ItemDisplay 능력 문양을 추가합니다. 정수 CustomModelData와 구형 모델 overrides를 사용합니다.
- 1.21.4~26.3: 새 item definition 방식과 버전에 맞는 아틀라스를 사용합니다.

접속 시 다운로드를 수락해야 전용 메뉴·능력 문양이 켜집니다. 거절·실패 시 기본 표시를 유지합니다. 1.20.3 이상은 팩 UUID로 응답을 구분합니다. 그 이전 서버 API는 응답에 팩 식별자가 없으므로 다른 플러그인이나 `server.properties`에서 별도 팩을 동시에 전송하지 마세요.

선택 기준은 **서버 버전**입니다. ViaVersion 같은 프로토콜 변환기를 통한 서로 다른 클라이언트 버전의 동시 접속에는 자동 개별 선택을 제공하지 않습니다. 미등록 버전과 Pre-release/RC는 자동 선택하지 않습니다.

## 직접 다운로드

같은 리소스 형식을 쓰는 버전들은 ZIP 하나를 공유합니다. 각 ZIP 옆의 SHA-1과 [전체 버전 목록](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/NewGodWar-Art-manifest.tsv)을 제공합니다.

| Minecraft | 형식 | ZIP | SHA-1 |
| --- | --- | --- | --- |
| 1.12, 1.12.1, 1.12.2 | 3 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/b5c34fc9a14334e04a5112bdc11019ae2e950f73/NewGodWar-Art-1.12.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/b5c34fc9a14334e04a5112bdc11019ae2e950f73/NewGodWar-Art-1.12.zip.sha1) |
| 1.13, 1.13.1, 1.13.2 | 4 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/148834d15ae48b686482fe6a25004e24fda6b4df/NewGodWar-Art-1.13.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/148834d15ae48b686482fe6a25004e24fda6b4df/NewGodWar-Art-1.13.zip.sha1) |
| 1.14, 1.14.1, 1.14.2, 1.14.3, 1.14.4 | 4 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/4cb6f864a19b25f6e2dfaa1000f6d9c903e98ef8/NewGodWar-Art-1.14.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/4cb6f864a19b25f6e2dfaa1000f6d9c903e98ef8/NewGodWar-Art-1.14.zip.sha1) |
| 1.15, 1.15.1, 1.15.2, 1.16, 1.16.1 | 5 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/3b85ea05f973c29d5d4df8a8b86c40463ac3b832/NewGodWar-Art-1.15-1.16.1.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/3b85ea05f973c29d5d4df8a8b86c40463ac3b832/NewGodWar-Art-1.15-1.16.1.zip.sha1) |
| 1.16.2, 1.16.3, 1.16.4, 1.16.5 | 6 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/33169c7af472844cabf28eaa9e8f24148c5ac613/NewGodWar-Art-1.16.2-1.16.5.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/33169c7af472844cabf28eaa9e8f24148c5ac613/NewGodWar-Art-1.16.2-1.16.5.zip.sha1) |
| 1.17, 1.17.1 | 7 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/7afd44e34d91c6c9bcae8e78e12d68b97eeb9c7e/NewGodWar-Art-1.17.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/7afd44e34d91c6c9bcae8e78e12d68b97eeb9c7e/NewGodWar-Art-1.17.zip.sha1) |
| 1.18, 1.18.1, 1.18.2 | 8 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/7121e9e93854bb3328bfc6aedf9ebafd917ee66a/NewGodWar-Art-1.18.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/7121e9e93854bb3328bfc6aedf9ebafd917ee66a/NewGodWar-Art-1.18.zip.sha1) |
| 1.19, 1.19.1, 1.19.2 | 9 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/f244d4d1f01edc26bbccf060465d3d0f1b62930a/NewGodWar-Art-1.19-1.19.2.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/f244d4d1f01edc26bbccf060465d3d0f1b62930a/NewGodWar-Art-1.19-1.19.2.zip.sha1) |
| 1.19.3 | 12 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/abf8198a14dde7a92cea70cdb1a6e2cf548a40ed/NewGodWar-Art-1.19.3.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/abf8198a14dde7a92cea70cdb1a6e2cf548a40ed/NewGodWar-Art-1.19.3.zip.sha1) |
| 1.19.4 | 13 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/a466ed271d4e2d6517b0882ade3982437b45c26b/NewGodWar-Art-1.19.4.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/a466ed271d4e2d6517b0882ade3982437b45c26b/NewGodWar-Art-1.19.4.zip.sha1) |
| 1.20, 1.20.1 | 15 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/3916c768cbfab34733d5b2defa6fbf85dbfe6ce1/NewGodWar-Art-1.20-1.20.1.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/3916c768cbfab34733d5b2defa6fbf85dbfe6ce1/NewGodWar-Art-1.20-1.20.1.zip.sha1) |
| 1.20.2 | 18 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/3175f5f2ef03e93e794a266ead928e45633f615c/NewGodWar-Art-1.20.2.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/3175f5f2ef03e93e794a266ead928e45633f615c/NewGodWar-Art-1.20.2.zip.sha1) |
| 1.20.3, 1.20.4 | 22 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/971c5bea63b2a4669bb96db6271a1307c2170417/NewGodWar-Art-1.20.3-1.20.4.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/971c5bea63b2a4669bb96db6271a1307c2170417/NewGodWar-Art-1.20.3-1.20.4.zip.sha1) |
| 1.20.5, 1.20.6 | 32 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/27b237beafb9829a3b7dfc02abedb2182fa47330/NewGodWar-Art-1.20.5-1.20.6.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/27b237beafb9829a3b7dfc02abedb2182fa47330/NewGodWar-Art-1.20.5-1.20.6.zip.sha1) |
| 1.21, 1.21.1 | 34 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/804cf8b8fffcff94790cf18f3ad15f73d74884c7/NewGodWar-Art-1.21-1.21.1.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/804cf8b8fffcff94790cf18f3ad15f73d74884c7/NewGodWar-Art-1.21-1.21.1.zip.sha1) |
| 1.21.2, 1.21.3 | 42 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/49e17cbed0085b531190e77b5bd1d89ecb0cd102/NewGodWar-Art-1.21.2-1.21.3.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/49e17cbed0085b531190e77b5bd1d89ecb0cd102/NewGodWar-Art-1.21.2-1.21.3.zip.sha1) |
| 1.21.4 | 46 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/d5d5c146b9c4852dff7956d9d35d1538f55349fe/NewGodWar-Art-1.21.4.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/d5d5c146b9c4852dff7956d9d35d1538f55349fe/NewGodWar-Art-1.21.4.zip.sha1) |
| 1.21.5 | 55 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/a423075ac21b621ae066689982eaa3572512ec6f/NewGodWar-Art-1.21.5.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/a423075ac21b621ae066689982eaa3572512ec6f/NewGodWar-Art-1.21.5.zip.sha1) |
| 1.21.6 | 63 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/8d265b1f2759942d35a1b6ad6f3f02a8f468687c/NewGodWar-Art-1.21.6.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/8d265b1f2759942d35a1b6ad6f3f02a8f468687c/NewGodWar-Art-1.21.6.zip.sha1) |
| 1.21.7, 1.21.8 | 64 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/d55549fe964471a9a95652260d48068fd452a56d/NewGodWar-Art-1.21.7-1.21.8.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/d55549fe964471a9a95652260d48068fd452a56d/NewGodWar-Art-1.21.7-1.21.8.zip.sha1) |
| 1.21.9, 1.21.10 | 69.0 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/f1a4e7a8668fe7178c32d608997276698dfa534b/NewGodWar-Art-1.21.9-1.21.10.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/f1a4e7a8668fe7178c32d608997276698dfa534b/NewGodWar-Art-1.21.9-1.21.10.zip.sha1) |
| 1.21.11 | 75.0 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/7c01689a0c56929dea9c77194f651e6eb7a8b9b5/NewGodWar-Art-1.21.11.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/7c01689a0c56929dea9c77194f651e6eb7a8b9b5/NewGodWar-Art-1.21.11.zip.sha1) |
| 26.1, 26.1.1, 26.1.2 | 84.0 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/bae8e34c1cf4361b88c641350c2a97666f1f2e60/NewGodWar-Art-26.1.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/bae8e34c1cf4361b88c641350c2a97666f1f2e60/NewGodWar-Art-26.1.zip.sha1) |
| 26.2 | 88.0 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/550eaee48f97ad935a4299bba23d88e31b6bb831/NewGodWar-Art-26.2.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/550eaee48f97ad935a4299bba23d88e31b6bb831/NewGodWar-Art-26.2.zip.sha1) |
| 26.3 | 97.1 | [다운로드](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/f3651555916484d0e28c788f59fa1ddb205fca7c/NewGodWar-Art-26.3.zip) | [확인](https://raw.githubusercontent.com/minjae9010/NewGodWar/refs/heads/codex/resource-packs/packs/f3651555916484d0e28c788f59fa1ddb205fca7c/NewGodWar-Art-26.3.zip.sha1) |

## 빌드·검증

플러그인만 수정할 때는 JDK 21로 `./gradlew clean build`를 실행합니다. 팩 생성·다운로드 없이 저장된 목록을 읽습니다. `python scripts/effect-art/verify_catalogue.py`로 목록과 JAR의 일치를 검사합니다.

그림이나 지원 버전을 수정할 때만 다음을 실행합니다.

```powershell
./gradlew.bat resourcePack
python scripts/effect-art/publish.py --publish
```

게시 도구는 생성된 팩의 형식·모델·해시를 검사한 뒤 전용 브랜치에 새 파일만 추가합니다. 이미 존재하는 해시 경로의 파일은 재사용하며 기존 경로를 지우지 않습니다. 업로드에 성공하면 `plugin/src/main/resources/art-*.properties`를 갱신합니다. 이 목록과 소스 변경을 함께 커밋하세요. 이후 플러그인 릴리즈에서는 팩을 다시 만들지 않습니다.

원본 그림은 `scripts/effect-art`의 Java2D 코드, 호환 구간은 `pack-versions.tsv`입니다. `verify_versions.py --skip-jar`는 팩 생성 후 전체 ZIP을 검사하는 유지보수 명령입니다. 실제 클라이언트 화면 검증 범위는 [테스트 안내](../testing.md)를 참고하세요.

형식 변경 근거: [1.21.4 아이템 모델](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-4), [1.21.9 팩 메타데이터](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-9), [1.21.11](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11), [26.1](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-1), [26.2](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-2), [26.3](https://feedback.minecraft.net/hc/en-us/articles/48913133328013-Minecraft-Java-Edition-26-3). 1.21.9·1.21.11·26.1.2의 아틀라스 경계는 Mojang 공식 클라이언트 JAR에서도 확인했습니다.

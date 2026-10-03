# 리소스팩

이 디렉토리는 `NewGodWar` 저장소의 `master/resoucepack/`입니다. ZIP을 여기에 보관하고 GitHub Raw URL로 다운로드합니다. ZIP은 플러그인 JAR에 포함되지 않습니다.

## 현재 팩

**하나의 팩이 1.14부터 26.3까지 모든 지원 버전에서 동작합니다.**

| Minecraft | ZIP | SHA-1 |
| --- | --- | --- |
| 1.14 – 26.3 | [NewGodWar-Art-d36217f4.zip](https://raw.githubusercontent.com/minjae9010/NewGodWar/master/resoucepack/NewGodWar-Art-d36217f4.zip) | `d36217f436eeaaeb6b1bd51a3ff3b4a4c830e96b` |

플러그인은 `abilities.effects.resource-pack.url: auto`에서 이 파일명과 SHA-1을 자동으로 사용합니다. [버전별 주소·SHA-1 목록](manifest.tsv)도 같은 파일을 가리킵니다.

- 텍스처는 모든 버전의 기본 아이템 아틀라스가 읽는 `textures/item/` 아래에 있어 버전별 아틀라스 파일이 필요 없습니다.
- 구형 `custom_model_data` 오버라이드(1.14–1.21.3)와 새 아이템 모델 정의(1.21.4+)가 함께 들어 있고, 각 클라이언트는 이해하지 못하는 쪽을 무시합니다.
- `pack.mcmeta`는 `pack_format`·`supported_formats`·`min_format`/`max_format`으로 형식 4–97 전체를 선언합니다.

## 이전 팩

`NewGodWar-Art-<버전>.zip` 25종은 v0.3.8 이하 플러그인이 내장한 파일명·SHA-1로 계속 받아 가는 팩입니다. 해당 버전을 쓰는 서버가 없어지면 삭제해도 됩니다. 새 팩은 파일명에 자체 해시가 들어가므로, 팩을 고쳐도 이미 배포된 플러그인이 받는 파일은 바뀌지 않습니다.

팩을 수정할 때만 저장소 루트에서 `./gradlew resourcePack`과 `python scripts/effect-art/stage.py`를 실행합니다. 새 ZIP과 플러그인의 파일명·해시 목록을 함께 커밋합니다. 별도 저장소나 배포 브랜치는 사용하지 않습니다.

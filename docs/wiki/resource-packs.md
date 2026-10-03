# 리소스팩

**하나의 팩**이 1.14부터 26.3까지 모든 지원 버전에서 동작합니다. 팩은 **현재 저장소의 [resoucepack 디렉토리](https://github.com/minjae9010/NewGodWar/tree/master/resoucepack)**에 있습니다. 플러그인 JAR에 포함되지 않고 해당 GitHub Raw 경로에서 다운로드합니다.

```yaml
abilities:
  effects:
    resource-pack:
      enabled: true
      url: 'auto'
      sha1: ''
```

`auto` 또는 빈 URL은 통합 팩의 파일명과 SHA-1을 사용합니다. 현재 주소는 [https://raw.githubusercontent.com/minjae9010/NewGodWar/master/resoucepack/NewGodWar-Art-d36217f4.zip](https://raw.githubusercontent.com/minjae9010/NewGodWar/master/resoucepack/NewGodWar-Art-d36217f4.zip)입니다. 파일명에 팩 자체의 해시가 들어가므로 팩을 고쳐도 이미 배포된 플러그인이 받는 파일은 바뀌지 않습니다. 직접 URL을 지정할 때는 같은 ZIP의 SHA-1도 입력합니다. 변경 후 서버를 재시작하세요.

[직접 다운로드](https://github.com/minjae9010/NewGodWar/tree/master/resoucepack)와 [전체 버전·주소·SHA-1 목록](https://raw.githubusercontent.com/minjae9010/NewGodWar/master/resoucepack/manifest.tsv)을 제공합니다. 별도 저장소나 배포 브랜치를 사용하지 않습니다. 일반 플러그인 빌드와 릴리즈는 팩을 생성하거나 첨부하지 않습니다.

## 지원 범위

- 1.12~1.13: 개별 CustomModelData가 없어 팩을 자동 제안하지 않습니다. 메뉴·음식은 기본 아이템, 능력은 파티클을 유지합니다.
- 1.14~1.19.3: 전용 메뉴·식신 음식. 공간 연출은 파티클입니다.
- 1.19.4~1.21.3: 전용 메뉴·음식과 ItemDisplay 문양. 구형 CustomModelData/overrides를 사용합니다.
- 1.21.4~26.3: 새 item definition을 사용합니다.

한 팩에 구형 `custom_model_data` 오버라이드와 새 item definition이 함께 들어 있어 각 클라이언트가 자기 방식을 사용합니다. 텍스처는 모든 버전의 기본 아이템 아틀라스가 읽는 `textures/item/` 아래에 있으며, `pack.mcmeta`는 형식 4–97을 `pack_format`·`supported_formats`·`min_format`/`max_format`으로 모두 선언합니다. 서버가 사용할 모델 방식(구형/신형)은 서버 버전으로 고릅니다.

다운로드 수락 후 전용 메뉴·문양이 표시됩니다. 1.20.3 미만 서버 API는 팩 응답에 UUID가 없어 다른 플러그인이나 `server.properties`에서 다른 팩을 동시에 전송하지 않아야 합니다. 자동 선택은 서버 버전 기준이며 ViaVersion을 통한 서로 다른 클라이언트별 팩 선택은 지원하지 않습니다.

## 이어지는 메뉴 배경

1.16 이상에서 팩 로드가 완료되면 설정·능력 정보·능력 도감·보상 뽑기는 큰 배경 이미지 한 장으로 표시됩니다. 아이템 칸 사이의 회색 격자까지 덮으며, 버튼 아이콘에는 별도의 사각 테두리가 없습니다. 메인 메뉴는 두 줄로 분류를 모으고 하단에 홈·도움말·시작 아이템·새로고침·닫기를 둡니다.

배경은 메뉴 제목의 전용 글리프를 사용하므로 일반 상자나 다른 플러그인의 창 배경을 바꾸지 않습니다. 팩을 받지 않았거나 `ui.resource-pack.enabled: false`이면 기존 한글 제목과 기본 아이템 메뉴를 사용합니다. 1.14~1.15는 개별 아이콘만 지원합니다. 시작 아이템 편집기는 실제 아이템을 넣는 창고이므로 기본 인벤토리 표시를 유지합니다.

1.21.2 이상에서는 마우스를 올릴 때 생기는 회색 슬롯 강조를 투명하게 처리합니다. 이 효과는 공용 슬롯 스프라이트이므로 팩이 로드된 동안 일반 인벤토리에도 적용됩니다. 메뉴 설정을 끄는 것만으로 복원되지 않으며 팩을 해제해야 합니다. 1.20.5 이상에서는 배경 장식·분류·고정 이동 버튼의 툴팁도 숨깁니다. 설정 값, 능력 설명, 보상 수량과 확률처럼 필요한 정보는 계속 툴팁으로 제공합니다. 이전 버전에서는 기본 마우스 강조나 툴팁이 남을 수 있습니다.

보상 뽑기는 4줄 화면에 보유량·1회 비용·최근 결과와 큰 실행 버튼을 둡니다. 버튼의 아이콘과 글자 영역 전체가 한 번의 뽑기로 동작합니다. **보상 보기**는 비용을 쓰지 않고 21개씩 상품과 확률을 확인하는 별도 화면입니다.

배경은 `scripts/effect-art/GuiPanels.java`, 아이콘은 `GuiArtwork.java`, 제목 위치는 `GuiTitle.java`에서 관리합니다. 배경은 두 배 해상도의 한 장을 4개 글리프로 나눠 틈 없이 합성합니다. 각 조각을 256px 이내로 유지해 Minecraft 폰트 아틀라스 제한과 작은 한글의 선명도를 함께 해결합니다. 한글 문구는 `GuiLabels.java`와 저장된 `menu-labels.png`·`menu-labels.tsv`를 사용하므로 빌드 PC에 한글 폰트를 설치할 필요가 없습니다. 새 JAR와 새 ZIP을 함께 배포해야 하며, `auto`를 사용하는 서버는 해당 ZIP이 GitHub Raw에 게시된 뒤 업데이트하세요.

## 팩 수정과 빌드

플러그인만 수정할 때는 `./gradlew clean build`를 실행합니다. ZIP은 다시 생성하지 않습니다. `python scripts/effect-art/verify_catalogue.py --plugin-only`는 디렉토리의 ZIP·SHA-1·버전 목록과 JAR에 팩이 포함되지 않았는지 검사합니다.

팩을 실제로 수정할 때만 다음을 실행합니다.

```powershell
./gradlew.bat resourcePack
python scripts/effect-art/stage.py
```

`stage.py`는 생성된 팩을 검증한 뒤 같은 저장소의 `resoucepack/`에 새 ZIP을 추가하고 플러그인의 파일명·해시 목록을 갱신합니다. 이전 ZIP은 이미 배포된 플러그인 버전을 위해 남겨 둡니다. 네트워크 업로드나 다른 저장소 생성을 하지 않습니다. 변경 파일을 평소처럼 함께 커밋·푸시하면 GitHub Raw 주소에서 받을 수 있습니다.

실제 클라이언트 화면 검사 범위는 [테스트 안내](../testing.md)에 구분되어 있습니다.

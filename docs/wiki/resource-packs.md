# 버전별 리소스팩

버전별 ZIP 25종은 **현재 저장소의 [resoucepack 디렉토리](https://github.com/minjae9010/NewGodWar/tree/master/resoucepack)**에 있습니다. 플러그인 JAR에 포함되지 않고 해당 GitHub Raw 경로에서 다운로드합니다.

```yaml
abilities:
  effects:
    resource-pack:
      enabled: true
      url: 'auto'
      sha1: ''
```

`auto` 또는 빈 URL은 서버 버전에 맞는 파일과 SHA-1을 선택합니다. 예를 들어 26.3의 주소는 [https://raw.githubusercontent.com/minjae9010/NewGodWar/master/resoucepack/NewGodWar-Art-26.3.zip](https://raw.githubusercontent.com/minjae9010/NewGodWar/master/resoucepack/NewGodWar-Art-26.3.zip)입니다. 직접 URL을 지정할 때는 같은 ZIP의 SHA-1도 입력합니다. 변경 후 서버를 재시작하세요.

[버전별 직접 다운로드](https://github.com/minjae9010/NewGodWar/tree/master/resoucepack)와 [전체 버전·주소·SHA-1 목록](https://raw.githubusercontent.com/minjae9010/NewGodWar/master/resoucepack/manifest.tsv)을 제공합니다. 별도 저장소나 배포 브랜치를 사용하지 않습니다. 일반 플러그인 빌드와 릴리즈는 팩을 생성하거나 첨부하지 않습니다.

## 지원 범위

- 1.12~1.13: 개별 CustomModelData가 없어 팩을 자동 제안하지 않습니다. 메뉴·음식은 기본 아이템, 능력은 파티클을 유지합니다.
- 1.14~1.19.3: 전용 메뉴·식신 음식. 공간 연출은 파티클입니다.
- 1.19.4~1.21.3: 전용 메뉴·음식과 ItemDisplay 문양. 구형 CustomModelData/overrides를 사용합니다.
- 1.21.4~26.3: 새 item definition과 버전에 맞는 아틀라스를 사용합니다.

다운로드 수락 후 전용 메뉴·문양이 표시됩니다. 1.20.3 미만 서버 API는 팩 응답에 UUID가 없어 다른 플러그인이나 `server.properties`에서 다른 팩을 동시에 전송하지 않아야 합니다. 자동 선택은 서버 버전 기준이며 ViaVersion을 통한 서로 다른 클라이언트별 팩 선택은 지원하지 않습니다.

## 팩 수정과 빌드

플러그인만 수정할 때는 `./gradlew clean build`를 실행합니다. ZIP은 다시 생성하지 않습니다. `python scripts/effect-art/verify_catalogue.py --plugin-only`는 디렉토리의 ZIP·SHA-1·버전 목록과 JAR에 팩이 포함되지 않았는지 검사합니다.

팩을 실제로 수정할 때만 다음을 실행합니다.

```powershell
./gradlew.bat resourcePack
python scripts/effect-art/stage.py
```

`stage.py`는 생성된 팩을 검증한 뒤 같은 저장소의 `resoucepack/`와 플러그인의 파일명·해시 목록만 갱신합니다. 네트워크 업로드나 다른 저장소 생성을 하지 않습니다. 변경 파일을 평소처럼 함께 커밋·푸시하면 GitHub Raw 주소에서 받을 수 있습니다.

실제 클라이언트 화면 검사 범위는 [테스트 안내](../testing.md)에 구분되어 있습니다.

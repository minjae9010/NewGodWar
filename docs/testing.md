# Testing NewGodWar Compatibility

이 프로젝트는 빌드, 실제 서버 기동, 기능 회귀 검사로 호환성을 확인합니다.

## 전체 능력 움직임 점검 (2026-10-11)

배포 버전은 v0.3.11입니다. 릴리즈 JAR SHA-256은 `c9b8457c5b8022cfff2a5cdfba7730eb761d4666dfeffdd2dae1fe17ebbf0882`이며, 아래 시각 검사본과 ZIP 항목을 비교해 `plugin.yml`·매니페스트의 버전 표기만 달라졌음을 확인했습니다. 실행 코드와 리소스는 같습니다. 새 버전의 전체 빌드·단위 테스트 127개와 저장된 팩의 모델·텍스처·해시 검사도 통과했습니다.

93개 능력과 150개 모델을 다시 점검했습니다. 니케 돌진·아테나 진형·암살자 뒤잡기는 시선을 수직으로 올리거나 내렸을 때 남쪽으로 방향이 바뀌던 처리를 제거하고 yaw를 유지합니다. 공통 밀쳐내기도 같은 수평 방향 계산을 사용합니다. 오딘 궁니르는 적중 순간에만 나타나던 방식에서 오른손 앞에서 출발해 0.6초 동안 움직이는 표적을 향해 날아가도록 바꿨습니다. 기존 비용·피해·적중 시점과 엄폐 판정은 유지합니다. 태양 화살의 기본 모델은 얼굴 옆에서 가로로 움직이던 화살을 활에서 정면으로 발사하도록 수정했습니다. 묠니르·궁니르의 근거리 출발점도 표적을 지나치지 않도록 제한합니다. 묠니르는 투척 자세를 유지한 손잡이 우선 회수를 계속 사용합니다.

수정 전 전체 발동 점검은 JAR `3d3c2d152f9e428376350910fad25ea8199ce67672f3fe25b792bdc3faea1b8b`로 진행했습니다. 실제 클라이언트에서 93개 능력의 해당 일반/고급 기술을 순회했고 발동 136건·준비 2건·안내 1건·해당 기술 없음 47건을 확인했습니다. 139개 영상·7,839개 원본 프레임과 패시브 콜백 경로 91종을 기록했습니다. 크리퍼·메구밍은 사용 후 사망하므로 이 순회의 패시브 콜백에서는 제외합니다. 특수 조건과 패시브는 서버 도구로 재현하며 실제 클라이언트 클릭 검사와 구분합니다. 기록은 `.build/minecraft-mcp/evidence/all-motion-audit-20261011/`에 있습니다.

최종 수정본 JAR `9a4f2e9fb337662a8d4b52b47e062ca314bc11d2ac654db9ec1c059b13a9f862`에서는 수직 시선 6조건, 밀쳐내기 10조건, 궁니르 8조건, 묠니르 8조건을 재검사했습니다. 궁니르는 고저차·이동 표적·근거리·비행 중 엄폐·능력 변경 취소·파티클 대체·애니메이션 끄기를 포함하며, 실제 창 아이템의 틱별 좌표와 자세를 확인합니다. 취소는 독립 모델을 지우는 명령 대신 실제 능력 재지정으로 검사합니다. 포세이돈의 물 흐름은 약간의 옆 이동을 만들므로 서쪽 이동 벡터의 정렬을 검사하고, 오딘 표적 지정은 시점 패킷 반영을 기다린 뒤 입력합니다.

최종본의 150개 모델 재생·소멸은 5,516프레임, 위 32조건과 주작·헤르메스 공중 비행은 2,050프레임으로 모두 통과했습니다. 총 184개 영상·7,566프레임이며 최저 캡처 속도는 17.82fps입니다. 이는 게임 렌더링 FPS 측정값이 아닙니다. 태양 화살·삼지창·낚싯바늘의 기본 파티클 윤곽도 별도 3개 영상·111프레임으로 확인했습니다. 자동 검사는 전체에 적용했고, 대표 투척·진형·돌진·날개·파티클 장면은 원본 화면을 수동 확인했습니다. 모든 능력 조합·GPU·구버전 클라이언트의 시각 결과를 보장하는 검사는 아닙니다.

전체 빌드와 단위 테스트 127개, 최종 JAR의 Paper 26.3 및 1.12.2 능력 회귀 검사를 통과했습니다. 서버 로그는 `.paper-smoke/ability-variety-26.3-4kg9rt83/logs/latest.log`와 `.paper-smoke/ability-variety-1.12.2-th9iziie/logs/latest.log`에 있습니다. 최종 화면·좌표 기록과 궁니르 원본 시간 간격 GIF는 `.build/minecraft-mcp/evidence/all-motion-final-20261011/`, 기본 파티클 기록은 `.build/minecraft-mcp/evidence/all-motion-fallback-20261011/`에 있습니다. 팩 SHA-1은 `b2d524b99e694910f993b7633033aa7d230696d9`로 유지합니다. 테스트 클라이언트와 격리 서버는 종료했습니다.

```powershell
$env:NGW_EVIDENCE='all-motion-audit-20261011'
$env:NGW_REQUIRE_ART='1'
$env:NGW_RECORD_CASTS='1'
node scripts/minecraft-mcp/suite.mjs casts
# 수정 후 최종본: 다른 경로에 기록
$env:NGW_EVIDENCE='all-motion-final-20261011'
node scripts/minecraft-mcp/animations.mjs directional
node scripts/minecraft-mcp/animations.mjs push
node scripts/minecraft-mcp/animations.mjs spear
node scripts/minecraft-mcp/animations.mjs motion
node scripts/minecraft-mcp/animations.mjs models
node scripts/minecraft-mcp/animations.mjs flight
$env:NGW_EVIDENCE='all-motion-fallback-20261011'
node scripts/minecraft-mcp/animations.mjs outlines design.SOLAR_ARROW design.TRIDENT design.FISH_HOOK
```

## 묠니르 자세를 유지하는 회수 (2026-10-11)

회수 시 이동 방향에 맞춰 망치를 뒤집던 동작을 제거했습니다. 머리가 앞서는 투척 자세를 그대로 유지하면서 손잡이부터 돌아옵니다. 시전자 이동·시점 변경은 회수 위치에만 반영하며 yaw/pitch는 바꾸지 않습니다. 금속 머리의 중심은 현재 손보다 투척 방향으로 0.65블록 앞에서 멈춥니다. 출발 위치도 같은 손잡이 기준을 사용합니다. 피해·적중 시점·전하 지급 시점은 유지했습니다.

전체 빌드와 테스트 123개를 통과했습니다. 실제 Minecraft 26.3의 옆 시점·1인칭·이동·고저차·근거리·애니메이션 끄기·파티클 대체·도중 종료 8개 조건을 원본 497프레임으로 기록했습니다. 서버 좌표 검사는 투척과 회수 양쪽의 관측, 투척 자세 보존, 손잡이 우선 회수, 좌표 점프와 종료 정리를 확인합니다. 옆 시점·1인칭·고저차 대표 프레임을 수동 확인했고, 실제 일반/고급 기술도 모두 발동했습니다. 이동 조건은 서버의 작은 위치 이동으로 재현합니다.

원본 PNG, 검사 JSON, 원본 시간 간격의 GIF는 `.build/minecraft-mcp/evidence/thor-grip-first-20261011/`에 있습니다. 당시 JAR SHA-256은 `3d3c2d152f9e428376350910fad25ea8199ce67672f3fe25b792bdc3faea1b8b`입니다. 팩은 기존 `b2d524b99e694910f993b7633033aa7d230696d9`를 사용하며 다시 만들 필요가 없습니다. 테스트 클라이언트와 서버는 종료했습니다. 아래의 회수 방향 영상과 판정은 이 검사로 대체됩니다. 도중 종료 조건은 위의 전체 점검에서 실제 능력 재지정으로 다시 확인했습니다.

```powershell
$env:NGW_EVIDENCE='thor-grip-first-20261011'
$env:NGW_REQUIRE_ART='1'
node scripts/minecraft-mcp/animations.mjs motion
node scripts/minecraft-mcp/suite.mjs casts thor
```

## 이전 묠니르 비행 자세 검사 (2026-10-11)

사용자 피드백에 따라 T자로 세워 이동시키던 자세를 수정했습니다. 옆에서 `ㅓ`처럼 보이도록 머리의 긴 축은 비행 방향과 직각으로, 손잡이는 뒤쪽으로 배치합니다. 팩 모델은 X축·Z축 회전을 조합하고 머리 중심을 비행 기준점에 맞췄습니다. 팩 미사용 오브젝트와 구버전 파티클 윤곽도 같은 형태를 사용합니다. 수평뿐 아니라 고저차가 있는 실제 이동 벡터에 yaw/pitch를 맞추며, 대상의 시선은 사용하지 않습니다.

기존의 '세워진 자세/pitch 0이면 통과'라는 잘못된 검사 기준을 제거했습니다. 머리·손잡이 축의 정렬과 기본 모델의 앞뒤 관계, 고저차·수직 경로를 검사합니다. 전체 빌드와 테스트 122개, 실제 Minecraft 26.3의 옆 시점·1인칭·이동·고저차·근거리·애니메이션 끄기·파티클 대체·도중 종료 8개 조건(497프레임)을 통과했습니다. 옆 시점의 원본 프레임으로 머리가 앞서고 손잡이가 뒤로 뻗은 모습과 회수 방향을 직접 확인했습니다. 원본과 새 GIF는 `.build/minecraft-mcp/evidence/thor-head-first-20261011/`에 있습니다.

Paper 26.3과 1.12.2 서버 회귀 검사도 통과했습니다. 로그는 `.paper-smoke/ability-variety-26.3-z_2vldy6/`와 `.paper-smoke/ability-variety-1.12.2-g7uv_k36/`에 있습니다. 테스트 클라이언트와 서버는 종료했습니다.

당시 JAR SHA-256은 `40c18ce8bdbb8c270d2d432eba77aa717291acfd016cf7c93570b3b8e1574c4b`입니다. 이 검사본은 회수할 때 머리가 시전자를 향해 뒤집히므로 위의 자세 유지 회수로 대체됩니다.

## 이전 움직임·투척 검사 기록 (2026-10-11)

묠니르의 기본 90도 눕힘과 드릴 형태 회전을 제거하고 작은 흔들림만 남겼습니다. 투척 위치의 pitch는 0으로 유지하고 yaw는 실제 수평 진행 방향에서 구하므로 대상의 시선이 회수 자세에 섞이지 않습니다. 오른손 앞에서 출발하며, 회수할 때 매 단계 현재 위치에서 움직인 손 위치를 향합니다. 22틱에 마지막 손 위치를 보내고 24틱에 제거하여 클라이언트가 마지막 2틱 이동을 보간할 수 있게 했습니다. 피해 5·충돌 판정 시점 12틱·전하 지급 24틱은 유지합니다.

궁니르의 불필요한 축 회전을 제거하고 창끝을 충돌 기준점에 맞췄습니다. 삼지창은 진행 방향을 향하도록 pitch 부호를 고쳤습니다. 낚싯바늘은 거리와 크기 인자가 뒤바뀌어 확대·축소되던 것을 실제 전진·회수로 수정했습니다. 카메라를 향하는 불꽃·김·잔상은 위치와 회전을 분리하여 카메라를 돌려도 원래 물체에서 이탈하지 않게 했습니다. 파츠가 로드되지 않은 청크에 걸리면 해당 장면만 정리하고 다음 장면은 복구합니다.

전체 빌드와 단위 테스트 121개가 통과했습니다. 실제 Minecraft 26.3에서 묠니르의 옆 시점·1인칭·이동 중 회수·고저차·근거리·애니메이션 끄기·파티클 대체·도중 종료 8개 조건을 원본 497프레임으로 기록했습니다. 원본 화면과 틱별 서버 좌표에서 망치의 세운 자세, 비정상 좌표 점프와 회수 후 잔류 여부를 확인했습니다. 이동 조건은 서버의 작은 위치 이동으로 재현합니다. 청크 경계 초기 생성/갱신 실패 후 복구·미로딩·Display 정리도 통과했습니다.

전체 150개 모델의 등장·동작·소멸 5,538프레임과 주작·헤르메스 실제 비행 161프레임을 추가 기록하여 총 160개 영상·6,196프레임을 검증했습니다. 모든 영상의 시간 간격·프레임 수와 모델 소멸을 자동 검사하고, 망치·삼지창·갈고리·단조·김·분사·날개 등 대표 장면은 원본 화면을 수동 확인했습니다. 최저 캡처 속도는 18.89fps이며 이는 게임 자체의 렌더링 FPS 측정값이 아닙니다. 실제 일반/고급 12개 발동(토르·오딘·포세이돈·무적·아테나·대장장이)과 노인과바다 패시브 경로도 통과했습니다. 모든 능력 조합이나 모든 GPU·클라이언트 버전의 시각 결과를 보장하는 검사는 아닙니다.

Paper 26.3과 1.12.2 능력 회귀 검사도 통과했습니다. 각 서버에서 93개 스타일·186개 시전 피드백과 파티클 대체, 시점/범위/설정 변경, 사망·접속 종료 정리를 확인합니다. 로그는 `.paper-smoke/ability-variety-26.3-wnsqi_84/`와 `.paper-smoke/ability-variety-1.12.2-royuq9vr/`에 있습니다. 검사용 클라이언트·서버는 종료했습니다. 로컬 OSHI/Perflib 및 오프라인 테스트 계정 인증 경고와 실제 회귀 실패는 구분합니다.

이전 검사본 JAR SHA-256은 `73efa91539884816d3aebd984f0b9d345bdc1594c603468fb1ad21c893938916`이며 팩은 `b2d524b99e694910f993b7633033aa7d230696d9`입니다. 당시 원본 기록은 `.build/minecraft-mcp/evidence/motion-verified-20261011/`에 있습니다. 아래 입체화 최초 검사와 부하 측정의 JAR·수치는 추가 동작 수정 전 기록입니다.

```powershell
$env:NGW_EVIDENCE='motion-verified-20261011'
$env:NGW_REQUIRE_ART='1'
node scripts/minecraft-mcp/animations.mjs motion
node scripts/minecraft-mcp/animations.mjs models
node scripts/minecraft-mcp/animations.mjs flight
node scripts/minecraft-mcp/suite.mjs casts thor odin poseidon invincibility athena fisher blacksmith
```

## 입체 리소스와 렌더러 최적화 검사 (2026-10-11)

렌더러가 같은 위치·변환·아이템을 반복 반영하지 않도록 수정했습니다. 같은 애니메이션 시점/강도의 모양은 재사용하지만, 관전자 거리·월드·접속 상태와 설정 변경은 계속 평가합니다. 기본 장면과 팩 장면이 같은 업데이트에 만료될 때 제거된 장면을 다시 접근하던 오류도 수정했습니다.

묠니르·궁니르·삼지창·단검·방패의 5가지 입체 형상을 10개 연출에 연결했습니다. 한 형상은 12~18개 직육면체로 구성하고, 묠니르·궁니르의 팩 장면은 Display 6개에서 5개로 줄였습니다. 기존 모델 ID 1,957개는 유지합니다. 미사용 평면 PNG 40개를 없애고 작은 재질 아틀라스로 대체했습니다. 실제 게임에서 확인한 아이템 렌더러의 추가 Y축 180도 회전을 반영하여 방패 장식이 바깥을 향하도록 보정합니다.

JDK 21 전체 빌드와 단위 테스트 110개가 통과했습니다. Paper 1.12.2와 26.3의 능력 회귀 검사에서 구버전 파티클 대체, 실제 Display 생성·재사용·관객 필터·설정 변경·예산·만료·사망/접속 종료 정리를 통과했습니다. `verify_versions.py`, `verify_solid_art.py`, `verify_pack.py`, `verify_gui_pack.py`, `verify_catalogue.py`로 54개 버전 목록, 기존/신형 아이템 모델 참조, 형상 범위·UV·텍스처·SHA-1과 JAR 메타데이터를 검사합니다. 구버전 실제 클라이언트 화면 전체를 검사한 결과는 아닙니다.

```powershell
./gradlew.bat --offline resourcePack
# stage 이전에 기존 ID와 새로 생성된 ID가 동일한지 확인
python scripts/effect-art/verify_solid_art.py
python scripts/effect-art/stage.py
./gradlew.bat --offline build :plugin:visualRegressionJar :plugin:loadRegressionJar
python scripts/effect-art/verify_versions.py
python scripts/effect-art/verify_catalogue.py
python scripts/Test-Abilities.py 1.12.2
python scripts/Test-Abilities.py 26.3
```

Minecraft 26.3 숨김 클라이언트에서 최종 팩 `b2d524b99e694910f993b7633033aa7d230696d9`의 10개 모델을 정면·1인칭·위쪽·최소 파티클·강제 파티클 대체의 50개 조건으로 촬영하고 소멸을 확인했습니다. 묠니르·삼지창·프로테고의 원본 애니메이션 3개(108프레임)도 기록했습니다. 삼지창의 입체감과 1인칭 시야, 방패 외부의 장식·내부의 손잡이를 수동으로 확인했으며 모델/텍스처 누락 로그는 없었습니다. 캡처 전체에 자동 검사를 적용했고 수동 시각 검토는 대표 장면을 대상으로 했습니다.

최종 원본 PNG·프레임·검사 JSON·JAR/팩 해시는 `.build/minecraft-mcp/evidence/solid-art-verified-20261011/`에 있습니다. 제작 미리보기 `build/effect-pack/solid-models.png`는 실제 게임 캡처와 구분합니다. 앞선 `solid-art-final-20261011/`의 방패 사진은 아이템 렌더러 회전 보정 전 기록입니다. 해당 폴더의 토르·오딘·포세이돈·어쌔신·무적·아테나 일반/고급 12개 발동 검사는 런타임 연결을 확인한 별도 기록입니다. 게임 소리·셰이더·다른 GPU와 모든 구버전 클라이언트는 이번 시각 검사 범위에 포함되지 않습니다.

동일한 `python scripts/Test-Load.py --players 16,32 --seconds 20`으로 변경 전과 최종 JAR를 비교했습니다. 최종 측정은 화면 검사·빌드·다른 테스트 서버를 모두 종료한 뒤 진행했습니다. Paper 26.3, 힙 최대 3GiB, 시야/시뮬레이션 거리 4, 8종 능력, 단계별 10초 준비와 20초 측정 조건입니다.

| 조건 | 변경 전 틱 p95 | 변경 후 틱 p95 | 변경 후 TPS | 변경 후 명령 왕복 p95 |
| --- | ---: | ---: | ---: | ---: |
| 16명 일반 | 7.23ms | 6.88ms | 20.00 | 7.70ms |
| 32명 일반 | 7.84ms | 7.88ms | 20.00 | 10.74ms |
| 32명 우르프 | 8.80ms | 8.01ms | 20.00 | 12.26ms |

세 단계 모두 성능 기준과 종료·접속 해제·재접속 후 정리 검사를 통과했고 서버/클라이언트 오류는 0건입니다. 짧은 단일 측정이고 전투 경로에 변동이 있어 큰 성능 향상이나 운영 서버 수용 인원을 증명하지는 않습니다. TCP 클라이언트는 리소스팩을 렌더링하지 않으므로 이 수치는 새 모델의 GPU 성능 측정도 아닙니다. 리소스팩 ZIP은 36,072,061바이트에서 35,538,202바이트로 줄었으며, 실제 GPU 아틀라스 메모리 감소를 의미하지는 않습니다.

원본 결과는 `.paper-smoke/load-26.3-zrl7ou_h/results.json`(변경 전)과 `.paper-smoke/load-26.3-y4ijr5ug/results.json`(최종)에 있습니다. 중간 측정 `load-26.3-gb9khaza`는 다른 검사가 겹쳐 비교표에서 제외했습니다. 최종 JAR SHA-256은 `52fe9eebc1fc9b046c6a717f3935d5b4e3b502f2574599c80a02167c2acf16c1`입니다. 모든 테스트 프로세스는 종료했습니다. 운영에서 `url: auto`를 사용할 때는 새 ZIP을 저장소에 게시한 뒤 해당 JAR를 적용해야 합니다.

## 저장소 resoucepack 디렉토리 검사

버전별 ZIP은 같은 저장소의 `resoucepack/` 디렉토리에 보관합니다. `clean build`는 팩을 재생성하지 않습니다. `python scripts/effect-art/verify_catalogue.py --plugin-only`는 ZIP 25종의 SHA-1과 버전 목록을 확인하고, 배포 JAR에 ZIP·텍스처가 포함되지 않았는지도 검사합니다.

팩을 실제 수정할 때만 `./gradlew resourcePack`과 `python scripts/effect-art/stage.py`로 디렉토리와 해시 목록을 함께 갱신합니다. 별도 배포 브랜치와 릴리즈 첨부는 사용하지 않습니다.

## 버전별 리소스팩 배포 검사 (2026-10-03)

JDK 21 빌드와 단위 테스트 76개, `verify_versions.py`의 ZIP 25종·54개 버전·1,583개 모델 ID·텍스처/모델 참조·기본 모델 복귀·SHA-1·배포 JAR 목록 일치 검사를 통과했습니다. 기존 음식·메뉴 팩 검사 및 actionlint도 통과했습니다. Paper 1.12.2·1.21.1·1.21.5·26.3에서 메뉴·인벤토리·코어 회귀 검사를 통과했습니다.

```powershell
./gradlew.bat clean build --offline
./gradlew.bat resourcePack --offline
python scripts/effect-art/verify_versions.py
python scripts/effect-art/verify_pack.py
python scripts/effect-art/verify_gui_pack.py
python scripts/Test-Inventory.py 1.12.2
python scripts/Test-Inventory.py 1.21.1
python scripts/Test-Inventory.py 1.21.5
python scripts/Test-Inventory.py 26.3
```

구버전 메뉴는 팩 미수신 시 원래 아이템 유지, 수신 후 PAPER/CustomModelData 전환, 재표시, 비활성화 후 원래 재질 복원, 외부 보상 모델 보존을 검사합니다. 최신 Paper는 Bukkit API 문자열과 Minecraft 버전이 다르므로 실제 서버 버전을 조회하며, 지원 서버가 팩 목록을 선택하지 못하면 검사에 실패합니다.

버전별 ZIP 구조와 서버 코드의 회귀 검사이며, 모든 버전의 실제 클라이언트 화면을 검증한 것은 아닙니다. 이전 26.3 화면 검증 기록은 아래에 별도로 남깁니다. [배포 파일·호환 범위](wiki/resource-packs.md)를 참고하세요.

## 메뉴와 리소스팩 검사 (2026-10-03)

`./gradlew build :plugin:visualRegressionJar --offline`과 전체 단위 테스트를 통과했습니다. `scripts/effect-art/verify_gui_pack.py`는 서버 아이콘 31종과 팩의 item/model/texture 참조, 97.1 형식, 아틀라스 중복, ZIP 항목과 SHA-1을 검사합니다.

```powershell
python scripts/effect-art/verify_gui_pack.py
python scripts/Test-Inventory.py 26.3
python scripts/Test-Commands.py 26.3
python scripts/Test-Inventory.py 1.12.2
python scripts/Test-Commands.py 1.12.2
```

위 서버 검사는 캐시된 Paper와 승인된 EULA를 읽어 별도 임시 월드에서 실행하며 모두 통과했습니다. 실제 CraftPlayer 인벤토리 이벤트로 도박 좌클릭 비용·결과 갱신, 우클릭/Shift/숫자키 차단, 미리보기·드래그 보호, 23개 보상의 21+2 페이지, 메뉴 팩 수신 여부와 비활성화에 따른 모델 대체, 외부 보상 모델 보존을 확인합니다. 명령 검사는 설정 화면 12곳의 직접 이동, 한글·영문 별칭, 잘못된 입력과 권한을 확인합니다.

26.3 / Fabric 0.19.5 / 960×540 실제 클라이언트에서도 팩 다운로드 성공, 홈→전투→홈 이동, 우르프·게임룰·팩 연결 안내의 한글 툴팁, 도박 확률·비용·당첨 결과와 조약돌 64→32 차감, 메뉴 텍스처 비활성화를 확인했습니다. 원본 캡처와 검사 해시는 `build/gui-preview/`에 있습니다. 이 검사는 쉐이더 조합이나 다른 버전의 실제 클라이언트 화면까지 검증한 결과는 아닙니다.

재현은 `Test-ClientVisuals.py run --hidden --art-pack`으로 전용 클라이언트를 시작하고 해당 팩의 수신 성공을 확인한 뒤 `node scripts/minecraft-mcp/scenario.mjs scripts/minecraft-mcp/menu-views.json`을 실행합니다. `menu_hover`와 `menu_click`은 게임 내부 슬롯 좌표·네트워크 클릭만 사용하며 OS 마우스를 움직이지 않습니다. 이미 실행 중인 시각 검사 세션에서는 동시에 실행하지 마세요.

## 기본 검사

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
.\scripts\Test-PaperMatrix.ps1 -SkipBuild -PluginJar .\build\libs\NewGodWar-0.3.8.jar -Versions 26.3
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

능력 표시 검사에는 네 방향 × 위/정면/아래 시선, 이동 중 날개, 저울 수평 유지, 오브젝트 예산·설정의 중간 변경, 32블록 이탈·복귀, 사망 시 정리를 포함합니다. 2026-10-02에 Paper 1.12.2·1.21.1·26.3에서 통과했습니다. 실제 서버 엔티티와 기록용 관전자 객체를 사용하는 검사이며, 클라이언트의 텍스처·셰이더·투명도·1인칭 가림을 시각적으로 검증한 결과는 아닙니다.

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

## 실제 클라이언트 화면 검사

실제 Minecraft 클라이언트의 화면을 검사하려면 [별도 MCP 테스트 환경](../scripts/minecraft-mcp/README.md)을 사용합니다. `Test-ClientVisuals.py prepare`와 `preflight`는 게임을 실행하지 않습니다. `run --hidden`은 표시·포커스·마우스 잡기가 차단된 SDL 창에서 실제 게임을 렌더링하고 MCP로 내부 프레임을 캡처합니다. 기존 서버 회귀 검사와 MCP 연결 검사만 통과한 상태를 화면 검증 완료로 간주하지 않습니다.

2026-10-02에는 Minecraft 26.3 숨김 클라이언트에서 93개 능력 전체의 일반/고급 사용 경로를 순회했습니다. 발동 137건, 준비 1건, 정보 표시 1건을 확인했으며 기술이 없는 47개 경로는 해당 없음으로 기록했습니다. 패시브와 특수 발동 조건에는 별도 서버 이벤트 도구를 사용했습니다. 97개 모델은 정면·1인칭·위쪽 시점·파티클 최소·강제 파티클 대체의 5가지 조건에서 촬영하고 생성 개수와 소멸을 검사했습니다. 자동 검사와 선별적인 수동 시각 검토는 구분합니다.

무적의 장갑판에 이어 8개 모델의 1인칭 시야 가림을 수정해 재촬영했고, 하데스의 고정 Y=-2 이동을 월드 최저 높이-2로 수정한 뒤 실제 좌표를 확인했습니다. 빌드와 Paper 26.3·1.12.2 능력 회귀 검사도 통과했습니다. [검사 도구 설명](../scripts/minecraft-mcp/README.md)에 재현 명령과 한계를 기록했습니다. 결과 보고서·JSON·원본 PNG는 `.build/minecraft-mcp/evidence/all-abilities/`에 있습니다. 다른 버전의 실제 클라이언트, 모든 확률 분기·다인전 조합, 다른 플레이어의 관전·거리 경계, 리소스팩·셰이더·소리는 이번 화면 검사 범위에 포함하지 않았습니다.

2026-10-03 GUI 점검은 Minecraft/Paper 26.3의 별도 숨김 클라이언트에서 통합 팩 `d5e87b24`를 로드해 진행했습니다. 설정 홈과 하위 설정 11개, 능력 목록·상세·미배정 정보, 뽑기·잔액 부족·보상 목록을 촬영했습니다. 분류 이름 부분 클릭, 뽑기 버튼의 아래쪽 클릭, 64→32 조약돌 차감, 부족할 때 무차감, 보상 목록 왕복 시 결과 유지, 메뉴 팩 표시 끄기/복원과 도움말 툴팁을 확인했습니다. 화면/연출 메뉴의 누락된 설정 슬롯도 이 과정에서 수정했습니다.

홈 분류와 뽑기 버튼의 마우스 진입 전후 메뉴 영역은 픽셀 차이가 0개였습니다. 잔액 부족 상태의 클릭 전후, 뽑기 직후와 보상 목록에서 돌아온 화면도 동일했습니다. 폰트 아틀라스 크기 제한 때문에 배경을 4개 글리프로 나눠 한 장으로 합성하며, 팩 검사에서 각 조각의 크기·좌표 복귀·투명 강조 스프라이트를 검사합니다. 전체 빌드와 단위 테스트 98개, 팩 자산·카탈로그 검사가 통과했습니다. 구버전 실제 클라이언트와 여러 GUI 배율은 이번 화면 검사 범위에 포함하지 않았습니다. 원본 PNG·시나리오·결과는 `.build/gui-live/evidence/ui-final/`과 `ui-verification/`에 저장했습니다.

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
4. 테스트가 성공하면 해당 태그의 GitHub Release를 만들고 `NewGodWar-*.jar`만 첨부합니다. 리소스팩은 같은 저장소의 `resoucepack/`에 저장하며 릴리즈에서 생성·첨부하지 않습니다. 릴리즈 본문은 `docs/releases/<태그>.md`를 사용합니다.

전체 Paper 매트릭스는 별도 호환성 신호로 계속 실행하지만, 구버전 개별 실패가 릴리즈 생성을 막지는 않습니다.

예시는 다음과 같습니다.

```bash
git tag v0.1.0
git push origin v0.1.0
```

GitHub Actions 화면에서 `Release` 워크플로를 수동 실행하고 태그를 입력해도 같은 릴리즈 작업을 다시 실행할 수 있습니다.

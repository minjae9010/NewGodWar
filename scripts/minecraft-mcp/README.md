# 실제 클라이언트 화면 검사

Minecraft Mod MCP 0.4.1의 Fabric 모드와 도구 스키마를 이용한다. `bridge.mjs`는 전용 포트와 실행기가 기록한 PID가 일치하는 클라이언트에만 연결한다. 게임·런처를 자동 실행하거나 다른 포트를 탐색하지 않는다. 화면 캡처는 원본 PNG를 MCP 이미지와 `.build/minecraft-mcp/captures/` 파일로 반환한다.

## 준비

Node.js 20 이상, Java 25, 설치된 Minecraft Java 26.3, `.paper-smoke/26.3`의 기존 Paper 캐시와 동의된 EULA, 빌드된 NewGodWar JAR가 필요하다.

```powershell
npm install --prefix .build/minecraft-mcp/bridge --cache .build/npm-cache --ignore-scripts --no-audit --no-fund --save-exact minecraft-mod-mcp@0.4.1
python scripts/Test-ClientVisuals.py prepare --minecraft-dir 'C:\Users\사용자\AppData\Roaming\.minecraft' --java 'C:\Java25\bin\java.exe'
node scripts/minecraft-mcp/check.mjs
python scripts/Test-ClientVisuals.py preflight
```

준비는 게임을 실행하지 않는다. Fabric Loader 0.19.5와 MCP 모드를 버전 고정하고 다운로드 해시를 검사한다. 기존 게임에서 클라이언트 JAR·라이브러리·에셋만 읽으며 계정·로그인 정보·개인 월드는 사용하지 않는다. 게임 설정, 모드, 서버, 로그와 캡처는 `.build/minecraft-mcp/`에 저장한다. 서버는 `127.0.0.1:25576`, MCP는 `127.0.0.1:19876`을 사용한다. 로컬 테스트 플레이어는 `NGWVisualTest`이다.

상류 0.4.1의 `McpHttpServer`는 문서와 달리 `0.0.0.0`에 바인딩한다. 준비 스크립트는 SHA-256을 확인한 원본을 `downloads/`에 보관하고, 해당 클래스의 문자열 상수 하나만 `127.0.0.1`로 바꾼 `-localhost.jar`를 테스트 클라이언트에 설치한다. 이 단계에서는 변경 항목·ZIP CRC·원본/수정본 해시를 검사한다. 버전이 바뀌거나 상수가 달라지면 자동 적용을 거부한다.

실행 시에는 `PrepareClientActions.java`가 `ReflectionHelper.sendCommand` 하나를 프로젝트의 `TestClientActions`로 교체한다. 상류의 잘못된 `Connection` 필드 탐색 대신 실제 26.3의 `getConnection().sendCommand()`를 사용한다. 카메라·좌/우클릭·HUD·파티클 설정은 Minecraft 메인 스레드의 내부 메서드로 처리하며 OS 입력을 보내지 않는다. 수정본의 해시는 실행 구성에 갱신한다. 상류 스크린샷은 최대 2초 전 캐시를 반환하므로 매 캡처 전에 캐시를 지워 짧은 효과를 놓치지 않게 한다.

## 실행과 검사

`--hidden`은 실제 게임을 렌더링하면서 창 표시와 포커스 요청을 차단한다. GPU는 사용한다. 테스트용 SDL 라이브러리 복사본에 `HIDDEN`·`NOT_FOCUSABLE`을 적용하고 표시·창 올리기·전체화면·마우스 잡기·커서 이동을 차단한다. 작은 숨김 SDL 창에 표시·포커스 요청을 보내도 계속 숨겨져 있는지 먼저 검사하며, 검사 실패 시 게임을 실행하지 않는다. 개인 Minecraft 설치의 JAR는 수정하지 않는다.

```powershell
python scripts/Test-ClientVisuals.py run --hidden
# 별도 터미널에서 종료 요청 (이 실행기가 시작한 프로세스만 정리)
python scripts/Test-ClientVisuals.py stop
```

실행기는 현재 빌드 JAR를 전용 서버로 복사하고 서버 준비 후 클라이언트를 시작한다. `stop` 또는 `Ctrl+C`로 종료한다. 창을 직접 보려는 경우에만 `--allow-client-window`를 사용한다. MCP `test_status`에서 전용 클라이언트 연결을 확인하고 `enter_control_mode`로 게임 내 자동화 모드에 진입한다. MCP 설정을 새로 읽는 세션에서 `newgodwar_visual_test` 도구를 사용할 수 있다. 재시작 전 세션에서는 아래 CLI도 같은 MCP 전송을 사용한다.

```powershell
node scripts/minecraft-mcp/call.mjs test_status
node scripts/minecraft-mcp/scenario.mjs scripts/minecraft-mcp/guard-views.json
```

시나리오는 원본 PNG, 실행 단계, 명령 결과, 능력 사용 채팅 확인 여부를 `.build/minecraft-mcp/evidence/<name>/`에 저장한다. 같은 이름을 재실행하면 해당 증거 파일을 갱신한다. 명령 전송 성공만으로 능력 발동을 판정하지 않으며 `expectChat`이 있는 단계는 실제 성공 메시지가 도착해야 진행한다.

1. `get_player_info` / `get_world_info` / `screenshot`으로 접속과 첫 화면을 확인한다.
2. `execute_command`로 `/gw test zeus`, `/gw dummy`, `/gw ability cooldown reset self`를 실행한다. 능력을 바꿀 때는 `/gw stop` 후 다음 `/gw test <능력>`을 시작한다. 블레이즈 막대기와 **조약돌(`minecraft:cobblestone`)**을 지급한다. 일반 돌은 능력 재료가 아니다.
3. `set_view_angle`로 정면·위·아래와 네 방향을 바꾸고, 능력 사용 전후 화면을 촬영한다. `client_action`의 `attack`/`use`는 실제 게임의 좌/우클릭 동작, `camera`의 `first`/`back`/`front`는 시점 전환이다. `hud`는 `true`/`false`, `particles`는 `ALL`/`MINIMAL` 값을 받는다. `use_item`도 내부 우클릭을 사용한다. 각 능력의 발동 조건과 비용, 클라이언트 자체 클릭 간격을 확인한다.
4. 1인칭·3인칭, 가까운 거리·32블록 경계, 파티클 설정, 리소스팩, 셰이더를 각각 비교한다. 기본 프로필은 리소스팩·셰이더 없이 시작하므로 추가 조합은 별도로 준비해야 한다.
5. 짧은 이펙트는 단일 스크린샷으로 놓칠 수 있다. 발동과 캡처를 같은 시나리오로 반복하고 실제 프레임 증거가 없는 항목은 미검증으로 남긴다.

`check.mjs`는 연결 프로토콜·도구 목록·다른 프로세스 거부·클라이언트가 꺼진 상태의 처리만 검사한다. `preflight`는 파일과 실행 구성을 검사한다. **둘 다 실제 렌더링 검증 결과가 아니다.**

## 실제 화면 확인 기록 (2026-10-02)

Minecraft 26.3 / Fabric 0.19.5 / 960×540 / 기본 리소스 / 셰이더 없음 / 음소거로 확인했다. 캡처 우상단 회색 사각형은 MCP 제어 모드 버튼이며 능력 효과가 아니다.

| 대상 | 확인 내용 | 로컬 증거 폴더 |
| --- | --- | --- |
| 무적 | 1인칭 중앙을 가리던 금색 고리를 몸 양옆 6장으로 수정. 네 방향·상하 시점·정면/후면, 최소 파티클 설정과 소멸 확인 | `evidence/guard-verified` |
| 무적 대체 효과 | 오브젝트를 끈 상태에서 양옆 파티클 형태와 1인칭 시야·소멸 확인 | `evidence/guard-fallback` |
| 크로노스 | 발밑 시계판, 큰 시간장, 시간에 따른 바늘 변화와 소멸 | `evidence/distinct-models` |
| 니케 | 실제 돌진 시 날개 생성, 후면/1인칭과 소멸 | `evidence/distinct-models` |
| 제우스 | 실제 낙뢰와 짧은 효과의 발동/소멸 | `evidence/distinct-models` |

전체 경로의 기준은 `.build/minecraft-mcp/`이다. 위의 첫 검사 범위와 플러그인 해시는 `evidence/visual-review.json`에 저장했다. 초기 `guard-front-active` 캡처는 캐시 수정 전이며, `guard-fixed`의 일부 단계는 클라이언트 클릭 간격 때문에 발동하지 않아 최종 판정에는 `guard-verified`를 사용한다.

## 전체 능력 재검증 (2026-10-02)

```powershell
.\gradlew.bat --offline build :plugin:visualRegressionJar
python scripts/Test-ClientVisuals.py run --hidden --visual-probe
# 서버와 전용 클라이언트의 접속 완료 후 별도 터미널에서 실행
node scripts/minecraft-mcp/suite.mjs models
node scripts/minecraft-mcp/suite.mjs casts
node scripts/minecraft-mcp/suite.mjs passives creeper
node scripts/minecraft-mcp/report.mjs
python scripts/Test-ClientVisuals.py stop
```

`VisualRegressionProbe`는 배포 플러그인에 포함되지 않는 별도 테스트 JAR이다. 격리 서버 주소·포트와 OP 테스트 계정을 확인한 뒤에만 명령을 처리한다. `/ngwvisual prepare`는 이 테스트 월드의 엔티티·인벤토리·주변 지형을 초기화한다. `--visual-probe` 없이 실행하면 이전 테스트 JAR도 제거한다.

결과는 `evidence/all-abilities/index.html`과 JSON·원본 PNG에 저장한다. `models`는 97개 모델의 정면·1인칭·위쪽 시점·파티클 최소·강제 파티클 대체를 촬영하며 Display 개수와 소멸을 검사한다. 모델을 직접 재생하는 검사는 능력 발동 검사와 구분한다. `casts`는 93개 능력의 일반/고급 사용을 순회하며 성공 채팅 또는 실제 조약돌 차감을 확인한다. 이번 결과는 발동 137건, 준비 동작 1건, 정보 표시 1건, 해당 기술 없음 47건이다. 크리퍼처럼 사용 즉시 사망하는 능력의 패시브는 사망 전에 별도로 검사한다.

일반 좌·우클릭은 실제 클라이언트 입력 경로를 사용한다. 주문, 팻말, 저격, 처치·피격 등의 특수 조건과 패시브는 서버 이벤트 도구로 재현하므로 모든 항목을 실제 사용자 네트워크 입력 검사로 표현하면 안 된다. 피해 면역·배율, 낚시·채굴 보상, 허기, 이동·정답에 따른 능력 변경, 비용 할인 등에는 상태 단언이 포함된다. 모든 확률 분기나 모든 능력 조합을 검증하는 도구는 아니다.

하데스가 최신 월드에서 고정 Y=-2로 올라가는 오류를 발견해 월드 최저 높이-2로 수정하고 실제 좌표를 재확인했다. 약초 지팡이·대지 꽃·석류·난초·로키 가면·위장 가면·레코드·부두 인형 8개 모델의 1인칭 시야 가림도 어깨 옆/머리 위 배치로 수정했다. 변경 모델은 5가지 표시 조건으로 다시 촬영했으며 수정 전후 PNG를 함께 보관한다. 빌드와 Paper 26.3 및 1.12.2 서버 능력 회귀 검사도 통과했다.

모든 모델에 자동 생성·소멸 검사와 화면 증거가 있지만 수동 시각 검토는 선별적으로 진행했다. 다른 버전·GPU의 실제 클라이언트, 다른 플레이어의 관전·거리 경계, 셰이더·리소스팩·소리 조합은 별도 확인이 필요하다. 개별 재검사는 `suite.mjs models <모델 ID...>` 또는 `casts <능력 ID...>`로 실행한다. 원본 결과를 보존하고 최신 재검사 파일을 보고서에 병합한다.

자료: [Minecraft Mod MCP](https://github.com/langyo/minecraft-mod-mcp), [공식 MCP 설정 문서](https://learn.chatgpt.com/docs/extend/mcp?surface=cli).

## 애니메이션 재생 (2026-10-03)

### 전용 아트 팩 검증

`scripts/effect-art/BuildEffectPack.java`로 팩을 만든 뒤 `run --hidden --visual-probe --art-pack`으로 실행한다. 테스트 러너는 127.0.0.1:19877에서 이 ZIP 하나만 제공하고 종료 시 서버를 닫고 원래 테스트 설정을 복원한다. 실제 클라이언트가 팩 확인 화면을 표시하면 `client_action`의 `pack / accept`를 사용한다. 이는 NewGodWar라는 이름이 포함된 게임 내부 확인 화면의 콜백만 호출하며 OS 입력을 보내지 않는다. 적용 성공은 `/ngwvisual state`의 `artPackLoaded`로 확인한다. 이전 폭발 테스트에서 사망한 채 저장되었으면 `respawn` 동작 후 `prepare`로 지형을 복구한다.

```powershell
$env:NGW_EVIDENCE='art-effects'
$env:NGW_REQUIRE_ART='1'
node scripts/minecraft-mcp/suite.mjs models
node scripts/minecraft-mcp/animations.mjs models
node scripts/minecraft-mcp/animations.mjs casts
node scripts/minecraft-mcp/art-report.mjs
```

이 환경변수는 기존 기본 리소스 검증 파일을 보존하고, 서버가 팩 수신 성공을 확인하지 않으면 촬영을 거부한다. 실제 시점 검증은 정면·1인칭·위쪽·파티클 최소·강제 파티클 대체를 포함한다. 전용 진형과 지면 문양의 영상은 잘 보이는 위쪽 시점으로 촬영한다. JSON의 플러그인/팩 해시와 원본 타임스탬프로 촬영 버전을 확인할 수 있다. 팩은 26.3의 아이템 아틀라스에만 한 번 등록해야 하며, 클라이언트 로그의 `Missing textures` 또는 `Duplicate sprite`가 없음을 확인한다.

정지 캡처와 별도로 97개 모델의 등장·동작·소멸, 무적·크로노스·토르·니케·페르세포네·메구밍의 실제 일반 발동을 연속 촬영했다. 보고서 상단의 애니메이션 재생기와 각 모델의 재생 버튼에서 원속도·0.5배속·0.25배속, 일시정지, 프레임 이동을 사용할 수 있다. 기존 보고서 탭은 새로고침해야 반영된다.

```powershell
# --hidden --visual-probe로 실행하고 전용 클라이언트가 접속한 뒤
node scripts/minecraft-mcp/animations.mjs models
node scripts/minecraft-mcp/animations.mjs casts
node scripts/minecraft-mcp/report.mjs
```

기존 MCP 단일 캡처에는 요청 왕복 지연이 있으므로 `record_clip`이 게임 렌더 스레드에서 원본 PNG를 최대 20 fps로 저장한다. 실제 프레임 간격도 기록해 느려진 촬영을 임의의 20 fps 영상으로 표현하지 않는다. 입력 이름과 촬영 시간(1~6초)을 제한하고 중복 촬영과 경로 이탈을 거부한다. 이미지 합성·중간 프레임 생성은 하지 않는다. 프레임 원본은 `.build/minecraft-mcp/client/recordings/`, 목록은 `evidence/all-abilities/animations.json`에 있으므로 보고서를 옮길 때 프레임 폴더도 함께 보존해야 한다.

모델 영상은 단독 형태 재생이며 투사체의 실제 이동 경로를 재현한 영상과 구분한다. 실제 능력 영상은 좌클릭 입력과 성공 채팅을 확인한다. 메구밍은 사망 후 서버 명령이 거부되므로 클라이언트 체력 0과 폭렬 완료 채팅으로 끝을 확인한다. 파일 URL은 브라우저 자동화 도구 정책상 열 수 없어, 저장 프레임과 보고서 파일·재생 코드 검사로 검증했다.

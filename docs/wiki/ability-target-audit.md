# 능력 대상·범위·상호작용 감사

2026-10-03 기준 등록된 93개 능력의 선언과 대상 선택 경로를 검토했습니다. 아래 표는 **소스 경로 감사**이며, 93개 각각의 모든 조합을 실제 클라이언트로 플레이했다는 뜻은 아닙니다. 실행 검증은 별도 숨김 Paper 회귀 검사와 클라이언트 영상에 기록합니다.

## 이번 수정

- 단일 반경을 쓰는 공통 탐색을 정육면체에서 **구형 거리 판정**으로 수정했습니다. 반경 5에서 (3, 0, 4)는 포함되고 (4, 0, 4)는 제외됩니다. 하데스의 직접 생물 탐색과 안락소녀의 수평 원도 같은 의미로 맞췄습니다.
- 사망·오프라인·관전자·게임 미참가자를 대상에서 제외합니다. 사망/리스폰 자체의 능력 콜백은 계속 실행합니다.
- 같은 종류의 상태 효과는 강한 기존 효과를 약한 효과로 덮지 않으며, 같은 세기의 짧은 효과로 남은 시간을 줄이지 않습니다. 더 강한 효과는 자체 지속시간으로 적용됩니다. 취소된 적용에는 새 상태 성공 연출을 내지 않습니다.
- 일반 방어/무적 판정을 공격 적중 능력보다 먼저 처리하고, 취소된 공격·투사체·섭취에는 능력을 발동시키지 않습니다. 외부 플러그인이 더 늦게 공격 이벤트를 변경하는 경우까지 되돌리는 구조는 아닙니다.
- 마법사의 지연 낙뢰는 해소 직전에 생존·접속·월드·팀을 재확인합니다. 식신은 최종 섭취 취소와 제작 세션까지 확인합니다.
- 34개 능력 설명에서 빠졌던 숫자 범위, 아군/적 구분, 같은 월드 또는 전역 적용, 실제 물/얼음 벽의 물리적 영향을 명시했습니다.

## 표 읽는 법

`nearbyPlayers(..., r, true/false)`는 같은 월드의 구 반경 r 안의 아군/적입니다. 시전자는 기본 제외되며 능력이 명시적으로 추가할 수 있습니다. x/y/z 세 값을 쓰는 탐색은 상자 후보 탐색이며 안락소녀는 추가 수평 원 판정을 합니다.

`targetPlayerInSight`는 거리·시선·시야가 모두 맞는 가장 가까운 아군/적입니다. `commandTargetPlayer`는 `/x`로 지정한 같은 월드의 대상이며 **거리와 시야 제한이 없습니다**. `alliedPlayers`와 `enemyPlayers`는 온라인 생존 참가자 전체를 먼저 골라, 개별 능력이 추가 월드/거리 조건을 적용합니다. `enemies`와 `alliesInRange`는 지속형 능력의 구형 범위 검사입니다.

`true`는 아군, `false`는 적을 뜻합니다. 같은 팀 판정과 적대 피해 허용 판정(킬타임 등)은 별도로 검사합니다. 범위 장식의 관람자와 실제 효과 대상은 다르며, 주변에서 보인다고 모두 효과를 받는 것은 아닙니다.

| 능력 | 실제 대상 선택 경로 | 상호작용·범위 주의점 |
|---|---|---|
| [독화살아처 · acidarcher](../../plugin/src/main/java/kr/newgodwar/ability/builtin/AcidArcherAbility.java) | 투사체 피격자 · 시전자 자신·해당 위치 | 공통 상태 중첩 규칙 |
| [아이올로스 · aeolus](../../plugin/src/main/java/kr/newgodwar/ability/builtin/AeolusAbility.java) | `nearbyPlayers(context, player, 20, true)`<br>`nearbyPlayers(context, player, 10, false)` | 공통 상태 중첩 규칙 |
| [안중근 · anjunggeun](../../plugin/src/main/java/kr/newgodwar/ability/builtin/AhnJungGeunAbility.java) | `targetPlayerInSight(context, player, 24, false)`<br>`targetPlayerInSight(context, player, 28, false)` | 공통 상태 중첩 규칙 |
| [아카샤 · akasha](../../plugin/src/main/java/kr/newgodwar/ability/builtin/AkashaAbility.java) | `nearbyPlayers(context, player, 20, true)`<br>`nearbyPlayers(context, player, 10, false)` | 공통 상태 중첩 규칙 |
| [아마테라스 · amaterasu](../../plugin/src/main/java/kr/newgodwar/ability/builtin/AmaterasuAbility.java) | `targetPlayerInSight(context, player, 24, false)` | 공통 상태 중첩 규칙 |
| [거식증 · anorexia](../../plugin/src/main/java/kr/newgodwar/ability/builtin/AnorexiaAbility.java) | 시전자 자신·해당 위치 | 해당 이벤트와 소유자에 한정 |
| [아누비스 · anubis](../../plugin/src/main/java/kr/newgodwar/ability/builtin/AnubisAbility.java) | `targetPlayerInSight(context, player, 20, false)`<br>`validEnemy(context, judged, 24)`<br>`validEnemy(context, opponent, 24)` | 지속 중 생존/거리 재검사 |
| [아폴론 · apollon](../../plugin/src/main/java/kr/newgodwar/ability/builtin/ApollonAbility.java) | `enemyPlayers(context, caster)` | 같은 월드 밝기15 적 전체 |
| [아프로디테 · aprodite](../../plugin/src/main/java/kr/newgodwar/ability/builtin/AproditeAbility.java) | `nearbyPlayers(context, player, 20, false)` | 해당 이벤트와 소유자에 한정 |
| [아처 · archer](../../plugin/src/main/java/kr/newgodwar/ability/builtin/ArcherAbility.java) | 투사체 피격자 · 시전자 자신·해당 위치 | 해당 이벤트와 소유자에 한정 |
| [아레스 · ares](../../plugin/src/main/java/kr/newgodwar/ability/builtin/AresAbility.java) | 공격/피격 당사자 | 해당 이벤트와 소유자에 한정 |
| [아르테미스 · artemis](../../plugin/src/main/java/kr/newgodwar/ability/builtin/ArtemisAbility.java) | `targetPlayerInSight(context, player, 30, false)`<br>`validEnemy(context, victim, 64)`<br>`validEnemy(context, target, 64)` | 지속 중 생존/거리 재검사 · 공통 상태 중첩 규칙 |
| [아스클리피어스 · asclepius](../../plugin/src/main/java/kr/newgodwar/ability/builtin/AsclepiusAbility.java) | `nearbyPlayers(context, player, 5, true)` | 해당 이벤트와 소유자에 한정 |
| [암살자 · assasin](../../plugin/src/main/java/kr/newgodwar/ability/builtin/AssasinAbility.java) | `nearbyPlayers(context, player, 10, false)` | 해당 이벤트와 소유자에 한정 |
| [아테나 · athena](../../plugin/src/main/java/kr/newgodwar/ability/builtin/AthenaAbility.java) | `validEnemy(context, opponent, 5)`<br>`alliesInRange(context, center, 5)` | 지속 중 생존/거리 재검사 · 공통 상태 중첩 규칙 |
| [대장장이 · blacksmith](../../plugin/src/main/java/kr/newgodwar/ability/builtin/BlacksmithAbility.java) | 시전자 자신·해당 위치 | 해당 이벤트와 소유자에 한정 |
| [블라인더 · blinder](../../plugin/src/main/java/kr/newgodwar/ability/builtin/BlinderAbility.java) | `nearbyPlayers(context, player, 5, false)` | 공통 상태 중첩 규칙 |
| [봄버 · bomber](../../plugin/src/main/java/kr/newgodwar/ability/builtin/BomberAbility.java) | 시전자 자신·해당 위치 | 실제 폭발/낙뢰 |
| [집사 · bulter](../../plugin/src/main/java/kr/newgodwar/ability/builtin/BulterAbility.java) | 블록 폭발 이벤트 전체 | 해당 이벤트와 소유자에 한정 |
| [크로노스 · chronos](../../plugin/src/main/java/kr/newgodwar/ability/builtin/ChronosAbility.java) | `enemies(context, center, 6)` | 지속 중 생존/거리 재검사 · 공통 상태 중첩 규칙 |
| [클로킹 · clocking](../../plugin/src/main/java/kr/newgodwar/ability/builtin/ClockingAbility.java) | 공격/피격 당사자 · 시전자 자신·해당 위치 | 공통 상태 중첩 규칙 |
| [카운터 · counter](../../plugin/src/main/java/kr/newgodwar/ability/builtin/CounterAbility.java) | `commandTargetPlayer(context, player, false)`<br>`nearbyPlayers(context, player, ADVANCED_RANGE, false)` | 공통 상태 중첩 규칙 |
| [크리퍼 · creeper](../../plugin/src/main/java/kr/newgodwar/ability/builtin/CreeperAbility.java) | 시전자 자신·해당 위치 | 실제 폭발/낙뢰 |
| [다크니스 · darkness](../../plugin/src/main/java/kr/newgodwar/ability/builtin/DarknessAbility.java) | 공격/피격 당사자 | 해당 이벤트와 소유자에 한정 |
| [데메테르 · demeter](../../plugin/src/main/java/kr/newgodwar/ability/builtin/DemeterAbility.java) | `alliesInRange(context, center, 5)` | 지속 중 생존/거리 재검사 |
| [디오니소스 · dionysus](../../plugin/src/main/java/kr/newgodwar/ability/builtin/DionysusAbility.java) | 공격/피격 당사자 | 공통 상태 중첩 규칙 |
| [메아리 검사 · echo](../../plugin/src/main/java/kr/newgodwar/ability/builtin/EchoAbility.java) | `validEnemy(context, opponent, 5)`<br>`validEnemy(context, opponent, 24)`<br>`enemies(context, center, 3)` | 지속 중 생존/거리 재검사 |
| [에리스 · eris](../../plugin/src/main/java/kr/newgodwar/ability/builtin/ErisAbility.java) | 공격/피격 당사자 | 해당 이벤트와 소유자에 한정 |
| [수험생 · examinee](../../plugin/src/main/java/kr/newgodwar/ability/builtin/ExamineeAbility.java) | 시전자 자신·해당 위치 · 시전자 채팅 입력 | 해당 이벤트와 소유자에 한정 |
| [노인과바다 · fisher](../../plugin/src/main/java/kr/newgodwar/ability/builtin/FisherAbility.java) | 행동한 시전자 | 해당 이벤트와 소유자에 한정 |
| [잭프로스트 · frost](../../plugin/src/main/java/kr/newgodwar/ability/builtin/FrostAbility.java) | `targetPlayerInSight(context, player, 20, false)` | 실제 지형 임시 변경 |
| [가이아 · gaia](../../plugin/src/main/java/kr/newgodwar/ability/builtin/GaiaAbility.java) | `nearbyPlayers(context, player, 8, true)`<br>`nearbyPlayers(context, player, 9, false)` | 공통 상태 중첩 규칙 |
| [정원사 · gardener](../../plugin/src/main/java/kr/newgodwar/ability/builtin/GardenerAbility.java) | 행동한 시전자 | 해당 이벤트와 소유자에 한정 |
| [기가채드 · gigachad](../../plugin/src/main/java/kr/newgodwar/ability/builtin/GigachadAbility.java) | `nearbyPlayers(context, player, 6, false)` | 공통 상태 중첩 규칙 |
| [안락소녀 · girl](../../plugin/src/main/java/kr/newgodwar/ability/builtin/GirlAbility.java) | `nearbyPlayers(context, player, 5, 0, 5, false)` | 공통 상태 중첩 규칙 · 수평 원 반경 5, 높이 판정은 기존 유지 |
| [금수저 · goldspoon](../../plugin/src/main/java/kr/newgodwar/ability/builtin/GoldspoonAbility.java) | 시전자 자신·해당 위치 | 해당 이벤트와 소유자에 한정 |
| [중력술사 · graviton](../../plugin/src/main/java/kr/newgodwar/ability/builtin/GravitonAbility.java) | `enemies(context, center, 5)`<br>`enemies(context, center, 6)` | 지속 중 생존/거리 재검사 |
| [하데스 · hades](../../plugin/src/main/java/kr/newgodwar/ability/builtin/HadesAbility.java) | 직접 생물 반경 조회 · 시전자 자신·해당 위치 | 구 반경 2/4; 일반만 자신 포함; 아군 제외 |
| [해리포터 · harry](../../plugin/src/main/java/kr/newgodwar/ability/builtin/HarryAbility.java) | `nearbyPlayers(context, player, 10, false)`<br>`targetPlayerInSight(context, player, 20, false)` | 공통 상태 중첩 규칙 · 실제 폭발/낙뢰 |
| [헤카테 · hecate](../../plugin/src/main/java/kr/newgodwar/ability/builtin/HecateAbility.java) | `commandTargetPlayer(context, player, false)` | 공통 상태 중첩 규칙 |
| [허준 · heojun](../../plugin/src/main/java/kr/newgodwar/ability/builtin/HeoJunAbility.java) | `nearbyPlayers(context, player, 10, true)` | 공통 상태 중첩 규칙 |
| [헤파이토스 · hephaestus](../../plugin/src/main/java/kr/newgodwar/ability/builtin/HephaestusAbility.java) | 공격/피격 당사자 · 시전자 자신·해당 위치 | 지속 중 생존/거리 재검사 · 공통 상태 중첩 규칙 |
| [헤라 · hera](../../plugin/src/main/java/kr/newgodwar/ability/builtin/HeraAbility.java) | `targetPlayerInSight(context, player, 8, true)`<br>`alliesInRange(context, context.player().getLocation(), 8)` | 지속 중 생존/거리 재검사 · 공통 상태 중첩 규칙 |
| [헤르메스 · hermes](../../plugin/src/main/java/kr/newgodwar/ability/builtin/HermesAbility.java) | 시전자 자신·해당 위치 | 공통 상태 중첩 규칙 |
| [헤르미온느 · hermione](../../plugin/src/main/java/kr/newgodwar/ability/builtin/HermioneAbility.java) | `targetPlayerInSight(context, player, 16, false)`<br>`alliedPlayers(context, context.player(), true)` | 지속 중 생존/거리 재검사 · 공통 상태 중첩 규칙 |
| [홍길동 · honggildong](../../plugin/src/main/java/kr/newgodwar/ability/builtin/HongGildongAbility.java) | `targetPlayerInSight(context, player, 20, false)` | 공통 상태 중첩 규칙 |
| [호른달 · horeundal](../../plugin/src/main/java/kr/newgodwar/ability/builtin/HoreundalAbility.java) | 시전자 자신·해당 위치 | 공통 상태 중첩 규칙 |
| [무적 · invincibility](../../plugin/src/main/java/kr/newgodwar/ability/builtin/InvincibilityAbility.java) | 시전자 자신·해당 위치 | 공통 상태 중첩 규칙 |
| [이리스 · iris](../../plugin/src/main/java/kr/newgodwar/ability/builtin/IrisAbility.java) | `nearbyPlayers(context, player, 8, true)` | 공통 상태 중첩 규칙 |
| [장영실 · jangyeongsil](../../plugin/src/main/java/kr/newgodwar/ability/builtin/JangYeongSilAbility.java) | `nearbyPlayers(context, player, 10, true)` | 공통 상태 중첩 규칙 |
| [주작 · jujak](../../plugin/src/main/java/kr/newgodwar/ability/builtin/JujakAbility.java) | 시전자 자신·해당 위치 | 해당 이벤트와 소유자에 한정 |
| [로키 · loki](../../plugin/src/main/java/kr/newgodwar/ability/builtin/LokiAbility.java) | `targetPlayerInSight(context, player, 20, false)` | 공통 상태 중첩 규칙 |
| [메구밍 · megumin](../../plugin/src/main/java/kr/newgodwar/ability/builtin/MeguminAbility.java) | 시전자 자신·해당 위치 | 실제 폭발/낙뢰 |
| [미도리야 · midoriya](../../plugin/src/main/java/kr/newgodwar/ability/builtin/MidoriyaAbility.java) | 공격/피격 당사자 · 시전자 자신·해당 위치 | 공통 상태 중첩 규칙 |
| [광부 · miner](../../plugin/src/main/java/kr/newgodwar/ability/builtin/MinerAbility.java) | 공격/피격 당사자 · 시전자 자신·해당 위치 · 행동한 시전자 | 공통 상태 중첩 규칙 |
| [모르피우스 · morpious](../../plugin/src/main/java/kr/newgodwar/ability/builtin/MorpiousAbility.java) | `commandTargetPlayer(context, player, false)` | 공통 상태 중첩 규칙 |
| [나로호 · naro](../../plugin/src/main/java/kr/newgodwar/ability/builtin/NaroAbility.java) | 시전자 자신·해당 위치 | 해당 이벤트와 소유자에 한정 |
| [나스닥 · nasdaq](../../plugin/src/main/java/kr/newgodwar/ability/builtin/NasdaqAbility.java) | 시전자 / 소속 팀 상태 | 해당 이벤트와 소유자에 한정 |
| [자연계 · nature](../../plugin/src/main/java/kr/newgodwar/ability/builtin/NatureAbility.java) | `alliedPlayers(context, player, true)` | 공통 상태 중첩 규칙 · 고급: 월드 무관 생존 아군 전체 |
| [니케 · nike](../../plugin/src/main/java/kr/newgodwar/ability/builtin/NikeAbility.java) | `alliesInRange(context, player.getLocation(), 8)` | 지속 중 생존/거리 재검사 · 공통 상태 중첩 규칙 |
| [오딘 · odin](../../plugin/src/main/java/kr/newgodwar/ability/builtin/OdinAbility.java) | `targetPlayerInSight(context, player, 24, false)`<br>`validEnemy(context, prey, 24)`<br>`validEnemy(context, target, 24)` | 지속 중 생존/거리 재검사 |
| [원펀치 · onepunch](../../plugin/src/main/java/kr/newgodwar/ability/builtin/OnePunchAbility.java) | 공격/피격 당사자 · 시전자 자신·해당 위치 | 공통 상태 중첩 규칙 |
| [판 · pan](../../plugin/src/main/java/kr/newgodwar/ability/builtin/PanAbility.java) | `alliesInRange(context, stage, radius)`<br>`enemies(context, stage, radius)` | 지속 중 생존/거리 재검사 · 공통 상태 중첩 규칙 |
| [페르세포네 · persephone](../../plugin/src/main/java/kr/newgodwar/ability/builtin/PersephoneAbility.java) | `targetPlayerInSight(context, player, 18, false)`<br>`nearbyPlayers(context, player, 7, true)` | 공통 상태 중첩 규칙 |
| [포켓몬고 · pokego](../../plugin/src/main/java/kr/newgodwar/ability/builtin/PokegoAbility.java) | 시전자 / 소속 팀 상태 | 해당 이벤트와 소유자에 한정 |
| [포세이돈 · poseidon](../../plugin/src/main/java/kr/newgodwar/ability/builtin/PoseidonAbility.java) | `nearbyPlayers(context, player, TIDAL_RANGE, false)` | 공통 상태 중첩 규칙 · 실제 지형 임시 변경 |
| [사제 · priest](../../plugin/src/main/java/kr/newgodwar/ability/builtin/PriestAbility.java) | `alliedPlayers(context, player, true)` | 공통 상태 중첩 규칙 · 고급: 월드 무관 생존 아군 전체 |
| [여왕벌 · queenbee](../../plugin/src/main/java/kr/newgodwar/ability/builtin/QueenBeeAbility.java) | `targetPlayerInSight(context, player, 20, false)`<br>`validEnemy(context, target, 20)`<br>`alliesInRange(context, center, 4)` | 지속 중 생존/거리 재검사 |
| [케찰코아틀 · quetzalcoatl](../../plugin/src/main/java/kr/newgodwar/ability/builtin/QuetzalcoatlAbility.java) | `nearbyPlayers(context, player, 9, false)` | 공통 상태 중첩 규칙 |
| [라 · ra](../../plugin/src/main/java/kr/newgodwar/ability/builtin/RaAbility.java) | `nearbyPlayers(context, player, 8, false)` | 공통 상태 중첩 규칙 |
| [반사 · reflection](../../plugin/src/main/java/kr/newgodwar/ability/builtin/ReflectionAbility.java) | 공격/피격 당사자 | 해당 이벤트와 소유자에 한정 |
| [릭롤 · rickroll](../../plugin/src/main/java/kr/newgodwar/ability/builtin/RickrollAbility.java) | `nearbyPlayers(context, player, 8, false)`<br>`targetPlayerInSight(context, player, 22, false)` | 공통 상태 중첩 규칙 |
| [룬 세공사 · runesmith](../../plugin/src/main/java/kr/newgodwar/ability/builtin/RunesmithAbility.java) | `enemies(context, rune.center, 3)` | 지속 중 생존/거리 재검사 · 공통 상태 중첩 규칙 |
| [스크루지 · scrooge](../../plugin/src/main/java/kr/newgodwar/ability/builtin/ScroogeAbility.java) | 시전자 자신·해당 위치 | 같은 팀 조약돌 비용; 거리 제한 없음 |
| [세종대왕 · sejong](../../plugin/src/main/java/kr/newgodwar/ability/builtin/SejongAbility.java) | `nearbyPlayers(context, player, 10, true)`<br>`targetPlayerInSight(context, player, 28, false)` | 공통 상태 중첩 규칙 |
| [셀레네 · selene](../../plugin/src/main/java/kr/newgodwar/ability/builtin/SeleneAbility.java) | `targetPlayerInSight(context, player, 18, false)` | 공통 상태 중첩 규칙 |
| [신사임당 · shinsaimdang](../../plugin/src/main/java/kr/newgodwar/ability/builtin/ShinSaimdangAbility.java) | `nearbyPlayers(context, player, 8, true)` | 공통 상태 중첩 규칙 |
| [식신 · siksin](../../plugin/src/main/java/kr/newgodwar/ability/builtin/SiksinAbility.java) | 음식 소유자·섭취자 · 시전자 자신·해당 위치 | 공통 상태 중첩 규칙 · 음식 이름/버프 1:1; 공유는 동월드 생존 아군+소유자 |
| [저격수 · sniper](../../plugin/src/main/java/kr/newgodwar/ability/builtin/SniperAbility.java) | 투사체 피격자 · 시전자 자신·해당 위치 | 해당 이벤트와 소유자에 한정 |
| [사이코스노우 · snow](../../plugin/src/main/java/kr/newgodwar/ability/builtin/SnowAbility.java) | 투사체 피격자 · 시전자 자신·해당 위치 | 해당 이벤트와 소유자에 한정 |
| [스탠스 · stance](../../plugin/src/main/java/kr/newgodwar/ability/builtin/StanceAbility.java) | 공격/피격 당사자 | 해당 이벤트와 소유자에 한정 |
| [수상한녀석 · sus](../../plugin/src/main/java/kr/newgodwar/ability/builtin/SusAbility.java) | `nearbyPlayers(context, player, 10, false)` | 공통 상태 중첩 규칙 |
| [타짜 · tajja](../../plugin/src/main/java/kr/newgodwar/ability/builtin/TajjaAbility.java) | 공격/피격 당사자 · 시전자 자신·해당 위치 | 해당 이벤트와 소유자에 한정 |
| [텔레포터 · teleporter](../../plugin/src/main/java/kr/newgodwar/ability/builtin/TeleporterAbility.java) | `targetPlayerInSight(context, player, 30, true)` | 해당 이벤트와 소유자에 한정 |
| [괜찮아 · thisisfine](../../plugin/src/main/java/kr/newgodwar/ability/builtin/ThisIsFineAbility.java) | `nearbyPlayers(context, player, 7, false)` | 공통 상태 중첩 규칙 |
| [토르 · thor](../../plugin/src/main/java/kr/newgodwar/ability/builtin/ThorAbility.java) | `targetPlayerInSight(context, player, 20, false)`<br>`validEnemy(context, target, 24)`<br>`enemies(context, player.getLocation(), 5)`<br>`validEnemy(context, target, 5)`<br>`validEnemy(context, opponent, 5)` | 지속 중 생존/거리 재검사 · 공통 상태 중첩 규칙 |
| [부두술사 · voodoo](../../plugin/src/main/java/kr/newgodwar/ability/builtin/VoodooAbility.java) | 시전자 / 소속 팀 상태 | 해당 이벤트와 소유자에 한정 |
| [마녀 · witch](../../plugin/src/main/java/kr/newgodwar/ability/builtin/WitchAbility.java) | `nearbyPlayers(context, player, 10, false)` | 공통 상태 중첩 규칙 |
| [마법사 · wizard](../../plugin/src/main/java/kr/newgodwar/ability/builtin/WizardAbility.java) | `nearbyPlayers(context, player, 10, false)`<br>`nearbyPlayers(context, player, 5, false)` | 실제 폭발/낙뢰 · 지연 낙뢰 직전 팀/세계/생존 재검사 |
| [이순신 · yisunsin](../../plugin/src/main/java/kr/newgodwar/ability/builtin/YiSunSinAbility.java) | `nearbyPlayers(context, player, 10, false)`<br>`targetPlayerInSight(context, player, 32, false)` | 공통 상태 중첩 규칙 · 실제 폭발/낙뢰 |
| [유관순 · yugwansun](../../plugin/src/main/java/kr/newgodwar/ability/builtin/YuGwanSunAbility.java) | `nearbyPlayers(context, player, 12, true)`<br>`nearbyPlayers(context, player, 12, false)`<br>`nearbyPlayers(context, player, 14, true)`<br>`enemyPlayers(context, player)` | 공통 상태 중첩 규칙 · 고급: 아군14, 적은 월드 무관 전체 |
| [제트기관 · zet](../../plugin/src/main/java/kr/newgodwar/ability/builtin/ZetAbility.java) | 시전자 자신·해당 위치 | 공통 상태 중첩 규칙 |
| [제우스 · zeus](../../plugin/src/main/java/kr/newgodwar/ability/builtin/ZeusAbility.java) | 시전자 자신·해당 위치 | 실제 폭발/낙뢰 |

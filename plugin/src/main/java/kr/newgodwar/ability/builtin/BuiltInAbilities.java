package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityFactory;
import kr.newgodwar.ability.api.GodAbility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class BuiltInAbilities {

    private BuiltInAbilities() {
    }

    public static Collection<AbilityDefinition> definitions() {
        List<AbilityDefinition> definitions = new ArrayList<AbilityDefinition>();
        add(definitions, "zeus", "제우스", "번개를 내리고 번개/폭발 피해를 무시합니다.", ZeusBuiltInAbility.class);
        add(definitions, "hades", "하데스", "주변 생물을 나락으로 떨어뜨리고 사망 시 확률로 아이템을 보존합니다.", HadesBuiltInAbility.class);
        add(definitions, "demeter", "데메테르", "빵을 만들고 허기와 재생에 강합니다.", DemeterBuiltInAbility.class);
        add(definitions, "athena", "아테나", "책과 인챈트 테이블을 만들고 사망자를 통해 경험을 얻습니다.", AthenaBuiltInAbility.class);
        add(definitions, "apollon", "아폴론", "태양을 띄우고 주변 적을 불태웁니다.", ApollonBuiltInAbility.class);
        add(definitions, "artemis", "아르테미스", "화살과 활을 만들며 화살로 즉사 확률을 가집니다.", ArtemisBuiltInAbility.class);
        add(definitions, "ares", "아레스", "공격 피해가 증가하고 일정 확률로 공격을 회피합니다.", AresBuiltInAbility.class);
        add(definitions, "hephaestus", "헤파이토스", "용암을 만들고 화염 피해를 무시합니다.", HephaestusBuiltInAbility.class);
        add(definitions, "asclepius", "아스클리피어스", "자신 또는 주변 아군을 완전히 회복합니다.", AsclepiusBuiltInAbility.class);
        add(definitions, "hermes", "헤르메스", "빠른 이동과 짧은 비행을 사용합니다.", HermesBuiltInAbility.class);
        add(definitions, "dionysus", "디오니소스", "피격 시 확률로 공격자를 취하게 합니다.", DionysusBuiltInAbility.class);
        add(definitions, "aprodite", "아프로디테", "주변 플레이어를 자신의 위치로 끌어옵니다.", AproditeBuiltInAbility.class);
        add(definitions, "eris", "에리스", "피격 시 확률로 공격자를 밀쳐냅니다.", ErisBuiltInAbility.class);
        add(definitions, "morpious", "모르피우스", "지정한 적을 수면 상태로 만듭니다.", MorpiousBuiltInAbility.class);
        add(definitions, "aeolus", "아이올로스", "아군에게 바람의 축복을 주거나 적을 밀쳐냅니다.", AeolusBuiltInAbility.class);
        add(definitions, "akasha", "아카샤", "아군에게 향락을, 적에게 고통을 부여합니다.", AkashaBuiltInAbility.class);
        add(definitions, "horeundal", "호른달", "현재 위치를 기억하고 잠시 후 되돌아옵니다.", HoreundalBuiltInAbility.class);
        add(definitions, "jujak", "주작", "화염 피해를 무시하고 짧게 비행합니다.", JujakBuiltInAbility.class);
        add(definitions, "frost", "잭프로스트", "얼음을 만들고 지정한 적을 얼음 안에 가둡니다.", FrostBuiltInAbility.class);
        add(definitions, "nasdaq", "나스닥", "철괴나 다이아몬드를 확률적으로 복사합니다.", NasdaqBuiltInAbility.class);

        add(definitions, "archer", "아처", "화살/활을 만들고 활 피해가 증가합니다.", ArcherBuiltInAbility.class);
        add(definitions, "miner", "광부", "코블스톤 채굴 보너스와 곡괭이 고정 피해를 가집니다.", MinerBuiltInAbility.class);
        add(definitions, "stance", "스탠스", "공격 넉백과 피해 증폭을 무시합니다.", StanceBuiltInAbility.class);
        add(definitions, "teleporter", "텔레포터", "바라보는 곳으로 이동하거나 아군과 위치를 바꿉니다.", TeleporterBuiltInAbility.class);
        add(definitions, "bomber", "봄버", "보이지 않는 폭탄을 설치하고 원격 폭발시킵니다.", BomberBuiltInAbility.class);
        add(definitions, "creeper", "크리퍼", "자폭하고 번개를 맞으면 폭발력이 달라집니다.", CreeperBuiltInAbility.class);
        add(definitions, "wizard", "마법사", "주변 플레이어를 날리거나 신의 심판을 내립니다.", WizardBuiltInAbility.class);
        add(definitions, "assasin", "암살자", "더블 점프와 기습 이동을 사용합니다.", AssasinBuiltInAbility.class);
        add(definitions, "reflection", "반사", "피격 시 확률로 받은 피해를 반사합니다.", ReflectionBuiltInAbility.class);
        add(definitions, "blinder", "블라인더", "주변 적이나 공격자에게 실명을 겁니다.", BlinderBuiltInAbility.class);
        add(definitions, "invincibility", "무적", "잠시 무적이 되거나 재생 효과를 얻습니다.", InvincibilityBuiltInAbility.class);
        add(definitions, "clocking", "클로킹", "투명화 후 공격 시 확률로 즉사시킵니다.", ClockingBuiltInAbility.class);
        add(definitions, "blacksmith", "대장장이", "코블스톤을 철로, 철을 다이아몬드로 바꿉니다.", BlacksmithBuiltInAbility.class);
        add(definitions, "priest", "사제", "자신 또는 팀원에게 무작위 축복을 부여합니다.", PriestBuiltInAbility.class);
        add(definitions, "witch", "마녀", "주변 적과 공격자에게 저주를 겁니다.", WitchBuiltInAbility.class);
        add(definitions, "sniper", "저격수", "스나이핑 모드에서 매우 빠른 화살을 발사합니다.", SniperBuiltInAbility.class);
        add(definitions, "voodoo", "부두술사", "팻말로 대상을 연결해 원격 피해를 줍니다.", VoodooBuiltInAbility.class);
        add(definitions, "anorexia", "거식증", "허기가 절반으로 유지됩니다.", AnorexiaBuiltInAbility.class);
        add(definitions, "bulter", "집사", "폭발을 안정시켜 막습니다.", BulterBuiltInAbility.class);
        add(definitions, "midoriya", "미도리야", "원 포 올을 준비한 뒤 맨손 공격으로 큰 피해를 줍니다.", MidoriyaBuiltInAbility.class);
        add(definitions, "goldspoon", "금수저", "리스폰할 때 레깅스를 받습니다.", GoldspoonBuiltInAbility.class);
        add(definitions, "queenbee", "여왕벌", "독침과 페로몬 끌어오기를 사용합니다.", QueenBeeBuiltInAbility.class);
        add(definitions, "snow", "사이코스노우", "눈덩이 피해와 성장하는 공격 지수를 가집니다.", SnowBuiltInAbility.class);
        add(definitions, "tajja", "타짜", "검을 숨겨 맨손 공격에 검 피해를 싣습니다.", TajjaBuiltInAbility.class);
        add(definitions, "girl", "안락소녀", "주변 적을 끌어와 굶주리게 합니다.", GirlBuiltInAbility.class);
        add(definitions, "megumin", "메구밍", "게임 중 한 번 폭렬 마법을 사용합니다.", MeguminBuiltInAbility.class);
        add(definitions, "pokego", "포켓몬고", "많이 걸으면 다른 능력으로 바뀝니다.", PokegoBuiltInAbility.class);
        add(definitions, "darkness", "다크니스", "받는 피해를 줄이고 자신 공격은 피해가 없습니다.", DarknessBuiltInAbility.class);
        add(definitions, "gasolin", "가솔린기관", "화염 피해를 받으면 빨라집니다.", GasolinBuiltInAbility.class);
        add(definitions, "zet", "제트기관", "화염 피해를 받으면 높은 속도로 가속합니다.", ZetBuiltInAbility.class);
        add(definitions, "hermione", "헤르미온느", "채팅 주문으로 다양한 마법을 사용합니다.", HermioneBuiltInAbility.class);
        add(definitions, "harry", "해리포터", "헤르미온느보다 숙련된 채팅 주문 마법을 사용합니다.", HarryBuiltInAbility.class);
        add(definitions, "gardener", "정원사", "나무를 캐면 꽃과 코블스톤을 얻습니다.", GardenerBuiltInAbility.class);
        add(definitions, "acidarcher", "독화살아처", "활 피해 대신 독을 부여합니다.", AcidArcherBuiltInAbility.class);
        add(definitions, "galbi", "명륜진사", "익힌 돼지고기를 만들고 허기와 재생에 강합니다.", GalbiBuiltInAbility.class);
        add(definitions, "naro", "나로호", "낙하 피해를 무시하고 크게 도약합니다.", NaroBuiltInAbility.class);
        add(definitions, "scrooge", "스크루지", "팀원의 능력 코블스톤 비용을 절반으로 낮춥니다.", ScroogeBuiltInAbility.class);
        add(definitions, "fisher", "노인과바다", "낚시로 잡동사니와 광물을 얻습니다.", FisherBuiltInAbility.class);
        add(definitions, "examinee", "수험생", "수학 문제를 맞히면 무작위 능력으로 바뀝니다.", ExamineeBuiltInAbility.class);
        return definitions;
    }

    private static void add(List<AbilityDefinition> definitions, final String id, String name, String description, final Class<? extends GodAbility> abilityClass) {
        definitions.add(new AbilityDefinition(id, name, description, "NewGodWar", true, new AbilityFactory() {
            @Override
            public GodAbility create() {
                try {
                    return abilityClass.newInstance();
                } catch (InstantiationException ex) {
                    throw new IllegalStateException("Cannot create ability " + id, ex);
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException("Cannot create ability " + id, ex);
                }
            }
        }));
    }
}

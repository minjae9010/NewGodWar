package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.feedback.DesignedEffect;
import kr.newgodwar.ability.feedback.ObjectModel;
import java.util.ArrayList;
import java.util.List;
import static kr.newgodwar.ability.feedback.ModelParts.box;

/** Hand-authored action silhouettes. Each ability selects its own actions in its STYLE. */
public final class AbilityDesigns {
    private AbilityDesigns() { }
    private static final String GOLD = "GOLD_BLOCK", WHITE = "QUARTZ_BLOCK", DARK = "OBSIDIAN";

    public static final DesignedEffect VENOM_BOW = bow("독니가 달린 활에서 녹색 화살이 맺힘", true, false);
    public static final DesignedEffect ARCHER_BOW = bow("휘어진 활과 당겨졌다 풀리는 시위", false, false);
    public static final DesignedEffect MOON_BOW = bow("초승달 활과 은빛 사냥 화살", false, true);
    public static final DesignedEffect VENOM = serpent("적중 부위를 조이는 녹색 독사", "LIME_CONCRETE", false);
    public static final DesignedEffect MEDICINE = serpent("치유 지팡이를 타고 오르는 금빛 뱀", GOLD, true);
    public static final DesignedEffect WIND_HEAL = feather("순풍의 세 깃털이 위로 피어남", WHITE, 3);
    public static final DesignedEffect GALE = stream("진행 방향으로 휘어지는 세 바람 날", WHITE, 3, false);
    public static final DesignedEffect BULLET = impact("발사 위치에서 짧게 번지는 총구 섬광", "IRON_BLOCK", 3, false);
    public static final DesignedEffect CHAMBER = gear("장전되며 맞물리는 탄창 톱니", "IRON_BLOCK", 6);
    public static final DesignedEffect AKASHIC_BOOK = book("펼쳐지는 기록서와 상승하는 책갈피", "ENCHANTED_BOOK", "PURPLE_CONCRETE", false);
    public static final DesignedEffect AKASHIC_CURSE = book("기록서의 세 글자가 바깥으로 흩어짐", "ENCHANTED_BOOK", "PURPLE_CONCRETE", true);
    public static final DesignedEffect SUN_MIRROR = sun("태양 거울의 팔각 테두리가 열림", 8, true);
    public static final DesignedEffect SOLAR_ARROW = bow("불타는 태양 활이 화살을 놓음", false, false, "ORANGE_CONCRETE");
    public static final DesignedEffect SOLAR_DISC = sun("라의 태양 원반과 좌우 매의 깃", 6, false);
    public static final DesignedEffect ROSE_GATE = flower("장미 꽃잎 여섯 장이 열리며 대상을 감쌈", "PINK_CONCRETE", 6, true);
    public static final DesignedEffect WAR_BLADES = blades("엇갈리는 두 붉은 전쟁 검", "RED_CONCRETE", 2);
    public static final DesignedEffect DAGGER_STEP = blades("등 뒤 이동 지점에서 접히는 단검", DARK, 1);
    public static final DesignedEffect ANVIL = forge("작은 모루 위로 내려치는 단조 망치", false);
    public static final DesignedEffect QUENCH = forge("달군 철과 증기 기둥이 갈라지는 담금질", true);
    public static final DesignedEffect SHUT_EYE = eye("가려지는 눈과 닫히는 검은 눈꺼풀", DARK);
    public static final DesignedEffect SERVANT_WALL = plates("블록 폭발을 가로막는 네 겹 석판", WHITE, 4, false);
    public static final DesignedEffect CLOAK = veil("양쪽으로 갈라졌다 접히는 어둠의 망토", DARK, 5);
    public static final DesignedEffect COUNTER_LOCK = lock("양쪽 쇠사슬이 중앙 자물쇠에 맞물림", false);
    public static final DesignedEffect CREEPER_FUSE = fuse("네 전극 사이에 차오르는 크리퍼 전하");
    public static final DesignedEffect BLACK_ARMOR = plates("충격 지점으로 겹쳐지는 검은 갑주", DARK, 3, false);
    public static final DesignedEffect WHEAT = sprout("줄기 끝에 여섯 낟알이 익어감", GOLD, true);
    public static final DesignedEffect WINE = chalice("기울어진 술잔 위에 떠오르는 포도 방울");
    public static final DesignedEffect DISCORD = gate("서로 반대 방향으로 갈라지는 불화의 문", "PURPLE_CONCRETE", true);
    public static final DesignedEffect FISH_HOOK = hook("당겨지는 낚싯바늘과 두 물결");
    public static final DesignedEffect EARTH_ROOTS = roots("땅에서 자라 발목을 감는 네 뿌리", false);
    public static final DesignedEffect EARTH_FLOWER = flower("대지의 여섯 잎과 중앙 새싹이 피어남", "LIME_CONCRETE", 6, false);
    public static final DesignedEffect SEEDLING = sprout("갈라진 씨앗에서 돋는 두 새잎", "LIME_CONCRETE", false);
    public static final DesignedEffect MUSCLE = fist("양 주먹에 맞물리는 강철 너클", "IRON_BLOCK", true);
    public static final DesignedEffect HEART_BIND = lock("분홍 고리와 심장 모양 걸쇠가 닫힘", true);
    public static final DesignedEffect GOLD_SPOON = spoon("금수저 위로 떠오르는 세 금 조각");
    public static final DesignedEffect SPELL_PAGES = book("지팡이 앞에서 펼쳐지는 주문서", "BOOK", "LIGHT_BLUE_CONCRETE", true);
    public static final DesignedEffect WITCH_MOONS = moons("세 방향의 초승달이 교차하는 마녀의 장막", 3);
    public static final DesignedEffect HERBS = herbs("약탕 그릇에서 올라오는 두 약초 줄기");
    public static final DesignedEffect CLOUD_STEP = stream("발뒤로 풀리는 세 구름 띠", WHITE, 3, true);
    public static final DesignedEffect RETURN_GATE = gate("양쪽 시간 문이 접히며 귀환 위치를 표시", GOLD, true);
    public static final DesignedEffect AEGIS_PLATES = aegis();
    public static final DesignedEffect RAINBOW = rainbow();
    public static final DesignedEffect ARMILLARY = gear("서로 맞물리는 여덟 천문 기어", GOLD, 8);
    public static final DesignedEffect TRICK_MASK = mask("엇갈리는 두 뿔의 로키 가면", "LIME_CONCRETE", true);
    public static final DesignedEffect GREEN_SMASH = fist("주먹 앞으로 뻗는 세 초록 전격", "LIME_CONCRETE", false);
    public static final DesignedEffect ORE_SPLIT = impact("타격점에서 갈라지는 광석 결정", "DIAMOND_BLOCK", 5, true);
    public static final DesignedEffect DREAM = dream();
    public static final DesignedEffect ROCKET = rocket();
    public static final DesignedEffect STOCK_SPLIT = coins("두 갈래로 나뉘어 복제되는 금화", 4);
    public static final DesignedEffect NATURE_LEAVES = sprout("손에서 자라나 펼쳐지는 네 잎", "GREEN_CONCRETE", false);
    public static final DesignedEffect ONE_PUNCH_READY = readyFists("양손에 모이는 원펀치 준비의 빛", WHITE);
    public static final DesignedEffect GREEN_READY = readyFists("손에만 차오르는 원 포 올 준비 전하", "LIME_CONCRETE");
    public static final DesignedEffect STATUS_HASTE = readyFists("손목에서 빠르게 도는 성급함 톱니", GOLD);
    public static final DesignedEffect STATUS_WEAKNESS = impact("몸 주변에서 힘없이 내려가는 약화 조각", "GRAY_CONCRETE", 4, false);
    public static final DesignedEffect NATURE_RECOIL = flower("몸 주변에서 시들며 떨어지는 식물 반동의 잎", "GREEN_CONCRETE", 4, false);
    public static final DesignedEffect ONE_PUNCH = fist("단일 주먹을 따라 터지는 흰 충격 쐐기", WHITE, false);
    public static final DesignedEffect UNDERWORLD_ROOTS = roots("어두운 덩굴 끝에 피는 붉은 석류", true);
    public static final DesignedEffect POMEGRANATE = flower("검붉은 석류 조각이 열리는 치유", "RED_CONCRETE", 5, false);
    public static final DesignedEffect TRIDENT = trident();
    public static final DesignedEffect BLESSING = cross();
    public static final DesignedEffect SERPENT_FEATHERS = feather("뱀의 등처럼 펼쳐지는 다섯 청록 깃", "PRISMARINE", 5);
    public static final DesignedEffect MIRROR = plates("기울어진 거울 조각이 공격 방향으로 튕김", "LIGHT_BLUE_STAINED_GLASS", 5, true);
    public static final DesignedEffect RECORD = record();
    public static final DesignedEffect ROYAL_DECREE = book("붉은 인장이 닫히는 왕의 칙령", "WRITTEN_BOOK", "RED_CONCRETE", false);
    public static final DesignedEffect ROYAL_GRACE = book("금빛 글줄이 올라가는 훈민정음 책", "BOOK", GOLD, false);
    public static final DesignedEffect LUNAR_VEIL = moons("은빛 초승달 두 겹이 서로 포개짐", 2);
    public static final DesignedEffect INK_ORCHID = flower("먹빛 줄기에서 피어나는 난초 다섯 잎", "PINK_CONCRETE", 5, false);
    public static final DesignedEffect FEAST = feast();
    public static final DesignedEffect SCOPE = scope();
    public static final DesignedEffect SNOWFLAKE = snowflake();
    public static final DesignedEffect ANCHOR = anchor();
    public static final DesignedEffect FALSE_FACE = mask("세 조각으로 해체되는 위장 가면", WHITE, false);
    public static final DesignedEffect HIDDEN_CARDS = cards();
    public static final DesignedEffect FOLD_GATE = gate("입구와 출구를 연결하듯 접히는 공간 문", "CYAN_CONCRETE", false);
    public static final DesignedEffect EMBERS = stream("아래에서 위로 번지는 세 불꽃 혀", "ORANGE_CONCRETE", 3, false);
    public static final DesignedEffect TEACUP = cup();
    public static final DesignedEffect VOODOO_DOLL = doll();
    public static final DesignedEffect CAULDRON = cauldron();
    public static final DesignedEffect WAND_GUST = stream("지팡이 끝에서 뻗는 네 굽은 바람", "LIGHT_BLUE_CONCRETE", 4, false);
    public static final DesignedEffect TURTLE_SHIP = ship();
    public static final DesignedEffect RESOLVE = banner();
    public static final DesignedEffect JET_EXHAUST = stream("두 분사구에서 뒤로 길게 뻗는 화염", "ORANGE_CONCRETE", 2, true);
    // Workshop, market and utility abilities: what they make, trade or test, shown as it happens.
    public static final DesignedEffect IRON_FORGE = forge("모루를 세 번 두드려 철 주괴를 꺼냄", false);
    public static final DesignedEffect GEM_FORGE = forge("철 주괴를 녹여 다이아몬드로 벼림", true);
    public static final DesignedEffect ARROW_BUNDLE = bow("제작대 칸이 차오르며 화살 다발이 완성됨", false, false);
    public static final DesignedEffect VENOM_ARROWS = bow("제작대 칸이 차오르며 독화살 다발이 완성됨", true, false);
    public static final DesignedEffect STOCK_CRASH = coins("폭락하는 차트와 함께 깨져 떨어지는 금화", 2);
    public static final DesignedEffect LEDGER = coins("가격표에 할인 도장이 찍히며 동전이 모여듦", 3);
    public static final DesignedEffect COMPASS = gear("탐험 나침반과 발자국, 퍼지는 탐지 신호", "LIME_CONCRETE", 4);
    public static final DesignedEffect EXAM = book("펼쳐지는 시험지 위로 떠오르는 물음표", "PAPER", "PURPLE_CONCRETE", false);
    public static final DesignedEffect EXAM_WRONG = effect("시험지 위에 붉은 X가 찍힘", (t,d) -> {
        List<ObjectModel.Part> o=parts();
        line(o,"RED_CONCRETE",-0.3,1.0,0.3,1.6,0.65,0.07); line(o,"RED_CONCRETE",0.3,1.0,-0.3,1.6,0.65,0.07);
        return o;
    });
    public static final DesignedEffect HUNGER_BALANCE = effect("흔들리던 허기 저울이 가운데에서 멈춤", (t,d) -> {
        List<ObjectModel.Part> o=parts(); double tilt=Math.sin(t*0.6)*Math.exp(-t/8)*0.35;
        line(o,GOLD,0,1.0,0,1.6,0.65,0.05);
        line(o,GOLD,-0.4*Math.cos(tilt),1.6-0.4*Math.sin(tilt),0.4*Math.cos(tilt),1.6+0.4*Math.sin(tilt),0.65,0.05);
        cube(o,"ORANGE_CONCRETE",-0.4*Math.cos(tilt),1.45-0.4*Math.sin(tilt),0.65,0.12);
        cube(o,"RED_CONCRETE",0.4*Math.cos(tilt),1.45+0.4*Math.sin(tilt),0.65,0.12);
        return o;
    });
    public static final DesignedEffect OATH_KNOT = effect("두 서약 고리가 겹쳐진 뒤 풀리는 치유", (t,d) -> {
        List<ObjectModel.Part> o=parts();
        for(int s:new int[]{-1,1}) arc(o,s<0?GOLD:WHITE,s*(0.18+Math.sin(t*0.12)*0.08),1.8,0.6,0.25,0,Math.PI*2,8,0.045);
        return o;
    });
    public static final DesignedEffect VICTORY = feather("승리한 아군 위로 펼쳐지는 월계 잎", GOLD, 5);
    public static final DesignedEffect THUNDER_FORK = effect("낙뢰 지점에서 벌어졌다 수축하는 세 전격", (t,d) -> {
        List<ObjectModel.Part> o=parts();
        for(int j=0;j<3;j++) for(int i=0;i<3;i++) {
            double x=(j-1)*0.3, y=0.3+i*0.55;
            line(o,i==1?GOLD:WHITE,x+(i%2==0?0.15:-0.15),y,x+(i%2==0?-0.15:0.15),y+0.55,0.1,0.04);
        } return o;
    });

    // Jang Yeong-sil: parts are assembled one by one until the third completes an iron pickaxe.
    public static final DesignedEffect PART_ONE = gear("첫 번째 부품 톱니가 맞물리며 조립됨 (1/3)", "IRON_BLOCK", 3);
    public static final DesignedEffect PART_TWO = gear("두 번째 부품이 더해져 장치가 돌기 시작함 (2/3)", "IRON_BLOCK", 5);
    public static final DesignedEffect PICKAXE_CRAFT = gear("세 부품이 합쳐져 철 곡괭이가 완성됨", GOLD, 8);
    public static final DesignedEffect DEVICE_FIELD = gear("발밑 혼천의 장치가 펼쳐지며 반경 10블록 아군에게 신호가 퍼짐", GOLD, 8);
    public static final DesignedEffect GEAR_MARK = gear("장치의 도움을 받은 아군 머리 위 톱니 표식", GOLD, 4);
    public static final DesignedEffect PICKAXE_STRIKE = impact("곡괭이가 내리찍혀 광석 파편과 감속 표식이 튐", "IRON_BLOCK", 4, true);

    // Shared reactions for status changes that an ability does not decorate itself.
    public static final DesignedEffect STATUS_SLOW = lock("발목을 붙잡는 얼음 사슬", false);
    public static final DesignedEffect STATUS_BLIND = eye("눈앞을 덮는 어둠", DARK);
    public static final DesignedEffect STATUS_SPEED = stream("발뒤로 흐르는 바람", WHITE, 3, true);
    public static final DesignedEffect STATUS_POISON = stream("머리 주위를 어지럽게 도는 독기", "LIME_CONCRETE", 3, false);
    public static final DesignedEffect STATUS_STRENGTH = fist("주먹에 차오르는 붉은 힘", "RED_CONCRETE", false);
    public static final DesignedEffect STATUS_HEAL = flower("몸을 따라 올라오는 회복의 빛", "PINK_CONCRETE", 5, false);
    public static final DesignedEffect STATUS_SUN = sun("하늘로 떠오르는 해", 8, false);
    public static final DesignedEffect STATUS_MOON = moons("하늘로 떠오르는 달", 2);
    public static final DesignedEffect STATUS_ITEM = coins("손에서 빛나며 생겨나는 물건", 3);
    public static final DesignedEffect STATUS_REPAIR = forge("수리된 장비 곁에서 반짝이는 작은 손 불빛", false);
    public static final DesignedEffect STATUS_CLEANSE = cross();
    public static final DesignedEffect STATUS_HIT = impact("타격 지점에서 튀는 파편", "IRON_BLOCK", 3, false);
    public static final DesignedEffect STATUS_STEALTH = veil("몸을 감싸며 사라지는 연기", DARK, 5);
    public static final DesignedEffect STATUS_SEAL = lock("능력을 묶는 봉인 자물쇠", false);
    public static final DesignedEffect STATUS_ROOT = roots("발목을 감는 뿌리", false);
    public static final DesignedEffect STATUS_FIRE = stream("몸을 타고 오르는 불꽃", "ORANGE_CONCRETE", 3, false);
    public static final DesignedEffect STATUS_FROST = snowflake();
    public static final DesignedEffect STATUS_WATER = stream("머리 위에서 쏟아지는 물방울", "LIGHT_BLUE_CONCRETE", 3, false);
    public static final DesignedEffect STATUS_MUSIC = record();
    public static final DesignedEffect STATUS_SLEEP = dream();
    public static final DesignedEffect STATUS_BLOOM = sprout("발밑에서 돋는 새싹", "LIME_CONCRETE", false);
    public static final DesignedEffect STATUS_ARCANE = impact("주문을 받은 부위에서 흩어지는 빛 조각", "LIGHT_BLUE_CONCRETE", 5, false);
    public static final DesignedEffect STATUS_HUNGER = feast();
    public static final DesignedEffect STATUS_PORTAL = gate("몸을 감싸는 이동 관문", "CYAN_CONCRETE", false);
    public static final DesignedEffect STATUS_SLASH = impact("맞은 부위에 남는 짧은 검흔과 파편", "IRON_BLOCK", 3, false);

    private static final java.util.Map<kr.newgodwar.ability.feedback.EffectCue, DesignedEffect> STATUS = statusTable();
    private static java.util.Map<kr.newgodwar.ability.feedback.EffectCue, DesignedEffect> statusTable() {
        java.util.Map<kr.newgodwar.ability.feedback.EffectCue, DesignedEffect> map =
            new java.util.EnumMap<kr.newgodwar.ability.feedback.EffectCue, DesignedEffect>(kr.newgodwar.ability.feedback.EffectCue.class);
        Object[][] pairs = {{"SLOW", STATUS_SLOW}, {"BLIND", STATUS_BLIND}, {"WIND", STATUS_SPEED}, {"POISON", STATUS_POISON},
            {"CHARGE", STATUS_STRENGTH}, {"HEAL", STATUS_HEAL}, {"SUN", STATUS_SUN}, {"MOON", STATUS_MOON}, {"ITEM", STATUS_ITEM},
            {"FORGE", STATUS_REPAIR}, {"CLEANSE", STATUS_CLEANSE}, {"HIT", STATUS_HIT}, {"STEALTH", STATUS_STEALTH},
            {"SEAL", STATUS_SEAL}, {"ROOT", STATUS_ROOT}, {"FIRE", STATUS_FIRE}, {"FROST", STATUS_FROST}, {"WATER", STATUS_WATER},
            {"MUSIC", STATUS_MUSIC}, {"SLEEP", STATUS_SLEEP}, {"BLOOM", STATUS_BLOOM}, {"ARCANE", STATUS_ARCANE},
            {"HUNGER", STATUS_HUNGER}, {"PORTAL", STATUS_PORTAL}, {"SLASH", STATUS_SLASH},
            {"HASTE", STATUS_HASTE}, {"WEAKNESS", STATUS_WEAKNESS}};
        for (Object[] pair : pairs) map.put(kr.newgodwar.ability.feedback.EffectCue.valueOf((String) pair[0]), (DesignedEffect) pair[1]);
        return java.util.Collections.unmodifiableMap(map);
    }
    /** The shared reaction for a cue the ability leaves undecorated; null for cues with their own shared model. */
    public static DesignedEffect status(kr.newgodwar.ability.feedback.EffectCue cue) { return STATUS.get(cue); }

    // Area scenes drawn at their real radius (detail).
    public static final ObjectModel FROST_CAGE = ObjectModel.animated((t,r) -> {
        // The block fallback keeps a low dome; the pack scene marks the full radius.
        List<ObjectModel.Part> o=parts(); double radius=Math.max(1,Math.min(3.5,r));
        for(int rib=0;rib<4;rib++) for(int i=1;i<4;i++) { double e=i*Math.PI/8,a=rib*Math.PI/4;
            box(o,"ICE",Math.cos(a)*Math.cos(e)*radius,Math.sin(e)*radius,Math.sin(a)*Math.cos(e)*radius,0.25,0.25,0.25,0,a); }
        return o;
    });
    public static final ObjectModel MELODY = ObjectModel.animated((t,r) -> {
        List<ObjectModel.Part> o=parts(); double radius=Math.max(1,r);
        for(int i=0;i<6;i++) { double a=i*Math.PI/3+t*0.03;
            box(o,"PINK_CONCRETE",Math.cos(a)*radius,1.5+Math.sin(t*0.2+i)*0.25,Math.sin(a)*radius,0.12,0.3,0.12,0,a); }
        return o;
    });
    public static final ObjectModel HUNT_MARK = ObjectModel.animated((t,marks) -> {
        List<ObjectModel.Part> o=parts();
        arc(o,WHITE,0,2.3,0,0.4,Math.PI*0.25,Math.PI*1.5,6,0.05);
        for(int i=0;i<3;i++) cube(o,"LIGHT_BLUE_CONCRETE",(i-1)*0.3,2.95,0,i<marks?0.1:0.03);
        return o;
    });
    public static final ObjectModel HARVEST = ObjectModel.animated((t,stage) -> {
        List<ObjectModel.Part> o=parts(); double height=0.2+Math.min(3,stage)*0.3;
        for(int i=0;i<8;i++) { double a=i*Math.PI/4;
            box(o,GOLD,Math.cos(a)*3.5,height/2,Math.sin(a)*3.5,0.08,height,0.08,0,a); }
        return o;
    });
    public static final ObjectModel LEVITATE = ObjectModel.animated((t,d) -> {
        List<ObjectModel.Part> o=parts();
        for(int i=0;i<6;i++) { double a=i*Math.PI/3+t*0.2; cube(o,"PURPLE_CONCRETE",Math.cos(a)*0.7,0.2+i*0.3,Math.sin(a)*0.7,0.08); }
        return o;
    });
    public static final ObjectModel PROTEGO = ObjectModel.animated((t,r) -> {
        List<ObjectModel.Part> o=parts(); double radius=Math.max(1,r);
        for(int i=0;i<6;i++) { double a=i*Math.PI/3;
            box(o,"LIGHT_BLUE_STAINED_GLASS",Math.cos(a)*radius,1.0,Math.sin(a)*radius,0.06,1.2,0.9,0,-a); }
        return o;
    });
    public static final ObjectModel ECHO_SLASH = ObjectModel.animated((t,r) -> {
        List<ObjectModel.Part> o=parts();
        box(o,WHITE,0,1.1,0,1.6,0.06,0.06,0.6,0); box(o,WHITE,0,1.1,0,1.6,0.06,0.06,-0.6,0);
        return o;
    });

    public static final ObjectModel CLOCK = ObjectModel.animated((t,r) -> {
        List<ObjectModel.Part> o=parts(); double radius=Math.max(0.5,r);
        for(int i=0;i<12;i++) { double a=i*Math.PI/6;
            box(o,i%3==0?GOLD:WHITE,Math.cos(a)*radius,0.13,Math.sin(a)*radius,0.06,0.05,0.24,0,Math.PI/2-a); }
        double a=-t*0.045;
        kr.newgodwar.ability.feedback.ModelParts.bar(o,GOLD,0,0,Math.cos(a)*radius*0.82,Math.sin(a)*radius*0.82);
        kr.newgodwar.ability.feedback.ModelParts.bar(o,WHITE,0,0,Math.cos(a*0.3+1)*radius*0.52,Math.sin(a*0.3+1)*radius*0.52);
        cube(o,GOLD,0,0.15,0,0.12); return o;
    });
    public static final ObjectModel RAVENS = flock(false);
    public static final ObjectModel BEES = flock(true);
    public static final ObjectModel BOMB = ObjectModel.animated((t,d) -> {
        List<ObjectModel.Part> o=parts(); cube(o,DARK,0,0.25,0,0.32);
        box(o,"RED_CONCRETE",0,0.25,0,0.34,0.06,0.34,0,0);
        line(o,"DARK_OAK_PLANKS",0,0.43,0.12,0.58,0,0.045);
        cube(o,"ORANGE_CONCRETE",0.12,0.6,0,0.05+Math.sin(t*0.5)*0.015); return o;
    });
    public static final ObjectModel EXPLOSION_CHARGE = ObjectModel.animated((t,d) -> {
        List<ObjectModel.Part> o=parts(); double radius=Math.max(1,d);
        for(int i=0;i<8;i++) { double a=i*Math.PI/4, r=radius*(0.5+0.2*Math.cos(t*0.13));
            box(o,i%2==0?"ORANGE_CONCRETE":GOLD,Math.cos(a)*r,0.5+Math.sin(t*0.15+i)*0.2,Math.sin(a)*r,0.12,0.55,0.12,0,a); }
        cube(o,"RED_CONCRETE",0,0.8,0,0.3+d*0.06); return o;
    });
    public static final ObjectModel ABYSS = ObjectModel.animated((t,d) -> {
        List<ObjectModel.Part> o=parts();
        for(int i=0;i<12;i++) { double a=i*Math.PI/6;
            box(o,DARK,Math.cos(a)*d,0.2,Math.sin(a)*d,0.12,0.45,0.12,0,a);
            box(o,"PURPLE_CONCRETE",Math.cos(a)*(d-0.22),0.1,Math.sin(a)*(d-0.22),0.07,0.18,0.07,0,a); }
        return o;
    });
    public static final ObjectModel LAUREL = ObjectModel.animated((t,d) -> {
        List<ObjectModel.Part> o=parts();
        for(int s:new int[]{-1,1}) for(int i=0;i<4;i++) {
            double a=0.3+i*0.32;
            box(o,GOLD,s*Math.sin(a)*0.45,2.2+Math.cos(a)*0.3,0,0.1,0.2,0.05,-s*a,0);
        }
        // Stable topology; unearned victory gems stay small instead of creating extra entities.
        for(int i=0;i<3;i++) cube(o,"LIME_CONCRETE",(i-1)*0.16,2.6,0,i<d?0.09:0.02);
        return o;
    });

    private static ObjectModel flock(boolean bees) {
        return ObjectModel.animated((t,d)->{ List<ObjectModel.Part> o=parts(); int count=bees?3:2;
            for(int i=0;i<count;i++) { double a=t*0.07+i*Math.PI*2/count,x=Math.cos(a)*0.8,z=Math.sin(a)*0.8,y=bees?1.3:2.5;
                box(o,bees?GOLD:DARK,x,y,z,0.2,0.16,0.28,0,a);
                cube(o,bees?DARK:GOLD,x,y,z+0.17,bees?0.1:0.06);
                for(int s:new int[]{-1,1}) box(o,bees?WHITE:DARK,x+s*0.2,y+Math.sin(t*0.7)*0.08,z,
                    bees?0.19:0.3,0.035,bees?0.12:0.17,s*Math.sin(t*0.7)*0.55,a);
                if(bees) box(o,DARK,x,y,z,0.21,0.17,0.055,0,a);
            } return o; });
    }
    private static DesignedEffect effect(String name, ObjectModel model) { return effectAt(name, model, 0, 0); }
    // Shoulder/overhead props leave the wearer's eye-level sightline clear in first person.
    private static DesignedEffect effectAt(String name, ObjectModel model, double x, double y) {
        return new DesignedEffect(name, ObjectModel.animated((phase, detail) -> {
            List<ObjectModel.Part> out = parts();
            for (ObjectModel.Part p : model.parts(phase, detail))
                out.add(new ObjectModel.Part(p.material, p.item, p.x + x, p.y + y, p.z,
                    p.sx, p.sy, p.sz, p.roll, p.turn));
            return out;
        }));
    }
    private static List<ObjectModel.Part> parts() { return new ArrayList<ObjectModel.Part>(); }
    private static void cube(List<ObjectModel.Part> o, String m, double x, double y, double z, double s) { box(o,m,x,y,z,s,s,s,0,0); }
    private static void line(List<ObjectModel.Part> o, String m, double x1, double y1, double x2, double y2, double z, double width) {
        box(o,m,(x1+x2)/2,(y1+y2)/2,z,width,Math.max(0.01,Math.hypot(x2-x1,y2-y1)),width,-Math.atan2(x2-x1,y2-y1),0);
    }
    private static void beam(List<ObjectModel.Part> o, String m, double x1, double y1, double z1,
                             double x2, double y2, double z2, double width) {
        double dx=x2-x1,dy=y2-y1,dz=z2-z1,length=Math.sqrt(dx*dx+dy*dy+dz*dz);
        box(o,m,(x1+x2)/2,(y1+y2)/2,(z1+z2)/2,width,width,Math.max(0.01,length),
            Math.atan2(dy,dx),length<0.001?0:Math.acos(Math.max(-1,Math.min(1,dz/length))));
    }
    private static void item(List<ObjectModel.Part> o, String m, double x, double y, double z, double s, double turn) {
        o.add(new ObjectModel.Part(m,true,x,y,z,s,s,s,0,turn));
    }
    private static void arc(List<ObjectModel.Part> o, String m, double x, double y, double z, double r, double start, double sweep, int n, double width) {
        for(int i=0;i<n;i++) { double a=start+sweep*i/n,b=start+sweep*(i+1)/n;
            line(o,m,x+Math.cos(a)*r,y+Math.sin(a)*r,x+Math.cos(b)*r,y+Math.sin(b)*r,z,width); }
    }
    private static DesignedEffect bow(String name, boolean venom, boolean moon) { return bow(name,venom,moon,moon?WHITE:"DARK_OAK_PLANKS"); }
    private static DesignedEffect bow(String name, boolean venom, boolean moon, String wood) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts(); double pull=Math.sin(Math.min(1,t/12)*Math.PI)*0.22;
            arc(o,wood,0,1.2,0.65,0.52,-Math.PI/2,Math.PI,6,0.06);
            line(o,WHITE,0,0.68,-pull,1.2,0.65,0.018); line(o,WHITE,-pull,1.2,0,1.72,0.65,0.018);
            double arrow=-pull+Math.max(0,t-8)*0.055;
            line(o,venom?"LIME_CONCRETE":GOLD,-0.25+arrow,1.2,0.7+arrow,1.2,0.65,0.045);
            line(o,WHITE,0.55+arrow,1.32,0.7+arrow,1.2,0.65,0.055);
            line(o,WHITE,0.55+arrow,1.08,0.7+arrow,1.2,0.65,0.055); return o; });
    }
    private static DesignedEffect serpent(String name,String m,boolean staff) {
        return effectAt(name,(t,d)->{ List<ObjectModel.Part> o=parts();
            if(staff) { line(o,"DARK_OAK_PLANKS",0,0.55,0,1.95,0.65,0.065); cube(o,GOLD,0,2.02,0.65,0.14); }
            for(int i=0;i<9;i++) { double a=i*0.65+t*0.08,b=a+0.65;
                beam(o,m,Math.cos(a)*0.25,0.65+i*0.11,0.65+Math.sin(a)*0.17,
                    Math.cos(b)*0.25,0.76+i*0.11,0.65+Math.sin(b)*0.17,0.065); }
            double head=9*0.65+t*0.08;
            cube(o,m,Math.cos(head)*0.25,1.64,0.65+Math.sin(head)*0.17,0.14);
            return o; },staff?0.75:0,0);
    }
    private static DesignedEffect stream(String name,String m,int n,boolean behind) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts();
            for(int j=0;j<n;j++) for(int i=0;i<4;i++) {
                double x=(j-(n-1)/2D)*0.27+Math.sin(i*0.6+t*0.12)*0.12;
                box(o,m,x,behind?0.25+i*0.07:0.4+i*0.32,behind?-0.4-i*0.25-t*0.018:0.55,
                    0.07,behind?0.04:0.22,behind?0.3:0.07,behind?0:Math.sin(t*0.1+i)*0.5,0);
            } return o; });
    }
    private static DesignedEffect feather(String name,String m,int n) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts();
            for(int i=0;i<n;i++) { double x=(i-(n-1)/2D)*0.32, y=1.15+Math.abs(x)*0.5+Math.sin(t*0.12)*0.15;
                line(o,GOLD,x,y-0.3,x+0.12,y+0.4,-0.4,0.03);
                box(o,m,x,y,-0.4,0.16,0.55,0.05,-x*0.6,0);
            } return o; });
    }
    private static DesignedEffect readyFists(String name, String material) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts();
            for(int side:new int[]{-1,1}) {
                double pulse=.10+.025*Math.sin(t*.7);
                box(o,material,side*.4,.95,.22,pulse,pulse,pulse,0,0);
            }
            return o;
        });
    }

    private static DesignedEffect impact(String name,String m,int n,boolean crystal) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts();
            for(int i=0;i<n;i++) { double a=i*Math.PI*2/n, r=0.18+t*0.025;
                box(o,m,Math.cos(a)*r,1+Math.sin(a)*r,0.7,0.09,crystal?0.3:0.18,0.1,-a+t*0.03,0); } return o; });
    }
    private static DesignedEffect gear(String name,String m,int n) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts(); double turn=t*0.055;
            arc(o,m,0.4,1.2,0.65,0.3,turn,Math.PI*2,n,0.045);
            for(int i=0;i<n;i++) { double a=i*Math.PI*2/n+turn;
                box(o,m,0.4+Math.cos(a)*0.36,1.2+Math.sin(a)*0.36,0.65,0.11,0.11,0.12,a,0); }
            line(o,WHITE,0.1,1.2,0.7,1.2,0.65,0.025); return o; });
    }
    private static DesignedEffect book(String name,String material,String ink,boolean scatter) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts(); double open=0.2+Math.min(1,t/6)*0.25;
            for(int side:new int[]{-1,1}) box(o,WHITE,side*open,1.25,0.7,0.43,0.5,0.045,0,side*(0.45-t*0.015));
            line(o,ink,0,0.99,0,1.51,0.7,0.065); item(o,material,0,1.25,0.75,0.26,0);
            for(int i=0;i<3;i++) box(o,ink,(i-1)*(scatter?0.18+t*0.025:0.15),1.65+i*0.13+t*0.01,0.7,0.12,0.025,0.025,scatter?t*0.03:0,0);
            return o; });
    }
    private static DesignedEffect sun(String name,int rays,boolean mirror) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts();
            cube(o,mirror?"LIGHT_BLUE_STAINED_GLASS":"ORANGE_CONCRETE",0,2.5,0,0.32);
            arc(o,GOLD,0,2.5,0,0.3,0,Math.PI*2,8,0.055);
            for(int i=0;i<rays;i++) { double a=i*Math.PI*2/rays+t*0.025;
                line(o,GOLD,Math.cos(a)*0.41,2.5+Math.sin(a)*0.41,Math.cos(a)*0.59,2.5+Math.sin(a)*0.59,0,0.055); }
            if(!mirror) for(int s:new int[]{-1,1}) for(int i=0;i<3;i++) box(o,GOLD,s*(0.48+i*0.17),2.4-i*0.06,0,0.2,0.065,0.1,s*0.3,0);
            return o; });
    }
    private static DesignedEffect flower(String name,String m,int petals,boolean surround) {
        return effectAt(name,(t,d)->{ List<ObjectModel.Part> o=parts(); double r=0.15+Math.min(t,8)*0.027;
            cube(o,GOLD,0,surround?1.15:1.8,0.6,0.12);
            for(int i=0;i<petals;i++) { double a=i*Math.PI*2/petals;
                box(o,m,Math.cos(a)*r,(surround?1.15:1.8)+Math.sin(a)*r,0.6,0.18,0.3,0.07,a-Math.PI/2,t*0.02); }
            line(o,"GREEN_CONCRETE",0,0.6,0,surround?0.95:1.7,0.6,0.04); return o; },surround?0:0.85,0);
    }
    private static DesignedEffect blades(String name,String m,int n) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts();
            for(int i=0;i<n;i++) { double s=i==0?1:-1, a=s*(-0.7+t*0.055);
                box(o,m,s*0.2,1.2,0.7,0.08,0.85,0.06,a,0);
                box(o,GOLD,s*0.2,0.82,0.7,0.32,0.055,0.1,a,0);
                box(o,"DARK_OAK_PLANKS",s*0.2,0.67,0.7,0.07,0.22,0.07,a,0); } return o; });
    }
    private static DesignedEffect forge(String name,boolean quench) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts();
            box(o,"IRON_BLOCK",0.4,0.85,0.6,0.58,0.12,0.3,0,0);
            box(o,"IRON_BLOCK",0.4,0.71,0.6,0.2,0.2,0.22,0,0);
            box(o,"IRON_BLOCK",0.4,0.59,0.6,0.46,0.06,0.3,0,0);
            double lift=t<7?Math.abs(Math.sin(Math.PI*(t-1)/3)):0;
            double y=1.03+lift*0.5;
            box(o,quench?"ORANGE_CONCRETE":"IRON_BLOCK",0.4,y,0.6,0.25,0.18,0.2,0,0);
            line(o,"DARK_OAK_PLANKS",0.5,y,0.85,y+0.3,0.6,0.05);
            for(int i=0;i<4;i++) cube(o,quench?WHITE:GOLD,0.4+(i-1.5)*0.2,1+(t%8)*0.025,0.6,0.045); return o; });
    }
    private static DesignedEffect eye(String name,String m) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts(); double h=Math.max(0.025,0.19*(1-t/20));
            for(int s:new int[]{-1,1}) { line(o,m,-0.45,2,0,2+s*h,0.6,0.055); line(o,m,0,2+s*h,0.45,2,0.6,0.055); }
            cube(o,"PURPLE_CONCRETE",0,2,0.6,0.1); return o; });
    }
    private static DesignedEffect aegis() {
        return effect("몸 양옆을 감싸 잠기는 여섯 무적 장갑판", (t,d)->{
            List<ObjectModel.Part> o=parts();
            double radius=0.68+0.08*Math.sin(t*0.12);
            // Keep the forward sightline open: a frontal ring blocks the caster's first-person view.
            for(int side:new int[]{-1,1}) for(int i=0;i<3;i++) {
                double angle=side*(Math.PI/4+i*Math.PI/4);
                box(o,GOLD,Math.sin(angle)*radius,1.05,Math.cos(angle)*radius,
                    0.26,0.6,0.055,0,angle);
            }
            return o;
        });
    }
    private static DesignedEffect plates(String name,String m,int n,boolean radial) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts();
            for(int i=0;i<n;i++) { double a=i*Math.PI*2/n, spread=0.25+0.2*Math.sin(t*0.12);
                box(o,m,radial?Math.cos(a)*spread:(i-(n-1)/2D)*0.22,radial?1.2+Math.sin(a)*spread:1.1+Math.abs(i-(n-1)/2D)*0.12,
                    0.7,0.22,0.48,0.055,radial?a:0,Math.sin(t*0.15)*0.25);
            } return o; });
    }
    private static DesignedEffect veil(String name,String m,int n) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts();
            for(int i=0;i<n;i++) box(o,m,(i-(n-1)/2D)*0.22,1.05+Math.sin(t*0.15+i)*0.12,-0.38,0.2,0.85,0.025,0,(i-2)*0.2+t*0.035); return o; });
    }
    private static DesignedEffect lock(String name,boolean heart) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts(); String m=heart?"PINK_CONCRETE":"IRON_BLOCK";
            box(o,m,0,1.2,0.65,0.35,0.3,0.1,heart?Math.PI/4:0,0);
            arc(o,GOLD,0,1.4+Math.max(0,7-t)*0.025,0.65,0.13,0,Math.PI,4,0.04);
            for(int s:new int[]{-1,1}) for(int i=0;i<3;i++) box(o,m,s*(0.26+i*0.16+Math.max(0,7-t)*0.02),1.2,0.65,0.17,0.055,0.06,s*0.35,0);
            cube(o,DARK,0,1.2,0.72,0.065); return o; });
    }
    private static DesignedEffect fuse(String name) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts(); cube(o,"LIME_CONCRETE",0,1.2,0.65,0.27);
            for(int i=0;i<4;i++) { double a=i*Math.PI/2; line(o,WHITE,Math.cos(a)*0.25,1.2+Math.sin(a)*0.25,Math.cos(a)*0.5,1.2+Math.sin(a)*0.5,0.65,0.045); }
            for(int i=0;i<3;i++) cube(o,WHITE,(i-1)*0.13,1.65+t*0.006,0.65,0.05); return o; });
    }
    private static DesignedEffect sprout(String name,String m,boolean wheat) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts(); double h=0.6+Math.min(t,10)*0.025;
            line(o,"GREEN_CONCRETE",0,0.35,0,0.35+h,0.65,0.045);
            for(int s:new int[]{-1,1}) for(int i=0;i<(wheat?3:2);i++) box(o,m,s*(wheat?0.09:0.18),0.65+i*0.15,0.65,wheat?0.11:0.3,0.08,0.07,s*0.6,0);
            cube(o,"DARK_OAK_PLANKS",-0.08,0.35,0.65,0.14); cube(o,"DARK_OAK_PLANKS",0.08,0.35,0.65,0.14); return o; });
    }
    private static DesignedEffect chalice(String name) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts();
            arc(o,GOLD,0,1.3,0.65,0.24,Math.PI,Math.PI,5,0.07); line(o,GOLD,0,0.7,0,1.08,0.65,0.05);
            box(o,GOLD,0,0.7,0.65,0.35,0.04,0.12,0,0);
            for(int i=0;i<4;i++) cube(o,"PURPLE_CONCRETE",Math.sin(i*2+t*0.1)*0.18,1.38+i*0.12,0.65,0.095); return o; });
    }
    private static DesignedEffect gate(String name,String m,boolean split) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts(); double x=0.35+Math.sin(t*Math.PI/18)*0.2;
            for(int s:new int[]{-1,1}) { box(o,m,s*x,1.1,0,0.075,1.65,0.12,split?s*0.2:0,s*t*0.04);
                box(o,m,s*(x-0.15),1.9,0,0.35,0.075,0.12,split?s*0.2:0,0);
                box(o,m,s*(x-0.15),0.3,0,0.35,0.075,0.12,split?-s*0.2:0,0); } return o; });
    }
    private static DesignedEffect hook(String name) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts(); line(o,WHITE,0,1.8,0,0.95,0.65,0.025);
            arc(o,"IRON_BLOCK",-0.12,0.95,0.65,0.12,Math.PI,Math.PI,4,0.045);
            for(int s:new int[]{-1,1}) arc(o,"LIGHT_BLUE_CONCRETE",s*0.3,0.6,0.65,0.2,0,Math.PI,4,0.035); return o; });
    }
    private static DesignedEffect roots(String name,boolean red) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts();
            for(int j=0;j<4;j++) for(int i=0;i<3;i++) { double a=j*Math.PI/2+i*0.45+t*0.025;
                box(o,red?DARK:"DARK_OAK_PLANKS",Math.cos(a)*0.43,0.1+i*0.18,Math.sin(a)*0.43,0.08,0.27,0.08,-Math.sin(a)*0.5,a);
            }
            for(int j=0;j<4;j++) cube(o,red?"RED_CONCRETE":"LIME_CONCRETE",Math.cos(j*Math.PI/2+0.9)*0.4,0.64,Math.sin(j*Math.PI/2+0.9)*0.4,0.11);
            return o; });
    }
    private static DesignedEffect fist(String name,String m,boolean both) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts();
            for(int j=0;j<(both?2:1);j++) { double x=j==0?0.38:-0.38, z=0.45+Math.min(t,10)*0.035;
                box(o,m,x,1,z,0.28,0.23,0.25,0,0);
                for(int i=0;i<3;i++) box(o,WHITE,x+(i-1)*0.075,1.12,z+0.12,0.055,0.06,0.06,0,0);
                if(!both) for(int i=0;i<3;i++) line(o,m,x+(i-1)*0.18,0.65,x+(i-1)*0.26,0.35,0.65,0.06);
            } return o; });
    }
    private static DesignedEffect spoon(String name) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts(); line(o,GOLD,0.35,0.75,0.35,1.3,0.65,0.05);
            box(o,GOLD,0.35,1.4,0.65,0.2,0.25,0.04,0,0);
            for(int i=0;i<3;i++) cube(o,GOLD,0.15+i*0.2,1.8+Math.sin(t*0.15+i)*0.1,0.65,0.1); return o; });
    }
    private static DesignedEffect moons(String name,int n) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts();
            for(int i=0;i<n;i++) arc(o,i==1?"PURPLE_CONCRETE":WHITE,(i-(n-1)/2D)*0.48,2.3+Math.sin(t*0.1+i)*0.1,0,0.26,0.7,4.8,6,0.065); return o; });
    }
    private static DesignedEffect herbs(String name) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts();
            arc(o,"DARK_OAK_PLANKS",0,0.8,0.6,0.26,Math.PI,Math.PI,5,0.08);
            for(int s:new int[]{-1,1}) { line(o,"GREEN_CONCRETE",0,0.8,s*0.2,1.5+t*0.007,0.6,0.035);
                for(int i=0;i<3;i++) box(o,"LIME_CONCRETE",s*(0.12+i*0.035),1+i*0.16,0.6,0.2,0.07,0.04,s*0.5,0); } return o; });
    }
    private static DesignedEffect rainbow() {
        return effect("세 색 무지개 다리가 위로 펼쳐짐",(t,d)->{ List<ObjectModel.Part> o=parts();
            String[] colors={"PINK_CONCRETE","YELLOW_CONCRETE","LIGHT_BLUE_CONCRETE"};
            for(int i=0;i<3;i++) arc(o,colors[i],0,1.6,0,0.55+i*0.12,0,Math.PI,6,0.06);
            for(int s:new int[]{-1,1}) box(o,WHITE,s*0.65,1.45,0,0.25,0.12,0.12,0,0); return o; });
    }
    private static DesignedEffect mask(String name,String m,boolean horns) {
        return effectAt(name,(t,d)->{ List<ObjectModel.Part> o=parts();
            for(int s:new int[]{-1,1}) { box(o,m,s*(0.15+t*0.009),1.7,0.55,0.25,0.45,0.035,s*t*0.025,0);
                box(o,DARK,s*0.14,1.8,0.59,0.12,0.04,0.035,0,0);
                if(horns) line(o,GOLD,s*0.2,1.95,s*0.4,2.35,0.55,0.06); }
            box(o,m,0,1.4,0.55,0.22,0.1,0.04,0,0); return o; },0,0.7);
    }
    private static DesignedEffect dream() {
        return effect("초승달 옆으로 작은 Z가 떠오름",(t,d)->{ List<ObjectModel.Part> o=parts();
            arc(o,WHITE,-0.18,2.4,0,0.26,0.7,4.8,7,0.07); double y=2.35+t*0.012;
            line(o,"LIGHT_BLUE_CONCRETE",0.25,y+0.2,0.48,y+0.2,0,0.035);
            line(o,"LIGHT_BLUE_CONCRETE",0.48,y+0.2,0.25,y,0,0.035);
            line(o,"LIGHT_BLUE_CONCRETE",0.25,y,0.48,y,0,0.035); return o; });
    }
    private static DesignedEffect rocket() {
        return effect("발아래 세 겹으로 벌어지는 로켓 분사 노즐",(t,d)->{ List<ObjectModel.Part> o=parts();
            for(int s:new int[]{-1,1}) { box(o,"IRON_BLOCK",s*0.24,0.2,-0.1,0.17,0.25,0.2,0,0);
                for(int i=0;i<3;i++) box(o,i==0?WHITE:"ORANGE_CONCRETE",s*0.24,-0.03-i*0.18,-0.1,0.12-i*0.025,0.2,0.12-i*0.025,0,0); } return o; });
    }
    private static DesignedEffect coins(String name,int n) {
        return effect(name,(t,d)->{ List<ObjectModel.Part> o=parts();
            for(int i=0;i<n;i++) box(o,GOLD,(i-(n-1)/2D)*(0.1+t*0.018),1.2+Math.sin(i+t*0.1)*0.16,0.65,0.2,0.2,0.035,Math.PI/4,t*0.12); return o; });
    }
    private static DesignedEffect trident() {
        return effect("삼지창 세 날 사이로 솟는 두 물결",(t,d)->{ List<ObjectModel.Part> o=parts();
            line(o,"PRISMARINE",0,0.25,0,1.9,0.65,0.065);
            for(int s:new int[]{-1,1}) { line(o,"PRISMARINE",0,1.25,s*0.3,1.4,0.65,0.07); line(o,"PRISMARINE",s*0.3,1.4,s*0.3,1.8,0.65,0.065);
                arc(o,"LIGHT_BLUE_CONCRETE",s*0.5,0.6,0.65,0.25,t*0.05,Math.PI,4,0.035); } return o; });
    }
    private static DesignedEffect cross() {
        return effect("두 성광판 사이에 완성되는 축복의 십자",(t,d)->{ List<ObjectModel.Part> o=parts();
            line(o,GOLD,0,1.6,0,2.35,0.6,0.09); line(o,GOLD,-0.25,2.1,0.25,2.1,0.6,0.09);
            for(int s:new int[]{-1,1}) box(o,WHITE,s*(0.4+Math.sin(t*0.15)*0.1),1.9,0.6,0.06,0.5,0.035,s*0.3,0); return o; });
    }
    private static DesignedEffect record() {
        return effectAt("회전하는 레코드와 튀어 오르는 세 음표",(t,d)->{ List<ObjectModel.Part> o=parts();
            arc(o,DARK,0,1.8,0.6,0.3,t*0.1,Math.PI*2,8,0.1); cube(o,"RED_CONCRETE",0,1.8,0.6,0.12);
            for(int i=0;i<3;i++) { double x=(i-1)*0.4,y=2.25+Math.sin(t*0.2+i)*0.1;
                cube(o,GOLD,x,y,0.6,0.09); line(o,GOLD,x+0.04,y,x+0.04,y+0.24,0.6,0.035); } return o; },0,0.6);
    }
    private static DesignedEffect feast() {
        return effect("펼쳐진 접시 위로 떠오르는 세 음식",(t,d)->{ List<ObjectModel.Part> o=parts();
            box(o,WHITE,0.35,0.85,0.65,0.75,0.035,0.35,0,0);
            String[] food={"BREAD","APPLE","COOKED_BEEF"};
            for(int i=0;i<3;i++) item(o,food[i],0.05+i*0.3,1.12+Math.sin(t*0.18+i)*0.08,0.65,0.24,0); return o; });
    }
    private static DesignedEffect scope() {
        return effect("준비 완료를 알리는 네 조준 쇠와 중앙 조준점",(t,d)->{ List<ObjectModel.Part> o=parts(); double r=0.25+Math.max(0,10-t)*0.018;
            for(int i=0;i<4;i++) { double a=i*Math.PI/2;
                line(o,"IRON_BLOCK",Math.cos(a)*r,1.3+Math.sin(a)*r,Math.cos(a)*(r+0.2),1.3+Math.sin(a)*(r+0.2),0.65,0.04); }
            cube(o,"RED_CONCRETE",0,1.3,0.65,0.06); return o; });
    }
    private static DesignedEffect snowflake() {
        return effect("여섯 갈래 얼음 결정이 바깥으로 자람",(t,d)->{ List<ObjectModel.Part> o=parts();
            for(int i=0;i<6;i++) { double a=i*Math.PI/3;
                line(o,"PACKED_ICE",0,1.2,Math.cos(a)*0.48,1.2+Math.sin(a)*0.48,0.65,0.045);
                for(int s:new int[]{-1,1}) line(o,"PACKED_ICE",Math.cos(a)*0.3,1.2+Math.sin(a)*0.3,Math.cos(a)*0.3+Math.cos(a+s)*0.14,1.2+Math.sin(a)*0.3+Math.sin(a+s)*0.14,0.65,0.035);
            } return o; });
    }
    private static DesignedEffect anchor() {
        return effect("발밑에 박히는 닻과 좌우 받침",(t,d)->{ List<ObjectModel.Part> o=parts();
            line(o,"IRON_BLOCK",0,0.15,0,0.8,0.55,0.07); line(o,"IRON_BLOCK",-0.28,0.63,0.28,0.63,0.55,0.055);
            for(int s:new int[]{-1,1}) { line(o,"IRON_BLOCK",0,0.15,s*0.35,0.38,0.55,0.07); line(o,"IRON_BLOCK",s*0.35,0.38,s*0.35,0.55,0.55,0.06); } return o; });
    }
    private static DesignedEffect cards() {
        return effect("부채처럼 벌어진 화투 세 장 뒤의 검날",(t,d)->{ List<ObjectModel.Part> o=parts();
            for(int i=0;i<3;i++) { box(o,WHITE,(i-1)*0.2,1.2,0.6,0.22,0.36,0.025,(i-1)*0.35,0);
                cube(o,"RED_CONCRETE",(i-1)*0.2,1.2,0.63,0.08); }
            line(o,"IRON_BLOCK",-0.4,0.95,0.45,1.5,0.7,0.045); return o; });
    }
    private static DesignedEffect cup() {
        return effect("작은 찻잔에서 올라오는 세 증기 조각",(t,d)->{ List<ObjectModel.Part> o=parts();
            box(o,WHITE,0.35,1.05,0.65,0.3,0.25,0.25,0,0); arc(o,WHITE,0.55,1.08,0.65,0.1,-Math.PI/2,Math.PI,4,0.04);
            for(int i=0;i<3;i++) cube(o,WHITE,0.35+Math.sin(t*0.15+i)*0.06,1.35+i*0.15,0.65,0.045); return o; });
    }
    private static DesignedEffect doll() {
        return effectAt("실로 꿰맨 인형에 꽂히는 세 바늘",(t,d)->{ List<ObjectModel.Part> o=parts();
            cube(o,"DARK_OAK_PLANKS",0,1.65,0.65,0.2); box(o,"DARK_OAK_PLANKS",0,1.3,0.65,0.2,0.45,0.12,0,0);
            for(int s:new int[]{-1,1}) { line(o,"DARK_OAK_PLANKS",0,1.4,s*0.3,1.3,0.65,0.08); line(o,"DARK_OAK_PLANKS",s*0.06,1.1,s*0.18,0.85,0.65,0.08); }
            for(int i=0;i<3;i++) line(o,"IRON_BLOCK",0.05,1.15+i*0.16,0.4+Math.max(0,8-t)*0.02,1.3+i*0.16,0.7,0.025); return o; },0.75,0);
    }
    private static DesignedEffect cauldron() {
        return effect("마녀 솥에서 올라오는 세 독방울",(t,d)->{ List<ObjectModel.Part> o=parts();
            arc(o,DARK,0,0.9,0.65,0.27,Math.PI,Math.PI,6,0.12); line(o,DARK,-0.3,0.92,0.3,0.92,0.65,0.045);
            for(int i=0;i<3;i++) cube(o,"LIME_CONCRETE",Math.sin(t*0.15+i)*0.2,1.1+i*0.2,0.65,0.09-i*0.02); return o; });
    }
    private static DesignedEffect ship() {
        return effect("거북선의 등판과 양쪽 노가 아군 앞을 지킴",(t,d)->{ List<ObjectModel.Part> o=parts();
            box(o,"DARK_OAK_PLANKS",0,1.1,0.75,0.9,0.22,0.25,0,0);
            for(int i=0;i<3;i++) box(o,"IRON_BLOCK",(i-1)*0.25,1.35-Math.abs(i-1)*0.07,0.75,0.25,0.12,0.3,0,0);
            for(int s:new int[]{-1,1}) for(int i=0;i<2;i++) line(o,"DARK_OAK_PLANKS",s*0.4,1.15+i*0.12,s*0.7,0.9+i*0.12,0.75,0.045); return o; });
    }
    private static DesignedEffect banner() {
        return effect("아군 곁에서 펼쳐지는 흰 깃발과 붉고 푸른 중심",(t,d)->{ List<ObjectModel.Part> o=parts();
            line(o,"DARK_OAK_PLANKS",-0.35,0.7,-0.35,2.2,0.65,0.04);
            for(int i=0;i<3;i++) box(o,WHITE,-0.2+i*0.2,1.85,0.65+Math.sin(t*0.15+i)*0.045,0.22,0.45,0.025,0,0);
            box(o,"RED_CONCRETE",0,1.91,0.74,0.16,0.09,0.025,0,0); box(o,"BLUE_CONCRETE",0,1.82,0.74,0.16,0.09,0.025,0,0); return o; });
    }
}

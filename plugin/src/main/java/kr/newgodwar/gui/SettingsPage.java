package kr.newgodwar.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Public navigation destinations; confirmation dialogs are intentionally not destinations. */
public enum SettingsPage {
    MAIN("main", "설정 메인", "전체 설정 메뉴", SettingsView.MAIN, "home", "메인"),
    GAME("game", "게임 진행", "시작 조건, 능력 추첨, 지급, 시작/종료", SettingsView.GAME, "게임", "진행"),
    TEAM("team", "팀", "팀 배정, 색상, 스폰, 심장 · 팀 이름을 붙이면 상세 화면", SettingsView.TEAM, "teams", "팀"),
    WORLD("world", "월드", "게임 월드, 초기화, 시간, 난이도", SettingsView.WORLD, "월드", "맵"),
    CORE("core", "코어 / 게임룰", "심장 보호, 팀킬, 게임룰", SettingsView.WORLD_CORE, "gamerule", "코어", "게임룰"),
    PROTECTION("protection", "코어 보호", "곡괭이별 심장 파괴 허용 시간", SettingsView.PICKAXE_UNLOCK, "pickaxe", "보호", "곡괭이"),
    DISPLAY("display", "표시 / 우르프", "스코어보드, 팀 Prefix, 보스바, 우르프", SettingsView.DISPLAY, "urf", "표시", "우르프"),
    GAMBLING("gambling", "도박", "도박 사용, 가격, 보상", SettingsView.GAMBLING, "gamble", "도박"),
    REWARDS("rewards", "도박 확률", "도박 상품별 확률 조정", SettingsView.GAMBLING_NORMAL, "reward", "chance", "보상", "확률"),
    ITEMS("items", "기본 지급 아이템", "시작 아이템 창고", null, "kit", "starteritems", "기본템", "시작템");

    private final String id;
    private final String title;
    private final String description;
    final SettingsView view;
    private final List<String> aliases;

    SettingsPage(String id, String title, String description, SettingsView view, String... aliases) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.view = view;
        this.aliases = Arrays.asList(aliases);
    }

    public String id() { return id; }
    public String title() { return title; }
    public String description() { return description; }

    public static SettingsPage parse(String token) {
        String lower = token == null ? "" : token.toLowerCase(Locale.ROOT);
        for (SettingsPage page : values()) {
            if (page.id.equals(lower) || page.aliases.contains(lower)) return page;
        }
        return null;
    }

    public static List<String> suggestions() {
        List<String> result = new ArrayList<String>();
        for (SettingsPage page : values()) {
            result.add(page.id);
            result.addAll(page.aliases);
        }
        return result;
    }
}

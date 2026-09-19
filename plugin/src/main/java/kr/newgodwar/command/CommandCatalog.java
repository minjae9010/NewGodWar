package kr.newgodwar.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Shared command names, permissions and help for every entry point. */
final class CommandCatalog {
    private static final Map<String, Entry> NAMES = new LinkedHashMap<String, Entry>();
    private static final Map<String, Entry> SHORTCUTS = new LinkedHashMap<String, Entry>();
    private static final List<Entry> ENTRIES = new ArrayList<Entry>();

    static {
        add("help", "general", false, "[분류|검색어|페이지] [페이지]", "분류별 도움말 / 명령어 검색", "ghelp", "h", "?", "도움말", "도움");
        add("status", "game", false, "", "현재 게임 상태 확인", "gstatus", "st", "상태");
        add("tips", "general", false, "", "서버 플레이 팁 보기", null, "tip", "팁");
        add("info", "team", false, "[team]", "본인 또는 지정한 팀의 팀원 확인", null, "i", "팀정보");
        add("ability", "ability", false, "[player]", "내 능력 / 같은 팀 능력 확인 (/a help로 관리 도움말)", null, "a", "능력");
        add("abilities", "ability", false, "[검색어]", "등록된 능력 도감 검색", null, "book", "도감");
        add("yes", "ability", false, "", "현재 능력 확정", "gconfirm", "y", "confirm", "확정");
        add("no", "ability", false, "", "내 능력 다시 뽑기", "greroll", "n", "rr", "다시뽑기");
        add("target", "ability", false, "<player>", "타깃형 능력 대상 지정", null, "x", "대상");
        add("gamble", "general", false, "", "도박 GUI 열기", null, "con", "도박");
        add("start", "game", true, "", "게임 시작 및 능력 배정", "gstart", "go", "시작");
        add("stop", "game", true, "", "게임 종료", "gstop", "end", "종료");
        add("test", "game", true, "[ability]", "혼자 능력 테스트 시작", null, "테스트");
        add("skip", "game", true, "[초]", "능력 확정 대기 종료 및 시작 카운트다운 조정", "gskip", "스킵");
        add("autoteam", "team", true, "", "온라인 플레이어 자동 팀 배정", "gautoteam", "at", "자동팀");
        add("join", "team", true, "<team> <player>", "플레이어 팀 수동 배정", "gjoin", "j", "team", "t", "배정");
        add("changeteam", "team", true, "<player> <team>", "능력을 유지한 채 팀 변경 (팀/플레이어 순서 교환 가능)", null, "ct", "teamchange", "switchteam", "팀변경", "팀바꾸기");
        add("midjoin", "team", true, "<player> [team|auto]", "진행 중 게임에 중간 참여", null, "mj", "cutin", "중간참여");
        add("leave", "team", true, "<player>", "플레이어 팀 배정 해제", null, "out", "팀해제");
        add("participants", "team", true, "[검색어|팀]", "참가자의 팀, 능력, 킬, 관전 상태 확인", "gplayers", "p", "participant", "players", "users", "list", "plist", "참가자", "유저");
        add("setspawn", "world", true, "<team>", "현재 위치를 팀 스폰으로 등록", null, "s", "ss", "spawn", "스폰설정");
        add("settemple", "world", true, "<team>", "바라보는 다이아 블록을 팀 심장으로 등록", null, "d", "dia", "심장설정");
        add("setlobby", "world", true, "", "현재 위치를 접속/종료 로비로 등록", null, "sl", "lobby", "로비", "로비설정");
        add("map", "world", true, "[world|clear|help]", "게임 맵 선택 / 해제 / 상세 도움말", "gmap", "maps", "맵", "지도");
        add("world", "world", true, "[help|gui|list|game|create|load|copy|tp|lobby|unload|delete|backup]", "월드 관리 및 상세 도움말", "gworld", "w", "worlds", "월드");
        add("setability", "ability", true, "<player> <ability>", "능력 수동 지정 (/a set도 사용 가능)", null, "sa", "능력지정");
        add("randomability", "ability", true, "[player]", "랜덤 능력 배정 (대상 생략 시 전체 참가자)", null);
        add("removeability", "ability", true, "<player>", "플레이어 능력 삭제", null);
        add("resetabilities", "ability", true, "[player]", "능력 배정 초기화 (대상 생략 시 전체)", null);
        add("assignedabilities", "ability", true, "[검색어]", "플레이어별 배정 능력 확인", null, "assigned", "assignments", "배정목록");
        add("clear", "ability", true, "[player|self|all]", "능력 쿨타임 초기화 (콘솔에서는 대상 필수)", "gcd", "c", "cd", "쿨초기화");
        add("rerolls", "settings", true, "<횟수>", "능력 재추첨 가능 횟수 설정", null, "reroll", "reassign", "재추첨", "재지정");
        add("skipseconds", "settings", true, "<초>", "관리자 skip 기본 카운트다운 설정", null, "skipsecond", "skiptime", "스킵초", "스킵시간");
        add("pickaxe", "settings", true, "[종류|all] [open|off|분]", "곡괭이 코어 파괴 허용 시간 확인 / 조정", null, "pickaxes", "곡괭이", "곡괭");
        add("urf", "settings", true, "[on|off|toggle|80%]", "우르프 모드와 쿨타임 감소율 설정", null, "우르프");
        add("blacklist", "settings", true, "<list|add|remove|toggle> [ability]", "랜덤 배정 제외 능력 관리", null, "black", "bl", "블랙리스트");
        add("gamerule", "settings", true, "<apply|restore>", "설정된 게임룰 수동 적용 / 복구", null, "게임룰");
        add("spectate", "team", true, "<player>", "플레이어 관전 모드 전환", null, "spec", "관전");
        add("unspectate", "team", true, "<player>", "플레이어 관전 해제", null, "unspec", "관전해제");
        add("observer", "team", true, "[list]", "내 옵저버 모드 전환 / 옵저버 목록 확인", null, "obs", "옵저버");
        add("settings", "settings", true, "[화면] [team]", "설정 화면 바로 열기 (/gw gui help로 화면 목록)", "gmenu", "gui", "set", "menu", "setting", "설정", "메뉴");
        add("defaultitems", "settings", true, "[gui|list|add|set|remove|clear|reset]", "게임 시작 기본 지급 아이템 창고 / 목록 관리", "gkit", "kit", "items", "defaultitem", "starteritem", "starteritems", "skyblockitem", "skyblockitems", "기본템", "시작템");
        add("gamblereward", "settings", true, "<normal> <번호|add> <hand|message|material> [값]", "도박 보상 아이템 / 멘트 수정", null, "gamblerewards", "gamblerwd", "도박상품");
        add("reload", "admin", true, "", "설정 파일 다시 불러오기", null, "rl", "리로드");
        add("update", "admin", true, "[check|download]", "업데이트 확인 / 다음 재시작용 다운로드", null, "updates", "업데이트");
    }

    private CommandCatalog() { }

    private static void add(String name, String category, boolean admin, String arguments,
                            String description, String shortcut, String... aliases) {
        Entry entry = new Entry(name, category, admin, arguments, description, shortcut, aliases);
        ENTRIES.add(entry);
        for (String token : entry.names()) {
            if (NAMES.put(token, entry) != null) {
                throw new IllegalStateException("Duplicate command alias: " + token);
            }
        }
        if (shortcut != null && SHORTCUTS.put(shortcut, entry) != null) {
            throw new IllegalStateException("Duplicate shortcut: " + shortcut);
        }
    }

    static Entry find(String name) {
        return NAMES.get(lower(name));
    }

    static String normalize(String name) {
        Entry entry = find(name);
        return entry == null ? lower(name) : entry.name;
    }

    static boolean requiresAdmin(String name) {
        Entry entry = find(name);
        return entry == null || entry.admin;
    }

    static List<String> names(boolean admin) {
        List<String> result = new ArrayList<String>();
        for (Entry entry : ENTRIES) {
            if (admin || !entry.admin) result.addAll(entry.names());
        }
        return result;
    }

    static Set<String> shortcutCommands() {
        return Collections.unmodifiableSet(SHORTCUTS.keySet());
    }

    static String[] expandShortcut(String command, String[] args) {
        Entry entry = SHORTCUTS.get(lower(command));
        if (entry == null) return args;
        String[] expanded = new String[args.length + 1];
        expanded[0] = entry.name.equals("settings") ? "gui" : entry.name;
        System.arraycopy(args, 0, expanded, 1, args.length);
        return expanded;
    }

    static List<Entry> helpEntries(boolean admin, String query) {
        String search = lower(query);
        String category = category(search);
        List<Entry> result = new ArrayList<Entry>();
        for (Entry entry : ENTRIES) {
            if (entry.admin && !admin) continue;
            if (search.isEmpty() || search.equals("all") || search.equals("전체")
                || (category != null && entry.category.equals(category))
                || (isShortcuts(search) && (entry.shortcut != null || !entry.aliases.isEmpty()))
                || (category == null && !isShortcuts(search) && entry.matches(search))) {
                result.add(entry);
            }
        }
        return result;
    }

    static List<String> helpTopics(boolean admin) {
        List<String> topics = new ArrayList<String>(Arrays.asList("all", "shortcuts", "general", "game", "team", "ability", "간편", "기본", "게임", "팀", "능력"));
        if (admin) topics.addAll(Arrays.asList("world", "map", "settings", "admin", "setup", "server", "cooldown", "월드", "맵", "설정", "관리", "위치설정", "서버", "쿨타임"));
        return topics;
    }

    static boolean isShortcuts(String value) {
        return value.equals("shortcuts") || value.equals("short") || value.equals("간편") || value.equals("단축");
    }

    private static String category(String value) {
        if (value.equals("general") || value.equals("기본")) return "general";
        if (value.equals("game") || value.equals("게임")) return "game";
        if (value.equals("team") || value.equals("팀")) return "team";
        if (value.equals("ability") || value.equals("능력")) return "ability";
        if (value.equals("world") || value.equals("월드")) return "world";
        if (value.equals("settings") || value.equals("설정")) return "settings";
        if (value.equals("admin") || value.equals("관리")) return "admin";
        return null;
    }

    static List<String> suggest(String token, boolean admin) {
        String input = lower(token);
        Set<String> suggestions = new LinkedHashSet<String>();
        if (input.isEmpty()) return new ArrayList<String>();
        for (String name : names(admin)) {
            if (name.startsWith(input) || (input.length() >= 3 && distance(input, name) <= 1)) {
                suggestions.add(normalize(name));
                if (suggestions.size() == 3) break;
            }
        }
        return new ArrayList<String>(suggestions);
    }

    private static int distance(String left, String right) {
        if (Math.abs(left.length() - right.length()) > 1) return 2;
        int[] row = new int[right.length() + 1];
        for (int j = 0; j < row.length; j++) row[j] = j;
        for (int i = 1; i <= left.length(); i++) {
            int previous = row[0];
            row[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int old = row[j];
                row[j] = Math.min(Math.min(row[j] + 1, row[j - 1] + 1), previous + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1));
                previous = old;
            }
        }
        return row[right.length()];
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    static final class Entry {
        final String name;
        final String category;
        final boolean admin;
        final String arguments;
        final String description;
        final String shortcut;
        final List<String> aliases;

        Entry(String name, String category, boolean admin, String arguments, String description, String shortcut, String[] aliases) {
            this.name = name;
            this.category = category;
            this.admin = admin;
            this.arguments = arguments;
            this.description = description;
            this.shortcut = shortcut;
            this.aliases = Collections.unmodifiableList(Arrays.asList(aliases));
        }

        String usage() {
            return name + (arguments.isEmpty() ? "" : " " + arguments);
        }

        List<String> names() {
            List<String> names = new ArrayList<String>();
            names.add(name);
            names.addAll(aliases);
            return names;
        }

        boolean matches(String query) {
            String text = lower(usage() + " " + CommandTree.preferredPath(name) + " " + description + " " + aliases + " " + (shortcut == null ? "" : shortcut));
            for (String token : query.split("\\s+")) if (!text.contains(token)) return false;
            return true;
        }
    }
}

package kr.newgodwar.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Translates structured commands to the same handlers used by legacy shortcuts. */
final class CommandTree {
    private static final Map<String, Group> ROOTS = new LinkedHashMap<String, Group>();

    static {
        Group game = root("game", "게임 진행", null, "게임");
        game.action("status", "현재 게임 상태", "status", "상태");
        game.action("tips", "서버 플레이 팁", "tips", "팁");
        game.action("start", "게임 시작", "start", "시작");
        game.action("stop", "게임 종료", "stop", "종료");
        game.action("test", "[ability] 혼자 능력 테스트", "test", "테스트");
        game.action("skip", "[초] 능력 선택 대기 종료", "skip", "스킵");
        game.action("dummy", "[spawn|remove] 타깃 테스트 더미", "dummy", "더미");

        Group team = root("team", "팀 / 참가자", null, "teams", "팀");
        team.action("info", "[team] 팀원 확인", "info", "정보");
        team.action("auto", "자동 팀 배정", "autoteam", "자동");
        team.action("join", "<team> <player> 팀 배정", "join", "배정");
        team.action("change", "<player> <team> 능력을 유지한 팀 변경", "changeteam", "변경");
        team.action("midjoin", "<player> [team|auto] 중간 참여", "midjoin", "cutin", "중간참여");
        team.action("leave", "<player> 팀 배정 해제", "leave", "해제");
        team.action("list", "[검색어|팀] 참가자 현황", "participants", "players", "목록");
        team.action("spectate", "<player> 관전 전환", "spectate", "관전");
        team.action("unspectate", "<player> 관전 해제", "unspectate", "관전해제");
        team.action("observer", "[list] 옵저버 전환 / 목록", "observer", "옵저버");

        Group ability = root("ability", "능력", "ability", "a", "능력");
        ability.action("show", "[player] 능력 확인", "ability", "보기");
        ability.action("catalog", "[검색어] 능력 도감", "abilities", "book", "all", "도감");
        ability.action("confirm", "내 능력 확정", "yes", "yes", "확정");
        ability.action("reroll", "내 능력 다시 뽑기", "no", "no", "다시뽑기");
        ability.action("target", "<player> 타깃형 능력 대상 지정", "target", "대상");
        ability.action("list", "[검색어] 배정 능력 목록", "assignedabilities", "assigned", "assignments", "배정");
        ability.action("set", "<player> <ability> 능력 지정", "setability", "assign", "지정");
        ability.action("random", "[player] 랜덤 능력 배정 (생략 시 전체 참가자)", "randomability", "랜덤");
        ability.action("remove", "<player> 능력 삭제", "removeability", "삭제");
        ability.action("reset", "[player] 능력 배정 초기화 (생략 시 전체)", "resetabilities", "초기화");
        ability.action("skip", "[초] 능력 선택 대기 종료", "skip", "스킵");
        ability.action("cutin", "<player> [team|auto] 중간 참여", "midjoin", "중간참여");
        Group cooldown = root("cooldown", "능력 쿨타임", null, "쿨타임");
        cooldown.action("reset", "[player|self|all] 쿨타임 초기화 (생략 시 본인, 콘솔은 대상 필수)", "clear", "clear", "초기화");
        ability.group("cooldown", cooldown, "cd", "쿨타임");

        Group settings = root("settings", "게임 설정", "settings", "setting", "config", "설정");
        settings.action("open", "[화면] [team] 설정 화면 바로 열기", "settings", "gui", "열기");
        settings.action("items", "[gui|list|add|set|remove|clear|reset] 기본 지급 아이템", "defaultitems", "kit", "기본템");
        settings.action("rerolls", "<횟수> 재추첨 횟수 설정", "rerolls", "재추첨");
        settings.action("skipseconds", "<초> 기본 시작 카운트다운", "skipseconds", "스킵시간");
        settings.action("pickaxe", "[종류|all] [open|off|분] 곡괭이 허용 시간", "pickaxe", "곡괭이");
        settings.action("urf", "[on|off|toggle|80%] 우르프 설정", "urf", "우르프");
        settings.action("blacklist", "<list|add|remove|toggle> [ability] 제외 능력", "blacklist", "블랙리스트");
        settings.action("gamerule", "<apply|restore> 게임룰", "gamerule", "게임룰");
        settings.action("rewards", "<normal> <번호|add> <hand|message|material> [값] 도박 보상", "gamblereward", "보상");

        Group setup = root("setup", "맵 위치 등록", null, "위치설정");
        setup.action("spawn", "<team> 현재 위치를 팀 스폰으로 등록", "setspawn", "스폰");
        setup.action("temple", "<team> 바라보는 다이아 블록을 심장으로 등록", "settemple", "심장");
        setup.action("lobby", "현재 위치를 로비로 등록", "setlobby", "로비");

        Group server = root("server", "서버 관리", null, "admin", "서버");
        server.action("reload", "설정 다시 불러오기", "reload", "리로드");
        server.action("update", "[check|download] 업데이트 관리", "update", "업데이트");
    }

    private CommandTree() { }

    private static Group root(String name, String title, String fallback, String... aliases) {
        Group group = new Group(name, title, fallback);
        ROOTS.put(name, group);
        for (String alias : aliases) ROOTS.put(alias, group);
        return group;
    }

    static List<String> roots(boolean admin) {
        List<String> names = new ArrayList<String>();
        for (Map.Entry<String, Group> entry : ROOTS.entrySet()) {
            if (entry.getValue().visible(admin)) names.add(entry.getKey());
        }
        return names;
    }

    static Result resolve(String[] args) {
        if (args.length == 0) return new Result(args, null, null);
        Group root = ROOTS.get(lower(args[0]));
        return root == null ? new Result(args, null, null) : resolve(root, args, 1, root.name);
    }

    private static Result resolve(Group group, String[] args, int index, String path) {
        if (args.length == index) {
            if (group.fallback != null) return new Result(new String[] {group.fallback}, null, null);
            return new Result(args, group, path);
        }
        String token = lower(args[index]);
        if (isHelp(token)) return new Result(args, group, path);
        Action action = group.actions.get(token);
        if (action == null) {
            if (group.name.equals("settings") && kr.newgodwar.gui.SettingsPage.parse(token) != null) {
                return new Result(replacePrefix("settings", args, index), null, null);
            }
            // Preserve /gw a <player|ability> and /gw team <team> <player>.
            if (group.name.equals("ability")) return new Result(replacePrefix("ability", args, index), null, null);
            if (group.name.equals("team") && kr.newgodwar.game.GodTeam.parse(token) != null) {
                return new Result(replacePrefix("join", args, index), null, null);
            }
            return new Result(args, group, path);
        }
        if (action.child != null) return resolve(action.child, args, index + 1, path + " " + action.name);
        return new Result(replacePrefix(action.target, args, index + 1), null, null, action.target.equals("ability"));
    }

    /** Returns null when argument completion should be delegated to the leaf handler. */
    static List<String> complete(String[] args, boolean admin) {
        if (args.length < 2) return null;
        Group group = ROOTS.get(lower(args[0]));
        if (group == null) return null;
        for (int index = 1; index < args.length; index++) {
            if (index == args.length - 1) {
                List<String> values = new ArrayList<String>();
                if (!group.visible(admin)) return values;
                values.addAll(Arrays.asList("help", "도움말"));
                for (Map.Entry<String, Action> entry : group.actions.entrySet()) {
                    if (entry.getValue().visible(admin)) values.add(entry.getKey());
                }
                if (group.name.equals("settings")) values.addAll(kr.newgodwar.gui.SettingsPage.suggestions());
                return values;
            }
            Action action = group.actions.get(lower(args[index]));
            if (action == null) {
                if (group.name.equals("settings") && kr.newgodwar.gui.SettingsPage.parse(args[index]) != null) return null;
                if (group.name.equals("ability") || (group.name.equals("team") && kr.newgodwar.game.GodTeam.parse(args[index]) != null)) return null;
                return Collections.emptyList();
            }
            if (!action.visible(admin)) return Collections.emptyList();
            if (action.child == null) return null;
            group = action.child;
        }
        return null;
    }

    static String preferredPath(String command) {
        for (Group group : ROOTS.values()) {
            for (Action action : group.actions.values()) {
                if (action.child == null && action.target.equals(command)) return group.name + " " + action.name;
                if (action.child != null) {
                    for (Action nested : action.child.actions.values()) {
                        if (nested.target.equals(command)) return group.name + " " + action.name + " " + nested.name;
                    }
                }
            }
        }
        return command;
    }

    private static boolean isHelp(String value) {
        return value.equals("help") || value.equals("h") || value.equals("?") || value.equals("도움말");
    }

    private static String[] replacePrefix(String target, String[] args, int start) {
        String[] prefix = target.split(" ");
        String[] result = Arrays.copyOf(prefix, prefix.length + args.length - start);
        System.arraycopy(args, start, result, prefix.length, args.length - start);
        return result;
    }

    private static String lower(String value) { return value.toLowerCase(Locale.ROOT); }

    static final class Result {
        final String[] args;
        final Group help;
        final String path;
        final boolean abilityView;
        Result(String[] args, Group help, String path) { this(args, help, path, false); }
        Result(String[] args, Group help, String path, boolean abilityView) {
            this.args = args; this.help = help; this.path = path; this.abilityView = abilityView;
        }
    }

    static final class Group {
        final String name;
        final String title;
        final String fallback;
        final Map<String, Action> actions = new LinkedHashMap<String, Action>();
        Group(String name, String title, String fallback) { this.name = name; this.title = title; this.fallback = fallback; }
        void action(String name, String description, String target, String... aliases) {
            if (CommandCatalog.find(target) == null) throw new IllegalStateException("Unknown command target: " + target);
            put(new Action(name, description, target, CommandCatalog.requiresAdmin(target), null), aliases);
        }
        void group(String name, Group child, String... aliases) {
            put(new Action(name, child.title + " 관리", null, true, child), aliases);
        }
        void put(Action action, String[] aliases) {
            actions.put(action.name, action);
            for (String alias : aliases) actions.put(alias, action);
        }
        boolean visible(boolean admin) {
            for (Action action : actions.values()) if (action.visible(admin)) return true;
            return false;
        }
    }

    static final class Action {
        final String name;
        final String description;
        final String target;
        final boolean admin;
        final Group child;
        Action(String name, String description, String target, boolean admin, Group child) {
            this.name = name; this.description = description; this.target = target; this.admin = admin; this.child = child;
        }
        boolean visible(boolean isAdmin) { return child == null ? isAdmin || !admin : child.visible(isAdmin); }
    }
}

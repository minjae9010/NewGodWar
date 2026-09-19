package kr.newgodwar.command;

import kr.newgodwar.gui.SettingsPage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** All command references use one bounded layout and one navigation model. */
final class CommandHelp {
    private static final int PAGE_SIZE = 5;

    private CommandHelp() { }

    static void show(CommandSender sender, String[] terms) {
        Query query = Query.parse(terms);
        if (query == null) {
            sender.sendMessage("§c페이지는 1 이상의 숫자입니다. 예: /gw help ability 2");
            return;
        }
        boolean admin = sender.hasPermission("newgodwar.admin");
        if (query.text.isEmpty() && !query.explicitPage) { overview(sender, admin); return; }
        String topic = query.text.isEmpty() ? "all" : query.text;
        List<Row> rows = rows(topic, admin);
        if (rows.isEmpty()) {
            sender.sendMessage("§e사용할 수 있는 명령 중 검색 결과가 없습니다: §f" + topic);
            HelpChat.send(sender, "§7 /gw help §8· 목차로 돌아가기", "/gw help", "도움말 목차", true);
            return;
        }
        render(sender, title(topic), rows, query.page, "/gw help " + topic);
    }

    static void group(CommandSender sender, CommandTree.Group group, String path, String[] pageArguments) {
        boolean admin = sender.hasPermission("newgodwar.admin");
        if (!group.visible(admin)) { sender.sendMessage("§c관리자 권한이 필요합니다."); return; }
        Query query = Query.parse(pageArguments);
        if (query == null || !query.text.isEmpty()) {
            sender.sendMessage("§c사용법: /gw " + path + " help [페이지]");
            return;
        }
        render(sender, group.title, groupRows(group, path, admin), query.page, "/gw " + path + " help");
    }

    static void gui(CommandSender sender, String[] pageArguments) {
        if (!sender.hasPermission("newgodwar.admin")) { sender.sendMessage("§c관리자 권한이 필요합니다."); return; }
        Query query = Query.parse(pageArguments);
        if (query == null || !query.text.isEmpty()) { sender.sendMessage("§c사용법: /gw gui help [페이지]"); return; }
        render(sender, "설정 화면 바로가기", guiRows(), query.page, "/gw gui help");
    }

    static List<String> complete(String[] terms, boolean admin) {
        List<String> values = new ArrayList<String>();
        if (terms.length <= 1) {
            values.addAll(CommandCatalog.helpTopics(admin));
            if (admin) values.addAll(Arrays.asList("gui", "화면"));
            for (CommandCatalog.Entry entry : CommandCatalog.helpEntries(admin, "")) values.add(CommandTree.preferredPath(entry.name));
            for (int page = 1; page <= pages(rows("all", admin).size()); page++) values.add(String.valueOf(page));
        } else {
            String topic = String.join(" ", Arrays.copyOf(terms, terms.length - 1));
            for (int page = 1; page <= pages(rows(topic, admin).size()); page++) values.add(String.valueOf(page));
            for (CommandCatalog.Entry entry : CommandCatalog.helpEntries(admin, "")) {
                String path = CommandTree.preferredPath(entry.name);
                if (path.startsWith(topic + " ")) values.add(path.substring(topic.length() + 1));
            }
        }
        return new ArrayList<String>(new LinkedHashSet<String>(values));
    }

    static List<String> groupPages(CommandTree.Group group, String path, boolean admin) {
        List<String> values = new ArrayList<String>();
        if (!group.visible(admin)) return values;
        for (int page = 1; page <= pages(groupRows(group, path, admin).size()); page++) values.add(String.valueOf(page));
        return values;
    }

    private static void overview(CommandSender sender, boolean admin) {
        HelpChat.header(sender, "명령어 안내", 1, 1, -1);
        category(sender, "게임", "상태 · 시작 · 종료", "game");
        category(sender, "팀", "팀원 · 배정 · 참가자", "team");
        category(sender, "능력", "확인 · 도감 · 추첨" + (admin ? " · 쿨타임" : ""), "ability");
        if (admin) {
            category(sender, "설정", "설정 값 변경 · 기본 아이템", "settings");
            category(sender, "설정 화면", "원하는 GUI로 바로 이동", "gui");
            category(sender, "맵 / 월드", "맵 선택 · 생성 · 백업", "world");
            category(sender, "위치 등록", "팀 스폰 · 심장 · 로비", "setup");
            category(sender, "서버", "설정 다시 읽기 · 업데이트", "server");
        }
        category(sender, "간편 명령", "/a · /tc · 짧은 별칭", "shortcuts");
        sender.sendMessage("§7 검색: §f/gw help <검색어> §8| §7전체: §f/gw help all");
        sender.sendMessage(sender instanceof Player
            ? "§8 분류 클릭: 이동 · 명령 클릭: 입력창에 넣기 · <필수> [선택]"
            : "§8 /gw help <분류>로 상세 보기 · <필수> [선택]");
    }

    private static void category(CommandSender sender, String name, String summary, String topic) {
        HelpChat.send(sender, "§e ▸ " + name + " §8· §7" + summary + " §8(/gw help " + topic + ")",
            "/gw help " + topic, "§f" + name + " 도움말 열기", true);
    }

    private static List<Row> rows(String topic, boolean admin) {
        String lower = topic.toLowerCase(Locale.ROOT);
        if (lower.equals("gui") || lower.equals("화면") || lower.equals("메뉴")) return admin ? guiRows() : new ArrayList<Row>();
        if (lower.equals("world") || lower.equals("월드") || lower.equals("worlds")) return admin ? worldRows() : new ArrayList<Row>();
        if (lower.equals("map") || lower.equals("맵") || lower.equals("maps") || lower.equals("지도")) return admin ? mapRows() : new ArrayList<Row>();
        String treeTopic = lower.equals("admin") || lower.equals("관리") ? "server" : lower;
        String[] path = (treeTopic + " help").split(" ");
        CommandTree.Result route = CommandTree.resolve(path);
        if (route.help != null && route.help.visible(admin) && route.path.split(" ").length == path.length - 1) {
            return groupRows(route.help, route.help.name.equals("cooldown") ? "ability cooldown" : route.path, admin);
        }
        List<Row> result = new ArrayList<Row>();
        for (CommandCatalog.Entry entry : CommandCatalog.helpEntries(admin, topic)) {
            String preferred = CommandTree.preferredPath(entry.name);
            String usage = "/gw " + preferred + (entry.arguments.isEmpty() ? "" : " " + entry.arguments);
            String shortcut = entry.shortcut == null ? "/gw " + entry.name : "/" + entry.shortcut;
            if (CommandCatalog.isShortcuts(lower)) usage = shortcut + (entry.arguments.isEmpty() ? "" : " " + entry.arguments);
            result.add(new Row(usage, entry.description, CommandCatalog.isShortcuts(lower) ? shortcut : "/gw " + preferred, entry.admin,
                "간편: " + shortcut + "\n별칭: " + String.join(", ", entry.aliases)));
        }
        if (admin && (lower.equals("all") || lower.equals("전체") || !CommandCatalog.helpTopics(true).contains(lower))) {
            List<Row> reference = new ArrayList<Row>(worldRows());
            reference.addAll(mapRows());
            reference.addAll(guiRows());
            for (Row row : reference) {
                if (lower.equals("all") || lower.equals("전체") || row.matches(lower)) result.add(row);
            }
        }
        if (CommandCatalog.isShortcuts(lower) || lower.equals("all") || lower.equals("전체") || lower.equals("general") || lower.equals("기본")) {
            result.add(new Row("/a [player]", "내 능력 / 같은 팀 능력 확인", "/a", false, "/a help로 능력 명령 전체 보기"));
            result.add(new Row("/tc [message]", "팀 채팅 전송 · 생략하면 모드 전환", "/tc", false, "/teamchat, /팀채팅"));
            result.add(new Row("/x <player>", "타깃형 능력 대상 지정", "/x", false, "/gw ability target <player>"));
        }
        LinkedHashMap<String, Row> unique = new LinkedHashMap<String, Row>();
        for (Row row : result) unique.put(row.usage, row);
        return new ArrayList<Row>(unique.values());
    }

    private static List<Row> groupRows(CommandTree.Group group, String path, boolean admin) {
        List<Row> rows = new ArrayList<Row>();
        for (CommandTree.Action action : new LinkedHashSet<CommandTree.Action>(group.actions.values())) {
            if (!action.visible(admin)) continue;
            if (action.child != null) {
                rows.add(new Row("/gw " + path + " " + action.name + " help", action.description, "/gw " + path + " " + action.name + " help", true, "하위 동작 보기"));
            } else {
                CommandCatalog.Entry entry = CommandCatalog.find(action.target);
                String usage = "/gw " + path + " " + action.name + (entry.arguments.isEmpty() ? "" : " " + entry.arguments);
                String shortcut = entry.shortcut == null ? "/gw " + entry.name : "/" + entry.shortcut;
                rows.add(new Row(usage, entry.description, "/gw " + path + " " + action.name, entry.admin, "간편: " + shortcut + "\n별칭: " + String.join(", ", entry.aliases)));
            }
        }
        return rows;
    }

    private static List<Row> guiRows() {
        List<Row> rows = new ArrayList<Row>();
        for (SettingsPage page : SettingsPage.values()) {
            rows.add(new Row("/gw gui " + page.id() + (page == SettingsPage.TEAM ? " [team]" : ""),
                page.title() + " · " + page.description(), "/gw gui " + page.id(), true,
                "간편: /gmenu " + page.id() + "\n/gw settings open " + page.id()));
        }
        return rows;
    }

    private static List<Row> worldRows() {
        List<Row> rows = new ArrayList<Row>();
        reference(rows, "world list", "로드된 월드와 로드할 수 있는 폴더 목록");
        reference(rows, "world gui", "월드 설정 화면 열기 (/gw gui world)");
        reference(rows, "world game <world|clear>", "게임 월드 지정 / 해제");
        reference(rows, "map <world>", "게임 맵 선택 · 맵별 스폰과 심장 설정 사용");
        reference(rows, "world create <world> [normal|flat|void]", "새 월드 생성");
        reference(rows, "world load <world> [normal|flat|void]", "기존 월드 로드");
        reference(rows, "world copy <sourceWorld> <newWorld> [normal|flat|void]", "월드 복사");
        reference(rows, "world tp <world> [player]", "본인 또는 지정 플레이어 이동");
        reference(rows, "world lobby [player]", "저장된 로비로 이동");
        reference(rows, "world unload <world> [save]", "플레이어가 없는 월드 언로드");
        reference(rows, "world backup create [이름]", "설정된 백업 대상 월드 저장");
        reference(rows, "world backup list", "저장된 백업 목록");
        reference(rows, "world backup load <이름> [newWorld]", "백업을 새 월드로 로드");
        reference(rows, "world delete <world> confirm", "월드와 폴더 삭제 · confirm 필수");
        return rows;
    }

    private static List<Row> mapRows() {
        List<Row> rows = new ArrayList<Row>();
        reference(rows, "map list", "사용 가능한 맵과 현재 선택 확인");
        reference(rows, "map select <world>", "게임 맵 선택 · 필요하면 자동 로드");
        reference(rows, "map clear", "선택된 맵 해제");
        reference(rows, "setup spawn <team>", "선택한 맵에 팀 스폰 등록");
        reference(rows, "setup temple <team>", "선택한 맵에 다이아 심장 등록");
        return rows;
    }

    private static void reference(List<Row> rows, String usage, String description) {
        String input = usage.split(" [<\\[]", 2)[0];
        rows.add(new Row("/gw " + usage, description, "/gw " + input, true, "<필수> [선택] 인수를 채워 입력하세요."));
    }

    private static void render(CommandSender sender, String title, List<Row> rows, int requestedPage, String prefix) {
        int pages = pages(rows.size());
        int page = Math.min(requestedPage, pages);
        HelpChat.header(sender, title, page, pages, rows.size());
        sender.sendMessage(sender instanceof Player
            ? "§8 명령 클릭: 입력창에 넣기 · 마우스를 올리면 간편 명령 표시"
            : "§8 <필수> [선택] · 간편 명령: /gw help shortcuts");
        for (int index = (page - 1) * PAGE_SIZE; index < Math.min(rows.size(), page * PAGE_SIZE); index++) {
            Row row = rows.get(index);
            HelpChat.send(sender, "§b " + row.usage + (row.admin ? " §8[관리]" : ""), row.input + " ", "§f" + row.description + "\n§7" + row.detail, false);
            sender.sendMessage("§7   " + row.description);
        }
        HelpChat.footer(sender, prefix, page, pages);
    }

    private static int pages(int count) { return Math.max(1, (count + PAGE_SIZE - 1) / PAGE_SIZE); }

    private static String title(String topic) {
        topic = topic.toLowerCase(Locale.ROOT);
        if (topic.equals("all") || topic.equals("전체")) return "전체 명령어";
        if (CommandCatalog.isShortcuts(topic)) return "간편 명령어";
        if (topic.equals("gui") || topic.equals("화면")) return "설정 화면 바로가기";
        if (topic.equals("world") || topic.equals("월드") || topic.equals("worlds")) return "월드 관리";
        if (topic.equals("map") || topic.equals("맵") || topic.equals("maps")) return "맵 선택";
        CommandTree.Result route = CommandTree.resolve((topic + " help").split(" "));
        if (route.help != null) return route.help.title;
        return "검색: " + topic;
    }

    private static final class Row {
        final String usage, description, input, detail;
        final boolean admin;
        Row(String usage, String description, String input, boolean admin, String detail) {
            this.usage = usage; this.description = description; this.input = input; this.admin = admin; this.detail = detail;
        }
        boolean matches(String query) {
            String text = (usage + " " + description + " " + detail).toLowerCase(Locale.ROOT);
            for (String token : query.split("\\s+")) if (!text.contains(token)) return false;
            return true;
        }
    }

    static final class Query {
        final String text;
        final int page;
        final boolean explicitPage;
        Query(String text, int page, boolean explicitPage) { this.text = text; this.page = page; this.explicitPage = explicitPage; }
        static Query parse(String[] terms) {
            int length = terms.length;
            int page = 1;
            boolean explicit = length > 0 && terms[length - 1].matches("-?\\d+");
            if (explicit) {
                try { page = Integer.parseInt(terms[--length]); } catch (NumberFormatException ignored) { return null; }
                if (page < 1) return null;
            }
            return new Query(String.join(" ", Arrays.copyOf(terms, length)), page, explicit);
        }
    }
}

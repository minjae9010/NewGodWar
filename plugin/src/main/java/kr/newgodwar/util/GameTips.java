package kr.newgodwar.util;

import kr.newgodwar.NewGodWarPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GameTips {

    private static final String BLAZE_ROD_RECIPE_TIP = "&e막대기 3개&7를 가로·세로·대각선으로 놓으면 &6블레이즈 막대기&7가 돼요.";
    private static final int PAGE_SIZE = 3;
    private static final List<String> LEGACY_DEFAULT_TIPS = Arrays.asList(
        "&f블막 조합: &e막대기 3개&7를 세로/가로/대각선으로 놓으면 &6블레이즈 막대기 1개&7를 만들 수 있습니다.",
        "&f능력 확인: &b/a&7로 내 능력 설명과 쿨타임을 확인하세요.",
        "&f재추첨: &b/gw yes&7로 확정, &c/gw no&7로 다시 뽑기를 선택합니다.",
        "&f타깃 능력: &b/x <닉네임>&7으로 대상을 빠르게 지정합니다.",
        "&f팀 설정: &b/gw settings&7의 팀 메뉴에서 팀 추가, 이름, 색상, 스폰, 심장을 관리할 수 있습니다.",
        "&f비활성 팀: &7비활성화된 팀은 자동 배정, 중간 참여, 시작 검사에서 제외됩니다.",
        "&f우르프: &b/gw urf 80%&7처럼 쿨타임 감소율을 바로 조정할 수 있습니다.",
        "&f도박: &b/도박&7에서 코블스톤으로 추가 자원을 노려볼 수 있습니다.",
        "&f팀 채팅: &b/tc&7로 모드를 켜면 일반 채팅이 팀챗으로 전송됩니다."
    );

    private GameTips() {
    }

    public static void send(CommandSender sender, NewGodWarPlugin plugin) {
        send(sender, plugin, 1);
    }

    public static void send(CommandSender sender, NewGodWarPlugin plugin, int requestedPage) {
        List<String> tips = tips(plugin);
        if (tips.isEmpty()) {
            plugin.messages().send(sender, "&e등록된 게임 팁이 없습니다.");
            return;
        }
        int pages = (tips.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int page = Math.max(1, Math.min(pages, requestedPage));
        sender.sendMessage(plugin.messages().prefix() + ChatColor.GOLD + "게임 팁 " + page + "/" + pages);
        for (int index = (page - 1) * PAGE_SIZE; index < Math.min(tips.size(), page * PAGE_SIZE); index++) {
            sender.sendMessage(ChatColor.DARK_GRAY + " · " + plugin.messages().color(tips.get(index)));
        }
        if (page > 1) sender.sendMessage(ChatColor.GRAY + "이전 팁: " + ChatColor.YELLOW + "/gw tips " + (page - 1));
        if (page < pages) sender.sendMessage(ChatColor.GRAY + "다음 팁: " + ChatColor.YELLOW + "/gw tips " + (page + 1));
    }

    public static int broadcastOnStart(NewGodWarPlugin plugin) {
        if (!plugin.getConfig().getBoolean("tips.enabled", true)
            || !plugin.getConfig().getBoolean("tips.show-on-start", true)) {
            return 0;
        }
        return broadcastTip(plugin, 0);
    }

    public static int broadcastTip(NewGodWarPlugin plugin, int index) {
        if (!plugin.getConfig().getBoolean("tips.enabled", true)) {
            return index;
        }
        List<String> tips = tips(plugin);
        if (tips.isEmpty()) {
            return index;
        }
        int normalizedIndex = Math.floorMod(index, tips.size());
        Bukkit.broadcastMessage(plugin.messages().prefix() + ChatColor.GOLD + "팁 · "
            + plugin.messages().color(tips.get(normalizedIndex)));
        return Math.max(0, index) + 1;
    }

    public static boolean timedTipsEnabled(NewGodWarPlugin plugin) {
        return plugin.getConfig().getBoolean("tips.enabled", true)
            && plugin.getConfig().getBoolean("tips.timed.enabled", true);
    }

    public static long timedInitialDelaySeconds(NewGodWarPlugin plugin) {
        return Math.max(0L, plugin.getConfig().getLong("tips.timed.initial-delay-seconds", 60L));
    }

    public static long timedIntervalSeconds(NewGodWarPlugin plugin) {
        return Math.max(1L, plugin.getConfig().getLong("tips.timed.interval-seconds", 180L));
    }

    public static boolean repeatTimedTips(NewGodWarPlugin plugin) {
        return plugin.getConfig().getBoolean("tips.timed.repeat", true);
    }

    public static int count(NewGodWarPlugin plugin) {
        return tips(plugin).size();
    }

    public static boolean repairLegacyConfiguredTips(NewGodWarPlugin plugin) {
        List<String> configured = plugin.getConfig().getStringList("tips.lines");
        if (configured.isEmpty()) {
            return false;
        }

        List<String> repaired = new ArrayList<String>(configured.size());
        boolean changed = false;
        for (String tip : configured) {
            String repairedTip = tip;
            if (isLegacyBlazeRodRecipeTip(repairedTip)) {
                repairedTip = BLAZE_ROD_RECIPE_TIP;
            }
            repairedTip = conciseDefaultTip(repairLegacyCommandTips(repairedTip));
            if (repairedTip == null ? tip != null : !repairedTip.equals(tip)) {
                changed = true;
            }
            repaired.add(repairedTip);
        }

        if (changed) {
            plugin.getConfig().set("tips.lines", repaired);
            plugin.saveConfig();
        }
        return changed;
    }

    static String conciseDefaultTip(String tip) {
        int index = LEGACY_DEFAULT_TIPS.indexOf(tip);
        return index < 0 ? tip : defaultTips().get(index);
    }

    private static List<String> tips(NewGodWarPlugin plugin) {
        List<String> configured = plugin.getConfig().getStringList("tips.lines");
        return configured.isEmpty() ? defaultTips() : configured;
    }

    private static boolean isLegacyBlazeRodRecipeTip(String tip) {
        return tip != null
            && tip.contains("막대기 2개")
            && (tip.contains("블막") || tip.contains("블레이즈 막대") || tip.contains("블레이즈막대"))
            && (tip.contains("조합") || tip.contains("만들"));
    }

    private static String repairLegacyCommandTips(String tip) {
        if (tip == null) {
            return null;
        }
        return tip.replace("/godwar", "/gw")
            .replace("/t yes", "/gw yes")
            .replace("/t no", "/gw no");
    }

    private static List<String> defaultTips() {
        return Arrays.asList(
            BLAZE_ROD_RECIPE_TIP,
            "&b/a&7에서 내 능력의 사용법과 쿨타임을 볼 수 있어요.",
            "&7능력이 마음에 들면 &b/gw yes&7, 다시 뽑으려면 &b/gw no&7를 입력하세요.",
            "&7대상을 정하는 능력은 먼저 &b/x <닉네임>&7을 입력하세요.",
            "&b/gw abilities&7에서 다른 능력도 구경해 보세요.",
            "&7우리 팀의 다이아 심장이 깨지면 팀이 탈락해요. 심장을 지켜 주세요!",
            "&7우르프가 켜져 있으면 능력을 더 자주 쓸 수 있어요.",
            "&b/도박&7에서 조약돌로 아이템을 뽑아 보세요.",
            "&b/tc&7로 팀 채팅을 켜고 끌 수 있어요."
        );
    }
}

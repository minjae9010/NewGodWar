package kr.newgodwar.util;

import kr.newgodwar.NewGodWarPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GameTips {

    private static final String BLAZE_ROD_RECIPE_TIP = "&f블막 조합: &e막대기 3개&7를 세로/가로/대각선으로 놓으면 &6블레이즈 막대기 1개&7를 만들 수 있습니다.";

    private GameTips() {
    }

    public static void send(CommandSender sender, NewGodWarPlugin plugin) {
        List<String> tips = tips(plugin);
        if (tips.isEmpty()) {
            plugin.messages().send(sender, "&e등록된 게임 팁이 없습니다.");
            return;
        }
        sender.sendMessage(plugin.messages().prefix() + ChatColor.GOLD + "서버 플레이 팁");
        for (String tip : tips) {
            sender.sendMessage(ChatColor.DARK_GRAY + " - " + plugin.messages().color(tip));
        }
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
        Bukkit.broadcastMessage(plugin.messages().prefix() + ChatColor.GOLD + "게임 팁");
        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + " - " + plugin.messages().color(tips.get(normalizedIndex)));
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
            if (isLegacyBlazeRodRecipeTip(tip)) {
                repaired.add(BLAZE_ROD_RECIPE_TIP);
                changed = true;
            } else {
                repaired.add(tip);
            }
        }

        if (changed) {
            plugin.getConfig().set("tips.lines", repaired);
            plugin.saveConfig();
        }
        return changed;
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

    private static List<String> defaultTips() {
        return Arrays.asList(
            BLAZE_ROD_RECIPE_TIP,
            "&f능력 확인: &b/a&7로 내 능력 설명과 쿨타임을 확인하세요.",
            "&f재추첨: &b/t yes&7로 확정, &c/t no&7로 다시 뽑기를 선택합니다.",
            "&f타깃 능력: &b/x <닉네임>&7으로 대상을 빠르게 지정합니다.",
            "&f팀 설정: &b/godwar settings&7의 팀 메뉴에서 팀 추가, 이름, 색상, 스폰, 심장을 관리할 수 있습니다.",
            "&f비활성 팀: &7비활성화된 팀은 자동 배정, 중간 참여, 시작 검사에서 제외됩니다.",
            "&f우르프: &b/godwar urf 80%&7처럼 쿨타임 감소율을 바로 조정할 수 있습니다.",
            "&f도박: &b/도박&7에서 코블스톤으로 추가 자원을 노려볼 수 있습니다.",
            "&f팀 채팅: &b/tc&7로 모드를 켜면 일반 채팅이 팀챗으로 전송됩니다."
        );
    }
}

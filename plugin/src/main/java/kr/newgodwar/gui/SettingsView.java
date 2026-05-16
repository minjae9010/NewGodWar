package kr.newgodwar.gui;

import org.bukkit.ChatColor;

enum SettingsView {
    MAIN(ChatColor.BLACK + "설정 메인"),
    GAME(ChatColor.BLACK + "게임 진행 설정"),
    WORLD_CORE(ChatColor.BLACK + "월드 / 코어 설정"),
    DISPLAY(ChatColor.BLACK + "표시 / 우르프 설정"),
    GAMBLING(ChatColor.BLACK + "도박 설정"),
    GAMBLING_NORMAL(ChatColor.BLACK + "일반 도박 확률"),
    GAMBLING_TAJJA(ChatColor.BLACK + "타짜 도박 확률");

    final String title;

    SettingsView(String title) {
        this.title = title;
    }
}

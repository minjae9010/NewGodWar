package kr.newgodwar.gui;

import org.bukkit.ChatColor;

enum SettingsView {
    MAIN(ChatColor.BLACK + "설정 메인"),
    STOP_CONFIRM(ChatColor.BLACK + "게임 종료 확인"),
    GAME(ChatColor.BLACK + "게임 진행 설정"),
    TEAM(ChatColor.BLACK + "팀 설정"),
    TEAM_DETAIL(ChatColor.BLACK + "팀 상세 설정"),
    WORLD(ChatColor.BLACK + "월드 설정"),
    WORLD_CORE(ChatColor.BLACK + "심장 보호 설정"),
    WORLD_RULES(ChatColor.BLACK + "월드 게임룰 설정"),
    PICKAXE_UNLOCK(ChatColor.BLACK + "코어 보호 설정"),
    DISPLAY(ChatColor.BLACK + "화면 / 연출 설정"),
    COMBAT(ChatColor.BLACK + "전투 / 부활 설정"),
    GAMBLING(ChatColor.BLACK + "도박 설정"),
    GAMBLING_NORMAL(ChatColor.BLACK + "도박 확률");

    final String title;

    SettingsView(String title) {
        this.title = title;
    }
}

package kr.newgodwar.gui;

import java.util.Arrays;

/** Shared slot geometry. Rendering and click routing use the same mapping. */
final class ChestLayout {
    static final int[] CATALOG = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};

    private ChestLayout() { }

    static int catalogIndex(int slot) {
        for (int i = 0; i < CATALOG.length; i++) if (CATALOG[i] == slot) return i;
        return -1;
    }

    static int[] settings(SettingsView view) {
        int[] slots = new int[27];
        Arrays.fill(slots, -1);
        switch (view) {
            case MAIN:
                put(slots, 4,13, 10,19, 11,21, 12,23, 13,25, 14,28, 15,30, 16,32, 17,34, 23,39, 24,41); break;
            case STOP_CONFIRM:
                put(slots, 10,20, 16,24); break;
            case GAME:
                put(slots, 4,13, 0,10, 1,11, 2,12, 5,14, 6,15, 7,16,
                    9,19, 10,20, 11,21, 12,22, 13,23, 14,24, 19,25,
                    15,28, 16,29, 17,30, 18,37, 20,43, 21,39, 22,33); break;
            case TEAM:
                for (int i = 10; i <= 16; i++) slots[i] = i + 9;
                put(slots, 21,46, 23,52, 24,40); break;
            case TEAM_DETAIL:
                put(slots, 4,13, 10,20, 12,29, 13,22, 14,33, 16,24); break;
            case WORLD:
                put(slots, 0,10, 1,11, 2,12, 3,13, 5,14, 6,15, 7,16,
                    9,20, 10,21, 11,22, 13,24, 15,30, 16,32, 18,38, 19,42); break;
            case WORLD_CORE:
                put(slots, 9,13, 10,20, 11,22, 12,24, 18,31); break;
            case WORLD_RULES:
                put(slots, 0,10, 1,12, 2,14, 3,16, 10,20, 11,22, 12,24,
                    13,29, 14,31, 15,33, 18,40); break;
            case PICKAXE_UNLOCK:
                put(slots, 4,13, 10,20, 11,21, 12,23, 13,24, 16,31); break;
            case DISPLAY:
                put(slots, 0,10, 1,12, 2,14, 3,16, 10,19, 11,21, 12,23, 13,25,
                    14,28, 15,30, 16,32, 17,34, 18,37, 19,39, 20,41, 21,43); break;
            case COMBAT:
                put(slots, 10,20, 11,22, 12,24, 14,13, 19,29, 20,30, 21,31, 22,33, 23,40); break;
            case GAMBLING:
                put(slots, 10,13, 12,20, 13,22, 14,24, 15,31, 17,40); break;
            case GAMBLING_NORMAL:
                for (int i = 0; i < 21; i++) slots[i] = CATALOG[i];
                put(slots, 21,40, 22,46, 23,52); break;
            default: throw new IllegalArgumentException("Unknown settings view");
        }
        return slots;
    }

    static int logicalSlot(SettingsView view, int displaySlot) {
        if (displaySlot < 0) return -1;
        int[] slots = settings(view);
        for (int i = 0; i < slots.length; i++) if (slots[i] == displaySlot) return i;
        return -1;
    }

    private static void put(int[] slots, int... pairs) {
        for (int i = 0; i < pairs.length; i += 2) slots[pairs[i]] = pairs[i + 1];
    }
}

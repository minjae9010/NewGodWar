package kr.newgodwar.gui;

/** The whole three-slot button is clickable; browsing never spends currency. */
final class GamblingLayout {
    static final int SIZE = 36;
    static final int REWARDS = 29;
    static final int CLOSE = 35;
    static final int GALLERY_BACK = 49;
    private GamblingLayout() { }

    static boolean draw(int slot) { return (slot >= 12 && slot <= 14) || (slot >= 21 && slot <= 23); }
    static boolean rewards(int slot) { return slot >= 28 && slot <= 30; }
    static int pages(int count, int perPage) { return Math.max(1, (count + perPage - 1) / perPage); }
}

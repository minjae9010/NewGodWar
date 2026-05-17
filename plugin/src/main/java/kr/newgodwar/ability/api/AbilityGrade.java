package kr.newgodwar.ability.api;

public enum AbilityGrade {
    UNRATED("?", "미평가"),
    S("S", "최상위"),
    A("A", "강함"),
    B("B", "보통"),
    C("C", "약함"),
    D("D", "낮음");

    private final String symbol;
    private final String label;

    AbilityGrade(String symbol, String label) {
        this.symbol = symbol;
        this.label = label;
    }

    public String symbol() {
        return symbol;
    }

    public String label() {
        return label;
    }

    public String displayText() {
        return symbol + " (" + label + ")";
    }
}

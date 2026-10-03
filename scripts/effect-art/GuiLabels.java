import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Pre-rasterized Korean labels keep pack builds independent of host font availability. */
final class GuiLabels {
    private static BufferedImage atlas;
    private static final Map<String, int[]> labels = new HashMap<>();

    static void load(Path repo) throws Exception {
        Path dir = repo.resolve("scripts/effect-art");
        atlas = ImageIO.read(dir.resolve("menu-labels.png").toFile());
        labels.clear();
        for (String line : Files.readAllLines(dir.resolve("menu-labels.tsv"))) {
            String[] p = line.split("\t");
            labels.put(p[0], new int[] {Integer.parseInt(p[1]), Integer.parseInt(p[2])});
        }
    }

    static void draw(Graphics2D g, String text, int center, int y) {
        int[] label = labels.get(text);
        if (label == null) throw new IllegalArgumentException("Missing GUI label: " + text);
        int width = (label[1] + 1) / 2;
        g.drawImage(atlas, center - width / 2, y, center - width / 2 + width, y + 9,
            0, label[0], label[1], label[0] + 18, null);
    }

    /** Maintenance only: java GuiLabels <repo> <font.ttf>. No font file ships in the pack. */
    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args[0]).resolve("scripts/effect-art");
        String[] texts = {"게임", "팀", "월드", "심장 보호", "전투", "화면", "뽑기", "게임룰",
            "홈", "도움말", "시작템", "새로고침", "뒤로", "닫기", "이전", "다음", "검색", "전체",
            "일반 능력", "고급 능력", "패시브", "세부 정보", "상태 확인", "능력 도감", "내 능력",
            "보상 보기", "한 번 뽑기", "최근 결과", "조약돌 부족", "뽑기로 돌아가기", "보상과 확률",
            "게임 계속", "게임 종료", "항목을 눌러 설정", "이름을 눌러 상세 보기", "보상은 가방에 지급됩니다",
            "원하는 항목을 선택하세요", "팀 선택", "확인", "사용법", "시작 준비", "1회 뽑기", "조약돌"};
        Font font = Font.createFont(Font.TRUETYPE_FONT, Path.of(args[1]).toFile()).deriveFont(14f);
        BufferedImage sheet = new BufferedImage(320, texts.length * 18, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font); g.setColor(new Color(218, 232, 231));
        StringBuilder index = new StringBuilder();
        for (int i = 0; i < texts.length; i++) {
            if (font.canDisplayUpTo(texts[i]) != -1) throw new IllegalArgumentException("Font lacks Korean glyphs");
            g.drawString(texts[i], 0, i * 18 + 14);
            index.append(texts[i]).append('\t').append(i * 18).append('\t')
                .append(g.getFontMetrics().stringWidth(texts[i]) + 1).append('\n');
        }
        g.dispose();
        ImageIO.write(sheet, "png", dir.resolve("menu-labels.png").toFile());
        Files.writeString(dir.resolve("menu-labels.tsv"), index);
    }
}

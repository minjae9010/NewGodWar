import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Continuous canvases split into four glyphs to retain sharp 2x Korean captions. */
final class GuiPanels {
    private static Graphics2D graphics(BufferedImage image) {
        Graphics2D g = image.createGraphics(); g.scale(2, 2);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        return g;
    }
    static BufferedImage draw(int rows) {
        int height = rows * 18 + 114;
        BufferedImage image = new BufferedImage(352, height * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(image);
        g.setColor(new Color(18,30,40)); g.fillRect(0,0,176,height);
        g.setPaint(new GradientPaint(0,16,new Color(30,48,60),176,rows*18,new Color(19,33,44)));
        g.fillRect(1,16,174,rows*18-16);
        g.setColor(new Color(72,101,109)); g.drawRect(0,0,175,height-1);
        g.setColor(new Color(91,218,197)); g.fillRect(8,14,22,1);
        g.setColor(new Color(49,72,83)); g.fillRect(32,14,136,1);
        int footer=rows*18, storage=footer+18;
        g.setColor(new Color(14,25,35)); g.fillRect(1,footer,174,18);
        g.setColor(new Color(49,72,83)); g.fillRect(8,footer-1,160,1);
        g.setColor(new Color(193,202,201)); g.fillRect(1,storage,174,height-storage-1);
        for(int row=0;row<4;row++) for(int col=0;col<9;col++) {
            int x=7+col*18,y=footer+(row==3?89:31+row*18);
            g.setColor(new Color(155,170,173)); g.fillRect(x,y,18,18);
            g.setColor(new Color(104,126,135)); g.drawRect(x,y,17,17);
        }
        g.dispose(); return image;
    }
    private static void nav(Graphics2D g,int rows,int col,String icon,String label) {
        int center=16+col*18,y=rows*18;
        g.drawImage(GuiArtwork.draw(icon),center-6,y-1,12,12,null);
        GuiLabels.draw(g,label,center,y+9);
    }
    static BufferedImage panel(String kind,int rows) {
        BufferedImage image=draw(rows); Graphics2D g=graphics(image);
        switch(kind) {
        case "home":
            String[] names={"게임","팀","월드","심장 보호","전투","화면","뽑기","게임룰"};
            GuiLabels.draw(g,"원하는 항목을 선택하세요",73,21);
            for(int row=0;row<2;row++) {
                int y=33+row*36;
                g.setColor(new Color(34,54,66)); g.fillRoundRect(17,y,142,32,4,4);
                for(int col=0;col<4;col++) {
                    int center=34+col*36;
                    if(col>0) { g.setColor(new Color(47,68,79)); g.fillRect(center-18,y+7,1,18); }
                    GuiLabels.draw(g,names[row*4+col],center,y+22);
                }
            }
            nav(g,rows,0,"home","홈"); nav(g,rows,1,"help","도움말");
            nav(g,rows,3,"items","시작템"); nav(g,rows,5,"refresh","새로고침"); nav(g,rows,8,"close","닫기");
            break;
        case "draw": case "poor":
            boolean ready=kind.equals("draw");
            GuiLabels.draw(g,"보상은 가방에 지급됩니다",88,20);
            g.setColor(ready?new Color(44,103,105):new Color(56,62,71)); g.fillRoundRect(61,35,54,36,6,6);
            g.drawImage(GuiArtwork.draw(ready?"gambling":"cancel"),78,36,20,20,null);
            GuiLabels.draw(g,ready?"한 번 뽑기":"조약돌 부족",88,57);
            GuiLabels.draw(g,"조약돌",34,56); GuiLabels.draw(g,"최근 결과",142,56);
            nav(g,rows,2,"rewards","보상 보기"); nav(g,rows,8,"close","닫기"); break;
        case "rewards":
            GuiLabels.draw(g,"보상과 확률",88,21); nav(g,rows,4,"back","뽑기"); nav(g,rows,6,"close","닫기"); break;
        case "catalog":
            GuiLabels.draw(g,"이름을 눌러 상세 보기",88,21);
            nav(g,rows,0,"help","도움말"); nav(g,rows,2,"ability","검색"); nav(g,rows,8,"close","닫기"); break;
        case "ability": case "detail":
            GuiLabels.draw(g,"사용법",88,21);
            GuiLabels.draw(g,"일반 능력",52,38); GuiLabels.draw(g,"고급 능력",124,38);
            GuiLabels.draw(g,"패시브",35,76); GuiLabels.draw(g,"세부 정보",141,76);
            nav(g,rows,0,"back","능력 도감"); nav(g,rows,4,"close","닫기");
            if(kind.equals("ability")) nav(g,rows,8,"refresh","상태 확인"); break;
        default:
            GuiLabels.draw(g,"항목을 눌러 설정",88,21);
            nav(g,rows,0,"home","홈"); nav(g,rows,2,"help","도움말");
            nav(g,rows,4,"back","뒤로"); nav(g,rows,8,"close","닫기");
        }
        g.dispose(); return image;
    }
    static void writePack(Path root,Path previews) throws Exception {
        GuiLabels.load(previews.getParent().getParent()); Files.createDirectories(previews);
        StringBuilder providers=new StringBuilder("{\"providers\":[{\"type\":\"space\",\"advances\":{\"\\uE100\":-8,\"\\uE103\":-168,\"\\uE10A\":-176,\"\\uE10B\":-1}}");
        String[] kinds={"ability","settings","home","draw","poor","rewards","catalog","detail"};
        int[] rows={5,6,6,4,4,6,6,5}, codes={0xE101,0xE102,0xE104,0xE105,0xE106,0xE107,0xE108,0xE109};
        for(int i=0;i<kinds.length;i++) {
            String file="panel_"+kinds[i]+".png"; BufferedImage image=panel(kinds[i],rows[i]);
            ImageIO.write(image,"png",previews.resolve(file).toFile());
            int halfHeight=image.getHeight()/2;
            for(int part=0;part<4;part++) {
                String tile="panel_"+kinds[i]+"_"+part+".png";
                Path target=root.resolve("assets/newgodwar/textures/gui/"+tile);
                Files.createDirectories(target.getParent());
                ImageIO.write(image.getSubimage((part%2)*176,(part/2)*halfHeight,176,halfHeight),"png",target.toFile());
                int code=part==0?codes[i]:0xE110+i*3+part-1;
                providers.append(",{\"type\":\"bitmap\",\"file\":\"newgodwar:item/gui/").append(tile)
                    .append("\",\"ascent\":").append(13-(part/2)*(halfHeight/2))
                    .append(",\"height\":").append(halfHeight/2)
                    .append(",\"chars\":[\"").append((char)code).append("\"]}");
            }
        }
        providers.append("]}\n");
        BuildEffectPack.write(root,"assets/minecraft/font/default.json",providers.toString());
        BuildEffectPack.write(root,"assets/minecraft/font/uniform.json",providers.toString());
        // These shared 1.21.2+ sprites also affect ordinary inventories while this pack is loaded.
        BufferedImage transparent=new BufferedImage(24,24,BufferedImage.TYPE_INT_ARGB);
        for(String layer:new String[]{"back","front"}) {
            Path sprite=root.resolve("assets/minecraft/textures/gui/sprites/container/slot_highlight_"+layer+".png");
            Files.createDirectories(sprite.getParent()); ImageIO.write(transparent,"png",sprite.toFile());
        }
    }
}

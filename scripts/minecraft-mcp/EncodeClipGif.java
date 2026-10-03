import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;

/** Package unretouched game captures as a looping GIF; the PNG player remains the lossless reference. */
class EncodeClipGif {
    public static void main(String[] args) throws Exception {
        Path clip=Path.of(args[0]),output=Path.of(args[1]);
        String json=Files.readString(clip.resolve("clip.json"));
        Matcher matcher=Pattern.compile("\\\"file\\\"\\s*:\\s*\\\"(\\d+\\.png)\\\",\\s*\\\"ms\\\"\\s*:\\s*([\\d.]+)").matcher(json);
        List<String> files=new ArrayList<>();List<Double> times=new ArrayList<>();
        while(matcher.find()){files.add(matcher.group(1));times.add(Double.parseDouble(matcher.group(2)));}
        if(files.size()<2)throw new IllegalStateException("No recorded frames");
        ImageWriter writer=ImageIO.getImageWritersByFormatName("gif").next();
        Files.createDirectories(output.toAbsolutePath().getParent());
        try(ImageOutputStream stream=ImageIO.createImageOutputStream(output.toFile())) {
            writer.setOutput(stream);writer.prepareWriteSequence(null);
            for(int i=0;i<files.size();i++) {
                BufferedImage image=ImageIO.read(clip.resolve(files.get(i)).toFile());
                IIOMetadata metadata=writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image),null);
                String format=metadata.getNativeMetadataFormatName();IIOMetadataNode root=(IIOMetadataNode)metadata.getAsTree(format);
                IIOMetadataNode control=(IIOMetadataNode)root.getElementsByTagName("GraphicControlExtension").item(0);
                control.setAttribute("disposalMethod","none");control.setAttribute("userInputFlag","FALSE");control.setAttribute("transparentColorFlag","FALSE");
                control.setAttribute("delayTime",String.valueOf(i+1==files.size()?35:Math.max(1,Math.round((times.get(i+1)-times.get(i))/10))));
                control.setAttribute("transparentColorIndex","0");
                if(i==0){IIOMetadataNode ext=new IIOMetadataNode("ApplicationExtensions"),loop=new IIOMetadataNode("ApplicationExtension");loop.setAttribute("applicationID","NETSCAPE");loop.setAttribute("authenticationCode","2.0");loop.setUserObject(new byte[]{1,0,0});ext.appendChild(loop);root.appendChild(ext);}
                metadata.setFromTree(format,root);writer.writeToSequence(new IIOImage(image,null,metadata),null);
            }
            writer.endWriteSequence();
        }finally{writer.dispose();}
        System.out.println(files.size()+" original frames encoded: "+output);
    }
}

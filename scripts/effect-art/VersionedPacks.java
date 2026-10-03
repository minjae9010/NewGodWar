import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/**
 * Compiles the shared artwork into ONE pack that every supported client (1.14 – 26.3) reads.
 *
 * <ul>
 *   <li>Textures live under {@code textures/item/}, which every release's default item sprite atlas already
 *       scans (blocks atlas up to 1.21.10, items atlas from 1.21.11), so no atlas file is needed.</li>
 *   <li>Both model pipelines ship side by side: legacy {@code custom_model_data} overrides (1.14 – 1.21.3) and
 *       item model definitions (1.21.4+). Each client ignores the one it does not understand.</li>
 *   <li>{@code pack.mcmeta} declares the whole format range in every syntax: {@code pack_format},
 *       {@code supported_formats} (1.20.2 – 1.21.8) and {@code min_format}/{@code max_format} (1.21.9+).</li>
 * </ul>
 * The file is named by its own hash, so an older plugin release keeps downloading the exact pack it expects.
 */
final class VersionedPacks {
    static final String[] FOODS = {"bread", "cooked_chicken", "cooked_beef", "baked_potato", "cooked_porkchop", "cooked_cod"};
    static final String[] FOOD_KEYS = {"wind_croissant", "herb_chicken", "flame_steak", "golden_potato", "honey_pork", "cloud_fish"};
    /** Resource pack formats of 1.14 (4) through 26.3 (97.x). */
    static final int MIN_FORMAT = 4, MAX_FORMAT = 97, PACK_FORMAT = 34;

    static byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }
    static String override(int id, String model) {
        return "{\"predicate\":{\"custom_model_data\":" + id + "},\"model\":\"" + model + "\"}";
    }
    static String legacyModel(String material, List<String> overrides) {
        return "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"minecraft:item/" + material
            + "\"},\"overrides\":[" + String.join(",", overrides) + "]}";
    }
    static void build(Path repo, Path root) throws Exception {
        SortedMap<String, byte[]> assets = new TreeMap<>();
        try (var files = Files.walk(root)) {
            for (Path path : files.filter(Files::isRegularFile).sorted().toList())
                assets.put(root.relativize(path).toString().replace('\\', '/'), Files.readAllBytes(path));
        }
        SortedMap<String, Integer> ids = new TreeMap<>();
        for (String path : assets.keySet()) {
            if (path.startsWith("assets/newgodwar/items/"))
                ids.put(path.substring("assets/newgodwar/items/".length(), path.length()-5), 74000 + ids.size()*2);
        }
        StringBuilder models = new StringBuilder();
        ids.forEach((key, id) -> models.append(key).append('=').append(id).append('\n'));
        Path generated = repo.resolve("build/generated/pack-resources");
        Files.createDirectories(generated);
        Files.writeString(generated.resolve("art-models.properties"), models);

        SortedMap<String, byte[]> entries = new TreeMap<>();
        for (var asset : assets.entrySet()) {
            String name = asset.getKey();
            if (name.equals("pack.mcmeta") || name.startsWith("assets/minecraft/atlases/")) continue;
            byte[] content = asset.getValue();
            if (name.startsWith("assets/newgodwar/textures/")) name = name.replace("/textures/", "/textures/item/");
            if (name.startsWith("assets/newgodwar/models/")) content = bytes(new String(content, StandardCharsets.UTF_8)
                .replace("newgodwar:art/", "newgodwar:item/art/").replace("newgodwar:gui/", "newgodwar:item/gui/"));
            entries.put(name, content);
        }
        List<String> overrides = new ArrayList<>();
        ids.forEach((key, id) -> {
            overrides.add(override(id, "newgodwar:"+key));
            overrides.add(override(id+1, "minecraft:item/paper"));
        });
        entries.put("assets/minecraft/models/item/paper.json", bytes(legacyModel("paper", overrides)));
        for (int i=0; i<FOODS.length; i++) entries.put("assets/minecraft/models/item/"+FOODS[i]+".json",
            bytes(legacyModel(FOODS[i], List.of(override(73101+i, "newgodwar:art/food/"+FOOD_KEYS[i]),
                override(73102+i, "minecraft:item/"+FOODS[i])))));
        entries.put("pack.mcmeta", bytes("{\"pack\":{\"description\":\"NewGodWar Arts & Menus · 1.14 – 26.3\",\"pack_format\":"
            + PACK_FORMAT + ",\"supported_formats\":[" + MIN_FORMAT + "," + MAX_FORMAT + "],\"min_format\":" + MIN_FORMAT
            + ",\"max_format\":" + MAX_FORMAT + "}}\n"));

        var buffer = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(buffer)) {
            for (var entry : entries.entrySet()) {
                ZipEntry item = new ZipEntry(entry.getKey()); item.setTime(0);
                out.putNextEntry(item); out.write(entry.getValue()); out.closeEntry();
            }
        }
        byte[] zip = buffer.toByteArray();
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(zip));
        String file = "NewGodWar-Art-" + sha.substring(0, 8) + ".zip";
        Path dist = repo.resolve("build/effect-pack/dist");
        if (Files.exists(dist)) try (var old = Files.list(dist)) { for (Path path : old.toList()) Files.delete(path); }
        Files.createDirectories(dist);
        Files.write(dist.resolve(file), zip);
        Files.writeString(dist.resolve(file + ".sha1"), sha + "\n");

        StringBuilder manifest = new StringBuilder();
        StringBuilder downloads = new StringBuilder("Minecraft\tfile\tsha1\n");
        for (String line : Files.readAllLines(repo.resolve("scripts/effect-art/pack-versions.tsv"))) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] p = line.split("\\|");
            for (String version : p[1].split(",")) {
                manifest.append(version).append('=').append(file).append('|').append(sha).append('|').append(p[0]).append('\n');
                downloads.append(version).append('\t').append(file).append('\t').append(sha).append('\n');
            }
        }
        Files.writeString(generated.resolve("art-packs.properties"), manifest);
        Files.writeString(dist.resolve("manifest.tsv"), downloads);
        System.out.println("Pack " + file + " / formats " + MIN_FORMAT + "–" + MAX_FORMAT + " / " + sha);
    }
}

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** Converts the shared artwork to each released client's actual model and atlas format. */
final class VersionedPacks {
    static final String[] FOODS = {"bread", "cooked_chicken", "cooked_beef", "baked_potato", "cooked_porkchop", "cooked_cod"};
    static final String[] FOOD_KEYS = {"wind_croissant", "herb_chicken", "flame_steak", "golden_potato", "honey_pork", "cloud_fish"};

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
        StringBuilder manifest = new StringBuilder();
        StringBuilder downloads = new StringBuilder("Minecraft\tfile\tsha1\n");
        for (String line : Files.readAllLines(repo.resolve("scripts/effect-art/pack-versions.tsv"))) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] p = line.split("\\|");
            boolean modern = p[2].equals("modern"), fallback = p[2].equals("fallback");
            SortedMap<String, byte[]> entries = new TreeMap<>();
            for (var asset : assets.entrySet()) {
                String name = asset.getKey();
                if (name.equals("pack.mcmeta") || name.startsWith("assets/minecraft/atlases/")) continue;
                if (!modern && name.contains("/items/")) continue;
                // Before explicit atlases, use the conventional item sprite directories.
                byte[] content = asset.getValue();
                if (p[3].equals("none")) {
                    if (name.startsWith("assets/newgodwar/textures/")) name = name.replace("/textures/", "/textures/item/");
                    if (name.startsWith("assets/newgodwar/models/")) {
                        String json = new String(content, StandardCharsets.UTF_8)
                            .replace("newgodwar:art/", "newgodwar:item/art/").replace("newgodwar:gui/", "newgodwar:item/gui/");
                        content = bytes(json);
                    }
                }
                entries.put(name, content);
            }
            String format = p[1].contains(".")
                ? "\"min_format\":["+p[1].replace('.', ',')+"],\"max_format\":["+p[1].replace('.', ',')+"]"
                : "\"pack_format\":"+p[1];
            entries.put("pack.mcmeta", bytes("{\"pack\":{\"description\":\"NewGodWar Arts & Menus / "+p[0]+"\","+format+"}}\n"));
            if (!p[3].equals("none")) entries.put("assets/minecraft/atlases/"+p[3]+".json", assets.get("assets/minecraft/atlases/items.json"));
            if (!modern && !fallback) {
                List<String> overrides = new ArrayList<>();
                ids.forEach((key, id) -> {
                    overrides.add(override(id, "newgodwar:"+key));
                    overrides.add(override(id+1, "minecraft:item/paper"));
                });
                entries.put("assets/minecraft/models/item/paper.json", bytes(legacyModel("paper", overrides)));
                for (int i=0; i<FOODS.length; i++) entries.put("assets/minecraft/models/item/"+FOODS[i]+".json",
                    bytes(legacyModel(FOODS[i], List.of(override(73101+i, "newgodwar:art/food/"+FOOD_KEYS[i]),
                        override(73102+i, "minecraft:item/"+FOODS[i])))));
            }
            Path zip = repo.resolve("build/libs/NewGodWar-Art-"+p[0]+".zip");
            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
                for (var entry : entries.entrySet()) {
                    ZipEntry item = new ZipEntry(entry.getKey()); item.setTime(0);
                    out.putNextEntry(item); out.write(entry.getValue()); out.closeEntry();
                }
            }
            String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(zip)));
            Files.writeString(zip.resolveSibling(zip.getFileName()+".sha1"), sha+"\n");
            for (String version : p[4].split(",")) {
                manifest.append(version).append('=').append(zip.getFileName()).append('|').append(sha).append('|').append(p[2]).append('\n');
                downloads.append(version).append('\t').append(zip.getFileName()).append('\t').append(sha).append('\n');
            }
            System.out.println("Pack " + p[0] + " / format " + p[1] + " / " + sha);
        }
        Files.writeString(generated.resolve("art-packs.properties"), manifest);
        Files.writeString(repo.resolve("build/libs/NewGodWar-Art-manifest.tsv"), downloads);
    }
}

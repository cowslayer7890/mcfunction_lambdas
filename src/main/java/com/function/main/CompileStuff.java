package com.function.main;

import com.esotericsoftware.minlog.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompileStuff {

    static final Map<String, Path> allTouched = new HashMap<>();
    static final Map<String, Path> allUntouched = new HashMap<>();

    public static void main(String[] args) throws IOException {
        Path dir = Paths.get("testfiles");
        compile(dir);
    }

    public static void compile(Path dir) throws IOException {
        allTouched.clear();
        allUntouched.clear();
        System.out.println();
        Log.info("Compiling: " + dir.getFileName().toString());
        List<McFunction> ticked = new ArrayList<>();
        List<McFunction> loaded = new ArrayList<>();
        getAllFilesInDataPack(dir, ".function")
                .peek(path -> Log.info(path.toString()))
                .map(path -> new McFunction(path, dir))
                .forEach(mcFunction -> {
                    mcFunction.parse();
                    switch (mcFunction.getSpecial()) {
                        case TICKED:
                            ticked.add(mcFunction);
                            break;
                        case LOADED:
                            loaded.add(mcFunction);
                            break;
                    }
                    allTouched.putAll(mcFunction.save());
                });

        if (!ticked.isEmpty()) {
            String content = ticked.stream()
                    .map(b -> b.getNameSpaceStack().get(0))
                    .collect(Collectors.joining("\",\n\t\t", "{\n\t\"values\": [\n\t\t\"", "\"\n\t]\n}"));

            Path path = dir.resolve("data/minecraft/tags/functions/tick.json");
            Files.createDirectories(path.getParent());
            Files.write(path, Collections.singleton(content));
        }
        if (!loaded.isEmpty()) {
            String content = loaded.stream()
                    .map(b -> b.getNameSpaceStack().get(0))
                    .collect(Collectors.joining("\",\n\t\t", "{\n\t\"values\": [\n\t\t\"", "\"\n\t]\n}"));
            Path path = dir.resolve("data/minecraft/tags/functions/load.json");
            Files.createDirectories(path.getParent());
            Files.write(path, Collections.singleton(content));
        }

        Path packMeta = dir.resolve("pack.mcmeta");
        if (Files.notExists(packMeta)) {
            Files.write(packMeta, Arrays.asList(
                    "{",
                    "   \"pack\": {",
                    "       \"pack_format\": 1,",
                    "       \"description\": \"\"",
                    "   }",
                    "}"));
        }

        getAllFilesInDataPack(dir, ".mcfunction")
                .filter(o -> !allTouched.containsValue(o))
                .filter(path -> {
                    try {
                        return Files.lines(path).findFirst().orElse("").startsWith("#Generated with");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return false;
                })
                .forEach(value -> {
                    Path data = dir.resolve("data");
                    Path absolutePath = data.toAbsolutePath();
                    Path pathRelative = absolutePath.relativize(value);
                    System.out.println(pathRelative);
                    System.out.println(value);
                    String item = pathRelative.toString().replaceFirst("\\\\functions\\\\", ":").replace('\\', '/');
                    allUntouched.put(item, value);
                });

    }

    private static Stream<Path> getAllFilesInDataPack(Path dir, String extension) {
        Path data = dir.resolve("data");
        Stream<Path> stream = Stream.empty();
        try {
            return Files.list(data).filter(Files::isDirectory)
                    .map(path -> path.resolve("functions"))
                    .filter(Files::isDirectory)
                    .map(functionPath -> {
                        try {
                            return Files.walk(functionPath).filter(path -> path.toString().endsWith(extension));
                        } catch (IOException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }).flatMap(Function.identity());
        } catch (IOException e) {
            e.printStackTrace();
        }


        return stream;
    }
}

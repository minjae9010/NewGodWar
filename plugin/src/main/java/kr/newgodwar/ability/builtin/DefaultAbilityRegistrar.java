package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.AbilityRegistry;
import kr.newgodwar.ability.api.AbilityInfo;
import kr.newgodwar.ability.api.AbilityRegistrar;
import kr.newgodwar.ability.api.GodAbility;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class DefaultAbilityRegistrar implements AbilityRegistrar {
    private static final String PACKAGE_NAME = "kr.newgodwar.ability.builtin";
    private static final String PACKAGE_PATH = PACKAGE_NAME.replace('.', '/');

    @Override
    public void registerAbilities(AbilityRegistry registry) {
        for (Class<? extends GodAbility> abilityClass : scanAbilityClasses()) {
            registry.register(abilityClass);
        }
    }

    private Collection<Class<? extends GodAbility>> scanAbilityClasses() {
        List<Class<? extends GodAbility>> classes = new ArrayList<Class<? extends GodAbility>>();
        ClassLoader loader = DefaultAbilityRegistrar.class.getClassLoader();
        try {
            Enumeration<URL> resources = loader.getResources(PACKAGE_PATH);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    scanDirectory(loader, classes, resource);
                } else if ("jar".equals(resource.getProtocol())) {
                    scanJar(loader, classes, resource);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot scan bundled abilities.", ex);
        }
        Collections.sort(classes, new Comparator<Class<? extends GodAbility>>() {
            @Override
            public int compare(Class<? extends GodAbility> first, Class<? extends GodAbility> second) {
                return first.getAnnotation(AbilityInfo.class).id().compareTo(second.getAnnotation(AbilityInfo.class).id());
            }
        });
        return classes;
    }

    private void scanDirectory(ClassLoader loader, List<Class<? extends GodAbility>> classes, URL resource) throws IOException {
        File directory = new File(URLDecoder.decode(resource.getPath(), "UTF-8"));
        scanDirectory(loader, classes, directory, PACKAGE_NAME);
    }

    private void scanDirectory(ClassLoader loader, List<Class<? extends GodAbility>> classes, File directory, String packageName) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName();
            if (file.isDirectory()) {
                scanDirectory(loader, classes, file, packageName + "." + name);
            } else if (file.isFile() && name.endsWith(".class") && name.indexOf('$') < 0) {
                addIfAbility(loader, classes, packageName + "." + name.substring(0, name.length() - ".class".length()));
            }
        }
    }

    private void scanJar(ClassLoader loader, List<Class<? extends GodAbility>> classes, URL resource) throws IOException {
        JarURLConnection connection = (JarURLConnection) resource.openConnection();
        JarFile jar = connection.getJarFile();
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!entry.isDirectory()
                && name.startsWith(PACKAGE_PATH + "/")
                && name.endsWith(".class")
                && name.indexOf('$') < 0) {
                String className = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                addIfAbility(loader, classes, className);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addIfAbility(ClassLoader loader, List<Class<? extends GodAbility>> classes, String className) {
        try {
            Class<?> candidate = Class.forName(className, false, loader);
            int modifiers = candidate.getModifiers();
            if (candidate == BaseAbility.class
                || !GodAbility.class.isAssignableFrom(candidate)
                || candidate.getAnnotation(AbilityInfo.class) == null
                || candidate.isInterface()
                || Modifier.isAbstract(modifiers)) {
                return;
            }
            classes.add((Class<? extends GodAbility>) candidate);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Cannot load ability class: " + className, ex);
        }
    }
}

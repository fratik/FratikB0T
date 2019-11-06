/*
 * Copyright (C) 2019 FratikB0T Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.fratik.core.manager.implementation;

import com.google.common.base.Joiner;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import io.sentry.Sentry;
import lombok.Getter;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Statyczne;
import pl.fratik.core.entity.*;
import pl.fratik.core.event.ModuleLoadedEvent;
import pl.fratik.core.event.ModuleUnloadedEvent;
import pl.fratik.core.manager.ManagerArgumentow;
import pl.fratik.core.manager.ManagerBazyDanych;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.manager.ManagerModulow;
import pl.fratik.core.moduly.Modul;
import pl.fratik.core.moduly.ModuleDescription;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.CommonUtil;
import pl.fratik.core.util.EventBusErrorHandler;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.GsonUtil;
import pl.fratik.core.util.graph.Graph;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("all")
public class ManagerModulowImpl implements ManagerModulow {

    private static final File MODULES_DIR = new File(System.getProperty("core.plugindir", "plugins"));
    public static ModuleClassLoader moduleClassLoader;

    private ManagerKomend managerKomend;
    private ManagerArgumentow managerArgumentow;
    private ShardManager shardManager;
    private ManagerBazyDanych managerBazyDanych;
    private Tlumaczenia tlumaczenia;
    private Logger logger;
    private EventBus moduleEventBus;
    private EventWaiter eventWaiter;
    @Getter private HashMap<String, Modul> modules;
    private HashMap<String, URLClassLoader> classLoaders;
    private HashMap<String, File> tempFiles;
    @Getter private ArrayList<String> started;
    private Injector injector;
    private GuildDao guildDao;
    private MemberDao memberDao;
    private UserDao userDao;
    private ScheduleDao scheduleDao;
    private EventBus eventBus;

    private LinkedHashSet<String> loadOrder = null;
    private Map<String, File> modNames = null;
    private Map<String, Collection<String>> dependencies = null;
    private Map<String, Collection<String>> peerDependencies = null;
    private Graph<String> graph = null;
    private GbanDao gbanDao;

    public ManagerModulowImpl(ShardManager shardManager, ManagerBazyDanych managerBazyDanych, GuildDao guildDao,
                              MemberDao memberDao, UserDao userDao, GbanDao gbanDao, ScheduleDao scheduleDao, ManagerKomend managerKomend,
                              ManagerArgumentow managerArgumentow, EventWaiter eventWaiter, Tlumaczenia tlumaczenia, EventBus eventBus) {
        this.guildDao = guildDao;
        this.memberDao = memberDao;
        this.userDao = userDao;
        this.gbanDao = gbanDao;
        this.scheduleDao = scheduleDao;
        moduleClassLoader = new ModuleClassLoader();
        moduleEventBus = new AsyncEventBus(Executors.newFixedThreadPool(16), EventBusErrorHandler.instance);
        logger = LoggerFactory.getLogger(getClass());
        this.managerKomend = managerKomend;
        this.managerArgumentow = managerArgumentow;
        this.managerBazyDanych = managerBazyDanych;
        this.shardManager = shardManager;
        this.tlumaczenia = tlumaczenia;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;

        modules = new LinkedHashMap<>();
        classLoaders = new HashMap<>();
        tempFiles = new HashMap<>();
        started = new ArrayList<>();
    }

    @Override
    public void loadModules() {
        tlumaczenia.loadMessages();
        try {
            new URL("file:///").openConnection().setDefaultUseCaches(false);
        } catch (Exception e) {
            logger.error("To stać się nie powinno!", e);
        }

        if (!classLoaders.isEmpty()) {
            managerKomend.unregisterAll();
            Iterator<Map.Entry<String, URLClassLoader>> in = classLoaders.entrySet().iterator();
            while (in.hasNext()) {
                String name = in.next().getKey();
                stopModule(name);
                unload(name, false);
                modules.remove(name);
                in.remove();
                tempFiles.remove(name);
            }
        }

        if (MODULES_DIR.exists()) {
            try (Stream<Path> pathStream = Files.list(MODULES_DIR.toPath())) {
                LinkedList<String> dependOrder = new LinkedList<>();
                loadOrder = new LinkedHashSet<>();
                modNames = new HashMap<>();
                dependencies = new HashMap<>();
                peerDependencies = new HashMap<>();

                graph = new Graph<>(dependOrder::add);

                List<File> modFiles = pathStream.filter(path -> path.toString().toLowerCase().endsWith(".jar"))
                        .map(Path::toFile).collect(Collectors.toList());

                modFiles.forEach(this::addDependencies);

                graph.generateDependencies();
                Collections.reverse(dependOrder);
                loadOrder.addAll(dependOrder);

                checkDependencies();
                loadJars();

                injector = Guice.createInjector(new AbstractModule() {
                    @Override
                    protected void configure() {
                        Multibinder<Modul> binder = Multibinder.newSetBinder(binder(), Modul.class);
                        for(Modul mod : modules.values()) {
                            binder.addBinding().to(mod.getClass());
                        }
                        bind(ShardManager.class).toInstance(shardManager);
                        bind(Tlumaczenia.class).toInstance(tlumaczenia);
                        bind(EventBus.class).toInstance(moduleEventBus);
                        bind(EventWaiter.class).toInstance(eventWaiter);
                        bind(ManagerBazyDanych.class).toInstance(managerBazyDanych);
                        bind(GuildDao.class).toInstance(guildDao);
                        bind(MemberDao.class).toInstance(memberDao);
                        bind(UserDao.class).toInstance(userDao);
                        bind(GbanDao.class).toInstance(gbanDao);
                        bind(ScheduleDao.class).toInstance(scheduleDao);
                        bind(ManagerKomend.class).toInstance(managerKomend);
                        bind(ManagerArgumentow.class).toInstance(managerArgumentow);
                        bind(ManagerModulow.class).toInstance(ManagerModulowImpl.this);
                    }
                });

                for (String s : modules.keySet()) {
                    startModule(s);
                }
            } catch (Throwable e) {
                logger.error("Error while (re)loading modules!", e);
            }
        }
    }

    private void addDependencies(File file) {
        if (!file.exists() || !file.isFile()) return;
        ModuleDescription desc = getDescription(file);
        if (desc != null) {
            if (desc.getName() == null || desc.getMain() == null) {
                logger.warn("Nieprawidłowy plik z opisem!");
                return;
            }

            if (modNames.containsKey(desc.getName())) {
                logger.warn("Nieznana nazwa modułu {}, pomijam!", desc.getName());
                return;
            }

            modNames.put(desc.getName(), file);
            dependencies.put(desc.getName(), desc.getDependencies());
            peerDependencies.put(desc.getName(), desc.getDependencies());

            if (desc.getDependencies() != null) {
                desc.getDependencies().forEach(dep -> {
                    logger.debug("Moduł {} jest zależny od {}", desc.getName(), dep);
                    graph.addDependency(desc.getName(), dep);
                });
            }
            if (desc.getPeerDependencies() != null) {
                desc.getPeerDependencies().forEach(dep -> {
                    logger.debug("Moduł {} jest zależny od {}, ale nie wymaga go do działania", desc.getName(), dep);
                    graph.addDependency(desc.getName(), dep);
                });
            }
            if (desc.getDependencies() == null && desc.getPeerDependencies() == null){
                logger.debug("Moduł {} nie ma zależności", desc.getName());
                loadOrder.add(desc.getName());
            }
        }

    }

    private void checkDependencies() {
        List<String> toRemove = new ArrayList<>();

        for (String name : loadOrder) {
            if (!modNames.containsKey(name)) {
                toRemove.add(name);

                dependencies.forEach((modName, modDeps) -> {
                    if (modDeps != null) {
                        if (modDeps.contains(name))
                            toRemove.add(modName);

                        modDeps.forEach(dep -> {
                            if (toRemove.contains(dep))
                                toRemove.add(modName);
                        });
                    }
                });

                peerDependencies.forEach((modName, modDeps) -> {
                    if (modDeps != null) {
                        if (modDeps.contains(name))
                            logger.warn("Zależność niebezpośrednia {} dla modułu {} nie została znaleziona!", name, modName);
                    }
                });

                logger.error("Nierozpoznana zależność {}, pomijam!", name);
            }
        }

        if (!toRemove.isEmpty())
            loadOrder.removeIf(toRemove::contains);
    }

    public ModuleDescription getDescription(File file) {
        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equals("plugin.json")) {
                    String data = CommonUtil.fromStream(zipFile.getInputStream(entry));

                    return GsonUtil.fromJSON(data, ModuleDescription.class);
                }
            }
        } catch (Exception e) {
            logger.error("Błąd w trakcie czytania opisu!", e);
        }
        return null;
    }

    private void loadJars() {
        logger.debug("Kolejność wczytywania modułów: {}", Joiner.on(" <- ").join(loadOrder));

        for (String module : loadOrder) {
            try {
                load(modNames.get(module).getAbsolutePath());
            } catch (Exception e) {
                logger.error("Błąd w trakcie ładowania modułu {}!", module, e);
            }
        }
    }

    @Override
    public boolean startModule(String name) {
        boolean odpowiedz = false;
        if (!started.contains(name)) {
            logger.info("Startuje moduł: {}", name);
            try {
                Modul mod = injector.getInstance(modules.get(name).getClass());
                ManagerKomendImpl.setLoadingModule(name);
                odpowiedz = mod.startUp();
                eventBus.post(new ModuleLoadedEvent(name, mod));
                ManagerKomendImpl.setLoadingModule(null);
                modules.replace(name, mod);
                if (odpowiedz) started.add(name);
            } catch (Exception e) {
                logger.error("Błąd w trakcie startowania modułu: " + name, e);
                Sentry.capture(e);
            }
        }
        return odpowiedz;
    }

    @Override
    public boolean stopModule(String name) {
        AtomicBoolean odpowiedz = new AtomicBoolean(false);
        AtomicBoolean czekamy = new AtomicBoolean(true);
        if (started.contains(name)) {
            logger.info("Zatrzymuje moduł: {}", name);
            Thread xd = new Thread(() -> {
                try {
                    ManagerKomendImpl.setLoadingModule(name);
                    eventBus.post(new ModuleUnloadedEvent(name, modules.get(name)));
                    odpowiedz.set(modules.get(name).shutDown());
                    ManagerKomendImpl.setLoadingModule(null);
                } catch (Exception e) {
                    logger.error("Błąd w trakcie zatrzymywania modułu: " + name, e);
                    Sentry.capture(e);
                }
                czekamy.set(false);
            });
            xd.setName("Zatrzymywanie modułu " + name);
            xd.start();
            int czekamyCzasu = 0;
            while (czekamy.get() == true) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                czekamyCzasu++;
                if (czekamyCzasu >= 1200) { //2 minuty
                    xd.interrupt();
                    break;
                }
            }
            Iterator<String> i = started.iterator();
            while (i.hasNext())
                if (i.next().equalsIgnoreCase(name)) i.remove();
        }
        return odpowiedz.get();
    }

    @Override
    public void unload(String name, Boolean remove) {
        if (classLoaders.get(name) == null) return;

        try {
            classLoaders.get(name).close();
            if (tempFiles.get(name) != null)
                Files.delete(tempFiles.get(name).toPath());
        } catch (IOException e) {
            logger.error("Błąd podczas wyładowywania modułu!", e);
            Sentry.capture(e);
        }
        if (remove) {
            modules.entrySet().removeIf(e -> e.getKey().equals(name));
            classLoaders.entrySet().removeIf(e -> e.getKey().equals(name));
            tempFiles.entrySet().removeIf(e -> e.getKey().equals(name));
        }
    }

    @Override
    public void load(String path) throws Exception {
        File jar = new File(path);
        File jar2 = File.createTempFile("cached-", ".fbmod");
        jar2.deleteOnExit();

        try (InputStream fis = new FileInputStream(jar)) {
            try (OutputStream fos = new FileOutputStream(jar2)) {
                IOUtils.copy(fis, fos);
            }
        }

        logger.debug("Zapisuje moduł w cache: {} -> {}", path, jar2.getAbsolutePath());

        URL jarUrl = jar2.toURI().toURL();
        URL[] classPath = new URL[]{jarUrl};
        URLClassLoader cl = new ModuleURLClassLoader(classPath, getClass().getClassLoader());

        ModuleDescription description = getDescription(jar2);

        if (description == null)
            throw new IllegalArgumentException("Wskazany moduł zawiera nieprawidłowy plik z opisem!");

        String name = description.getName();
        String main = description.getMain();
        String coreVersionM = description.getCoreVersion();
        String coreVersion = Statyczne.CORE_VERSION.replaceAll("(-SNAPSHOT|-BETA|-ALPHA|-TEST)", "");
        if (coreVersionM != null)
            coreVersionM = coreVersionM.replaceAll("(-SNAPSHOT|-BETA|-ALPHA|-TEST)", "");

        if (name == null) throw new IllegalArgumentException("Nazwa modułu jest niesprecyzowana!");
        if (main == null) throw new IllegalArgumentException("Klasa główna jest niesprecyzowana!");
        try {
            if (coreVersionM != null) {
                if (Integer.parseInt(coreVersionM.split("\\.")[0]) >
                        Integer.parseInt(coreVersion.split("\\.")[0]))
                    throw new IllegalArgumentException("Niezgodność wersji rdzenia!");
                if (Integer.parseInt(coreVersionM.split("\\.")[1])
                        > Integer.parseInt(coreVersion.split("\\.")[1]))
                    throw new IllegalArgumentException("Niezgodność wersji rdzenia!");
                if (Integer.parseInt(coreVersionM.split("\\.")[2].split("_")[0])
                        > Integer.parseInt(coreVersion.split("\\.")[2].split("_")[0]))
                    throw new IllegalArgumentException("Niezgodność wersji rdzenia!");
                if (Integer.parseInt(coreVersionM.split("\\.")[2].split("_")[1])
                        > Integer.parseInt(coreVersion.split("\\.")[2].split("_")[1]))
                    throw new IllegalArgumentException("Niezgodność wersji rdzenia!");
            } else logger.warn("Wersja rdzenia nie jest sprecyzowana");
        } catch (NumberFormatException e) {
            logger.warn("Nie udało się odczytać wersji rdzenia!");
        } catch (IllegalArgumentException e) {
            logger.warn("Nastąpiła niezgodność rdzenia - mogą wystąpić problemy!");
            Sentry.getStoredClient().addExtra("uwagi",
                    "co najmniej jeden z pluginów ma niezgodność wersji");
        }

        if (description.getDependencies() != null)
            for (String s : description.getDependencies()) {
                if (!isLoaded(s))
                    throw new IllegalStateException("Moduł " + name + " jest zależny od " + s
                            + " który jest niezaładowany!");
            }

        if (isLoaded(name))
            throw new IllegalArgumentException("Moduł jest już załadowany!");

        classLoaders.put(name, cl);

        try {
            Class jarClass = moduleClassLoader.loadClass(main);

            if (!Modul.class.isAssignableFrom(jarClass)) {
                classLoaders.remove(name);
                throw new IllegalArgumentException("Główna klasa modułu nie implementuje Modul!");
            }

            Modul mod = (Modul) jarClass.newInstance();
            modules.put(name, mod);
            tempFiles.put(name, jar2);
        } catch (Exception e) {
            classLoaders.remove(name);
            logger.error("Błąd w trakcie ładowania głównej klasy modułu!", e);
        }
    }

    @Override
    public boolean isLoaded(String name) {
        return modules.keySet().contains(name);
    }

    @Override
    public boolean isStarted(String name) {
        return started.contains(name);
    }

    @Override
    public File getPath(String modul) {
        return modNames.get(modul);
    }

    public static class ModuleURLClassLoader extends URLClassLoader {

        private static final Set<ModuleURLClassLoader> loaders = new HashSet<>();
        private static ClassLoader lastClassLoader;

        public ModuleURLClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
            loaders.add(this);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            try {
                try {
                    lastClassLoader = this;
                    return super.loadClass(name, resolve);
                } catch (ClassNotFoundException e) {
                    for (ModuleURLClassLoader classLoader : loaders) {
                        lastClassLoader = classLoader;
                        if (classLoader.equals(this)) continue;
                        try {
                            return classLoader.loadClassDirect(name, resolve);
                        } catch (ClassNotFoundException e1) {
                            continue;
                        }
                    }
                }
                throw new ClassNotFoundException(name);
            } catch (StackOverflowError e) {
                throw new ClassNotFoundException(name);
            }
        }

        private Class<?> loadClassDirect(String name, boolean resolve) throws ClassNotFoundException {
            return super.loadClass(name, resolve);
        }

        @Override
        public void close() throws IOException {
            loaders.remove(this);
            super.close();
        }
    }

    public class ModuleClassLoader extends ClassLoader {
        private static final boolean CLASSLOADER_DEBUG = true;

        ModuleClassLoader() {
        }

        @Override
        public Class<?> loadClass(String s) throws ClassNotFoundException {
            if (CLASSLOADER_DEBUG)
                logger.debug("loadClass: {}", s);

            Class<?> c;

            for (URLClassLoader classLoader : classLoaders.values()) {
                try {
                    c = classLoader.loadClass(s);

                    if (c != null)
                        return c;
                } catch (ClassNotFoundException e) {
                    // ignore, go to next one
                }
            }

            throw new ClassNotFoundException(s);
        }

        @Nullable
        @Override
        public URL getResource(String s) {
            if (CLASSLOADER_DEBUG)
                logger.debug("getResource: {}", s);

            URL resource;

            for (URLClassLoader classLoader : classLoaders.values()) {
                resource = classLoader.getResource(s);

                if (resource != null)
                    return resource;
            }

            return null;
        }

        @Override
        public InputStream getResourceAsStream(String s) {
            if (CLASSLOADER_DEBUG)
                logger.debug("getResourceAsStream: {}", s);

            InputStream stream;

            for (URLClassLoader classLoader : classLoaders.values()) {
                stream = classLoader.getResourceAsStream(s);

                if (stream != null)
                    return stream;
            }

            return null;
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onEvent(Object object) {
        if (moduleEventBus != null)
            moduleEventBus.post(object);
    }

}

package org.levimc.launcher.core.mods;

import android.os.FileObserver;
import androidx.lifecycle.MutableLiveData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.levimc.launcher.core.versions.GameVersion;

import android.util.Log;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import org.levimc.launcher.util.LLModBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.*;
import java.io.InputStreamReader;

public class ModManager {
    private static volatile ModManager instance;
    private File modsDir;
    private File configFile;
    private final Map<String, Boolean> enabledMap = new LinkedHashMap<>();
    private final List<String> modOrder = new ArrayList<>();
    private FileObserver modDirObserver;
    private GameVersion currentVersion;
    private final MutableLiveData<Void> modsChangedLiveData = new MutableLiveData<>();
    private final Gson gson = new Gson();
    private final String TAG = "ModManager";

    private ModManager() {}

    public static ModManager getInstance() {
        ModManager result = instance;
        if (result == null) {
            synchronized (ModManager.class) {
                result = instance;
                if (result == null) {
                    instance = result = new ModManager();
                }
            }
        }
        return result;
    }

    public static native void nativeLoadMod(String path, Mod mod);

    private void loadModMetadata(Mod mod) {
    
        try {
    
            File modFile = new File(modsDir, mod.getFileName());
            ZipFile zip = new ZipFile(modFile);
    
            ZipEntry entry = zip.getEntry("manifest.json");
    
            if (entry == null) {
                zip.close();
                return;
            }
    
            InputStreamReader reader = new InputStreamReader(zip.getInputStream(entry));
    
            Map<String, Object> json = gson.fromJson(reader, Map.class);
    
            String version = (String) json.get("version");
            String description = (String) json.get("description");
            String icon = (String) json.get("icon");
            String author = (String) json.get("author");
            String jsentry = (String) json.get("entry");
            boolean isJS = (jsentry != null && !jsentry.equals(""));

            mod.setMetadata(version, description, icon, author, isJS);
            if(isJS) mod.setEntry(jsentry);
    
            zip.close();
    
        } catch (Exception ignored) {}
    }

    public synchronized void setCurrentVersion(GameVersion version) {
        if (Objects.equals(currentVersion, version)) return;
        stopFileObserver();
        currentVersion = version;

        if (version != null && version.modsDir != null) {
            modsDir = version.modsDir;
            modsDir.mkdirs();
            configFile = new File(modsDir, "mods_config.json");
            loadConfig();
            initFileObserver();
        } else {
            modsDir = null;
            configFile = null;
            enabledMap.clear();
            modOrder.clear();
        }
        notifyModsChanged();
    }

    public GameVersion getCurrentVersion() {
        return currentVersion;
    }

    public void updateMods() {
        File[] files = modsDir.listFiles((dir, name) -> name.endsWith(".so"));
        if (files != null) {
            for (File file : files) {
                String newPath = file.getAbsolutePath().replace(".so", ".llmod");
                File nf = new File(newPath);
                LLModBuilder.buildLLMod(file,nf);
                file.delete();
            }
        }
        refreshMods();
    }

   public boolean isThereSoMods() {
        List<Mod> mods = getMods();
        if (mods == null) return false;
    
        for (Mod mod : mods) {
            if (mod.isOldMod()) return true;
        }
        return false;
    }

    public synchronized List<Mod> getMods() {
        if (modsDir == null) return new ArrayList<>();

        List<Mod> mods = new ArrayList<>();
        File[] files = modsDir.listFiles((dir, name) -> (name.endsWith(".llmod") || name.endsWith(".so")));
        boolean changed = false;

        // Add new mods
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                if (!enabledMap.containsKey(fileName)) {
                    enabledMap.put(fileName, true);
                    modOrder.add(fileName);
                    changed = true;
                }
            }
        }

        // Remove deleted mods
        Iterator<String> it = modOrder.iterator();

        while (it.hasNext()) {
            String fileName = it.next();
            File file = new File(modsDir, fileName);
        
            if (!file.exists()) {
                it.remove();
                enabledMap.remove(fileName);
            }
        }

        for (int i = 0; i < modOrder.size(); i++) {
            String fileName = modOrder.get(i);
            Mod mod = new Mod(fileName, enabledMap.getOrDefault(fileName, true), i);
            loadModMetadata(mod);
            mods.add(mod);
        }

        if (changed) saveConfig();
        return mods;
    }

    public synchronized void setModEnabled(String fileName, boolean enabled) {
        if (modsDir == null) return;
        if (!fileName.endsWith(".llmod") && !fileName.endsWith(".so")) fileName += ".llmod";
        if (enabledMap.containsKey(fileName)) {
            enabledMap.put(fileName, enabled);
            saveConfig();
            notifyModsChanged();
        }
    }

    private void loadConfig() {
        enabledMap.clear();
        modOrder.clear();

        if (!configFile.exists()) {
            updateConfigFromDirectory();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> configList = gson.fromJson(reader, type);

            if (configList != null) {
                for (Map<String, Object> item : configList) {
                    String name = (String) item.get("name");
                    Boolean enabled = (Boolean) item.get("enabled");
                    if (name != null && enabled != null) {
                        enabledMap.put(name, enabled);
                        modOrder.add(name);
                    }
                }
            } else {
                updateConfigFromDirectory();
            }
        } catch (Exception e) {
            updateConfigFromDirectory();
        }
    }

    private void updateConfigFromDirectory() {
        File[] files = modsDir.listFiles((dir, name) -> (name.endsWith(".llmod") || name.endsWith(".so")));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                enabledMap.put(fileName, true);
                modOrder.add(fileName);
            }
        }
        saveConfig();
    }

    private void saveConfig() {
        if (configFile == null) return;
        try (FileWriter writer = new FileWriter(configFile)) {
            List<Map<String, Object>> configList = new ArrayList<>();
            for (int i = 0; i < modOrder.size(); i++) {
                String fileName = modOrder.get(i);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", fileName);
                item.put("enabled", enabledMap.get(fileName));
                item.put("order", i);
                configList.add(item);
            }
            gson.toJson(configList, writer);
        } catch (Exception ignored) {}
    }

    private void initFileObserver() {
        if (modsDir == null) return;
        modDirObserver = new FileObserver(modsDir.getAbsolutePath(),
                FileObserver.CREATE | FileObserver.DELETE | FileObserver.MOVED_FROM | FileObserver.MOVED_TO) {
            @Override
            public void onEvent(int event, String path) {
                notifyModsChanged();
            }
        };
        modDirObserver.startWatching();
    }

    private void stopFileObserver() {
        if (modDirObserver != null) {
            modDirObserver.stopWatching();
            modDirObserver = null;
        }
    }

    public synchronized void deleteMod(String fileName) {
        if (modsDir == null) return;
        if (!fileName.endsWith(".llmod") && !fileName.endsWith(".so")) fileName += ".llmod";

        File modFile = new File(modsDir, fileName);
        if (modFile.exists() && modFile.delete()) {
            enabledMap.remove(fileName);
            modOrder.remove(fileName);
            saveConfig();
            notifyModsChanged();
        }
    }

    public synchronized void reorderMods(List<Mod> reorderedMods) {
        if (modsDir == null) return;

        modOrder.clear();
        for (Mod mod : reorderedMods) {
            modOrder.add(mod.getFileName());
        }
        saveConfig();
        notifyModsChanged();
    }

    private void notifyModsChanged() {
        modsChangedLiveData.postValue(null);
    }

    public MutableLiveData<Void> getModsChangedLiveData() {
        return modsChangedLiveData;
    }

    public synchronized void refreshMods() {
        notifyModsChanged();
    }
    
    public void loadMods(File cacheDir) {
        List<Mod> mods = getMods();
        for (Mod mod : mods) {
            if (!mod.isEnabled() || mod.isOldMod()) continue;
            File src = new File(currentVersion.modsDir, mod.getFileName());
            File dir = new File(cacheDir, "mods/" + mod.getDisplayName());
            if (!dir.exists()) dir.mkdirs();
            try {
                copyFromArchive(src,"libs", dir);
                File[] files = dir.listFiles((folder, name) -> name.endsWith(".so"));
                if (files != null) {
                    for (File file : files) {
                        String path = file.getAbsolutePath();
                        nativeLoadMod(path, mod);
                        Log.i(TAG, "Loaded native mod from: " + mod.getDisplayName() + ", path: " + path);
                    }
                } else {
                    dir.delete();
                }
            } catch (IOException e) {
                Log.e(TAG, "Can't load " + src.getName() + ": " + e.getMessage());
            }
        }
    }

    private static void copyFromArchive(File archive, String entryPath, File dest) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archive))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.getName().startsWith(entryPath)) continue;

                File outFile = new File(dest, entry.getName().substring(entryPath.length()));

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    File parent = outFile.getParentFile();
                    if (parent != null) parent.mkdirs();

                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }

                zis.closeEntry();
            }
        }
    }
}
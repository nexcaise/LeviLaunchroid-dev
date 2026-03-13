package org.levimc.launcher.util;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import android.util.Log;

public class LLModBuilder {

    private static final String TAG = "LLModBuilder";
    
    public static boolean hasJSLoader(File dir) {
        File file = new File(dir, "libjsLoader.so");
        if(!(dir.exists() || file.exists())) return false;
        return true;
    }

    public static void buildLLMod(File soFile, File outputLLMod) {
        try {

            if (!soFile.exists()) {
                throw new FileNotFoundException(".so file not found: " + soFile);
            }

            String fileName = soFile.getName();
            String modName = fileName.substring(0, fileName.lastIndexOf('.'));

            String uuid = UUID.randomUUID().toString();

            String manifest = "{\n" +
                    "  \"name\": \"" + modName + "\",\n" +
                    "  \"description\": \"" + modName + "\",\n" +
                    "  \"author\": \"\",\n" +
                    "  \"icon\": \"\",\n" +
                    "  \"version\": \"v1.0\",\n" +
                    "  \"uuid\": \"" + uuid + "\"\n" +
                    "}";

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputLLMod))) {

                ZipEntry manifestEntry = new ZipEntry("manifest.json");
                zos.putNextEntry(manifestEntry);
                zos.write(manifest.getBytes());
                zos.closeEntry();

                ZipEntry libEntry = new ZipEntry("libs/" + fileName);
                zos.putNextEntry(libEntry);

                Files.copy(soFile.toPath(), zos);

                zos.closeEntry();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }
}
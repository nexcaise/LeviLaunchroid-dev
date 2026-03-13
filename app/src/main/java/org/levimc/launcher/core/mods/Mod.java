package org.levimc.launcher.core.mods;

public class Mod {

    private final String fileName;
    private boolean enabled;
    private int order;

    private String version = "0.0.0";
    private String description = "-";
    private String iconName = "icon.png";
    private String author = "-";
    private String entry = "scripts/index.js";
    private boolean isJS = false;

    public Mod(String fileName, boolean enabled, int order) {
        this.fileName = fileName;
        this.enabled = enabled;
        this.order = order;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isOldMod() {
        return getFileName().endsWith(".so");
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getDisplayName() {
        return fileName.replace(".llmod", "");
    }

    public String getVersion() {
        return version;
    }
    
    public String getEntry() {
        return entry;
    }
    
    public boolean isJSmod() {
        return isJS;
    }

    public String getDescription() {
        return description;
    }

    public String getIconName() {
        return iconName;
    }

    public String getAuthor() {
        return author;
    }

    public void setMetadata(String version, String description, String iconName, String author, boolean ij) {
        if(version != null || version != "null" || version != "") {
            this.version = version;
        }
        if(description != null || description != "null" || description != "") {
            this.description = description;
        }
        if(author != null || author != "null" || author != "") {
            this.author = author;
        }
        
        this.iconName = iconName;
        this.isJS = ij;
    }
    
    public void setEntry(String entry) {
        this.entry = entry;
    }
}
package org.levimc.launcher.core.mods;

public class Mod {

    private final String fileName;
    private boolean enabled;
    private int order;

    private String version = "1.0";
    private String description = "";
    private String iconName;
    private String author = "Unknown";
    private String entry = "";
    private boolean isJS;

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
        this.version = version;
        this.description = description;
        this.iconName = iconName;
        this.author = author;
        this.isJS = ij;
    }
    
    public void setEntry(String entry) {
        this.entry = entry;
    }
}
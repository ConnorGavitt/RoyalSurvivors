package org.royaldev.royalsurvivors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfManager extends YamlConfiguration {

    private File pconfl = null;
    private final Object saveLock = new Object();
    private final String path;

    /**
     * Configuration file manager
     * <p/>
     * If file does not exist, it will be created.
     *
     * @param filename Filename (local) for the config
     */
    public ConfManager(String filename) {
        super();
        File dataFolder = RoyalSurvivors.dataFolder;
        path = dataFolder + File.separator + filename;
        pconfl = new File(path);
        try {
            load(pconfl);
        } catch (Exception ignored) {
        }
    }

    /**
     * Configuration file manager
     * <p/>
     * If file does not exist, it will be created.
     *
     * @param file File object for the config
     */
    public ConfManager(File file) {
        super();
        File dataFolder = RoyalSurvivors.dataFolder;
        path = dataFolder + File.separator + file.getName();
        pconfl = new File(path);
        try {
            load(pconfl);
        } catch (Exception ignored) {
        }
    }

    public boolean exists() {
        return pconfl.exists();
    }

    public boolean createFile() {
        try {
            return pconfl.createNewFile();
        } catch (IOException ignored) {
            return false;
        }
    }

    public void forceSave() {
        synchronized (saveLock) {
            try {
                save(pconfl);
            } catch (IOException ignored) {
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    /**
     * Gets a Location from config
     * <p/>
     * This <strong>will</strong> throw an exception if the saved Location is invalid or missing parts.
     *
     * @param path Path in the yml to fetch from
     * @return Location or null if path does not exist or if config doesn't exist
     */
    public Location getLocation(String path) {
        if (get(path) == null) return null;
        String world = getString(path + ".w");
        double x = getDouble(path + ".x");
        double y = getDouble(path + ".y");
        double z = getDouble(path + ".z");
        float pitch = getFloat(path + ".pitch");
        float yaw = getFloat(path + ".yaw");
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }

    /**
     * Sets a location in config
     *
     * @param value Location to set
     * @param path  Path in the yml to set
     */
    public void setLocation(String path, Location value) {
        set(path + ".w", value.getWorld().getName());
        set(path + ".x", value.getX());
        set(path + ".y", value.getY());
        set(path + ".z", value.getZ());
        set(path + ".pitch", value.getPitch());
        set(path + ".yaw", value.getYaw());
    }

    public Float getFloat(String path) {
        return (float) getDouble(path);
    }
}

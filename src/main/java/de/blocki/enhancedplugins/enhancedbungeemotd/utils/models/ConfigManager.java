package de.blocki.enhancedplugins.enhancedbungeemotd.utils.models;

import de.blocki.enhancedplugins.enhancedbungeemotd.EnhancedBungeeMOTD;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ConfigManager {

    static {
        if (!EnhancedBungeeMOTD.getPlugin().getDataFolder().exists()) { EnhancedBungeeMOTD.getPlugin().getDataFolder().mkdir(); }
    }

    private static File file = new File(EnhancedBungeeMOTD.getPlugin().getDataFolder(), "config.yml");
    private static Configuration yamlConfiguration;

    static {
        try {
            if(!file.exists()){
                file.createNewFile();
            }
            yamlConfiguration = YamlConfiguration.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void set(String path, Object value){
        yamlConfiguration.set(path, value);
        save();
    }
    public static void setString(String path, String value){
        yamlConfiguration.set(path, value);
        save();
    }
    public static void setDef(String path, Object value){
        if(!isSet(path)){
            yamlConfiguration.set(path, value);
            save();
        }
    }

    public static boolean isSet(String path){
        return yamlConfiguration.contains(path);
    }

    public static String getString(String path){
        if(isSet(path)) {
            return yamlConfiguration.getString(path);
        }else {
            return "Not Found: " + path;
        }
    }
    public static Object getObj(String path){
        if(isSet(path)) {
            return yamlConfiguration.get(path);
        }else {
            return "Not Found: " + path;
        }
    }
    public static boolean getBool(String path){ return yamlConfiguration.getBoolean(path); }
    public static Integer getInteger(String path){ return yamlConfiguration.getInt(path); }
    public static List<?> getList(String path){
        return yamlConfiguration.getList(path);
    }

    public static void save(){
        try{
            YamlConfiguration.getProvider(YamlConfiguration.class).save(yamlConfiguration, file);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}

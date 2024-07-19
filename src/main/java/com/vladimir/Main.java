package com.vladimir;

import org.apache.commons.lang3.StringUtils;
import org.bspfsystems.yamlconfiguration.configuration.ConfigurationSection;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
//native-image -jar C:\Users\vlad\stompWrapper\build\libs\stompWrapper-1.0-SNAPSHOT-all.jar -O3 --enable-http --enable-https -R:MinHeapSize=128m -R:MaxHeapSize=512m
public class Main implements Runnable{
    public String url;
    public String login;
    public String password;
    public String endpoint;
    public static PrintStream localOut;
    public static void main(String[] args) {
        Thread main = new Thread(new Main());
        main.start();
    }

    @Override
    public void run() {
        try {
            URL loc = Main.class.getProtectionDomain().getCodeSource().getLocation();
            
            String path = String.valueOf(loc);
            path = path.substring(6);
            path = path.replace(new java.io.File(loc.getPath()).getName(), "\\");
            
            File targetFile = new File(path + "config.yml");
            
            File log = new File(path + "log.txt");
            log.deleteOnExit();
            localOut = new PrintStream(log);
                    
            localOut.println("Starting");

            if (!targetFile.exists()) {
                try (InputStream is = Main.class.getClassLoader().getResourceAsStream("config.yml")) {
                    assert is != null;
                    Files.copy(is, Path.of(path + "config.yml"));
                }
            }

            YamlConfiguration config = new YamlConfiguration();
            config.load(targetFile);
            Relay.fromConfig(config);

            ConfigurationSection creds = config.getConfigurationSection("credentials");
            assert creds != null;
            String ip = creds.getString("ip");
            String port = creds.getString("port");

            endpoint = creds.getString("endpoint");

            url = "ws://" + ip + ":" + port + "/spot";

            login = creds.getString("login");
            password = creds.getString("password");

            Stomp stomp = Stomp.build(url, 0, 0, ip + ":" + port);
            stomp.initialize();
            stomp.subscribe(endpoint, 0, string -> Relay.callAll(parse(string)));

            localOut.println("Ended Init");
            Thread.currentThread().join();

        } catch(Exception e) {
            e.printStackTrace(localOut);
        }
    }
    private static String parse(String s){
        return StringUtils.substringBefore(StringUtils.substringAfter(s , "cameraId:"), "\n");
    }
}
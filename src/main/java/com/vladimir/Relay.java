package com.vladimir;

import org.bspfsystems.yamlconfiguration.configuration.ConfigurationSection;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;

import static com.vladimir.Main.localOut;

public class Relay {
    public final List<String> camIDs = new ArrayList<>();
    private static final List<Relay> callmap = new ArrayList<>();
    private final String url, password, login;
    private final int id;

    public Relay(String url, int id, String login, String password, String... camIDs){
        this.camIDs.addAll(List.of(camIDs));
        this.url = url;
        this.password = password;
        this.login = login;
        this.id = id;
        callmap.add(this);
    }
    public void call(){
        String request;
        if(password == null && login == null){
            request = "http://" + url + "/rb" + id + "s.cgi";
        }else{
            request = "http://"+ login + ":" + password + "@" + url + "/protect/rb" + id + "s.cgi";
        }

        String command = "curl -X GET " + request;

        try {
            Process p = new ProcessBuilder(command.split(" ")).start();
            String out = new String(p.getInputStream().readAllBytes());
            if(out.contains("Success!")) localOut.println("Sucessfuly activated " + url);
            else localOut.println("error: " + url);
        } catch(Exception e){
            e.printStackTrace(localOut);
        }

        localOut.println();
    }
    public static void callAll(String cam){
        localOut.println("Got packet from SPOT");
        for (Relay relay : callmap) {
            if(relay.camIDs.contains(cam)) {
                localOut.println("Matched: " + cam);
                relay.call();
            }
            else localOut.println("Mismatched: " + relay.camIDs + " with " + cam);
        }
    }
    public static void fromConfig(YamlConfiguration conf){
        ConfigurationSection relays = conf.getConfigurationSection("relays");
        assert relays != null;

        for (String key : relays.getKeys(false)) {
            ConfigurationSection relay = relays.getConfigurationSection(key);
            assert relay != null;

            int id = relay.getInt("id");
            String ip = relay.getString("ip");
            String login = relay.getString("login");
            String password = relay.getString("password");
            List<String> cameras = relay.getStringList("cameras-uuid");

            new Relay(ip, id, login, password, cameras.toArray(new String[0]));
        }
    }
}

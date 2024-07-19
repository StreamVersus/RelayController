package com.vladimir;

import org.apache.commons.lang3.StringUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

import static com.vladimir.Main.localOut;

//Self-created Stomp-Over-WebSocket Implementation
//Reason: All libs are deprecated, or too big for this task
public class Stomp extends WebSocketClient{

    private final int cy, cx;
    private final String host;
    private final Heartbeat heartbeatService;
    private final Map<Integer, Consumer<String>> map = new HashMap<>();
    public static Stomp build(String url, int clientH, int serverH, String host){
        Stomp object;

        while(true) {
            object = new Stomp(url, clientH, serverH, host);
            object.connectThis();

            if (!object.isClosed()) break;

            System.out.println("No such server\nRetrying in 5 seconds");
            LockSupport.parkNanos(5000 * 1000000L);
        }

        return object;
    }
    public Stomp(String url, int clientH, int serverH, String host){
        super(URI.create(url));
        cx = clientH;
        cy = serverH;
        heartbeatService = new Heartbeat(this, cx, cy);
        this.host = host;
    }

    public void connectThis(){
        try{
            connectBlocking();
        } catch (Exception e){
            System.out.println("WebSocketError!");
            e.printStackTrace(localOut);
        }
    }

    public void initialize(){
        String connectionHead = "CONNECT\n" +
                "accept-version:1.0,1.1,1.2\n" +
                "host:" + host + "\n" +
                "heart-beat:"+ cx+ "," + cy + "\n" +
                "\n" +
                "\000";
        send(connectionHead);
    }
    public void subscribe(String dest, int id, Consumer<String> run){
        String raw = "SUBSCRIBE\n" +
                "id:"+ id +"\n" +
                "destination:"+dest+"\n" +
                "\n" +
                "\000";
        send(raw);
        map.put(id, run);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {}

    @Override
    public void onMessage(String s) {
        heartbeatService.isOnline = true;
        if(s.contains("CONNECTED")){
            Thread t = new Thread(heartbeatService);
            t.setDaemon(true);
            t.start();
        } else if (s.contains("MESSAGE")) {
            int id = Integer.parseInt(StringUtils.substringBefore(StringUtils.substringAfter(s, "subscription:"), "\n"));
            map.get(id).accept(StringUtils.substringBefore(s, "{\"grids\":"));
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        System.out.println("Connection closed");
    }

    @Override
    public void onError(Exception e) {

    }
}
class Heartbeat implements Runnable{
    private final WebSocketClient client;
    private final int i, z;
    public boolean isOnline = true;
    public Heartbeat(WebSocketClient client, int i, int z){
        this.client = client;
        this.i = i;
        this.z = z;
    }
    @Override
    public void run() {
        if(i != 0) clientLoop();
        if(z != 0) serverLoop();
    }
    public void clientLoop(){
        while(true){
            LockSupport.parkNanos(i * 1000000L);
            client.send("\n");
        }
    }
    public void serverLoop(){
        while(true){
            if(!isOnline) {
                codeRed();
                break;
            }
            isOnline = false;
            LockSupport.parkNanos(z * 1000000L);
        }
    }
    public void codeRed(){
        LockSupport.parkNanos(z * 1000000L * 5);
        if(!isOnline){
            localOut.println("Server skipped heartbeated\nDisconecting");
            client.close(200, "no pulse");
        }
    }
}

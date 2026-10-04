package com.server;

import org.java_websocket.WebSocket;
import org.json.JSONArray;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ClientRegistry {

    private final Map<WebSocket, String> bySocket = new ConcurrentHashMap<>();

    private final Map<String, WebSocket> byName = new ConcurrentHashMap<>();

    void add(WebSocket socket, String name) {
        bySocket.put(socket, name);
        byName.put(name, socket);
    }

    String remove(WebSocket socket) {
        String name = bySocket.remove(socket);

        if (name != null) {
            byName.remove(name);
        }

        return name;
    }

    String nameBySocket(WebSocket socket) {
        return bySocket.get(socket);
    }

    void changeName(WebSocket socket, String newName) {
        String oldName = bySocket.get(socket);

        if (oldName != null) {
            bySocket.remove(socket);
            byName.remove(oldName);
        }

        bySocket.put(socket, newName);
        byName.put(newName, socket);
    }

    JSONArray currentNames() {
        JSONArray arr = new JSONArray();

        for (String n : byName.keySet()) {
            arr.put(n);
        }

        return arr;
    }

    String cleanupDisconnected(WebSocket socket) {
        return remove(socket);
    }

    Map<WebSocket, String> snapshot() {
        return Map.copyOf(bySocket);
    }
}
package com.server;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * Servidor WebSocket del Sudoku.
 *
 * Mensajes utilizados:
 * - clients: lista de jugadores y puntuaciones.
 * - name: registra el nombre del jugador.
 * - move: comprueba una jugada.
 * - error: informa de un error.
 * - restart: reinicia la partida.
 */
public class Main extends WebSocketServer {

    /** Port per defecte on escolta el servidor. */
    public static final int DEFAULT_PORT = 3000;

    // Claus JSON
    private static final String K_TYPE = "type";
    private static final String K_MESSAGE = "message";
    private static final String K_LIST = "list";

    // Tipus de missatge
    private static final String T_CLIENTS = "clients";
    private static final String T_ERROR = "error";
    private static final String T_NAME = "name";
    private static final String T_MOVE = "move";
    

    /** Registre de clients connectats. */
    private final ClientRegistry clients;
    private final Map<String, Integer> scores = new java.util.concurrent.ConcurrentHashMap<>();
    private int casillasCompletadas = 0;

    private static int[][] solucion = {
        {9, 2, 3, 5, 1, 4, 6, 8, 7},
        {8, 5, 7, 3, 6, 9, 2, 4, 1},
        {1, 4, 6, 8, 2, 7, 5, 9, 3},
        {5, 3, 4, 7, 9, 6, 1, 2, 8},
        {7, 9, 2, 1, 8, 3, 4, 5, 6},
        {6, 1, 8, 4, 5, 2, 7, 3, 9},
        {2, 6, 5, 9, 7, 8, 3, 1, 4},
        {4, 8, 1, 6, 3, 5, 9, 7, 2},
        {3, 7, 9, 2, 4, 1, 8, 6, 5}
    };    

    /**
     * Crea un servidor WebSocket que escolta a l'adreça indicada.
     *
     * @param address adreça i port d'escolta del servidor
     */
    public Main(InetSocketAddress address) {
        super(address);
        this.clients = new ClientRegistry();
    }

    // ----------------- Helpers JSON -----------------

    /**
     * Crea un objecte JSON amb el camp type inicialitzat.
     *
     * @param type valor per a type
     * @return instància de JSONObject amb el tipus establert
     */
    private static JSONObject msg(String type) {
        return new JSONObject().put(K_TYPE, type);
    }

    /**
     * Afegeix clau-valor al JSONObject si el valor no és null.
     *
     * @param o objecte JSON destí
     * @param k clau
     * @param v valor (ignorat si és null)
     */
    

    /**
     * Envia de forma segura un payload i, si el socket no està connectat,
     * el neteja del registre.
     *
     * @param to socket destinatari
     * @param payload cadena JSON a enviar
     */
    private void sendSafe(WebSocket to, String payload) {
        if (to == null) return;
        try {
            to.send(payload);
        } catch (WebsocketNotConnectedException e) {
            String name = clients.cleanupDisconnected(to);
            System.out.println("Client desconnectat durant send: " + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Envia un missatge a tots els clients excepte l’emissor.
     *
     * @param sender  socket emissor
     * @param payload cadena JSON a enviar
     */
    private void broadcastMessage(String payload) {
        for (Map.Entry<WebSocket, String> e : clients.snapshot().entrySet()) {
            sendSafe(e.getKey(), payload);
        }
    }

    /**
     * Envia la llista actualitzada de clients a tots els clients connectats.
     */
    private void sendClientsListToAll() {

        JSONArray list = clients.currentNames();
        JSONArray scoresList = new JSONArray();

        for (int i = 0; i < list.length(); i++) {
            String name = list.getString(i);
            scoresList.put(scores.get(name));
        }

        for (Map.Entry<WebSocket, String> e : clients.snapshot().entrySet()) {

            JSONObject rst = msg(T_CLIENTS);

            rst.put(K_LIST, list);
            rst.put("scores", scoresList);

            sendSafe(e.getKey(), rst.toString());
        }
    }

    // ----------------- WebSocketServer overrides -----------------

    /** Registra la connexió del client. */
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
    }

    /** Elimina el client del registre i notifica la llista actualitzada. */
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String name = clients.remove(conn);
        scores.remove(name);
        System.out.println("Client desconnectat: " + name);
        sendClientsListToAll();
    }

    /** Processa el missatge rebut i el ruteja segons el seu type. */
    @Override
    public void onMessage(WebSocket conn, String message) {
        String origin = clients.nameBySocket(conn);
        JSONObject obj;
        try {
            obj = new JSONObject(message);
        } catch (Exception ex) {
            sendSafe(conn, msg(T_ERROR).put(K_MESSAGE, "JSON invàlid").toString());
            return;
        }

        String type = obj.optString(K_TYPE, "");
        switch (type) {
            case T_NAME -> {
                String name = obj.optString("name", "");

                if (name.isBlank()) {
                    sendSafe(conn, msg(T_ERROR).put(K_MESSAGE, "El nom no pot estar buit").toString());
                    return;
                }
                System.out.println("Client connectat: " + name);

                if (clients.nameBySocket(conn) == null) {
                    clients.add(conn, name);
                    scores.put(name, 0);
                } else {
                    clients.changeName(conn, name);
                    scores.put(name, 0);
                }

                sendClientsListToAll();
            }
            case T_MOVE -> {
                int row = obj.getInt("row");
                int column = obj.getInt("column");
                int value = obj.getInt("value");

                if (solucion[row][column] == value) {
                    System.out.println("Correcto +2");

                    scores.put(origin, scores.get(origin) + 2);
                    casillasCompletadas++;
                    System.out.println("CASILLAS COMPLETADAS: " + casillasCompletadas);

                    JSONObject result = new JSONObject();
                    result.put("type", "score");
                    result.put("player", origin);
                    result.put("score", scores.get(origin));
                    result.put("correct", true);
                    result.put("row", row);
                    result.put("column", column);
                    result.put("value", value);

                    broadcastMessage(result.toString());
                    if (casillasCompletadas >= 50) {
                        JSONObject end = new JSONObject();
                        end.put("type", "end");
                        broadcastMessage(end.toString());                    }

                } else {
                    System.out.println("Incorrecto -1");

                    scores.put(origin, scores.get(origin) - 1);

                    JSONObject result = new JSONObject();
                    result.put("type", "score");
                    result.put("player", origin);
                    result.put("score", scores.get(origin));
                    result.put("correct", false);
                    result.put("row", row);
                    result.put("column", column);
                    result.put("value", value);


                    broadcastMessage(result.toString());
                }
            }
            case "restart" -> {

                casillasCompletadas = 0;

                for (String name : scores.keySet()) {
                    scores.put(name, 0);
                }

                JSONObject result = new JSONObject();
                result.put("type", "restart");

                broadcastMessage(result.toString());

                sendClientsListToAll();
            }
            default -> {
                sendSafe(conn, msg(T_ERROR).put(K_MESSAGE, "Tipus desconegut: " + type).toString());
            }
        }
    }

    /** Log d'error global o de socket concret. */
    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    /** Arrencada: log i configuració del timeout de connexió perduda. */
    @Override
    public void onStart() {
        System.out.println("Servidor WebSocket engegat al port: " + getPort());
        setConnectionLostTimeout(100);
    }

    // ----------------- Lifecycle util -----------------

    /**
     * Registra un shutdown hook per aturar netament el servidor en finalitzar el procés.
     *
     * @param server instància a aturar
     */
    private static void registerShutdownHook(Main server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Aturant servidor (shutdown hook)...");
            try {
                server.stop(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            System.out.println("Servidor aturat.");
        }));
    }

    /**
     * Bloqueja el fil principal indefinidament fins que sigui interromput.
     * Útil per mantenir viu el procés sense REPL.
     */
    private static void awaitForever() {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            // CountDownLatch permet que el fil principal 
            // s'esperi indefinidament amb latch.await()
            latch.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Punt d'entrada: arrenca el servidor al port per defecte i espera senyals.
     *
     * @param args arguments de línia d'ordres (no utilitzats)
     */
    public static void main(String[] args) {
        Main server = new Main(new InetSocketAddress(DEFAULT_PORT));
        server.start();
        registerShutdownHook(server);

        System.out.println("Servidor WebSocket en execució al port " + DEFAULT_PORT + ". Prem Ctrl+C per aturar-lo.");
        awaitForever();
    }
}

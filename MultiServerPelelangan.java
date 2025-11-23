import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JTextArea;

/**
 * MultiServerPelelangan (updated)
 * - expects BarangLelang in external file (your BarangLelang.java)
 * - accepts messages:
 *   NAME:<username>
 *   JOIN:<auctionId>   or SELECT:<auctionId>
 *   BID:<auctionId>:<name>:<amount>   (explicit)
 *   or if client already joined auction, a plain number line "30000" will be treated as bid
 *
 * - broadcasts UPDATE messages using BarangLelang.getUpdateMessage() which returns "UPDATE:harga:penawar"
 */
public class MultiServerPelelangan {

    private ServerSocket serverSocket;
    private static final int PORT = 1234;

    private final Map<String, BarangLelang> currentAuctions = new ConcurrentHashMap<>();
    private final Map<String, Boolean> auctionActiveStatus = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    private final List<PrintWriter> allClientWriters = new CopyOnWriteArrayList<>();
    private final Map<PrintWriter, String> clientAuctionMap = new ConcurrentHashMap<>(); // writer -> auctionId
    private final Map<PrintWriter, String> clientNameMap = new ConcurrentHashMap<>();    // writer -> name

    private final AtomicInteger auctionIdCounter = new AtomicInteger(1);

    private JTextArea guiLog; // GUI log


    // ====================================================================
    // DIPANGGIL OLEH GUI SAAT TAMBAH BARANG
    // ====================================================================
    public void addAuctionFromGUI(String namaBarang, int hargaAwal, int durasi, JTextArea areaLog) {
        this.guiLog = areaLog;

        String auctionId = "ID" + auctionIdCounter.getAndIncrement();

        // use external BarangLelang
        BarangLelang barang = new BarangLelang(auctionId, namaBarang, hargaAwal, this);
        currentAuctions.put(auctionId, barang);
        auctionActiveStatus.put(auctionId, true);

        appendToGUI("Barang terdaftar: " + auctionId + " | " + namaBarang + "\n");

        // schedule stop
        scheduler.schedule(() -> {
            stopAuction(auctionId);
            appendToGUI("Lelang selesai untuk: " + namaBarang + "\n");
        }, durasi, TimeUnit.SECONDS);

        // broadcast new auctions list to all clients
        broadcastMessage(getAvailableAuctions());
    }


    // ====================================================================
    // START SERVER
    // ====================================================================
    public void startServer() throws IOException {
        serverSocket = new ServerSocket(PORT);
        appendToGUI("Server berjalan di port " + PORT + "\n");

        while (true) {
            Socket client = serverSocket.accept();
            appendToGUI("Penawar Baru: " + client.getRemoteSocketAddress() + "\n");

            new PenawarClientHandler(client, this, guiLog).start();
        }
    }


    // ====================================================================
    // STOP AUCTION
    // ====================================================================
    public void stopAuction(String auctionId) {
        BarangLelang barang = currentAuctions.get(auctionId);
        if (barang == null) return;

        auctionActiveStatus.put(auctionId, false);

        String info = barang.getPemenangInfo();
        appendToGUI("Lelang selesai: " + info + "\n");

        broadcastMessageToAuction(auctionId, "CLOSED:" + info);

        currentAuctions.remove(auctionId);

        broadcastMessage(getAvailableAuctions());
    }


    // ====================================================================
    // BROADCAST
    // ====================================================================
    public void broadcastMessage(String message) {
        for (PrintWriter w : allClientWriters) {
            w.println(message);
        }
    }

    public void broadcastMessageToAuction(String auctionId, String message) {
        for (var entry : clientAuctionMap.entrySet()) {
            if (entry.getValue() != null && entry.getValue().equals(auctionId)) {
                entry.getKey().println(message);
            }
        }
    }


    // ====================================================================
    // CLIENT MANAGEMENT
    // ====================================================================
    public void addClientWriter(PrintWriter w) { allClientWriters.add(w); }
    public void removeClientWriter(PrintWriter w) {
        allClientWriters.remove(w);
        clientAuctionMap.remove(w);
        clientNameMap.remove(w);
    }
    public void registerClientToAuction(PrintWriter w, String id) { clientAuctionMap.put(w, id); }
    public void unregisterClientFromAuction(PrintWriter w) { clientAuctionMap.remove(w); }
    public void setClientName(PrintWriter w, String name) { clientNameMap.put(w, name); }
    public String getClientName(PrintWriter w) { return clientNameMap.get(w); }

    public BarangLelang getAuction(String id) { return currentAuctions.get(id); }
    public boolean isAuctionActive(String id) { return auctionActiveStatus.getOrDefault(id, false); }


    // ====================================================================
    // LIST LELANG KE CLIENT
    // ====================================================================
    public String getAvailableAuctions() {
        StringBuilder sb = new StringBuilder("AUCTIONS:");
        boolean first = true;
        for (BarangLelang b : currentAuctions.values()) {
            if (!first) sb.append("|");
            sb.append(b.idLelang).append(",").append(b.namaBarang).append(",").append(b.hargaTawaranTertinggi);
            first = false;
        }
        return sb.toString();
    }


    // ====================================================================
    // APPEND GUI
    // ====================================================================
    private void appendToGUI(String text) {
        if (guiLog != null) {
            javax.swing.SwingUtilities.invokeLater(() -> guiLog.append(text));
        } else {
            System.out.print(text);
        }
    }


    // ====================================================================
    // CLIENT HANDLER
    // ====================================================================
    public static class PenawarClientHandler extends Thread {

        private final Socket client;
        private final MultiServerPelelangan server;
        private PrintWriter out;
        private BufferedReader in;
        private final JTextArea guiLog;

        public PenawarClientHandler(Socket socket, MultiServerPelelangan server, JTextArea guiLog) {
            this.client = socket;
            this.server = server;
            this.guiLog = guiLog;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true); // auto-flush
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                server.addClientWriter(out);

                // send initial auctions
                out.println(server.getAvailableAuctions());

                String line;
                while ((line = in.readLine()) != null) {
                    if (line == null) break;
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    appendToGUI("Client: " + line + "\n");

                    // NAME:<username>
                    if (line.startsWith("NAME:")) {
                        String name = line.substring(5).trim();
                        server.setClientName(out, name);
                        out.println("HELLO:" + name);
                        continue;
                    }

                    // SELECT:<id> or JOIN:<id>
                    if (line.startsWith("SELECT:") || line.startsWith("JOIN:")) {
                        String id = line.substring(line.indexOf(':') + 1).trim();
                        server.registerClientToAuction(out, id);
                        out.println("JOINED:" + id);
                        appendToGUI("Client joined auction: " + id + "\n");
                        continue;
                    }

                    // Explicit BID:<id>:<name>:<amount>
                    if (line.startsWith("BID:")) {
                        String[] p = line.split(":", 4);
                        if (p.length >= 4) {
                            String id = p[1];
                            String name = p[2];
                            int value;
                            try { value = Integer.parseInt(p[3]); }
                            catch (NumberFormatException ex) { out.println("BID-FAIL:FORMAT"); continue; }

                            BarangLelang barang = server.getAuction(id);
                            if (barang != null && server.isAuctionActive(id)) {
                                boolean success = barang.ajukanTawaran(name, value);
                                if (success) {
                                    appendToGUI("BID diterima: " + name + " => " + value + "\n");
                                    server.broadcastMessageToAuction(id, barang.getUpdateMessage()); // UPDATE:harga:penawar
                                } else {
                                    out.println("BID-FAIL:HARUS_LEBIH_TINGGI");
                                }
                            } else {
                                out.println("BID-FAIL:LELANG_TIDAK_AKTIF");
                            }
                        } else {
                            out.println("BID-FAIL:FORMAT");
                        }
                        continue;
                    }

                    // If line is numeric (simple bid) and client already joined an auction,
                    // treat it as a bid for the auction the client is registered to.
                    if (line.matches("^\\d+$")) {
                        String auctionId = server.clientAuctionMap.get(out);
                        if (auctionId == null) {
                            out.println("BID-FAIL:NOT_JOINED");
                            continue;
                        }
                        int value;
                        try { value = Integer.parseInt(line); }
                        catch (NumberFormatException ex) { out.println("BID-FAIL:FORMAT"); continue; }

                        String bidderName = server.getClientName(out);
                        if (bidderName == null || bidderName.isEmpty()) {
                            // fallback to remote address if name not provided
                            bidderName = client.getRemoteSocketAddress().toString();
                        }

                        BarangLelang barang = server.getAuction(auctionId);
                        if (barang != null && server.isAuctionActive(auctionId)) {
                            boolean success = barang.ajukanTawaran(bidderName, value);
                            if (success) {
                                appendToGUI("BID diterima: " + bidderName + " => " + value + "\n");
                                server.broadcastMessageToAuction(auctionId, barang.getUpdateMessage());
                            } else {
                                out.println("BID-FAIL:HARUS_LEBIH_TINGGI");
                            }
                        } else {
                            out.println("BID-FAIL:LELANG_TIDAK_AKTIF");
                        }
                        continue;
                    }

                    // LIST command
                    if (line.equalsIgnoreCase("LIST")) {
                        out.println(server.getAvailableAuctions());
                        continue;
                    }

                    // Unknown
                    out.println("UNKNOWN_COMMAND");
                }

            } catch (IOException e) {
                appendToGUI("ERROR ClientHandler: " + e.getMessage() + "\n");
            } finally {
                server.removeClientWriter(out);
                appendToGUI("Client terputus\n");
                try { client.close(); } catch (IOException ignored) {}
            }
        }

        private void appendToGUI(String text) {
            if (guiLog != null) {
                javax.swing.SwingUtilities.invokeLater(() -> guiLog.append(text));
            } else {
                System.out.print(text);
            }
        }
    }
}

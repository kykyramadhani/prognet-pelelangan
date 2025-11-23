// ===================== MultiServerPelelangan.java =====================
import java.io.PrintWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JTextArea;

public class MultiServerPelelangan {
    private ServerSocket serverSocket;
    private static final int PORT = 1234;

    private final Map<String, BarangLelang> currentAuctions = new HashMap<>();
    private final Map<String, Boolean> auctionActiveStatus = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final List<PrintWriter> allClientWriters = new CopyOnWriteArrayList<>();
    private final Map<PrintWriter, String> clientAuctionMap = new HashMap<>();
    private final AtomicInteger auctionIdCounter = new AtomicInteger(1);

    // ===================== START DARI GUI =====================
    public static void startFromGUI(String namaBarang, int hargaAwal, int durasi, JTextArea areaLog) throws Exception {
        MultiServerPelelangan server = new MultiServerPelelangan();

        String auctionId = "ID" + server.auctionIdCounter.getAndIncrement();
        BarangLelang barang = new BarangLelang(auctionId, namaBarang, hargaAwal, server);
        server.currentAuctions.put(auctionId, barang);
        server.auctionActiveStatus.put(auctionId, true);

        areaLog.append("Barang terdaftar dengan ID: " + auctionId + "\n");

        server.scheduler.schedule(() -> {
            server.stopAuction(auctionId);
            javax.swing.SwingUtilities.invokeLater(() ->
                areaLog.append("⏳ Lelang selesai untuk " + namaBarang + "\n")
            );
        }, durasi, TimeUnit.SECONDS);

        new Thread(() -> {
            try {
                server.startServer();
            } catch (Exception e) {
                javax.swing.SwingUtilities.invokeLater(() ->
                    areaLog.append("ERROR Server: " + e.getMessage() + "\n")
                );
            }
        }).start();
    }

    // ===================== SERVER NORMAL =====================
    private void startServer() throws IOException {
        serverSocket = new ServerSocket(PORT);
        System.out.println("Server aktif di port " + PORT);

        while (true) {
            Socket client = serverSocket.accept();
            System.out.println("Penawar baru: " + client.getInetAddress().getHostName());
            new PenawarClientHandler(client, this).start();
        }
    }

    private void stopAuction(String auctionId) {
        BarangLelang barang = currentAuctions.get(auctionId);
        if (barang == null) return;

        auctionActiveStatus.put(auctionId, false);
        System.out.println("Lelang " + auctionId + " selesai. " + barang.getPemenangInfo());
        broadcastMessageToAuction(auctionId, "CLOSED:" + barang.getPemenangInfo());
        currentAuctions.remove(auctionId);
        broadcastMessage(getAvailableAuctions());
    }

    public void broadcastMessage(String message) {
        for (PrintWriter writer : allClientWriters) writer.println(message);
    }

    public void broadcastMessageToAuction(String auctionId, String message) {
        for (Map.Entry<PrintWriter, String> entry : clientAuctionMap.entrySet()) {
            if (entry.getValue().equals(auctionId)) entry.getKey().println(message);
        }
    }

    public void addClientWriter(PrintWriter writer) { allClientWriters.add(writer); }
    public void removeClientWriter(PrintWriter writer) { allClientWriters.remove(writer); }
    public void registerClientToAuction(PrintWriter writer, String auctionId) { clientAuctionMap.put(writer, auctionId); }
    public void unregisterClientFromAuction(PrintWriter writer) { clientAuctionMap.remove(writer); }

    public BarangLelang getAuction(String auctionId) { return currentAuctions.get(auctionId); }
    public boolean isAuctionActive(String auctionId) { return auctionActiveStatus.getOrDefault(auctionId, false); }

    public String getAvailableAuctions() {
        StringBuilder sb = new StringBuilder("AUCTIONS:");
        boolean first = true;
        for (BarangLelang b : currentAuctions.values()) {
            if (!first) sb.append("|");
            sb.append(b.idLelang).append(",")
              .append(b.namaBarang).append(",")
              .append(b.hargaTawaranTertinggi);
            first = false;
        }
        return sb.toString();
    }
}

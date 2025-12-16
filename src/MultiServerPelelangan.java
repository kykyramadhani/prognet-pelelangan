import java.beans.PropertyChangeEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities; 

public class MultiServerPelelangan {

    private ServerSocket serverSocket;
    private static final int PORT = 1234;

    private final Map<String, BarangLelang> currentAuctions = new ConcurrentHashMap<>();
    private final Map<String, Boolean> auctionActiveStatus = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    private final Map<String, PenawarClientHandler> clientHandlers = new ConcurrentHashMap<>();
    private final Map<PrintWriter, String> clientAuctionMap = new ConcurrentHashMap<>(); 

    private final AtomicInteger auctionIdCounter = new AtomicInteger(1);

    private JTextArea guiLog; 

    // ADMIN CHAT & GUI FIRE HELPERS (PERBAIKAN AKSES PROTECTED)
    // fix: helper buat micu update list klien (biar gak error access)
    public void fireClientListUpdate() {
        if (guiLog != null) {
            SwingUtilities.invokeLater(() -> {
                PropertyChangeEvent event = new PropertyChangeEvent(
                    this, "clientListUpdate", false, true
                );
                // kirim event ke listener servergui
                for (java.beans.PropertyChangeListener listener : guiLog.getPropertyChangeListeners()) {
                    listener.propertyChange(event);
                }
            });
        }
    }

    // fix: helper buat trigger pesan chat baru (ngatasin protected access)
    public void fireNewClientChat(String senderName, String chatMessage) {
        if (guiLog != null) {
            SwingUtilities.invokeLater(() -> {
                PropertyChangeEvent event = new PropertyChangeEvent(
                    this, "newClientChat", (Object)senderName, (Object)chatMessage
                );
                // kirim event ke listener (servergui)
                for (java.beans.PropertyChangeListener listener : guiLog.getPropertyChangeListeners()) {
                    listener.propertyChange(event);
                }
            });
        }
    }
    
    public boolean sendPrivateMessage(String recipientName, String message) {
        PenawarClientHandler handler = clientHandlers.get(recipientName);
        if (handler != null && handler.out != null) {
            handler.sendMessage("CHAT_PRIVATE:Admin Lelang:" + message);
            return true;
        }
        return false;
    }

    public void broadcastChatMessage(String senderName, String message) {
        String fullMessage = "CHAT_BROADCAST:" + senderName + ":" + message;
        broadcastMessage(fullMessage);
    }
    
    
    // METODE INTI SERVER  
    public void addAuctionFromGUI(String namaBarang, int hargaAwal, int durasi, JTextArea areaLog) {
        this.guiLog = areaLog;
        String auctionId = "ID" + auctionIdCounter.getAndIncrement();
        BarangLelang barang = new BarangLelang(auctionId, namaBarang, hargaAwal, this);
        currentAuctions.put(auctionId, barang);
        auctionActiveStatus.put(auctionId, true);
        appendToGUI("Barang terdaftar: " + auctionId + " | " + namaBarang + "\n");

        scheduler.schedule(() -> {
            stopAuction(auctionId);
            appendToGUI("Lelang selesai untuk: " + namaBarang + "\n");
        }, durasi, TimeUnit.SECONDS);

        broadcastMessage(getAvailableAuctions());
    }

    public void startServer() throws IOException {
        serverSocket = new ServerSocket(PORT);
        appendToGUI("Server berjalan di port " + PORT + "\n");

        while (true) {
            Socket client = serverSocket.accept();
            appendToGUI("Penawar Baru Terkoneksi: " + client.getRemoteSocketAddress() + "\n");
            new PenawarClientHandler(client, this, guiLog).start(); 
        }
    }

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

    public void broadcastMessage(String message) {
        for (PenawarClientHandler handler : clientHandlers.values()) {
            handler.sendMessage(message);
        }
    }

    public void broadcastMessageToAuction(String auctionId, String message) {
        for (var entry : clientAuctionMap.entrySet()) {
            PenawarClientHandler handler = getHandlerByWriter(entry.getKey());
            if (entry.getValue() != null && entry.getValue().equals(auctionId) && handler != null) {
                handler.sendMessage(message);
            }
        }
    }

    public void addClientHandler(String name, PenawarClientHandler handler) { 
        clientHandlers.put(name, handler); 
    }
    
    public void removeClientHandler(PrintWriter w) {
        String nameToRemove = null;
        for(var entry : clientHandlers.entrySet()) {
            if(entry.getValue().out == w) {
                nameToRemove = entry.getKey();
                break;
            }
        }
        if (nameToRemove != null) {
            clientHandlers.remove(nameToRemove);
            appendToGUI("Klien " + nameToRemove + " terputus.\n");
        }
        clientAuctionMap.remove(w);
    }
    
    private PenawarClientHandler getHandlerByWriter(PrintWriter w) {
        for(PenawarClientHandler handler : clientHandlers.values()) {
            if(handler.out == w) return handler;
        }
        return null;
    }

    public void registerClientToAuction(PrintWriter w, String id) { clientAuctionMap.put(w, id); }
    public String getClientName(PrintWriter w) {
        PenawarClientHandler handler = getHandlerByWriter(w);
        return (handler != null) ? handler.clientName : null;
    }
    public List<String> getClientNames() {
        return new ArrayList<>(clientHandlers.keySet());
    }

    public BarangLelang getAuction(String id) { return currentAuctions.get(id); }
    public boolean isAuctionActive(String id) { return auctionActiveStatus.getOrDefault(id, false); }

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

    private void appendToGUI(String text) {
        if (guiLog != null) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                guiLog.append(text);
                guiLog.setCaretPosition(guiLog.getDocument().getLength());
            });
        } else {
            System.out.print(text);
        }
    }

    
    // CLIENT HANDLER
    public static class PenawarClientHandler extends Thread {

        private final Socket client;
        private final MultiServerPelelangan server;
        private PrintWriter out;
        private BufferedReader in;
        private final JTextArea guiLog;
        private String clientName;

        public PenawarClientHandler(Socket socket, MultiServerPelelangan server, JTextArea guiLog) {
            this.client = socket;
            this.server = server;
            this.guiLog = guiLog;
        }
        
        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true); 
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                String line;
                while ((line = in.readLine()) != null) {
                    if (line == null) break;
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    // 1. login:username:idtoken
                    if (line.startsWith("LOGIN:")) {
                        String[] parts = line.split(":", 3);
                        // kita ambil parts[1] buat nama klien. idtoken (parts[2]) gak dipake dulu.
                        this.clientName = (parts.length > 1) ? parts[1].trim() : "Penawar Tanpa Nama";
                        
                        server.addClientHandler(this.clientName, this); 
                        
                        appendToGUI("LOGIN Sukses: " + this.clientName + "\n");
                        out.println(server.getAvailableAuctions()); 
                        
                        server.fireClientListUpdate();
                        continue;
                    }

                    // 2. select:<id>
                    if (line.startsWith("SELECT:") || line.startsWith("JOIN:")) {
                        String id = line.substring(line.indexOf(':') + 1).trim();
                        server.registerClientToAuction(out, id);
                        
                        BarangLelang barang = server.getAuction(id);
                        if (barang != null) {
                            out.println(barang.getStatusInfo());
                        } else {
                            out.println("ERROR:Lelang ID '" + id + "' tidak ditemukan.");
                        }
                        
                        appendToGUI("Client " + this.clientName + " joined auction: " + id + "\n");
                        continue;
                    }
                    
                    // 3. chat:pesan obrolan (ke admin)
                    if (line.startsWith("CHAT:")) {
                        String chatMessage = line.substring(5).trim();
                        String senderName = this.clientName;
                        
                        appendToGUI("[CHAT dari " + senderName + "]: " + chatMessage + "\n");
                        
                        // panggil helper yg udh dimodif
                        server.fireNewClientChat(senderName, chatMessage);
                        continue;
                    }


                    // 4. tawaran (numeric)
                    if (line.matches("^\\d+$")) {
                        String auctionId = server.clientAuctionMap.get(out);
                        if (auctionId == null) {
                            out.println("ERROR:Harap pilih lelang terlebih dahulu (SELECT:ID_LELANG).");
                            continue;
                        }
                        int value;
                        try { value = Integer.parseInt(line); }
                        catch (NumberFormatException ex) { out.println("ERROR:Harap kirim angka saja untuk menawar."); continue; }

                        String bidderName = this.clientName;

                        BarangLelang barang = server.getAuction(auctionId);
                        if (barang != null && server.isAuctionActive(auctionId)) {
                            if (barang.ajukanTawaran(bidderName, value)) {
                                out.println("ACCEPTED:Tawaran Anda (Rp "+ value +") diterima.");
                            } else {
                                out.println("REJECTED:Tawaran Anda terlalu rendah. Harga saat ini Rp " + barang.hargaTawaranTertinggi);
                            }
                        } else {
                            out.println("CLOSED:Lelang yang Anda ikuti sudah berakhir.");
                            server.clientAuctionMap.remove(out);
                        }
                        continue;
                    }

                    // 5. list command
                    if (line.equalsIgnoreCase("LIST")) {
                        out.println(server.getAvailableAuctions());
                        continue;
                    }

                    // kl gak dikenali
                    out.println("UNKNOWN_COMMAND");
                }

            } catch (IOException e) {
                appendToGUI("Koneksi terputus dengan " + (this.clientName != null ? this.clientName : "klien tak dikenal") + "\n");
            } finally {
                server.removeClientHandler(out); 
                // panggil helper yg udh diubah td
                server.fireClientListUpdate();
                try { client.close(); } catch (IOException ignored) {}
            }
        }

        private void appendToGUI(String text) {
            if (guiLog != null) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    guiLog.append(text);
                    guiLog.setCaretPosition(guiLog.getDocument().getLength());
                });
            } else {
                System.out.print(text);
            }
        }
    }
}
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiServerPelelangan {
    private ServerSocket serverSocket;
    private static final int PORT = 1234;
    
    // Instance Fields (Non-Static)
    private final Map<String, BarangLelang> currentAuctions = new HashMap<>(); 
    private final Map<String, Boolean> auctionActiveStatus = new HashMap<>(); 
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final List<PrintWriter> allClientWriters = new CopyOnWriteArrayList<>();
    private final Map<PrintWriter, String> clientAuctionMap = new HashMap<>();
    
    // Penghitung ID barang otomatis
    private final AtomicInteger auctionIdCounter = new AtomicInteger(1); 

    public static void main(String[] args) throws IOException {
        MultiServerPelelangan server = new MultiServerPelelangan(); // Instance server
        server.setupAuctions(); // Panggil setup untuk input pengguna
        server.startServer();
    }
    
    // PERUBAHAN UTAMA: Menerima input dari konsol
    private void setupAuctions() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("--- PENDAFTARAN SESI LELANG ---");
        
        while (true) {
            System.out.print("\nApakah Anda ingin mendaftarkan barang lelang baru? (ya/tidak): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if (!choice.equals("ya")) {
                break;
            }
            
            // 1. Ambil Input
            System.out.print("Masukkan Nama Barang: ");
            String namaBarang = scanner.nextLine();
            
            System.out.print("Masukkan Harga Awal (Rp): ");
            int hargaAwal;
            try {
                hargaAwal = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Input harga tidak valid. Batalkan pendaftaran barang ini.");
                continue;
            }

            System.out.print("Masukkan Durasi Lelang (dalam detik): ");
            int durasiLelang;
            try {
                durasiLelang = Integer.parseInt(scanner.nextLine());
                if (durasiLelang <= 0) {
                     System.out.println("Durasi harus lebih dari 0. Batalkan pendaftaran barang ini.");
                     continue;
                }
            } catch (NumberFormatException e) {
                System.out.println("Input durasi tidak valid. Batalkan pendaftaran barang ini.");
                continue;
            }

            System.out.print("Masukkan URL Gambar Barang: ");
            String urlGambar = scanner.nextLine();
            
            // 2. Buat Lelang
            String auctionId = "ID" + auctionIdCounter.getAndIncrement();
            
            BarangLelang newBarang = new BarangLelang(auctionId, namaBarang, hargaAwal, urlGambar, this);
            currentAuctions.put(auctionId, newBarang);
            auctionActiveStatus.put(auctionId, true);
            
            // 3. Jadwalkan Penutupan Lelang
            scheduler.schedule(() -> stopAuction(auctionId), durasiLelang, TimeUnit.SECONDS);
            
            System.out.println("Barang '" + namaBarang + "' berhasil didaftarkan dengan ID: " + auctionId + " selama " + durasiLelang + " detik.");
        }
        
        System.out.println("\n--- PENDAFTARAN SELESAI ---");
        System.out.println("Total " + currentAuctions.size() + " lelang dijadwalkan.");
        if (currentAuctions.isEmpty()) {
            System.out.println("Tidak ada lelang aktif. Server akan berjalan hanya untuk koneksi.");
        }
    }
    
    private void startServer() throws IOException {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("\nServer Pelelangan Aktif di Port " + PORT + ". Menunggu koneksi...");

            while (true) {
                try {
                    Socket client = serverSocket.accept();
                    System.out.println("\nPenawar baru terhubung: " + client.getInetAddress().getHostName());
                    
                    PenawarClientHandler handler = new PenawarClientHandler(client, this);
                    handler.start();

                } catch (SocketException e) {
                    if (serverSocket != null && serverSocket.isClosed()) {
                        System.out.println("Server socket ditutup, server berhenti.");
                        break;
                    }
                    e.printStackTrace();
                }
            }
        } catch (IOException ioEx) {
            System.out.println("\nTidak bisa men-set port!");
            System.exit(1);
        } finally {
            shutdownServer();
        }
    }

    private void shutdownServer() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Server pelelangan telah berhenti.");
    }

    private void stopAuction(String auctionId) {
        BarangLelang barang = currentAuctions.get(auctionId);
        if (barang == null) return;

        auctionActiveStatus.put(auctionId, false);
        System.out.println("\n--- WAKTU LELANG (" + auctionId + ") HABIS ---");
        System.out.println(barang.getPemenangInfo());
        
        broadcastMessageToAuction(auctionId, "CLOSED:" + barang.getPemenangInfo());
        
        currentAuctions.remove(auctionId);
        System.out.println("Lelang ID " + auctionId + " telah dihapus dari daftar aktif.");

        broadcastMessage(getAvailableAuctions());
    }

    // --- Metode Utilitas Server (Diakses oleh Handler) ---

    public void broadcastMessage(String message) {
        for (PrintWriter writer : allClientWriters) {
            writer.println(message);
        }
    }

    public void broadcastMessageToAuction(String auctionId, String message) {
        for (Map.Entry<PrintWriter, String> entry : clientAuctionMap.entrySet()) {
            if (entry.getValue().equals(auctionId)) {
                entry.getKey().println(message);
            }
        }
    }

    public void addClientWriter(PrintWriter writer) { allClientWriters.add(writer); }
    public void removeClientWriter(PrintWriter writer) { allClientWriters.remove(writer); }
    public void registerClientToAuction(PrintWriter writer, String auctionId) { clientAuctionMap.put(writer, auctionId); }
    public void unregisterClientFromAuction(PrintWriter writer) { clientAuctionMap.remove(writer); }

    public BarangLelang getAuction(String auctionId) {
        return currentAuctions.get(auctionId);
    }
    
    public boolean isAuctionActive(String auctionId) {
        return auctionActiveStatus.getOrDefault(auctionId, false);
    }
    
    public String getAvailableAuctions() {
        // Format: AUCTIONS:ID1,NAMA1,HARGA1,URL1|ID2,NAMA2,HARGA2,URL2|...
        StringBuilder sb = new StringBuilder("AUCTIONS:");
        boolean first = true;
        for (BarangLelang b : currentAuctions.values()) {
            if (!first) {
                sb.append("|");
            }
            sb.append(b.idLelang).append(",")
              .append(b.namaBarang).append(",")
              .append(b.hargaTawaranTertinggi).append(",")
              .append(b.urlGambar);
            first = false;
        }
        return sb.toString();
    }
}













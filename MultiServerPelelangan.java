import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


class BarangLelang {
    String namaBarang = "";
    int hargaTawaranTertinggi = 0;
    String penawarTertinggi = "";

    BarangLelang(String nama, int hargaAwal) {
        this.namaBarang = nama;
        this.hargaTawaranTertinggi = hargaAwal;
        this.penawarTertinggi = "Belum ada (Harga Awal)";
        System.out.println("Barang '" + nama + "' dibuka dengan harga awal Rp " + hargaAwal);
    }

    public synchronized boolean ajukanTawaran(String namaPenawar, int jumlahTawaran) {
        if (jumlahTawaran > this.hargaTawaranTertinggi) {
            this.hargaTawaranTertinggi = jumlahTawaran;
            this.penawarTertinggi = namaPenawar;
            
            System.out.println("\n---------------------------------");
            System.out.println("[UPDATE HARGA DI SERVER]");
            System.out.println("Tawaran diterima dari: " + namaPenawar);
            System.out.println("Harga tertinggi baru: Rp " + this.hargaTawaranTertinggi);
            System.out.println("---------------------------------");

            String updateMessage = "UPDATE:" + this.hargaTawaranTertinggi + ":" + this.penawarTertinggi;
            MultiServerPelelangan.broadcastMessage(updateMessage);
            return true;
        } else {
            return false;
        }
    }
    
    public synchronized String getPemenangInfo() {
        return "Pemenang: " + penawarTertinggi + " dengan tawaran Rp " + hargaTawaranTertinggi;
    }

    public synchronized String getStatusInfo() {
        return "STATUS:" + this.namaBarang + ":" + this.hargaTawaranTertinggi + ":" + this.penawarTertinggi;
    }
}



public class MultiServerPelelangan {
    private static ServerSocket serverSocket;
    private static final int PORT = 1234;
    public static BarangLelang barang;
    private static volatile boolean isAuctionActive = true;
    private static ScheduledExecutorService scheduler;

    private static List<PrintWriter> allClientWriters = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        Scanner inputAdmin = new Scanner(System.in);

        System.out.print("Nama Barang yang akan dilelang: ");
        String namaBarang = inputAdmin.nextLine();
        System.out.print("Harga Awal Barang (Rp): ");
        int hargaAwal = inputAdmin.nextInt();
        System.out.print("Durasi lelang (dalam detik): ");
        int durasiLelang = inputAdmin.nextInt();

        barang = new BarangLelang(namaBarang, hargaAwal);

        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> stopAuction(), durasiLelang, TimeUnit.SECONDS);

        
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("\nServer Pelelangan Aktif di Port " + PORT + ". Lelang akan berlangsung " + durasiLelang + " detik.");

            while (isAuctionActive) {
                try {
                    Socket client = serverSocket.accept();
                    System.out.println("\nPenawar baru terhubung: " + client.getInetAddress().getHostName());
                    
                    PenawarClientHandler handler = new PenawarClientHandler(client, barang);
                    handler.start();

                } catch (SocketException e) {
                    if (!isAuctionActive) {
                        System.out.println("Server socket ditutup, lelang berakhir.");
                    } else {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException ioEx) {
            if (isAuctionActive) { 
                System.out.println("\nTidak bisa men-set port!");
                System.exit(1);
            }
        } finally {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
            }
            System.out.println("Server pelelangan telah berhenti.");
        }
    }

    private static void stopAuction() {
        isAuctionActive = false;
        System.out.println("\n--- WAKTU LELANG HABIS ---");
        System.out.println(barang.getPemenangInfo());
        System.out.println("---------------------------------");
        
        broadcastMessage("CLOSED:" + barang.getPemenangInfo());

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        scheduler.shutdown();
    }

    public static void broadcastMessage(String message) {
        for (PrintWriter writer : allClientWriters) {
            writer.println(message);
        }
    }

    public static void addClientWriter(PrintWriter writer) {
        allClientWriters.add(writer);
    }

    public static void removeClientWriter(PrintWriter writer) {
        allClientWriters.remove(writer);
    }

    public static boolean isAuctionActive() {
        return isAuctionActive;
    }
}


class PenawarClientHandler extends Thread {
    private Socket client;
    private Scanner input;
    private PrintWriter output;
    private BarangLelang barang;
    private String namaPenawar;

    public PenawarClientHandler(Socket socket, BarangLelang barang) {
        this.client = socket;
        this.barang = barang;
        try {
            this.input = new Scanner(client.getInputStream());
            this.output = new PrintWriter(client.getOutputStream(), true);
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
    }

    public void run() {
        try {
            if (input.hasNextLine()) {
                String loginMessage = input.nextLine(); 
                if (loginMessage.startsWith("LOGIN:")) {
                    this.namaPenawar = loginMessage.split(":")[1];
                    System.out.println(namaPenawar + " telah bergabung dalam lelang.");
                } else {
                    System.out.println("Klien tidak mengirim nama. Koneksi ditutup.");
                    return; 
                }
            }
            
            MultiServerPelelangan.addClientWriter(this.output);

            if (MultiServerPelelangan.isAuctionActive()) {
                 this.output.println(barang.getStatusInfo());
            } else {
                 this.output.println("CLOSED:Lelang sudah berakhir.");
                 return; 
            }

            while (MultiServerPelelangan.isAuctionActive() && input.hasNextLine()) {
                String message = input.nextLine(); 
                
                try {
                    int tawaran = Integer.parseInt(message);
                    
                    if (!barang.ajukanTawaran(this.namaPenawar, tawaran)) {
                        System.out.println("Tawaran DITOLAK: " + this.namaPenawar + " menawar Rp " + tawaran + " (Harga saat ini Rp " + barang.hargaTawaranTertinggi + ")");
                        this.output.println("REJECTED:Tawaran Anda terlalu rendah. Harga saat ini Rp " + barang.hargaTawaranTertinggi);
                    } else {
                        this.output.println("ACCEPTED:Tawaran Anda (Rp "+ tawaran +") diterima.");
                    }
                    
                } catch (NumberFormatException e) {
                    this.output.println("ERROR: Harap kirim angka saja untuk menawar.");
                }
            }

        } catch (Exception e) {
            System.out.println("Koneksi dengan " + (namaPenawar != null ? namaPenawar : "klien") + " terputus.");
        } finally {
            if (this.output != null) {
                MultiServerPelelangan.removeClientWriter(this.output);
            }
            try {
                if (client != null) {
                    client.close();
                }
            } catch (IOException ioEx) {
            }
            if(namaPenawar != null) {
                System.out.println(namaPenawar + " telah meninggalkan lelang.");
            }
        }
    }
}
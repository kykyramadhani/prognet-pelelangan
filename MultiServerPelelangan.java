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
import javax.swing.*;
import java.awt.*;


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

    // ======================================================
    // ===============       GUI SERVER       ===============
    // ======================================================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame frame = new JFrame("Server Pelelangan");
            frame.setSize(450, 400);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            // Input Panel
            JPanel inputPanel = new JPanel();
            inputPanel.setLayout(new GridLayout(4, 2));

            JTextField namaBarangField = new JTextField();
            JTextField hargaAwalField = new JTextField();
            JTextField durasiField = new JTextField();

            inputPanel.add(new JLabel("Nama Barang:"));
            inputPanel.add(namaBarangField);

            inputPanel.add(new JLabel("Harga Awal (Rp):"));
            inputPanel.add(hargaAwalField);

            inputPanel.add(new JLabel("Durasi (detik):"));
            inputPanel.add(durasiField);

            JButton startButton = new JButton("Mulai Server");

            inputPanel.add(startButton);

            // Log Area
            JTextArea logArea = new JTextArea();
            logArea.setEditable(false);

            frame.add(inputPanel, BorderLayout.NORTH);
            frame.add(new JScrollPane(logArea), BorderLayout.CENTER);

            frame.setVisible(true);


            // Tombol Start ditekan
            startButton.addActionListener(e -> {

                try {
                    String namaBarang = namaBarangField.getText();
                    int hargaAwal = Integer.parseInt(hargaAwalField.getText().trim());
                    int durasi = Integer.parseInt(durasiField.getText().trim());

                    startButton.setEnabled(false);

                    new Thread(() -> {
                        try {
                            startFromGUI(namaBarang, hargaAwal, durasi, logArea);
                        } catch (Exception ex) {
                            SwingUtilities.invokeLater(() ->
                                logArea.append("Error: " + ex.getMessage() + "\n")
                            );
                        }
                    }).start();

                } catch (Exception ex) {
                    logArea.append("Input tidak valid!\n");
                }

            });

        });

    }


    // ======================================================
    // ===============  SERVER LOGIC FOR GUI ===============
    // ======================================================
    public static void startFromGUI(String namaBarang, int hargaAwal, int durasi, JTextArea logArea) throws IOException {

        barang = new BarangLelang(namaBarang, hargaAwal);

        isAuctionActive = true;

        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> stopAuctionGUI(logArea), durasi, TimeUnit.SECONDS);

        serverSocket = new ServerSocket(PORT);

        logArea.append("Server berjalan di port " + PORT + "\n");
        logArea.append("Lelang dimulai...\n\n");

        while (isAuctionActive) {
            try {
                Socket client = serverSocket.accept();

                SwingUtilities.invokeLater(() ->
                    logArea.append("Penawar terhubung: " + client.getInetAddress().getHostAddress() + "\n")
                );

                PenawarClientHandler handler = new PenawarClientHandler(client, barang);
                handler.start();

            } catch (SocketException e) {
                break;
            }
        }

        SwingUtilities.invokeLater(() -> logArea.append("Server berhenti.\n"));
    }



    private static void stopAuctionGUI(JTextArea logArea) {
        isAuctionActive = false;

        SwingUtilities.invokeLater(() -> {
            logArea.append("\n--- WAKTU LELANG HABIS ---\n");
            logArea.append(barang.getPemenangInfo() + "\n");
        });

        broadcastMessage("CLOSED:" + barang.getPemenangInfo());

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        scheduler.shutdown();
    }

    // =================== ORIGINAL METHODS ===================

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




// ======================================================
// ===============     CLIENT HANDLER     ===============
// ======================================================

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

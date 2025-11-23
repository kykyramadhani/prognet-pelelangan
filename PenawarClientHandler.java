import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

class PenawarClientHandler extends Thread {
    private Socket client;
    private Scanner input;
    private PrintWriter output;
    private String namaPenawar;
    private MultiServerPelelangan serverInstance; 
    private String selectedAuctionId; 

    public PenawarClientHandler(Socket socket, MultiServerPelelangan serverInstance) {
        this.client = socket;
        this.serverInstance = serverInstance; // Menerima instance server
        try {
            this.input = new Scanner(client.getInputStream());
            this.output = new PrintWriter(client.getOutputStream(), true);
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
    }

    public void run() {
        try {
            // 1. Proses LOGIN
            if (input.hasNextLine()) {
                String loginMessage = input.nextLine(); 
                if (loginMessage.startsWith("LOGIN:")) {
                    String[] parts = loginMessage.split(":");
                    this.namaPenawar = parts[1]; 
                    System.out.println(namaPenawar + " telah bergabung.");
                } else {
                    System.out.println("Klien tidak mengirim LOGIN. Koneksi ditutup.");
                    return; 
                }
            }
            
            serverInstance.addClientWriter(this.output);

            // 2. Kirim daftar lelang yang tersedia
            this.output.println(serverInstance.getAvailableAuctions());

            // 3. Loop untuk menerima perintah
            while (input.hasNextLine()) {
                String message = input.nextLine(); 
                
                // --- Perintah SELECT ---
                if (message.startsWith("SELECT:")) {
                    String auctionId = message.split(":")[1];
                    BarangLelang selectedAuction = serverInstance.getAuction(auctionId);
                    
                    if (selectedAuction != null && serverInstance.isAuctionActive(auctionId)) {
                        this.selectedAuctionId = auctionId;
                        serverInstance.registerClientToAuction(this.output, auctionId);
                        
                        this.output.println("INFO:Anda telah memilih lelang " + auctionId);
                        this.output.println(selectedAuction.getStatusInfo());
                    } else if (selectedAuction != null) {
                         this.output.println("CLOSED:Lelang " + auctionId + " sudah berakhir. " + selectedAuction.getPemenangInfo());
                    } else {
                        this.output.println("ERROR:Lelang ID '" + auctionId + "' tidak ditemukan.");
                    }
                    continue; 
                }

                // --- Tawaran (Hanya jika lelang telah dipilih) ---
                if (selectedAuctionId == null) {
                    this.output.println("ERROR:Harap pilih lelang terlebih dahulu (SELECT:ID_LELANG).");
                    continue;
                }
                
                BarangLelang currentBarang = serverInstance.getAuction(selectedAuctionId);
                
                if (currentBarang == null || !serverInstance.isAuctionActive(selectedAuctionId)) {
                    this.output.println("CLOSED:Lelang yang Anda ikuti sudah berakhir.");
                    this.selectedAuctionId = null;
                    this.output.println(serverInstance.getAvailableAuctions());
                    continue;
                }
                
                try {
                    int tawaran = Integer.parseInt(message);
                    
                    if (!currentBarang.ajukanTawaran(this.namaPenawar, tawaran)) {
                        System.out.println("Tawaran DITOLAK di " + selectedAuctionId + ": " + this.namaPenawar + " menawar Rp " + tawaran);
                        this.output.println("REJECTED:Tawaran Anda terlalu rendah. Harga saat ini Rp " + currentBarang.hargaTawaranTertinggi);
                    } else {
                        // ajukanTawaran sudah melakukan broadcast UPDATE
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
                serverInstance.removeClientWriter(this.output);
                serverInstance.unregisterClientFromAuction(this.output);
            }
            try {
                if (client != null) {
                    client.close();
                }
            } catch (IOException ioEx) {}
            if(namaPenawar != null) {
                System.out.println(namaPenawar + " telah meninggalkan lelang.");
            }
        }
    }
}

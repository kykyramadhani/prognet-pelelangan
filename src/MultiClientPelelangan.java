import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

class ServerListener implements Runnable {
    private Scanner networkInput;
    private volatile boolean lelangAktif = true;

    public ServerListener(Scanner networkInput) {
        this.networkInput = networkInput;
    }

    public boolean isLelangAktif() {
        return this.lelangAktif;
    }

    @Override
    public void run() {
        try {
            while (networkInput.hasNextLine()) {
                String serverMessage = networkInput.nextLine();
                
                String[] parts = serverMessage.split(":", 2);
                String command = parts[0];
                String data = (parts.length > 1) ? parts[1] : "";

                switch (command) {
                    case "STATUS": 
                        String[] statusData = data.split(":");
                        System.out.println("\n--- SELAMAT DATANG DI LELANG ---");
                        System.out.println("Barang: " + statusData[0]);
                        System.out.println("Harga saat ini: Rp " + statusData[1]);
                        System.out.println("Dipegang oleh: " + statusData[2]);
                        System.out.println("---------------------------------");
                        System.out.print("Masukkan tawaran Anda: ");
                        break;
                    
                    case "UPDATE": 
                        String[] updateData = data.split(":");
                        System.out.println("\n--- UPDATE HARGA ---");
                        System.out.println("Harga baru: Rp " + updateData[0]);
                        System.out.println("Oleh: " + updateData[1]);
                        System.out.println("----------------------");
                        System.out.print("Masukkan tawaran Anda: ");
                        break;
                        
                    case "REJECTED": 
                        System.out.println("\n[TAWARAN DITOLAK] " + data);
                        System.out.print("Masukkan tawaran Anda: ");
                        break;

                    case "ACCEPTED": 
                        System.out.println("\n[TAWARAN DITERIMA] " + data);
                        System.out.print("Masukkan tawaran Anda: ");
                        break;

                    case "CLOSED":
                        System.out.println("\n--- LELANG TELAH DITUTUP ---");
                        System.out.println(data);
                        lelangAktif = false; 
                        break;
                        
                    case "ERROR":
                        System.out.println("\n[ERROR DARI SERVER] " + data);
                        System.out.print("Masukkan tawaran Anda: ");
                        break;
                        
                    default:
                        System.out.println("\nSERVER> " + serverMessage);
                }
            }
        } catch (Exception e) {
            System.out.println("\nKoneksi ke server terputus.");
        } finally {
            lelangAktif = false; 
        }
    }
}


public class MultiClientPelelangan {
    private static final int PORT = 1234;

    public static void main(String[] args) {
        Socket socket = null;
        Scanner userInput = new Scanner(System.in);

        try {
            socket = new Socket("10.10.162.233", PORT);
            
            Scanner networkInput = new Scanner(socket.getInputStream());
            PrintWriter networkOutput = new PrintWriter(socket.getOutputStream(), true);

            System.out.print("Masukkan nama Anda untuk bergabung di lelang: ");
            String nama = userInput.nextLine();
            
            networkOutput.println("LOGIN:" + nama);

            ServerListener listener = new ServerListener(networkInput);
            Thread listenerThread = new Thread(listener);
            listenerThread.start();

            System.out.println("Anda telah bergabung. Silakan tunggu status lelang...");

            while (listener.isLelangAktif()) {
                String inputTawaran = userInput.nextLine();
                
                if (!listener.isLelangAktif()) {
                    break;
                }
                
                networkOutput.println(inputTawaran);
            }

            System.out.println("Anda telah keluar dari lelang.");

        } catch (IOException ioEx) {
            System.out.println("Tidak dapat terhubung ke server.");
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
                userInput.close();
            } catch (IOException ioEx) {
                System.out.println("Gagal menutup koneksi.");
            }
        }
    }
}
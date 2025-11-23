import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class BarangLelang {
    String idLelang; 
    String namaBarang = "";
    String urlGambar = ""; 
    int hargaTawaranTertinggi = 0;
    String penawarTertinggi = "";
    private final MultiServerPelelangan serverInstance; // Instance Server

    BarangLelang(String idLelang, String nama, int hargaAwal, String urlGambar, MultiServerPelelangan serverInstance) {
        this.idLelang = idLelang;
        this.namaBarang = nama;
        this.hargaTawaranTertinggi = hargaAwal;
        this.penawarTertinggi = "Belum ada (Harga Awal)";
        this.urlGambar = urlGambar;
        this.serverInstance = serverInstance;
        System.out.println("Barang '" + nama + "' (ID: " + idLelang + ") dibuka dengan harga awal Rp " + hargaAwal);
    }

    public synchronized boolean ajukanTawaran(String namaPenawar, int jumlahTawaran) {
        if (jumlahTawaran > this.hargaTawaranTertinggi) {
            this.hargaTawaranTertinggi = jumlahTawaran;
            this.penawarTertinggi = namaPenawar;
            
            System.out.println("\n---------------------------------");
            System.out.println("[UPDATE HARGA DI SERVER] - " + idLelang);
            System.out.println("Tawaran diterima dari: " + namaPenawar);
            System.out.println("Harga tertinggi baru: Rp " + this.hargaTawaranTertinggi);
            System.out.println("---------------------------------");

            // Menggunakan instance server untuk broadcast ke lelang spesifik
            serverInstance.broadcastMessageToAuction(this.idLelang, getUpdateMessage());
            return true;
        } else {
            return false;
        }
    }
    
    public synchronized String getPemenangInfo() {
        return "Barang: " + namaBarang + ". Pemenang: " + penawarTertinggi + " dengan tawaran Rp " + hargaTawaranTertinggi;
    }

    // Format: STATUS:NAMA:HARGA:PENAWAR:URL_GAMBAR
    public synchronized String getStatusInfo() {
        return "STATUS:" + this.namaBarang + ":" + this.hargaTawaranTertinggi + ":" + this.penawarTertinggi + ":" + this.urlGambar;
    }
    
    // Format: UPDATE:HARGA:PENAWAR
    public synchronized String getUpdateMessage() {
        return "UPDATE:" + this.hargaTawaranTertinggi + ":" + this.penawarTertinggi;
    }
}

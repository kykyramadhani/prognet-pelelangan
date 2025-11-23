class BarangLelang {
    String idLelang; 
    String namaBarang = ""; 
    int hargaTawaranTertinggi = 0;
    String penawarTertinggi = "";

    private MultiServerPelelangan serverInstance; // Instance Server

    public BarangLelang() {} // default constructor

    public BarangLelang(String idLelang, String nama, int hargaAwal, MultiServerPelelangan serverInstance) {
        this.idLelang = idLelang;
        this.namaBarang = nama;
        this.hargaTawaranTertinggi = hargaAwal;
        this.penawarTertinggi = "Belum ada (Harga Awal)";
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

            // broadcast update
            serverInstance.broadcastMessageToAuction(this.idLelang, getUpdateMessage());
            return true;
        }
        return false;
    }

    public synchronized String getPemenangInfo() {
        return "Barang: " + namaBarang + ". Pemenang: " + penawarTertinggi + " dengan tawaran Rp " + hargaTawaranTertinggi;
    }

    // sekarang sederhana tanpa urlGambar
    public synchronized String getStatusInfo() {
        return "STATUS:" + this.namaBarang + ":" + this.hargaTawaranTertinggi + ":" + this.penawarTertinggi;
    }

    public synchronized String getUpdateMessage() {
        return "UPDATE:" + this.hargaTawaranTertinggi + ":" + this.penawarTertinggi;
    }
}

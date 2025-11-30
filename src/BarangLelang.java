// File: BarangLelang.java

class BarangLelang {
    String idLelang; 
    String namaBarang = ""; 
    int hargaTawaranTertinggi = 0;
    String penawarTertinggi = "";

    private MultiServerPelelangan serverInstance; 

    public BarangLelang() {} 

    public BarangLelang(String idLelang, String nama, int hargaAwal, MultiServerPelelangan serverInstance) {
        this.idLelang = idLelang;
        this.namaBarang = nama;
        this.hargaTawaranTertinggi = hargaAwal;
        this.penawarTertinggi = "Belum ada (Harga Awal)";
        this.serverInstance = serverInstance;
    }

    public synchronized boolean ajukanTawaran(String namaPenawar, int jumlahTawaran) {
        if (jumlahTawaran > this.hargaTawaranTertinggi) {
            this.hargaTawaranTertinggi = jumlahTawaran;
            this.penawarTertinggi = namaPenawar;
            serverInstance.broadcastMessageToAuction(this.idLelang, getUpdateMessage());
            return true;
        }
        return false;
    }

    public synchronized String getPemenangInfo() {
        return "Barang: " + namaBarang + ". Pemenang: " + penawarTertinggi + " dengan tawaran Rp " + hargaTawaranTertinggi;
    }

    public synchronized String getStatusInfo() {
        return "STATUS:" + this.namaBarang + ":" + this.hargaTawaranTertinggi + ":" + this.penawarTertinggi;
    }

    public synchronized String getUpdateMessage() {
        return "UPDATE:" + this.hargaTawaranTertinggi + ":" + this.penawarTertinggi;
    }
}
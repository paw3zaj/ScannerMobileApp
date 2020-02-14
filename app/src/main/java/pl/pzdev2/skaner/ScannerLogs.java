package pl.pzdev2.skaner;

public class ScannerLogs {

    private String barcode;
    private String createdDate;

    public ScannerLogs() {
    }

    public ScannerLogs(String barcode, String createdDate) {
        this.barcode = barcode;
        this.createdDate = createdDate;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }
}

public class StoreBufferFields {
    String tag;
    String busy;
    int address;
    double v;
    String q;
    int cycles;
    int issuedAt;

    public StoreBufferFields(String tag) {
        this.busy = "0";
        this.q = "0";
        this.tag = tag;
    }
}

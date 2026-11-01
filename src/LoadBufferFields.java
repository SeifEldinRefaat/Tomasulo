public class LoadBufferFields {
    String tag;
    String busy;
    String qz;
    int address;
    int result;
    int cycles;
    int issuedAt;
    public LoadBufferFields(String tag) {
        this.tag = tag;
        this.busy = "0";
        this.qz = "0";
    }
}

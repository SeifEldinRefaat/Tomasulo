public class StationFields {
    String tag;
    String op;
    String busy;
    double vj, vk;
    String qj, qk, qz;
    int result;
    int cycles;
    int issuedAt;

    public StationFields(String tag) {
        this.tag = tag;
        busy = "0";
        qz = "0";
        qj = "0";
        qk = "0";
    }

}

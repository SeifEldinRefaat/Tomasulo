public class StoreBuffer {
    int size;
    StoreBufferFields[] storeBuffer;
    int cycles;
    public StoreBuffer(int size) {
        this.size = size;
        storeBuffer = new StoreBufferFields[size];
    }
    public void print() {
        System.out.println("Store Buffer:");
//        System.out.printf("%-10s%-10s%-10s%-10s%-10s%-10s\n", "Tag", "Busy", "Address", "Value", "Qi", "Cycles");
//
//        for (StoreBufferFields entry : storeBuffer) {
//            if (entry != null) {
//                System.out.printf("%-10s%-10s%-10d%-10.2f%-10s%-10d\n",
//                        entry.tag, entry.busy, entry.address, entry.v, entry.q, entry.cycles);
//            }
//        }
        System.out.printf("%-10s%-10s%-10s%-10s%-10s\n", "Tag", "Busy", "Address", "Value", "Qi");

        for (StoreBufferFields entry : storeBuffer) {
            if (entry != null) {
                System.out.printf("%-10s%-10s%-10d%-10.2f%-10s\n",
                        entry.tag, entry.busy, entry.address, entry.v, entry.q);
            }
        }
        System.out.println();
    }
}

public class LoadBuffer {

    int size;
    LoadBufferFields[] loadBuffer;
    public LoadBuffer(int size) {
        this.size = size;
        loadBuffer = new LoadBufferFields[size];

    }
    public void print() {
        System.out.println("Load Buffer:");
//        System.out.printf("%-10s%-10s%-10s%-10s%-10s\n", "Tag", "Busy", "Address", "Result", "Cycles");
//
//        for (LoadBufferFields entry : loadBuffer) {
//            System.out.printf("%-10s%-10s%-10d%-10d%-10d\n",
//                    entry.tag, entry.busy, entry.address, entry.result, entry.cycles);
//        }
        System.out.printf("%-10s%-10s%-10s%-10s\n", "Tag", "Busy", "Address", "Result");

        for (LoadBufferFields entry : loadBuffer) {
            System.out.printf("%-10s%-10s%-10d%-10d\n",
                    entry.tag, entry.busy, entry.address, entry.result);
        }
        System.out.println();
    }
}

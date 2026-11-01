import java.util.Arrays;

public class ReservationStation {
    int size;
    StationFields[] sf;
    String Type;
    public ReservationStation(int size  , String Type){
        this.size = size;
        sf = new StationFields[size];
        this.Type = Type;
    }

    public void print() {
        System.out.println(Type + " Reservation Station:");
//        System.out.printf("%-10s%-10s%-10s%-10s%-10s%-10s%-10s%-10s\n",
//                "Tag", "Busy", "Vj", "Vk", "Qj", "Qk", "Result", "Cycles");
//
//        for (StationFields entry : sf) {
//            System.out.printf("%-10s%-10s%-10.2f%-10.2f%-10s%-10s%-10s%-10d\n",
//                    entry.tag, entry.busy, entry.vj, entry.vk, entry.qj, entry.qk, entry.result, entry.cycles);
//        }
        System.out.printf("%-10s%-10s%-10s%-10s%-10s%-10s%-10s\n",
                "Tag", "Busy", "Vj", "Vk", "Qj", "Qk", "Result");
        for (StationFields entry : sf) {
            System.out.printf("%-10s%-10s%-10.2f%-10.2f%-10s%-10s%-10s\n",
                    entry.tag, entry.busy, entry.vj, entry.vk, entry.qj, entry.qk, entry.result);
        }
        System.out.println();
    }

}

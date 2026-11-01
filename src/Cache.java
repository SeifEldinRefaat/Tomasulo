public class Cache {
    int size;
    double[] cache;

    public Cache(int size){
        this.size = size;
        cache = new double[size];
    }
    public void print() {
        System.out.println("Cache:");

        for(int i = 0; i < size; ++i){
            System.out.print(cache[i] + " ");
        }
        System.out.println();
        System.out.println();
    }
}

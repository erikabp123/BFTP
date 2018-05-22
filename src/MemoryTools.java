public class MemoryTools {


    public static double usedMemory() {
        Runtime runtime = Runtime.getRuntime();
        double total = runtime.totalMemory();
        double free = runtime.freeMemory();
        double used = (total - free);
        return convertToMB(used);
    }

    public static double convertToMB(double value){
        return value / (1024 * 1024);
    }

}

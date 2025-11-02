package compression;

public interface BitPacking {

    int[] compress(int[] array);
    int[] decompress(int[] compressedArray, int[] outputArray);
    int get(int[] compressedArray, int i);
}

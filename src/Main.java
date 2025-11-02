import compression.BitPacking;
import factory.CompressionFactory;
import java.util.Random;

public class Main {
    private static final int WARMUP = 100;
    private static final int ITERATIONS = 5000;

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("         BENCHMARK BIT PACKING - ANALYSE SIMPLE            ");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        // Test 1 : Valeurs uniformes petites
        System.out.println("TEST 1 : Valeurs uniformes (0-255)");
        test(generateArray(100, 255), "Uniform");

        // Test 2 : Valeurs uniformes moyennes
        System.out.println("\n" + "─".repeat(60));
        System.out.println("TEST 2 : Valeurs moyennes (0-4095)");
        test(generateArray(100, 4095), "Medium");

        // Test 3 : Cas parfait pour overflow (peu d'outliers)
        System.out.println("\n" + "─".repeat(60));
        System.out.println("TEST 3 : Peu d'outliers (2 sur 100)");
        test(generateWithOutliers(100, 15, 2, 100000), "FewOutliers");

        // Test 4 : Cas limite pour overflow (beaucoup d'outliers)
        System.out.println("\n" + "─".repeat(60));
        System.out.println("TEST 4 : Beaucoup d'outliers (20 sur 100)");
        test(generateWithOutliers(100, 15, 20, 100000), "ManyOutliers");

        // Test 5 : Grand tableau
        System.out.println("\n" + "─".repeat(60));
        System.out.println("TEST 5 : Grand tableau (10000 éléments)");
        test(generateArray(10000, 1023), "Large");

        // Test 6 : Test personnalisé
        System.out.println("\n" + "─".repeat(60));
        System.out.println("TEST 6 : Test personnalisé (500 éléments, max 65535)");
        // Variable à changer ici pour un test personnalisé
        test(new int[]{4, 8, 15, 16, 23, 42}, "Custom");


    }

    private static void test(int[] data, String name) {
        System.out.printf("Données : %d éléments, Max=%d\n\n", data.length, findMax(data));

        Result[] results = new Result[3];
        results[0] = benchmark(CompressionFactory.CompressionType.CONSECUTIVE, data);
        results[1] = benchmark(CompressionFactory.CompressionType.NON_CONSECUTIVE, data);
        results[2] = benchmark(CompressionFactory.CompressionType.OVERFLOW, data);

        // Affichage des résultats
        System.out.println("┌──────────────────┬──────────┬──────────┬──────────┬──────────┐");
        System.out.println("│ Méthode          │ Taille   │ Gain %   │ Temps µs │ Get µs   │");
        System.out.println("├──────────────────┼──────────┼──────────┼──────────┼──────────┤");
        printRow("Consecutive", results[0]);
        printRow("Non-Consecutive", results[1]);
        printRow("Overflow", results[2]);
        System.out.println("└──────────────────┴──────────┴──────────┴──────────┴──────────┘");

        // Analyse
        analyzeResults(data, results);
    }

    private static Result benchmark(CompressionFactory.CompressionType type, int[] data) {
        BitPacking packer = CompressionFactory.createCompressor(type);

        // Warm-up
        for (int i = 0; i < WARMUP; i++) {
            int[] c = packer.compress(data);
            packer.decompress(c, new int[data.length]);
            if (data.length > 0) packer.get(c, 0);
        }

        // Mesures
        long compTime = 0, decompTime = 0, getTime = 0;
        int[] compressed = null;

        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            compressed = packer.compress(data);
            compTime += System.nanoTime() - start;

            int[] decompressed = new int[data.length];
            start = System.nanoTime();
            packer.decompress(compressed, decompressed);
            decompTime += System.nanoTime() - start;

            if (data.length > 0) {
                start = System.nanoTime();
                packer.get(compressed, i % data.length);
                getTime += System.nanoTime() - start;
            }
        }

        return new Result(
                type.name(),
                data.length,
                compressed.length,
                compTime / ITERATIONS / 1000.0,
                decompTime / ITERATIONS / 1000.0,
                getTime / ITERATIONS / 1000.0
        );
    }

    private static void printRow(String name, Result r) {
        double gain = 100.0 - (100.0 * r.compressedSize / r.originalSize);
        System.out.printf("│ %-16s │  %6d  │  %6.1f%% │ %8.2f │ %8.2f │\n",
                name, r.compressedSize, gain, r.totalTime(), r.getTime);
    }

    private static void analyzeResults(int[] data, Result[] results) {
        // Trouver le meilleur
        Result best = results[0];
        for (Result r : results) {
            if (r.compressedSize < best.compressedSize ||
                    (r.compressedSize == best.compressedSize && r.totalTime() < best.totalTime())) {
                best = r;
            }
        }

        System.out.printf("\n→ MEILLEUR : %s (taille=%d, temps=%.2fµs)\n",
                best.name, best.compressedSize, best.totalTime());

        // Calcul du break-even
        int saved = data.length - best.compressedSize;
        if (saved > 0) {
            double overhead = best.totalTime() * 1000; // en ns
            double breakEven = overhead / saved;
            System.out.printf("→ RENTABLE si latence > %.1f ns/int (%.2f µs/int)\n",
                    breakEven, breakEven / 1000.0);
        }
    }

    private static int[] generateArray(int size, int maxValue) {
        Random rand = new Random(42);
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = rand.nextInt(maxValue + 1);
        }
        return array;
    }

    private static int[] generateWithOutliers(int size, int maxValue, int numOutliers, int outlierValue) {
        int[] array = generateArray(size, maxValue);
        Random rand = new Random(42);
        for (int i = 0; i < numOutliers && i < size; i++) {
            array[rand.nextInt(size)] = outlierValue;
        }
        return array;
    }

    private static int findMax(int[] array) {
        int max = 0;
        for (int v : array) if (v > max) max = v;
        return max;
    }

    static class Result {
        String name;
        int originalSize;
        int compressedSize;
        double compTime;
        double decompTime;
        double getTime;

        Result(String name, int originalSize, int compressedSize,
               double compTime, double decompTime, double getTime) {
            this.name = name;
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
            this.compTime = compTime;
            this.decompTime = decompTime;
            this.getTime = getTime;
        }

        double totalTime() {
            return compTime + decompTime;
        }
    }
}

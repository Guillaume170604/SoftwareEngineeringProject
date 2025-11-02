package compression;

import java.util.ArrayList;
import java.util.List;

public class OverflowBitPacking implements BitPacking {

    @Override
    public int[] compress(int[] array) {
        if (array == null || array.length == 0) {
            return new int[0];
        }

        // 1. Trouver le seuil optimal
        int maxBits = findMaxBits(array);
        int optimalBits = findOptimalBits(array, maxBits);
        int threshold = (1 << optimalBits) - 1;

        // 2. Identifier les valeurs en overflow
        List<Integer> overflowValues = new ArrayList<>();
        for (int value : array) {
            if (value > threshold) {
                overflowValues.add(value);
            }
        }

        // 3. Calculer la taille et créer le tableau
        int bitsPerElement = optimalBits + 1; // +1 pour le bit d'overflow
        int compressedSize = (int) (((long) array.length * bitsPerElement + 31) / 32);
        int[] compressed = new int[1 + compressedSize + overflowValues.size()];

        // 4. Stocker les métadonnées (format: [size][overflow count][bits])
        compressed[0] = (array.length << 16) | (overflowValues.size() << 8) | bitsPerElement;

        // 5. Compresser
        long bitPos = 0;
        int overflowIdx = 0;
        int mask = (1 << bitsPerElement) - 1;

        for (int value : array) {
            int encoded;
            if (value > threshold) {
                encoded = (1 << (bitsPerElement - 1)) | overflowIdx++;
            } else {
                encoded = value;
            }

            int arrayIdx = (int) (bitPos / 32) + 1;
            int bitOffset = (int) (bitPos % 32);

            if (bitOffset + bitsPerElement <= 32) {
                compressed[arrayIdx] |= (encoded & mask) << bitOffset;
            } else {
                int firstBits = 32 - bitOffset;
                compressed[arrayIdx] |= (encoded & ((1 << firstBits) - 1)) << bitOffset;
                compressed[arrayIdx + 1] |= (encoded >>> firstBits);
            }

            bitPos += bitsPerElement;
        }

        // 6. Copier les valeurs overflow
        int overflowStart = 1 + compressedSize;
        for (int i = 0; i < overflowValues.size(); i++) {
            compressed[overflowStart + i] = overflowValues.get(i);
        }

        return compressed;
    }

    @Override
    public int[] decompress(int[] compressedArray, int[] outputArray) {
        if (compressedArray == null || compressedArray.length < 1) {
            throw new IllegalArgumentException("Tableau compressé invalide");
        }

        // Extraction métadonnées
        int meta = compressedArray[0];
        int size = (meta >>> 16) & 0xFFFF;
        int overflowCount = (meta >>> 8) & 0xFF;
        int bitsPerElement = meta & 0xFF;

        if (outputArray == null || outputArray.length < size) {
            outputArray = new int[size];
        }

        int compressedSize = (int) (((long) size * bitsPerElement + 31) / 32);
        int overflowStart = 1 + compressedSize;
        int valueMask = (1 << (bitsPerElement - 1)) - 1;
        long bitPos = 0;

        for (int i = 0; i < size; i++) {
            int arrayIdx = (int) (bitPos / 32) + 1;
            int bitOffset = (int) (bitPos % 32);
            int value;

            if (bitOffset + bitsPerElement <= 32) {
                value = (compressedArray[arrayIdx] >>> bitOffset) & ((1 << bitsPerElement) - 1);
            } else {
                int firstBits = 32 - bitOffset;
                int lower = (compressedArray[arrayIdx] >>> bitOffset) & ((1 << firstBits) - 1);
                int upper = compressedArray[arrayIdx + 1] & ((1 << (bitsPerElement - firstBits)) - 1);
                value = lower | (upper << firstBits);
            }

            // Vérifier le bit d'overflow
            if ((value >>> (bitsPerElement - 1)) == 1) {
                outputArray[i] = compressedArray[overflowStart + (value & valueMask)];
            } else {
                outputArray[i] = value & valueMask;
            }

            bitPos += bitsPerElement;
        }

        return outputArray;
    }

    @Override
    public int get(int[] compressedArray, int index) {
        if (compressedArray == null || compressedArray.length < 1) {
            throw new IllegalArgumentException("Tableau compressé invalide");
        }

        int meta = compressedArray[0];
        int size = (meta >>> 16) & 0xFFFF;
        int bitsPerElement = meta & 0xFF;

        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index hors limites");
        }

        long bitPos = (long) index * bitsPerElement;
        int arrayIdx = (int) (bitPos / 32) + 1;
        int bitOffset = (int) (bitPos % 32);
        int value;

        if (bitOffset + bitsPerElement <= 32) {
            value = (compressedArray[arrayIdx] >>> bitOffset) & ((1 << bitsPerElement) - 1);
        } else {
            int firstBits = 32 - bitOffset;
            int lower = (compressedArray[arrayIdx] >>> bitOffset) & ((1 << firstBits) - 1);
            int upper = compressedArray[arrayIdx + 1] & ((1 << (bitsPerElement - firstBits)) - 1);
            value = lower | (upper << firstBits);
        }

        int valueMask = (1 << (bitsPerElement - 1)) - 1;
        if ((value >>> (bitsPerElement - 1)) == 1) {
            int compressedSize = (int) (((long) size * bitsPerElement + 31) / 32);
            int overflowStart = 1 + compressedSize;
            return compressedArray[overflowStart + (value & valueMask)];
        }

        return value & valueMask;
    }

    private int findMaxBits(int[] array) {
        int max = 0;
        for (int value : array) {
            if (value > max) max = value;
        }
        return max == 0 ? 1 : 32 - Integer.numberOfLeadingZeros(max);
    }

    private int findOptimalBits(int[] array, int maxBits) {
        int bestBits = maxBits;
        long bestSize = Long.MAX_VALUE;

        for (int bits = 4; bits < maxBits; bits++) {
            int threshold = (1 << bits) - 1;
            int overflowCount = 0;

            for (int value : array) {
                if (value > threshold) overflowCount++;
            }

            long totalSize = (long) array.length * (bits + 1) + (long) overflowCount * 32;
            if (totalSize < bestSize) {
                bestSize = totalSize;
                bestBits = bits;
            }
        }

        return bestBits;
    }
}

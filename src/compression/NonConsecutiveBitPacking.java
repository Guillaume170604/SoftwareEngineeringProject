package compression;

public class NonConsecutiveBitPacking implements BitPacking{
    @Override
    public int[] compress(int[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Le tableau ne peut pas être null ou vide");
        }

        int originalSize = array.length;
        int bitsPerElement = calculateMaxBitsNeeded(array);

        // Calcul de la taille du tableau compressé
        long totalBits = (long) originalSize * bitsPerElement;
        int compressedSize = (int) ((totalBits +31) / 32);

        int[] compressed = new int[compressedSize + 1]; // +1 pour les métadonnés

        //Stockage des métadonnées dans l'en tête
        compressed[0] = (originalSize << 16) | (bitsPerElement & 0xFFFF);

        // Compression des données
        long bitPosition = 0;
        int mask = (1 << bitsPerElement) - 1; // Masque pour récup seulement les bits nécessaires

        for (int i = 0; i < originalSize; i++) {
            int value = array[i] & mask;
            long currentBitPos = bitPosition;

            // Calcul de la position
            int arrayIndex = (int) (currentBitPos / 32) + 1;
            int bitOffset = (int) (currentBitPos % 32);

            // Si la valeur tient entièrement dans l'entier courant
            if (bitOffset + bitsPerElement <= 32) {
                compressed[arrayIndex] |= (value << bitOffset);
            }

            bitPosition += bitsPerElement;
        }

        return compressed;
    }

    @Override
    public int[] decompress(int[] compressedArray, int[] outputArray) {
        if (compressedArray == null || compressedArray.length == 0) {
            throw new IllegalArgumentException("Le tableau compressé ne peut pas être null ou vide");
        }

        // Extraction des métadonnées
        int originalSize = (compressedArray[0] >>> 16) & 0xFFFF;
        int bitsPerElement = compressedArray[0] & 0xFFFF;

        if (outputArray == null || outputArray.length < originalSize) {
            outputArray = new int[originalSize];
        }

        int mask = (1 << bitsPerElement) - 1;
        long bitPosition = 0;

        for (int i = 0; i < originalSize; i++) {
            long currentBitPos = bitPosition;
            int arrayIndex = (int) (currentBitPos / 32) + 1;
            int bitOffset = (int) (currentBitPos % 32);

            // Extraction de la valeur depuis le tableau compressé
            int value = (compressedArray[arrayIndex] >>> bitOffset) & mask;
            outputArray[i] = value;

            bitPosition += bitsPerElement;
        }

        return outputArray;
    }

    @Override
    public int get(int[] compressedArray, int i) {
        if (compressedArray == null || compressedArray.length == 0) {
            throw new IllegalArgumentException("Le tableau compressé ne peut pas être null ou vide");
        }

        // Extraction des métadonnées
        int originalSize = (compressedArray[0] >>> 16) & 0xFFFF;
        int bitsPerElement = compressedArray[0] & 0xFFFF;

        if (i < 0 || i >= originalSize) {
            throw new IndexOutOfBoundsException("Index " + i + " hors limites pour la taille " + originalSize);
        }

        // Calcul de la position du i-ème élément
        long bitPosition = (long) i * bitsPerElement;
        int arrayIndex = (int) (bitPosition / 32) + 1;
        int bitOffset = (int) (bitPosition % 32);

        int mask = (1 << bitsPerElement) - 1;

        // Extraction de la valeur
        int value = (compressedArray[arrayIndex] >>> bitOffset) & mask;

        return value;
    }

    private int calculateMaxBitsNeeded(int[] array) {
        int max = 0;
        for (int value : array) {
            max = Math.max(max, value);
        }
        return max == 0 ? 1 : Integer.SIZE - Integer.numberOfLeadingZeros(max);
    }
}

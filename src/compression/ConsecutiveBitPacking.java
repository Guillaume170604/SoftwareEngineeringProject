package compression;

public class ConsecutiveBitPacking implements BitPacking {

    @Override
    public int[] compress(int[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Le tableau ne peut pas être null ou vide");
        }

        int originalSize = array.length;
        int bitsPerElement = calculateMaxBitsNeeded(array);

        // Calcul de la taille du tableau compressé
        long totalBits = (long) originalSize * bitsPerElement;
        int compressedSize = (int) ((totalBits + 31) / 32); // Arrondi supérieur pour les bits

        int[] compressed = new int[compressedSize + 1]; // +1 pour stocker les métadonnées

        // Stockage des métadonnées dans le premier entier
        compressed[0] = (originalSize << 16) | (bitsPerElement & 0xFFFF);

        // Compression des données
        long bitPosition = 0;
        int mask = (1 << bitsPerElement) - 1; // Masque pour extraire les bits nécessaires

        for (int i = 0; i < originalSize; i++) {
            int value = array[i] & mask; // Application du masque pour sécurité
            long currentBitPos = bitPosition;

            // Calcul de la position dans le tableau compressé (en tenant compte des métadonnées)
            int arrayIndex = (int) (currentBitPos / 32) + 1;
            int bitOffset = (int) (currentBitPos % 32);

            // Si la valeur tient entièrement dans l'entier courant
            if (bitOffset + bitsPerElement <= 32) {
                compressed[arrayIndex] |= (value << bitOffset);
            } else {
                // La valeur chevauche sur deux entiers
                int bitsInCurrentInt = 32 - bitOffset;
                int bitsInNextInt = bitsPerElement - bitsInCurrentInt;

                // Partie dans l'entier courant
                compressed[arrayIndex] |= ((value & ((1 << bitsInCurrentInt) - 1)) << bitOffset);

                // Partie dans l'entier suivant
                if (arrayIndex + 1 < compressed.length) {
                    compressed[arrayIndex + 1] |= (value >>> bitsInCurrentInt);
                }
            }

            bitPosition += bitsPerElement;
        }

        return compressed;
    }

    private int calculateMaxBitsNeeded(int[] array) {
        int max = 0;
        for (int value : array) {
            max = Math.max(max, value);
        }
        return max == 0 ? 1 : Integer.SIZE - Integer.numberOfLeadingZeros(max);
    }


    @Override
    public int[] decompress(int[] compressedArray, int[] outputArray) {
        if (compressedArray == null || compressedArray.length < 2) {
            throw new IllegalArgumentException("Tableau compressé invalide");
        }

        // Extraction des métadonnées
        int metadata = compressedArray[0];
        int origSize = (metadata >>> 16) & 0xFFFF;
        int bitsPerElem = metadata & 0xFFFF;

        if (outputArray.length < origSize) {
            throw new IllegalArgumentException("Tableau de sortie trop petit");
        }

        long bitPosition = 0;
        int mask = (1 << bitsPerElem) - 1;

        for (int i = 0; i < origSize; i++) {
            long currentBitPos = bitPosition;
            int arrayIndex = (int) (currentBitPos / 32) + 1;
            int bitOffset = (int) (currentBitPos % 32);

            int value;

            if (bitOffset + bitsPerElem <= 32) {
                // Valeur entièrement dans un entier
                value = (compressedArray[arrayIndex] >>> bitOffset) & mask;
            } else {
                // Valeur répartie sur deux entiers
                int bitsInCurrentInt = 32 - bitOffset;
                int bitsInNextInt = bitsPerElem - bitsInCurrentInt;

                int lowerBits = (compressedArray[arrayIndex] >>> bitOffset) & ((1 << bitsInCurrentInt) - 1);
                int upperBits = (compressedArray[arrayIndex + 1] & ((1 << bitsInNextInt) - 1)) << bitsInCurrentInt;

                value = lowerBits | upperBits;
            }

            outputArray[i] = value;
            bitPosition += bitsPerElem;
        }

        return outputArray;
    }

    @Override
    public int get(int[] compressedArray, int index) {
        if (compressedArray == null || compressedArray.length < 2) {
            throw new IllegalArgumentException("Tableau compressé invalide");
        }

        // Extraction des métadonnées
        int metadata = compressedArray[0];
        int origSize = (metadata >>> 16) & 0xFFFF;
        int bitsPerElem = metadata & 0xFFFF;

        if (index < 0 || index >= origSize) {
            throw new IndexOutOfBoundsException("Index hors limites: " + index);
        }

        long bitPosition = (long) index * bitsPerElem;
        int arrayIndex = (int) (bitPosition / 32) + 1;
        int bitOffset = (int) (bitPosition % 32);
        int mask = (1 << bitsPerElem) - 1;

        if (bitOffset + bitsPerElem <= 32) {
            // Valeur entièrement dans un entier
            return (compressedArray[arrayIndex] >>> bitOffset) & mask;
        } else {
            // Valeur répartie sur deux entiers
            int bitsInCurrentInt = 32 - bitOffset;
            int bitsInNextInt = bitsPerElem - bitsInCurrentInt;

            int lowerBits = (compressedArray[arrayIndex] >>> bitOffset) & ((1 << bitsInCurrentInt) - 1);
            int upperBits = (compressedArray[arrayIndex + 1] & ((1 << bitsInNextInt) - 1)) << bitsInCurrentInt;

            return lowerBits | upperBits;
        }
    }


}

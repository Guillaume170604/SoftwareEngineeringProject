package factory;

import compression.BitPacking;
import compression.ConsecutiveBitPacking;
import compression.NonConsecutiveBitPacking;
import compression.OverflowBitPacking;

public class CompressionFactory {
    public enum CompressionType {
        CONSECUTIVE,
        NON_CONSECUTIVE,
        OVERFLOW
    }

    public static BitPacking createCompressor(CompressionType type) {
        switch (type) {
            case CONSECUTIVE:
                return new ConsecutiveBitPacking();
            case NON_CONSECUTIVE:
                return new NonConsecutiveBitPacking();
            case OVERFLOW:
                return new OverflowBitPacking();
            default:
                throw new IllegalArgumentException("Type de compression non supporté : " + type);
        }
    }
}

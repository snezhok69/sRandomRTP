package org.sRandomRTP.Utils;

public final class CoordinateUtils {

    private CoordinateUtils() {
    }

    /**
     * Mixes two seeds using the finalizer from MurmurHash3/SplitMix64.
     * Used to derive a deterministic-but-spread seed from a base seed and a per-session nonce.
     */
    public static long mixSeeds(long baseSeed, long sessionNonce) {
        long mixed = baseSeed ^ sessionNonce;
        mixed ^= (mixed >>> 30);
        mixed *= 0xbf58476d1ce4e5b9L;
        mixed ^= (mixed >>> 27);
        mixed *= 0x94d049bb133111ebL;
        mixed ^= (mixed >>> 31);
        return mixed;
    }
}

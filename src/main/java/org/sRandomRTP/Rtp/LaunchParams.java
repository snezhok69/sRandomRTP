package org.sRandomRTP.Rtp;

/** Search parameters built once per RTP launch and passed into the attempt loop. */
final class LaunchParams {
    final int centerX, centerZ, radius, minRadius, maxAttempts;
    final boolean useSessionNonce;

    LaunchParams(int centerX, int centerZ, int radius, int minRadius,
                 int maxAttempts, boolean useSessionNonce) {
        this.centerX      = centerX;
        this.centerZ      = centerZ;
        this.radius       = radius;
        this.minRadius    = minRadius;
        this.maxAttempts  = maxAttempts;
        this.useSessionNonce = useSessionNonce;
    }
}

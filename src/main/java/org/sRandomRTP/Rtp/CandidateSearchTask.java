package org.sRandomRTP.Rtp;

import org.sRandomRTP.DifferentMethods.Teleport.RtpCandidateResolution;

import java.util.concurrent.CompletableFuture;

/** Holds a single candidate coordinate pair and its async resolution future. */
final class CandidateSearchTask {
    final int x;
    final int z;
    final String searchStage;
    final CompletableFuture<RtpCandidateResolution> future;

    CandidateSearchTask(int x, int z, String searchStage, CompletableFuture<RtpCandidateResolution> future) {
        this.x = x;
        this.z = z;
        this.searchStage = searchStage;
        this.future = future;
    }
}

package org.sRandomRTP.Services;

import org.bukkit.Bukkit;
import org.sRandomRTP.Main;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Utils.ChatUtils;
import org.sRandomRTP.Files.FilesCreate;
import org.sRandomRTP.Files.FilesUpdate;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BootstrapCoordinator {

    private final Main plugin;
    private final PluginContext pluginContext;

    public BootstrapCoordinator(Main plugin, PluginContext pluginContext) {
        this.plugin = plugin;
        this.pluginContext = pluginContext;
    }

    public void initializeDatabase() {
        pluginContext.getMigrationRunner().runDatabaseMigrations();
    }

    public FileChangeSummary synchronizeFiles() {
        try {
            FilesCreate filesCreate = new FilesCreate();
            FilesUpdate filesUpdate = new FilesUpdate();
            List<String> createdFiles = filesCreate.filesCreate();
            pluginContext.getMigrationRunner().runConfigMigrations();
            List<String> updatedFiles = filesUpdate.filesUpdate();
            mergeUnique(updatedFiles, pluginContext.getConfigVersionSupport().synchronizeManagedConfigVersions());
            pluginContext.getConfigRegistry().reload();
            return new FileChangeSummary(createdFiles, updatedFiles, null);
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(BootstrapCoordinator.class, e);
            return new FileChangeSummary(Collections.<String>emptyList(), Collections.<String>emptyList(), e);
        }
    }

    public void shutdown() {
        pluginContext.getReleaseCheckService().shutdown();
        pluginContext.getTeleportMetrics().logSummaryIfEnabled();
        try {
            pluginContext.getPortalRepository().closeAsync().get(3, TimeUnit.SECONDS);
        } catch (Exception ex) {
            Bukkit.getLogger().warning(ChatUtils.PLUGIN_NAME + " Failed to close portal repository cleanly: " + ex.getMessage());
        }
    }

    private void mergeUnique(List<String> target, List<String> additions) {
        if (target == null || additions == null) {
            return;
        }
        for (String addition : additions) {
            if (addition != null && !target.contains(addition)) {
                target.add(addition);
            }
        }
    }

    public static final class FileChangeSummary {
        private final List<String> createdFiles;
        private final List<String> updatedFiles;
        private final Throwable failure;

        public FileChangeSummary(List<String> createdFiles, List<String> updatedFiles, Throwable failure) {
            this.createdFiles = createdFiles;
            this.updatedFiles = updatedFiles;
            this.failure = failure;
        }

        public List<String> getCreatedFiles() {
            return createdFiles;
        }

        public List<String> getUpdatedFiles() {
            return updatedFiles;
        }

        public boolean isSuccessful() {
            return failure == null;
        }

        public Throwable getFailure() {
            return failure;
        }
    }
}

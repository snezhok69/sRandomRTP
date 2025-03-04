package org.sRandomRTP.DataPortals;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Location;

public class PortalDataTasks {
    private String playerName;
    private String portalName;
    private String taskType;
    private long delay;
    private long period;
    private Location center;
    private int radius;
    private String taskIds;
    private WrappedTask particlesTask;
    private WrappedTask triggerTask;
    private String shape;

    public PortalDataTasks(String playerName, String portalName, String taskType, long delay, long period, Location center, int radius, String taskIds, WrappedTask particlesTask, WrappedTask triggerTask) {
        this(playerName, portalName, taskType, delay, period, center, radius, taskIds, particlesTask, triggerTask, "circle");
    }

    public PortalDataTasks(String playerName, String portalName, String taskType, long delay, long period, Location center, int radius, String taskIds, WrappedTask particlesTask, WrappedTask triggerTask, String shape) {
        this.playerName = playerName;
        this.portalName = portalName;
        this.taskType = taskType;
        this.delay = delay;
        this.period = period;
        this.center = center;
        this.radius = radius;
        this.taskIds = taskIds;
        this.particlesTask = particlesTask;
        this.triggerTask = triggerTask;
        this.shape = shape != null ? shape : "circle";
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPortalName() {
        return portalName;
    }

    public String getTaskType() {
        return taskType;
    }

    public long getDelay() {
        return delay;
    }

    public long getPeriod() {
        return period;
    }

    public Location getCenter() {
        return center;
    }

    public int getRadius() {
        return radius;
    }

    public String getTaskIds() {
        return taskIds;
    }

    public WrappedTask getParticlesTask() {
        return particlesTask;
    }

    public WrappedTask getTriggerTask() {
        return triggerTask;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }
}
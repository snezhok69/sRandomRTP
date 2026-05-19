package org.sRandomRTP.Services;

public enum AdminBarType {
    TPS("tpsbar", "TPS bar", "sRandomRTP.Command.TpsBar", "admin-bars.tpsbar"),
    RAM("rambar", "RAM bar", "sRandomRTP.Command.RamBar", "admin-bars.rambar"),
    MSPT("msptbar", "MSPT bar", "sRandomRTP.Command.MsptBar", "admin-bars.msptbar");

    private final String subCommand;
    private final String displayName;
    private final String permissionNode;
    private final String configPath;

    AdminBarType(String subCommand, String displayName, String permissionNode, String configPath) {
        this.subCommand = subCommand;
        this.displayName = displayName;
        this.permissionNode = permissionNode;
        this.configPath = configPath;
    }

    public String getSubCommand() {
        return subCommand;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPermissionNode() {
        return permissionNode;
    }

    public String getConfigPath() {
        return configPath;
    }

    public static AdminBarType fromSubCommand(String value) {
        if (value == null) {
            return null;
        }
        for (AdminBarType type : values()) {
            if (type.subCommand.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}

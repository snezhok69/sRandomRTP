package org.sRandomRTP.DataPortals;

/**
 * Centralises the delimiter constants and parse/serialize helpers for the
 * portal name/data CSV format stored in the database.
 *
 * <p>Format:
 * <ul>
 *   <li>portal_names column: {@code "name1, name2, name3"} (delimiter ", ")</li>
 *   <li>portal_data  column: {@code "world, x, y, z, shape -> name1 | world, x, y, z, shape -> name2 | ..."}
 *       (delimiter " | ")</li>
 * </ul>
 */
public final class PortalDataSerializer {

    static final String NAME_DELIMITER  = ", ";
    static final String DATA_DELIMITER  = " | ";
    static final String DATA_DELIMITER_REGEX = " \\| ";

    private PortalDataSerializer() {}

    /** Splits the portal_names string into an array. */
    public static String[] parseNames(String namesStr) {
        return namesStr == null ? new String[0] : namesStr.split(NAME_DELIMITER);
    }

    /** Splits the portal_data string into an array of per-portal data entries. */
    public static String[] parseData(String dataStr) {
        return dataStr == null ? new String[0] : dataStr.split(DATA_DELIMITER_REGEX);
    }

    /** Joins name and data arrays back into the stored format. */
    public static String[] serialize(String[] names, String[] data) {
        return new String[]{
                String.join(NAME_DELIMITER, names),
                String.join(DATA_DELIMITER, data)
        };
    }
}

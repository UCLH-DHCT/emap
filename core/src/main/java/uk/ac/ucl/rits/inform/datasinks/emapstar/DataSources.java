package uk.ac.ucl.rits.inform.datasinks.emapstar;

import java.util.List;

/**
 * Utility class for data source types.
 */
public final class DataSources {
    private static final List<String> TRUSTED_SOURCES = List.of(new String[]{"EPIC"});

    private DataSources() {
        // not called
    }

    /**
     * Is the datasource one that we trust to update already existing information in the database.
     * @param dataSource Datasource to be tested
     * @return true if the datasource is trusted.
     */
    public static boolean isTrusted(String dataSource) {
        return TRUSTED_SOURCES.contains(dataSource);
    }
}

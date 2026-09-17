package com.crisisscope.provider.dwd;

import java.util.Map;

/**
 * Representative coordinates for a DWD warning's affected area.
 * <p>
 * DWD's community-level warnings feed identifies areas by name (Gemeinde/Landkreis
 * plus Bundesland) rather than by polygon or point geometry. Resolving the exact
 * warncell shape would require pairing this feed with DWD's separate warncell
 * shapefile, which is out of scope here; instead each warning is placed at its
 * federal state's capital as a representative coordinate, close enough for the
 * dashboard map while keeping this provider free of extra static geo data.
 */
final class BundeslandCoordinates {

    private static final double[] GERMANY_CENTER = {51.1657, 10.4515};

    private static final Map<String, double[]> STATE_CAPITALS = Map.ofEntries(
            Map.entry("Baden-Württemberg", new double[]{48.7758, 9.1829}),
            Map.entry("Bayern", new double[]{48.1351, 11.5820}),
            Map.entry("Berlin", new double[]{52.5200, 13.4050}),
            Map.entry("Brandenburg", new double[]{52.4009, 12.5378}),
            Map.entry("Bremen", new double[]{53.0793, 8.8017}),
            Map.entry("Hamburg", new double[]{53.5511, 9.9937}),
            Map.entry("Hessen", new double[]{50.0782, 8.2398}),
            Map.entry("Mecklenburg-Vorpommern", new double[]{53.6355, 11.4012}),
            Map.entry("Niedersachsen", new double[]{52.3759, 9.7320}),
            Map.entry("Nordrhein-Westfalen", new double[]{51.4332, 7.6616}),
            Map.entry("Rheinland-Pfalz", new double[]{50.1183, 8.2417}),
            Map.entry("Saarland", new double[]{49.2354, 6.9969}),
            Map.entry("Sachsen", new double[]{51.0504, 13.7373}),
            Map.entry("Sachsen-Anhalt", new double[]{52.1205, 11.6276}),
            Map.entry("Schleswig-Holstein", new double[]{54.3233, 10.1228}),
            Map.entry("Thüringen", new double[]{50.9848, 11.0299})
    );

    private BundeslandCoordinates() {
    }

    static double[] representativeCoordinates(String state) {
        if (state == null || state.isBlank()) {
            return GERMANY_CENTER;
        }
        return STATE_CAPITALS.getOrDefault(state.strip(), GERMANY_CENTER);
    }
}

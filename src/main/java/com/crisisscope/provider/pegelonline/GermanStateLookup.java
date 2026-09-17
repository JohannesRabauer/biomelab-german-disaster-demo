package com.crisisscope.provider.pegelonline;

import java.util.Map;

/**
 * Best-effort Bundesland (federal state) resolution from a coordinate.
 * <p>
 * PEGELONLINE stations carry exact coordinates but no administrative region,
 * while {@link com.crisisscope.model.Location#region()} needs a Bundesland for
 * filtering and the regional details panel. Resolving the true containing
 * state would need polygon/boundary data this project doesn't otherwise
 * carry, so each station is instead assigned to its nearest state capital by
 * straight-line distance — close enough for a station's actual state in the
 * vast majority of cases without pulling in a geo/boundary dependency.
 */
final class GermanStateLookup {

    private record Capital(String state, double latitude, double longitude) {
    }

    private static final Capital[] CAPITALS = {
            new Capital("Baden-Württemberg", 48.7758, 9.1829),
            new Capital("Bayern", 48.1351, 11.5820),
            new Capital("Berlin", 52.5200, 13.4050),
            new Capital("Brandenburg", 52.4009, 12.5378),
            new Capital("Bremen", 53.0793, 8.8017),
            new Capital("Hamburg", 53.5511, 9.9937),
            new Capital("Hessen", 50.0782, 8.2398),
            new Capital("Mecklenburg-Vorpommern", 53.6355, 11.4012),
            new Capital("Niedersachsen", 52.3759, 9.7320),
            new Capital("Nordrhein-Westfalen", 51.4332, 7.6616),
            new Capital("Rheinland-Pfalz", 50.1183, 8.2417),
            new Capital("Saarland", 49.2354, 6.9969),
            new Capital("Sachsen", 51.0504, 13.7373),
            new Capital("Sachsen-Anhalt", 52.1205, 11.6276),
            new Capital("Schleswig-Holstein", 54.3233, 10.1228),
            new Capital("Thüringen", 50.9848, 11.0299),
    };

    private GermanStateLookup() {
    }

    /** Nearest Bundesland to a coordinate, or {@code "Germany"} if none is given. */
    static String nearestState(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return "Germany";
        }
        Capital nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Capital capital : CAPITALS) {
            double dLat = capital.latitude() - latitude;
            double dLon = capital.longitude() - longitude;
            double distance = dLat * dLat + dLon * dLon;
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = capital;
            }
        }
        return nearest != null ? nearest.state() : "Germany";
    }
}

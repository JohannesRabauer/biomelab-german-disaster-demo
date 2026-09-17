package com.crisisscope.provider.bbk;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Maps the two-letter Bundesland code embedded in a MoWaS CAP alert's
 * {@code sender} (e.g. {@code DE-BW-LB-W026} → {@code BW}) to a representative
 * location.
 * <p>
 * BBK's CAP feed identifies affected areas by free-text description and
 * administrative codes, not by polygon or point geometry, so each alert is
 * placed at its federal state's capital as a stand-in coordinate — close
 * enough for the dashboard map without pulling in a separate geocoder.
 */
final class GermanStates {

    private static final double[] GERMANY_CENTER = {51.1657, 10.4515};

    private static final Pattern SENDER_STATE_CODE = Pattern.compile("^DE-([A-Z]{2})-");

    private record State(String name, double latitude, double longitude) {
    }

    private static final Map<String, State> STATES_BY_CODE = Map.ofEntries(
            Map.entry("BW", new State("Baden-Württemberg", 48.7758, 9.1829)),
            Map.entry("BY", new State("Bayern", 48.1351, 11.5820)),
            Map.entry("BE", new State("Berlin", 52.5200, 13.4050)),
            Map.entry("BB", new State("Brandenburg", 52.4009, 12.5378)),
            Map.entry("HB", new State("Bremen", 53.0793, 8.8017)),
            Map.entry("HH", new State("Hamburg", 53.5511, 9.9937)),
            Map.entry("HE", new State("Hessen", 50.0782, 8.2398)),
            Map.entry("MV", new State("Mecklenburg-Vorpommern", 53.6355, 11.4012)),
            Map.entry("NI", new State("Niedersachsen", 52.3759, 9.7320)),
            Map.entry("NW", new State("Nordrhein-Westfalen", 51.4332, 7.6616)),
            Map.entry("RP", new State("Rheinland-Pfalz", 50.1183, 8.2417)),
            Map.entry("SL", new State("Saarland", 49.2354, 6.9969)),
            Map.entry("SN", new State("Sachsen", 51.0504, 13.7373)),
            Map.entry("ST", new State("Sachsen-Anhalt", 52.1205, 11.6276)),
            Map.entry("SH", new State("Schleswig-Holstein", 54.3233, 10.1228)),
            Map.entry("TH", new State("Thüringen", 50.9848, 11.0299))
    );

    private GermanStates() {
    }

    /** Full Bundesland name for a CAP alert's {@code sender}, or {@code "Germany"} if it can't be resolved. */
    static String regionFor(String sender) {
        State state = stateFor(sender);
        return state != null ? state.name() : "Germany";
    }

    /** {@code [latitude, longitude]} for a CAP alert's {@code sender}, falling back to Germany's centroid. */
    static double[] coordinatesFor(String sender) {
        State state = stateFor(sender);
        return state != null ? new double[]{state.latitude(), state.longitude()} : GERMANY_CENTER;
    }

    private static State stateFor(String sender) {
        if (sender == null) {
            return null;
        }
        var matcher = SENDER_STATE_CODE.matcher(sender);
        if (!matcher.find()) {
            return null;
        }
        return STATES_BY_CODE.get(matcher.group(1));
    }
}

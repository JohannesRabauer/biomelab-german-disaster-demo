package com.crisisscope.provider.pegelonline;

import java.util.List;

/**
 * Fetches raw PEGELONLINE station data. Kept separate from parsing/mapping so
 * tests can supply fixture data without any HTTP layer.
 */
interface PegelonlineClient {

    /** All stations, each including its current water level measurement. */
    List<PegelonlineStation> fetchStations();
}

package com.crisisscope.provider.bbk;

import java.util.List;

/**
 * Fetches raw BBK warning data. Kept separate from parsing/mapping so tests
 * can supply fixture data without any HTTP layer. Implementations should
 * throw on failure rather than return partial/null data — callers decide how
 * to degrade.
 */
interface BbkWarningsClient {

    /** Lightweight listing for a channel (e.g. {@code "mowas"}, {@code "katwarn"}, {@code "biwapp"}). */
    List<BbkMapDataEntry> fetchMapData(String channel);

    /** Full CAP alert detail for a single warning id. */
    BbkAlert fetchAlertDetail(String id);
}

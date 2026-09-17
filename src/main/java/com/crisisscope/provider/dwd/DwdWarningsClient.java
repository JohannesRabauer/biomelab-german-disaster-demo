package com.crisisscope.provider.dwd;

/**
 * Fetches the raw DWD community-level warnings payload. Kept separate from
 * parsing/mapping so tests can supply fixture content without any HTTP layer.
 */
interface DwdWarningsClient {

    /**
     * Returns the raw response body, including its JSONP wrapper
     * (e.g. {@code warnWetter.loadWarnings({...});}).
     */
    String fetchRawWarningsPayload();
}

package com.embabel.gekko.dataflows;

import com.embabel.gekko.util.AgentUtils;
import com.embabel.gekko.util.ResultCache;
import org.springframework.web.client.RestTemplate;

abstract class BaseExternalDataService {

    protected static final String NO_DATA = "NO_DATA_AVAILABLE";

    private final RestTemplate restTemplate;
    private final ResultCache resultCache;

    protected BaseExternalDataService(int connectTimeoutMs, int readTimeoutMs, ResultCache resultCache) {
        this.resultCache = resultCache;
        this.restTemplate = AgentUtils.restTemplate(connectTimeoutMs, readTimeoutMs);
    }

    protected RestTemplate restTemplate() {
        return restTemplate;
    }

    protected ResultCache resultCache() {
        return resultCache;
    }

    protected String cachedGet(String cachePrefix, String[] keyParts, java.util.function.Supplier<String> fetcher) {
        String key = ResultCache.canonicalKey(ResultCache.CATEGORY_EXTERNAL_HTTP,
                java.util.stream.Stream.concat(java.util.stream.Stream.of(cachePrefix),
                        java.util.stream.Stream.of(keyParts)).toArray(String[]::new));
        return resultCache.getOrCompute(ResultCache.CATEGORY_EXTERNAL_HTTP, key, String.class, fetcher);
    }

    protected String noData(String reason) {
        return NO_DATA + ": " + reason;
    }
}

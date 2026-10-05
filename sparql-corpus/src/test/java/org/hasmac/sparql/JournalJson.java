package org.hasmac.sparql;

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Lossless JSON output even when a parser diagnostic contains an unpaired UTF-16 surrogate. */
final class JournalJson {
    private JournalJson() {}
    static ObjectMapper mapper() {
        return JsonMapper.builder().enable(JsonWriteFeature.ESCAPE_NON_ASCII).build();
    }
}

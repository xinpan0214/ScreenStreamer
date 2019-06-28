package com.mp4parser.streaming.extensions;

import com.mp4parser.streaming.SampleExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CompositionTimeSampleExtension implements SampleExtension {
    public static Map<Integer, CompositionTimeSampleExtension> pool =
            Collections.synchronizedMap(new HashMap<Integer, CompositionTimeSampleExtension>());

    public static CompositionTimeSampleExtension create(int offset) {
        CompositionTimeSampleExtension c = pool.get(offset);
        if (c == null) {
            c = new CompositionTimeSampleExtension();
            c.ctts = offset;
            pool.put(offset, c);
        }
        return c;
    }

    private int ctts;

    /**
     * This value provides the offset between decoding time and composition time. The offset is expressed as
     * signed long such that CT(n) = DT(n) + CTTS(n). This method is
     *
     * @return offset between decoding time and composition time.
     */
    public int getCompositionTimeOffset() {
        return ctts;
    }
}

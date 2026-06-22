package li.cil.oc.core.impl.common.component.traits;

import java.util.HashMap;
import java.util.Map;

public abstract class VideoRamDevice {
    protected final Map<Integer, li.cil.oc.core.impl.common.component.GpuTextBuffer> internalBuffers = new HashMap<>();

    @SuppressWarnings("EmptyMethod")
    public void onBufferRamDestroy(int ignoredId) {
    }

    public int[] bufferIndexes() {
        return internalBuffers.keySet().stream().mapToInt(i -> i).toArray();
    }

    public boolean addBuffer(li.cil.oc.core.impl.common.component.GpuTextBuffer ram) {
        boolean preexists = internalBuffers.containsKey(ram.id);
        internalBuffers.put(ram.id, ram);
        return preexists;
    }

    public int removeBuffers(int[] ids) {
        int count = 0;
        for (int id : ids) {
            if (internalBuffers.remove(id) != null) {
                onBufferRamDestroy(id);
                count++;
            }
        }
        return count;
    }

    public li.cil.oc.core.impl.common.component.GpuTextBuffer getBuffer(int id) {
        return internalBuffers.get(id);
    }

}

package li.cil.oc.core.impl.common.component.traits;

import java.util.Map;
import li.cil.oc.core.impl.common.component.GpuTextBuffer;
import li.cil.oc.core.impl.util.PackedColor;
import li.cil.oc.core.impl.util.TextBuffer;
import net.minecraft.nbt.CompoundTag;

public interface VideoRamRasterizer {
    Map<String, VideoRamDevice> getInternalBuffers();

    void onBufferRamInit(li.cil.oc.core.impl.common.component.GpuTextBuffer ram);

    void onBufferBitBlt(int col, int row, int w, int h, li.cil.oc.core.impl.common.component.GpuTextBuffer ram, int fromCol, int fromRow);

    void onBufferRamDestroy(li.cil.oc.core.impl.common.component.GpuTextBuffer ram);

    default boolean addBuffer(GpuTextBuffer ram) {
        var gpu = getInternalBuffers().get(ram.owner);
        if (gpu == null) {
            gpu = new VirtualRamDevice();
            getInternalBuffers().put(ram.owner, gpu);
        }
        boolean preexists = gpu.addBuffer(ram);
        if (!preexists || ram.dirty) {
            onBufferRamInit(ram);
        }
        return preexists;
    }

    default boolean removeBuffer(String owner, int id) {
        var gpu = getInternalBuffers().get(owner);
        if (gpu != null) {
            var ram = gpu.getBuffer(id);
            if (ram != null) {
                onBufferRamDestroy(ram);
                return gpu.removeBuffers(new int[]{id}) == 1;
            }
        }
        return false;
    }

    default int removeAllBuffers(String owner) {
        int count = 0;
        var gpu = getInternalBuffers().get(owner);
        if (gpu != null) {
            for (int id : gpu.bufferIndexes()) {
                if (removeBuffer(owner, id)) count++;
            }
        }
        return count;
    }

    @SuppressWarnings("UnusedReturnValue")
    default int removeAllBuffers() {
        int count = 0;
        for (String owner : getInternalBuffers().keySet()) {
            count += removeAllBuffers(owner);
        }
        return count;
    }

    default boolean loadBuffer(String owner, int id, CompoundTag nbt) {
        var src = new TextBuffer(1, 1, PackedColor.SingleBitFormat.INSTANCE);
        src.load(nbt, null);
        return addBuffer(li.cil.oc.core.impl.common.component.GpuTextBuffer.wrap(owner, id, src));
    }

    default li.cil.oc.core.impl.common.component.GpuTextBuffer getBuffer(String owner, int id) {
        var gpu = getInternalBuffers().get(owner);
        if (gpu != null) return gpu.getBuffer(id);
        return null;
    }

    class VirtualRamDevice extends VideoRamDevice {
    }
}

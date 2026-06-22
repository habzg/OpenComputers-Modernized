package li.cil.oc.neoforge.event;

import li.cil.oc.api.event.GeolyzerEvent;
import li.cil.oc.api.network.EnvironmentHost;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import java.util.HashMap;
import java.util.Map;

public class GeolyzerEventImpl extends Event implements GeolyzerEvent, ICancellableEvent {
    @SuppressWarnings("NonExtendableApiUsage")
    @Override
    public boolean isCanceled() {
        return ICancellableEvent.super.isCanceled();
    }

    @Override
    public void setCanceled(boolean c) {
        ICancellableEvent.super.setCanceled(c);
    }

    protected final EnvironmentHost host;
    protected final Map<?, ?> options;

    public GeolyzerEventImpl(EnvironmentHost host, Map<?, ?> options) {
        this.host = host;
        this.options = options;
    }

    @Override
    public EnvironmentHost host() {
        return host;
    }

    @Override
    public Map<?, ?> options() {
        return options;
    }

    public static class Scan extends GeolyzerEventImpl implements GeolyzerEvent.Scan {
        protected final int minX;
        protected final int minY;
        protected final int minZ;
        protected final int maxX;
        protected final int maxY;
        protected final int maxZ;
        private final float[] data = new float[64];

        public Scan(EnvironmentHost host, Map<?, ?> options, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            super(host, options);
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        @Override
        public int minX() {
            return minX;
        }

        @Override
        public int minY() {
            return minY;
        }

        @Override
        public int minZ() {
            return minZ;
        }

        @Override
        public int maxX() {
            return maxX;
        }

        @Override
        public int maxY() {
            return maxY;
        }

        @Override
        public int maxZ() {
            return maxZ;
        }

        @Override
        public float[] data() {
            return data;
        }
    }

    public static class Analyze extends GeolyzerEventImpl implements GeolyzerEvent.Analyze {
        protected final int x;
        protected final int y;
        protected final int z;
        private final Map<String, Object> data = new HashMap<>();

        public Analyze(EnvironmentHost host, Map<?, ?> options, int x, int y, int z) {
            super(host, options);
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public int x() {
            return x;
        }

        @Override
        public int y() {
            return y;
        }

        @Override
        public int z() {
            return z;
        }

        @Override
        public Map<String, Object> data() {
            return data;
        }
    }
}

package li.cil.oc.api.event;

import li.cil.oc.api.network.EnvironmentHost;

import java.util.Map;

/**
 * This event is fired by the geolyzer block/upgrade.
 * <br>
 * When cancelling this event, the respective method will bail and report
 * that the operation failed.
 */
public interface GeolyzerEvent extends Event {
    /**
     * The container of the geolyzer li.cil.oc.common.component. This can either be the
     * geolyzer block, or something with the geolyzer upgrade (a robot).
     */
    @SuppressWarnings("unused")
    EnvironmentHost host();

    /**
     * The options the operation was invoked with.
     */
    @SuppressWarnings("unused")
    Map<?, ?> options();

    /**
     * Long-distance scan, getting quantified information about blocks around
     * the geolyzer. By default this will yield a (noisy) listing of the
     * hardness of the blocks.
     * <br>
     * The bounds are guaranteed to not define a volume larger than 64.
     * Resulting data should be written to the {@link #data()} array such that
     * <code>index = x + z*w + y*w*d</code>, with <code>w = maxX - minX</code>
     * and <code>d = maxZ - minZ</code> (<code>h</code> meaning height, <code>d</code>
     * meaning depth).
     */
    interface Scan extends GeolyzerEvent, CancellableEvent {
        /**
         * The <em>relative</em> minimal x coordinate of the box being scanned (inclusive).
         */
        @SuppressWarnings("unused")
        int minX();

        /**
         * The <em>relative</em> minimal y coordinate of the box being scanned (inclusive).
         */
        @SuppressWarnings("unused")
        int minY();

        /**
         * The <em>relative</em> minimal z coordinate of the box being scanned (inclusive).
         */
        @SuppressWarnings("unused")
        int minZ();

        /**
         * The <em>relative</em> maximal x coordinate of the box being scanned (inclusive).
         */
        @SuppressWarnings("unused")
        int maxX();

        /**
         * The <em>relative</em> maximal y coordinate of the box being scanned (inclusive).
         */
        @SuppressWarnings("unused")
        int maxY();

        /**
         * The <em>relative</em> maximal z coordinate of the box being scanned (inclusive).
         */
        @SuppressWarnings("unused")
        int maxZ();

        /**
         * The data for the column of blocks being scanned, which is an
         * interval around the geolyzer itself, with the geolyzer block
         * being at index 32.
         */
        float[] data();
    }

    /**
     * Zero-range scan, getting in-depth information about blocks directly
     * adjacent to the geolyzer. By default this will yield the block's
     * name, metadata, hardness and harvest information.
     */
    interface Analyze extends GeolyzerEvent, CancellableEvent {
        /**
         * The x position of the block to scan.
         * <br>
         * Note: get the Level via the host if you need it.
         */
        @SuppressWarnings("unused")
        int x();

        /**
         * The y position of the block to scan.
         */
        @SuppressWarnings("unused")
        int y();

        /**
         * The z position of the block to scan.
         */
        @SuppressWarnings("unused")
        int z();

        /**
         * The retrieved data for the block being scanned.
         */
        Map<String, Object> data();
    }
}

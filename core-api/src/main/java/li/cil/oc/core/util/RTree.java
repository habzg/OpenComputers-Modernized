package li.cil.oc.core.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class RTree<Data> {
    private final int M;
    private final int m;
    private final Function<Data, double[]> coordinate;
    private final Map<Data, Leaf> entries = new LinkedHashMap<>();
    private NonLeaf root;

    public RTree(int M, Function<Data, double[]> coordinate) {
        if (M < 2) throw new IllegalArgumentException("maxEntries must be larger or equal to 2.");
        this.M = M;
        this.m = Math.max(M / 2, 1);
        this.coordinate = coordinate;
        this.root = new NonLeaf();
    }

    public synchronized double[] apply(Data value) {
        Leaf leaf = entries.get(value);
        if (leaf != null) return leaf.bounds.min.asArray();
        return null;
    }

    public synchronized List<Object[]> allBounds() {
        return root.allBounds(0);
    }

    @SuppressWarnings("unused")
    public synchronized void add(Data value) {
        boolean replaced = remove(value);
        Leaf entry = new Leaf(value, new Point(coordinate.apply(value)));
        entries.put(value, entry);
        Node newNode = root.add(entry);
        if (newNode != root) {
            root = new NonLeaf(newNode, root);
        }
    }

    public synchronized boolean remove(Data value) {
        Leaf node = entries.remove(value);
        if (node != null) {
            Node change = root.remove(node);
            if (change != null && root.children.size() == 1 && root.children.iterator().next() instanceof NonLeaf nl) {
                root = nl;
            } else {
                root.bounds = root.bounds.around(root.children);
            }
            return true;
        }
        return false;
    }

    public synchronized List<Data> query(double[] from, double[] to) {
        return root.query(new Rectangle(new Point(from), new Point(to)));
    }

    private record Point(double x, double y, double z) {
        static final Point NegativeInfinity = new Point(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        static final Point PositiveInfinity = new Point(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

        Point(double[] arr) {
            this(arr[0], arr[1], arr[2]);
        }

        Point min(Point other) {
            return new Point(Math.min(x, other.x), Math.min(y, other.y), Math.min(z, other.z));
        }

        Point max(Point other) {
            return new Point(Math.max(x, other.x), Math.max(y, other.y), Math.max(z, other.z));
        }

        double[] asArray() {
            return new double[]{x, y, z};
        }
    }

    private abstract class Node {
        Rectangle bounds;

        abstract List<Object[]> allBounds(int level);

        boolean isLeaf() {
            return true;
        }

        abstract Node add(Node value);

        abstract Node remove(Node value);

        abstract List<Data> query(Rectangle query);
    }

    private class NonLeaf extends Node {
        final Set<Node> children = new LinkedHashSet<>();

        NonLeaf() {
            bounds = new Rectangle(Point.PositiveInfinity, Point.NegativeInfinity);
        }

        @SafeVarargs
        NonLeaf(Node... nodes) {
            this();
            for (Node child : nodes) {
                children.add(child);
                bounds = bounds.including(child.bounds);
            }
        }

        @Override
        List<Object[]> allBounds(int level) {
            List<Object[]> result = new ArrayList<>();
            result.add(new Object[]{bounds.asPair(), level});
            for (Node child : children) {
                result.addAll(child.allBounds(level + 1));
            }
            return result;
        }

        @Override
        boolean isLeaf() {
            Iterator<Node> it = children.iterator();
            return it.hasNext() && it.next() instanceof Leaf;
        }

        @Override
        Node add(Node value) {
            uncheckedAdd(value);
            if (children.size() > M) {
                return split();
            }
            bounds = bounds.including(value.bounds);
            return this;
        }

        private void uncheckedAdd(Node value) {
            Node bestChild = null;
            double bestGrowth = Double.POSITIVE_INFINITY;
            double bestVolume = Double.POSITIVE_INFINITY;
            for (Node child : children) {
                if (!child.isLeaf() || value instanceof Leaf) {
                    double oldVolume = child.bounds.volume();
                    double volume = child.bounds.including(value.bounds).volume();
                    double growth = volume - oldVolume;
                    if (growth < bestGrowth || (growth == bestGrowth && volume < bestVolume)) {
                        bestChild = child;
                        bestGrowth = growth;
                        bestVolume = volume;
                    }
                }
            }
            if (bestChild != null) {
                children.add(bestChild.add(value));
            } else {
                children.add(value);
            }
        }

        @Override
        Node remove(Node value) {
            if (bounds.intersects(value.bounds)) {
                for (Node child : new ArrayList<>(children)) {
                    Node change = child.remove(value);
                    if (change != null) {
                        if (change == child) {
                            children.remove(child);
                            if (child instanceof NonLeaf nl) {
                                for (Node c : nl.children) {
                                    uncheckedAdd(c);
                                }
                                if (children.size() > M) {
                                    return split();
                                }
                            }
                            if (children.size() < m) {
                                return this;
                            }
                            bounds = bounds.around(children);
                            return value;
                        } else if (change == value) {
                            bounds = bounds.around(children);
                            return value;
                        } else {
                            uncheckedAdd(change);
                            if (children.size() > M) {
                                return split();
                            }
                            bounds = bounds.around(children);
                            return value;
                        }
                    }
                }
            }
            return null;
        }

        @Override
        List<Data> query(Rectangle query) {
            if (query.intersects(bounds)) {
                List<Data> result = new ArrayList<>();
                for (Node child : children) {
                    result.addAll(child.query(query));
                }
                return result;
            }
            return List.of();
        }

        private Node split() {
            List<Node> values = new ArrayList<>(children);
            Node seed1 = null, seed2 = null;
            double worst = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < values.size(); i++) {
                Node si = values.get(i);
                for (int j = i + 1; j < values.size(); j++) {
                    Node sj = values.get(j);
                    double vol1 = si.bounds.volume();
                    double vol2 = sj.bounds.volume();
                    double vol = si.bounds.including(sj.bounds).volume();
                    double d = vol - vol1 - vol2;
                    if (d > worst) {
                        seed1 = si;
                        seed2 = sj;
                        worst = d;
                    }
                }
            }
            if (seed1 == null) throw new AssertionError();

            SplitResult r1 = new SplitResult(new LinkedHashSet<>(List.of(seed1)), seed1.bounds);
            SplitResult r2 = new SplitResult(new LinkedHashSet<>(List.of(seed2)), seed2.bounds);

            Set<Node> list = new LinkedHashSet<>(values);
            list.remove(seed1);
            list.remove(seed2);

            while (!list.isEmpty()) {
                if (m - r1.set.size() >= list.size()) {
                    r1.set.addAll(list);
                    for (Node v : list) r1.bounds = r1.bounds.including(v.bounds);
                    list.clear();
                } else if (m - r2.set.size() >= list.size()) {
                    r2.set.addAll(list);
                    for (Node v : list) r2.bounds = r2.bounds.including(v.bounds);
                    list.clear();
                } else {
                    Node bestValue = null;
                    SplitResult r = r1;
                    double best = Double.NEGATIVE_INFINITY;
                    for (Node value : list) {
                        double newVol1 = r1.volumeIncluding(value);
                        double newVol2 = r2.volumeIncluding(value);
                        double growth1 = newVol1 - r1.volume();
                        double growth2 = newVol2 - r2.volume();
                        double d = Math.abs(growth2 - growth1);
                        if (d > best) {
                            bestValue = value;
                            r = (growth1 < growth2 || (growth1 == growth2 && newVol1 < newVol2)) ? r1 : r2;
                            best = d;
                        }
                    }
                    if (bestValue != null) {
                        list.remove(bestValue);
                        r.set.add(bestValue);
                        r.bounds = r.bounds.including(bestValue.bounds);
                    } else throw new AssertionError();
                }
            }

            children.clear();
            children.addAll(r1.set);
            bounds = r1.bounds;

            NonLeaf LL = new NonLeaf();
            LL.children.addAll(r2.set);
            LL.bounds = r2.bounds;
            return LL;
        }
    }

    private class Leaf extends Node {
        final Data data;

        Leaf(Data data, Point point) {
            this.data = data;
            this.bounds = new Rectangle(point, point);
        }

        @Override
        List<Object[]> allBounds(int level) {
            return List.<Object[]>of(new Object[]{bounds.asPair(), level});
        }

        @Override
        Node add(Node value) {
            return value;
        }

        @Override
        Node remove(Node value) {
            return value == this ? this : null;
        }

        @Override
        List<Data> query(Rectangle query) {
            if (query.intersects(bounds)) return List.of(data);
            return List.of();
        }
    }

    private class Rectangle {
        final Point min, max;

        Rectangle(Point min, Point max) {
            this.min = min;
            this.max = max;
        }

        Rectangle including(Rectangle value) {
            return new Rectangle(value.min.min(min), value.max.max(max));
        }

        boolean intersects(Rectangle value) {
            return value.min.x <= max.x && value.min.y <= max.y && value.min.z <= max.z &&
                    value.max.x >= min.x && value.max.y >= min.y && value.max.z >= min.z;
        }

        double volume() {
            double sx = max.x - min.x;
            double sy = max.y - min.y;
            double sz = max.z - min.z;
            return sx * sy * sz;
        }

        Object[] asPair() {
            return new Object[]{min.asArray(), max.asArray()};
        }

        Rectangle around(Iterable<Node> values) {
            Point min = Point.PositiveInfinity;
            Point max = Point.NegativeInfinity;
            for (Node value : values) {
                min = value.bounds.min.min(min);
                max = value.bounds.max.max(max);
            }
            return new Rectangle(min, max);
        }
    }

    private class SplitResult {
        final Set<Node> set;
        Rectangle bounds;

        SplitResult(Set<Node> set, Rectangle bounds) {
            this.set = set;
            this.bounds = bounds;
        }

        double volume() {
            return bounds.volume();
        }

        double volumeIncluding(Node value) {
            return bounds.including(value.bounds).volume();
        }
    }
}

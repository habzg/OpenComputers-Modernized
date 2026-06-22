package li.cil.oc.api.detail;

import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;

@FunctionalInterface
public interface FileSystemEnvironmentFactory {
    @SuppressWarnings("unused")
    ManagedEnvironment create(li.cil.oc.api.fs.FileSystem fileSystem, Label label, EnvironmentHost host, String accessSound, int speed);
}

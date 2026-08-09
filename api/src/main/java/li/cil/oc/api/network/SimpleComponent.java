package li.cil.oc.api.network;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This interface can be used to easily convert block entities to components,
 * without having to implement {@link li.cil.oc.api.network.Environment}
 * themselves. The simple implementation will provide no access to OC's internal
 * component network, since you won't have access to the node representing the
 * block entity. Use this only for simple cases, where you want to expose a
 * couple of methods to the programs running computers.
 * <br>
 * <br>
 * Classes implementing this interface will be expanded with the methods
 * required for them to function as native block components (say, like the
 * screen or keyboard). This means functions in the <code>Environment</code>
 * interface have to created using a class transformer. If any of the methods
 * already exist, this will fail! If things don't work, check your logs, first.
 * <br>
 * To expose methods to OC, tag them with {@link li.cil.oc.api.machine.Callback}
 * and have them use the according signature (see the documentation on the
 * <code>Callback</code> annotation).
 * <br>
 * Alternatively, implement {@link li.cil.oc.api.network.ManagedPeripheral} in
 * addition to this interface, to make methods available ComputerCraft style.
 * <br>
 * So, in short:
 * <ul>
 * <li>Implement this interface on a block entity that should expose
 * methods to computers.</li>
 * <li>Annotate methods with <code>Callback</code> so they exported.</li>
 * <li>Alternatively/additionally implement <code>ManagedPeripheral</code> to
 * provide methods via a list of names and single callback method.</li>
 * </ul>
 * <br>
 * For example:
 * <pre>
 *    public class MyBlockEntity extends BlockEntity
 *           implements SimpleComponent
 *    {
 *        {@literal @}Override
 *        public String getComponentName() {
 *            return "fancy_thing";
 *        }
 *
 *        {@literal @}Callback
 *        public Object[] greet(Context context, Arguments args) {
 *            return new Object[]{String.format("Hello, %s!", args.checkString(0))};
 *        }
 *    }
 * </pre>
 * Using the alternative method to provide methods:
 * <pre>
 *    public class MyBlockEntity extends BlockEntity
 *           implements SimpleComponent, ManagedPeripheral
 *    {
 *        {@literal @}Override
 *        public String getComponentName() {
 *            return "fancy_thing";
 *        }
 *
 *        public String[] methods() {
 *            return new String[] {"greet"};
 *        }
 *
 *        public Object[] invoke(String method, Context context, Arguments args) {
 *            if ("greet".equals(method)) {
 *                return new Object[]{String.format("Hello, %s!", args.checkString(0))};
 *            } else {
 *                throw new RuntimeException(new NoSuchMethodException());
 *            }
 *        }
 *    }
 * </pre>
 */
public interface SimpleComponent {
    /**
     * The name the component should be made available as.
     * <br>
     * This is the name as seen in the <code>li.cil.oc.common.component.list()</code> in Lua, for
     * example. You'll want to make this short and descriptive. The convention
     * for component names is: all lowercase, underscores where necessary. Good
     * component names are for example: disk_drive, furnace, crafting_table.
     *
     * @return the component's name.
     */
    String getComponentName();

    /**
     * Use this to skip logic injection for the class this is implemented by.
     * <br>
     * For example, if you have a class transformer that injects logic from a
     * template class into your actual block entities, OC's class transformer
     * would complain when it finds the interface on the template class. That
     * warning can be suppressed by using this annotation on the template.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @SuppressWarnings("unused")
    @interface SkipInjection {
    }
}

package li.cil.oc.api.machine;

import java.io.Serial;

/**
 * Used to signal that the direct call limit for the current server tick has
 * been reached in {@link Machine#invoke(String, String, Object[])}.
 */
public class LimitReachedException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -5199996679654187159L;
}

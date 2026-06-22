package li.cil.oc.neoforge.integration.cbmultipart;

import codechicken.multipart.api.MultipartClientRegistry;
import codechicken.multipart.api.part.render.PartBakedModelRenderer;

public final class ClientMultipartInit {
    private ClientMultipartInit() {
    }

    public static void registerRenderers() {
        MultipartClientRegistry.register(MultipartRegistrations.CABLE_TYPE.get(), new CablePartRenderer());
        MultipartClientRegistry.register(MultipartRegistrations.PRINT_TYPE.get(), PartBakedModelRenderer.<PrintPart>simple());
    }
}

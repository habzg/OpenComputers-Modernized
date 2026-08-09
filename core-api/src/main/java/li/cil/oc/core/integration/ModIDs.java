package li.cil.oc.core.integration;

public final class ModIDs {
    private static ModResolver resolver = id -> false;

    public static void setModResolver(ModResolver r) {
        resolver = r;
    }

    public static boolean isModLoaded(String modId) {
        return resolver.isModLoaded(modId);
    }

    public static final String AppliedEnergistics2 = "ae2";
    public static final String AppliedMekanistics = "appmek";
    public static final String ComputerCraft = "computercraft";
    public static final String Create = "create";
    public static final String EnderIO = "enderio";
    public static final String EnderStorage = "enderstorage";
    public static final String CBMultipart = "cb_multipart";
    public static final String Mekanism = "mekanism";
    public static final String MoreRed = "morered";
    public static final String Minecraft = "Minecraft";
    public static final String OpenComputers = "opencomputers";
    public static final String ProjectRedTransmission = "projectred_transmission";
    public static final String RefinedStorage2 = "refinedstorage";
    public static final String RefinedStorageMekanism = "refinedstorage_mekanism_integration";
    public static final String RFTools = "rftoolsutility";
    public static final String Sable = "sable";
    public static final String Jade = "jade";
    public static final String TheOneProbe = "theoneprobe";
    public static final String TIS3D = "tis3d";
    public static final String TeamRebornEnergy = "team_reborn_energy";

    private ModIDs() {
    }
}

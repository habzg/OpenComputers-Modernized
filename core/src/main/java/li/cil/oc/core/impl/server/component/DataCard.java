package li.cil.oc.core.impl.server.component;

import com.google.common.hash.Hashing;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.ByteArrayOutputStream;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

public abstract class DataCard extends li.cil.oc.api.prefab.ManagedEnvironment implements DeviceInfo {
    private static final ThreadLocal<SecureRandom> SecureRandomInstance = ThreadLocal.withInitial(() -> {
        try {
            return SecureRandom.getInstance("SHA1PRNG");
        } catch (Exception e) {
            return new SecureRandom();
        }
    });
    public final ComponentConnector node = Network.newNode(this, Visibility.Neighbors)
            .withComponent("data", Visibility.Neighbors)
            .withConnector()
            .create();

    protected byte[] checkCost(Context context, Arguments args, double baseCost, double byteCost) {
        byte[] data = args.checkByteArray(0);
        if (data.length > Settings.get().dataCardHardLimit)
            throw new IllegalArgumentException("data size limit exceeded");
        double cost = baseCost + data.length * byteCost;
        if (!node.tryChangeBuffer(-cost))
            throw new RuntimeException("not enough energy");
        if (data.length > Settings.get().dataCardSoftLimit)
            context.pause(Settings.get().dataCardTimeout);
        return data;
    }

    protected void checkCost(double baseCost) {
        if (!node.tryChangeBuffer(-baseCost))
            throw new RuntimeException("not enough energy");
    }

    protected byte[] trivialCost(Context context, Arguments args) {
        return checkCost(context, args, Settings.get().dataCardTrivial, Settings.get().dataCardTrivialByte);
    }

    protected byte[] simpleCost(Context context, Arguments args) {
        return checkCost(context, args, Settings.get().dataCardSimple, Settings.get().dataCardSimpleByte);
    }

    protected byte[] complexCost(Context context, Arguments args) {
        return checkCost(context, args, Settings.get().dataCardComplex, Settings.get().dataCardComplexByte);
    }

    protected byte[] asymmetricCost(Context context, Arguments args) {
        return checkCost(context, args, Settings.get().dataCardAsymmetric, Settings.get().dataCardComplexByte);
    }

    @Callback(direct = true, doc = "function():number -- The maximum size of data that can be passed to other functions of the card.")
    public Object[] getLimit(Context context, Arguments args) {
        return ResultWrapper.result(Settings.get().dataCardHardLimit);
    }

    public static class Tier1 extends DataCard {
        private final Map<String, String> deviceInfo;

        public Tier1() {
            this.deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Processor, DeviceAttribute.Description, "Data processor card", DeviceAttribute.Vendor, "S.C. Ltd.", DeviceAttribute.Product, "SC01D H45h3r");
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return deviceInfo;
        }

        @Callback(direct = true, limit = 32, doc = "function(data:string):string -- Applies base64 encoding to the data.")
        public Object[] encode64(Context context, Arguments args) {
            return ResultWrapper.result((Object) Base64.encodeBase64(trivialCost(context, args)));
        }

        @Callback(direct = true, limit = 32, doc = "function(data:string):string -- Applies base64 decoding to the data.")
        public Object[] decode64(Context context, Arguments args) {
            return ResultWrapper.result((Object) Base64.decodeBase64(trivialCost(context, args)));
        }

        @Callback(direct = true, limit = 4, doc = "function(data:string):string -- Applies deflate compression to the data.")
        public Object[] deflate(Context context, Arguments args) {
            byte[] data = complexCost(context, args);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
            try (DeflaterOutputStream deos = new DeflaterOutputStream(baos)) {
                deos.write(data);
                deos.finish();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return ResultWrapper.result((Object) baos.toByteArray());
        }

        @Callback(direct = true, limit = 4, doc = "function(data:string):string -- Applies inflate decompression to the data.")
        public Object[] inflate(Context context, Arguments args) {
            byte[] data = complexCost(context, args);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
            try (InflaterOutputStream inos = new InflaterOutputStream(baos)) {
                inos.write(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return ResultWrapper.result((Object) baos.toByteArray());
        }

        @Callback(direct = true, limit = 32, doc = "function(data:string):string -- Computes CRC-32 hash of the data.")
        public Object[] crc32(Context context, Arguments args) {
            byte[] data = trivialCost(context, args);
            return ResultWrapper.result((Object) Hashing.crc32().hashBytes(data).asBytes());
        }

        @Callback(direct = true, limit = 8, doc = "function(data:string):string -- Computes MD5 hash of the data.")
        public Object[] md5(Context context, Arguments args) {
            byte[] data = simpleCost(context, args);
            // noinspection deprecation - this is using MD5 on purpose
            return ResultWrapper.result((Object) Hashing.md5().hashBytes(data).asBytes());
        }

        @Callback(direct = true, limit = 4, doc = "function(data:string):string -- Computes SHA2-256 hash of the data.")
        public Object[] sha256(Context context, Arguments args) {
            byte[] data = complexCost(context, args);
            return ResultWrapper.result((Object) Hashing.sha256().hashBytes(data).asBytes());
        }

        @Callback(direct = true, limit = 32, doc = "function(data:string):table -- Decode gzipped binary NBT data.")
        public Object[] decodeNBT(Context context, Arguments args) {
            byte[] data = complexCost(context, args);
            try {
                return ResultWrapper.result(net.minecraft.nbt.NbtIo.readCompressed(new java.io.ByteArrayInputStream(data), net.minecraft.nbt.NbtAccounter.unlimitedHeap()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Tier2 extends Tier1 {
        private final Map<String, String> deviceInfo;

        public Tier2() {
            this.deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Processor, DeviceAttribute.Description, "Data processor card", DeviceAttribute.Vendor, "S.C. Ltd.", DeviceAttribute.Product, "SC02D Cryptic");
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return deviceInfo;
        }

        @Override
        @Callback(direct = true, limit = 8, doc = "function(data:string[, hmacKey:string]):string -- Computes MD5 hash.")
        public Object[] md5(Context context, Arguments args) {
            if (args.count() > 1) {
                byte[] data = simpleCost(context, args);
                byte[] key = args.checkByteArray(1);
                return hash(data, key, "HmacMD5");
            }
            return super.md5(context, args);
        }

        @Override
        @Callback(direct = true, limit = 4, doc = "function(data:string[, hmacKey:string]):string -- Computes SHA2-256 hash.")
        public Object[] sha256(Context context, Arguments args) {
            if (args.count() > 1) {
                byte[] data = complexCost(context, args);
                byte[] key = args.checkByteArray(1);
                return hash(data, key, "HmacSHA256");
            }
            return super.sha256(context, args);
        }

        @Callback(direct = true, limit = 8, doc = "function(data:string, key:string, iv:string):string -- Encrypt data with AES.")
        public Object[] encrypt(Context context, Arguments args) {
            return crypt(context, args, Cipher.ENCRYPT_MODE);
        }

        @Callback(direct = true, limit = 8, doc = "function(data:string, key:string, iv:string):string -- Decrypt data with AES.")
        public Object[] decrypt(Context context, Arguments args) {
            return crypt(context, args, Cipher.DECRYPT_MODE);
        }

        @Callback(direct = true, limit = 4, doc = "function(len:number):string -- Generates secure random binary data.")
        public Object[] random(Context context, Arguments args) {
            int len = args.checkInteger(0);
            if (len <= 0 || len > 1024)
                throw new IllegalArgumentException("length must be in range [1..1024]");
            checkCost(Settings.get().dataCardComplex + Settings.get().dataCardComplexByte * len);
            byte[] target = new byte[len];
            SecureRandomInstance.get().nextBytes(target);
            return ResultWrapper.result((Object) target);
        }

        private Object[] crypt(Context context, Arguments args, int mode) {
            try {
                byte[] data = simpleCost(context, args);
                byte[] key = args.checkByteArray(1);
                if (key.length != 16)
                    throw new IllegalArgumentException("expected a 128-bit AES key");
                byte[] iv = args.checkByteArray(2);
                if (iv.length != 16)
                    throw new IllegalArgumentException("expected a 128-bit AES IV");
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(mode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
                return ResultWrapper.result((Object) cipher.doFinal(data));
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }

        private Object[] hash(byte[] data, byte[] key, String hmacMode) {
            try {
                Mac hmac = Mac.getInstance(hmacMode);
                hmac.init(new SecretKeySpec(key, hmacMode));
                return ResultWrapper.result((Object) hmac.doFinal(data));
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Tier3 extends Tier2 {
        private final Map<String, String> deviceInfo;

        public Tier3() {
            this.deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Processor, DeviceAttribute.Description, "Data processor card", DeviceAttribute.Vendor, "S.C. Ltd.", DeviceAttribute.Product, "SC03D Signer");
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return deviceInfo;
        }

        @Callback(direct = true, limit = 1, doc = "function([bitLen:number]):userdata, userdata -- Generates key pair.")
        public Object[] generateKeyPair(Context context, Arguments args) {
            try {
                checkCost(Settings.get().dataCardAsymmetric);
                int bitLen = args.optInteger(0, 384);
                if (bitLen != 256 && bitLen != 384)
                    throw new IllegalArgumentException("invalid key length, must be 256 or 384");
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
                kpg.initialize(bitLen, SecureRandomInstance.get());
                KeyPair kp = kpg.generateKeyPair();
                return ResultWrapper.result(new ECUserdata(kp.getPublic()), new ECUserdata(kp.getPrivate()));
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }

        @Callback(direct = true, limit = 8, doc = "function(data:string, type:string):userdata -- Restores key from string.")
        public Object[] deserializeKey(Context context, Arguments args) {
            byte[] data = simpleCost(context, args);
            String t = args.checkString(1);
            return ResultWrapper.result(new ECUserdata(ECUserdata.deserializeKey(t, data)));
        }

        @Callback(direct = true, limit = 1, doc = "function(priv:userdata, pub:userdata):string -- Generates a shared key.")
        public Object[] ecdh(Context context, Arguments args) {
            try {
                checkCost(Settings.get().dataCardAsymmetric);
                ECUserdata privUd = checkUserdata(args, 0, false);
                ECUserdata pubUd = checkUserdata(args, 1, true);
                KeyAgreement ka = KeyAgreement.getInstance("ECDH");
                ka.init(privUd.value);
                ka.doPhase(pubUd.value, true);
                return ResultWrapper.result((Object) ka.generateSecret());
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }

        @Callback(direct = true, limit = 1, doc = "function(data:string, key:userdata[, sig:string]):string or boolean -- Signs or verifies.")
        public Object[] ecdsa(Context context, Arguments args) {
            try {
                byte[] data = asymmetricCost(context, args);
                ECUserdata keyUd = checkUserdata(args);
                byte[] sig = args.optByteArray(2, null);
                Signature sign = Signature.getInstance("SHA256withECDSA");
                if (sig != null) {
                    if (keyUd.value instanceof PublicKey) {
                        sign.initVerify((PublicKey) keyUd.value);
                        sign.update(data);
                        return ResultWrapper.result(sign.verify(sig));
                    }
                    throw new IllegalArgumentException("public key expected");
                } else {
                    if (keyUd.value instanceof PrivateKey) {
                        sign.initSign((PrivateKey) keyUd.value);
                        sign.update(data);
                        return ResultWrapper.result((Object) sign.sign());
                    }
                    throw new IllegalArgumentException("private key expected");
                }
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }

        private ECUserdata checkUserdata(Arguments args) {
            return checkUserdata(args, 1, null);
        }

        private ECUserdata checkUserdata(Arguments args, int i, Boolean isPublic) {
            Object value = args.checkAny(i);
            if (value instanceof ECUserdata ec) {
                if (isPublic == null || isPublic == ec.isPublic()) return ec;
                throw new IllegalArgumentException((isPublic ? "public" : "private") + " key expected at " + (i + 1));
            }
            if (value == null)
                throw new IllegalArgumentException("bad argument #" + (i + 1) + " (userdata expected, got no value)");
            throw new IllegalArgumentException("bad argument #" + (i + 1) + " (userdata expected, got " + value.getClass().getName() + ")");
        }
    }

    public static class ECUserdata extends li.cil.oc.api.prefab.AbstractValue {
        public static final String PrivateTypeName = "ec-private";
        public static final String PublicTypeName = "ec-public";
        public Key value;

        @SuppressWarnings("unused")
        public ECUserdata() {
            this(null);
        }

        public ECUserdata(Key value) {
            this.value = value;
        }

        public static Key deserializeKey(String typeName, byte[] data) {
            try {
                if (PrivateTypeName.equals(typeName))
                    return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(data));
                if (PublicTypeName.equals(typeName))
                    return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(data));
                throw new IllegalArgumentException("invalid key type, must be ec-public or ec-private");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public boolean isPublic() {
            return value instanceof ECPublicKey;
        }

        public String keyType() {
            return isPublic() ? PublicTypeName : PrivateTypeName;
        }

        @Callback(direct = true, doc = "function():boolean -- Returns whether key is public.")
        public Object[] isPublic(Context context, Arguments args) {
            return ResultWrapper.result(isPublic());
        }

        @Callback(direct = true, doc = "function():string -- Returns type of key.")
        public Object[] keyType(Context context, Arguments args) {
            return ResultWrapper.result(keyType());
        }

        @Callback(direct = true, limit = 4, doc = "function():string -- Returns string representation.")
        public Object[] serialize(Context context, Arguments args) {
            return ResultWrapper.result((Object) value.getEncoded());
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            String keyType = nbt.getString("Type");
            byte[] data = nbt.getByteArray("Data");
            value = deserializeKey(keyType, data);
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            nbt.putString("Type", keyType());
            nbt.putByteArray("Data", value.getEncoded());
        }
    }
}

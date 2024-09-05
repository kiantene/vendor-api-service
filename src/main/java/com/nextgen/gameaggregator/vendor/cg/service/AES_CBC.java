package com.nextgen.gameaggregator.vendor.cg.service;

import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class AES_CBC {

    // The default block size
    public static int blockSize = 16;
    PaddedBufferedBlockCipher encryptCipher = null;
    PaddedBufferedBlockCipher decryptCipher = null;
    // The key
    byte[] key = null;
    // The initialization vector needed by the CBC mode
    byte[] IV = null;

    public AES_CBC(byte[] keyBytes, byte[] iv) {
        // get the key
        key = new byte[keyBytes.length];
        System.arraycopy(keyBytes, 0, key, 0, keyBytes.length);

        // get the IV
        IV = new byte[blockSize];
        System.arraycopy(iv, 0, IV, 0, iv.length);
    }

    public void InitCiphers() throws Exception {
        BlockCipherPadding padding = new PKCS7Padding();
        // create the ciphers
        // AES block cipher in CBC mode with padding
        encryptCipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(
                new AESEngine()), padding);

        decryptCipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(
                new AESEngine()), padding);

        // create the IV parameter
        ParametersWithIV parameterIV = new ParametersWithIV(new KeyParameter(
                key), IV);

        encryptCipher.init(true, parameterIV);
        decryptCipher.init(false, parameterIV);
    }

    public void ResetCiphers() {
        if (encryptCipher != null)
            encryptCipher.reset();
        if (decryptCipher != null)
            decryptCipher.reset();
    }

    public byte[] CBCEncrypt(byte[] encrypted)
            throws DataLengthException, IllegalStateException,
            InvalidCipherTextException, UnsupportedEncodingException {

        byte[] buffer = new byte[encryptCipher.getOutputSize(encrypted.length)];
        int len = encryptCipher.processBytes(encrypted, 0, encrypted.length, buffer, 0);
        len += encryptCipher.doFinal(buffer, len);
        byte[] out = Arrays.copyOfRange(buffer, 0, len);

        return out;

    }

    public byte[] CBCDecrypt(byte[] decrypted)
            throws DataLengthException, IllegalStateException,
            InvalidCipherTextException, UnsupportedEncodingException {

        byte[] buffer = new byte[decryptCipher.getOutputSize(decrypted.length)];
        int len = decryptCipher.processBytes(decrypted, 0, decrypted.length, buffer, 0);
        len += decryptCipher.doFinal(buffer, len);
        byte[] out = Arrays.copyOfRange(buffer, 0, len);

        return out;

    }
}

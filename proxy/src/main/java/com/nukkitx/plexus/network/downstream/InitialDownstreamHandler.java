package com.nukkitx.plexus.network.downstream;

import com.nimbusds.jwt.SignedJWT;
import com.nukkitx.plexus.network.session.ProxyPlayerSession;
import com.nukkitx.plexus.utils.EncryptionUtils;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.session.BedrockSession;
import lombok.RequiredArgsConstructor;

import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Base64;

@RequiredArgsConstructor
public class InitialDownstreamHandler implements BedrockPacketHandler {
    private final ProxyPlayerSession player;

    @Override
    public boolean handle(ServerToClientHandshakePacket packet) {
        try {
            SignedJWT saltJwt = SignedJWT.parse(packet.getJwt());
            URI x5u = saltJwt.getHeader().getX509CertURL();
            ECPublicKey serverKey = EncryptionUtils.generateKey(x5u.toASCIIString());
            byte[] encryptionKey = EncryptionUtils.getServerKey(player.getProxyKeyPair(), serverKey,
                    Base64.getDecoder().decode(saltJwt.getJWTClaimsSet().getStringClaim("salt")));
            player.getDownstream().enableEncryption(new SecretKeySpec(encryptionKey, "AES"));
        } catch (ParseException | NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        ClientToServerHandshakePacket clientToServerHandshake = new ClientToServerHandshakePacket();
        player.getDownstream().sendPacketImmediately(clientToServerHandshake);
        return true;
    }

    public boolean handle(StartGamePacket packet) {
        player.setDimensionId(packet.getDimensionId());

        return false;
    }
}
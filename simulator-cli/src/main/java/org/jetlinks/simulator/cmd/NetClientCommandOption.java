package org.jetlinks.simulator.cmd;

import io.vertx.core.net.*;
import picocli.CommandLine;

import java.io.File;

public class NetClientCommandOption extends NetClientOptions {

    @CommandLine.Option(names = {"--interface"}, paramLabel = "interface", description = "Network Interface", order = 40, completionCandidates = NetworkInterfaceCompleter.class)
    public void setLocalAddress0(String localAddress) {
        super.setLocalAddress(localAddress);
    }

    @CommandLine.Option(names = {"--ssl"}, description = "Enable SSL", order = 50, defaultValue = "false")
    public void setSSL0(boolean trustAll) {
        setSsl(trustAll);
    }

    @CommandLine.Option(names = {"--trustAll"}, description = "Trust All Server", order = 51, defaultValue = "true")
    public void setTrustAll0(boolean trustAll) {
        setTrustAll(trustAll);
    }

    @CommandLine.Option(names = {"--key-path"}, description = "PEM format key path", order = 52)
    public void setKeyPath(File path) {
        keyOptions().addKeyPath(path.getPath());
    }

    @CommandLine.Option(names = {"--cert-path"}, description = "PEM format cert path", order = 53)
    public void setCertPath(File path) {
        keyOptions().addCertPath(path.getPath());
    }

    @CommandLine.Option(names = {"--trust-path"}, description = "PEM format trust cert path", order = 54)
    public void setTrustPath(File path) {
        trustOptions().addCertPath(path.getPath());
    }

    public void apply(TCPSSLOptions another) {
        another.setSsl(isSsl());
        another.setPemTrustOptions(getPemTrustOptions());
        another.setPemKeyCertOptions(getPemKeyCertOptions());
        if (another instanceof ClientOptionsBase) {
            ((ClientOptionsBase) another).setLocalAddress(getLocalAddress());
        }
    }

    public PemKeyCertOptions keyOptions() {
        if (getPemKeyCertOptions() == null) {
            setPemKeyCertOptions(new PemKeyCertOptions());
        }
        return getPemKeyCertOptions();
    }

    public PemTrustOptions trustOptions() {
        if (getPemTrustOptions() == null) {
            setPemTrustOptions(new PemTrustOptions());
        }
        return getPemTrustOptions();
    }

}

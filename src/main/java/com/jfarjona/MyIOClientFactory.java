package com.jfarjona;

import org.xbill.DNS.DefaultIoClient;
import org.xbill.DNS.io.DefaultIoClientFactory;
import org.xbill.DNS.io.TcpIoClient;
import org.xbill.DNS.io.UdpIoClient;

public class MyIOClientFactory extends DefaultIoClientFactory {


    @Override
    public TcpIoClient createOrGetTcpClient() {
        return new DefaultIoClient();
    }

    @Override
    public UdpIoClient createOrGetUdpClient() {
        return new DefaultIoClient();
    }
}

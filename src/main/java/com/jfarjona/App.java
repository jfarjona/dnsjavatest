package com.jfarjona;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hello world!
 */
public class App {

    static final         List<String>                DEFAULT_DNS_SERVERS =
            List.of("8.8.8.8", "8.8.4.4", "76.76.2.0", "76.76.10.0", "9.9.9.9", "149.112.112.112",
                    "208.67.222.222",
                    "208.67.220.220", "1.1.1.1", "1.0.0.1", "94.140.14.14", "94.140.15.15", "185.228.168.9",
                    "185.228.169.9", "8.26.56.26", "8.20.247.20", "64.6.64.6", "64.6.65.6");
    private static final int                         MAX_TRIES           = 10;
    private static final Logger                      log                 = LoggerFactory.getLogger(App.class);
    static               Map<String, SimpleResolver> resolverMap         = new ConcurrentHashMap<>();
    final                String[]                    domains             = {
            "google.com", "amazon.com", "microsoft.com", "apple.com", "facebook.com",
            "tesla.com", "netflix.com", "adobe.com", "spotify.com", "twitter.com",
            "linkedin.com", "paypal.com", "github.com", "dropbox.com", "oracle.com",
            "zoom.us", "salesforce.com", "intel.com", "hp.com", "ibm.com",
            "ebay.com", "walmart.com", "target.com", "costco.com", "bestbuy.com",
            "homedepot.com", "lowes.com", "krispykreme.com", "starbucks.com", "mcdonalds.com",
            "burgerking.com", "kfc.com", "wendys.com", "dunkindonuts.com", "chipotle.com",
            "pizzahut.com", "dominos.com", "papa-johns.com", "subway.com", "arbys.com",
            "tacobell.com", "olivegarden.com", "redlobster.com", "cheesecakefactory.com", "outback.com",
            "texasroadhouse.com", "pandaexpress.com", "fiveguys.com", "ww.com", "ups.com",
            "fedex.com", "dhl.com", "usps.com", "cisco.com", "verizon.com",
            "att.com", "tmobile.com", "sprint.com", "qualcomm.com", "nvidia.com",
            "amd.com", "lenovo.com", "samsung.com", "lg.com", "sony.com",
            "panasonic.com", "philips.com", "bose.com", "jbl.com",
            "nike.com", "adidas.com", "underarmour.com", "puma.com", "reebok.com",
            "newbalance.com", "converse.com", "vans.com", "skechers.com", "columbia.com",
            "patagonia.com", "northface.com", "canada-goose.com", "levis.com", "guess.com",
            "ralphlauren.com", "gap.com", "oldnavy.com", "zara.com",
            "forever21.com", "urbanoutfitters.com", "americaneagle.com", "abercrombie.com"};

    static MyIOClientFactory myIoClientFactory = new MyIOClientFactory();

    public static Resolver makeResolver(String resolver) throws UnknownHostException {
        /**
        SimpleResolver answ = resolverMap.computeIfAbsent(resolver,
                key -> {
                    try {
                        SimpleResolver res = new SimpleResolver(key);
                        res.setLocalAddress(new InetSocketAddress(0));
                        res.setIoClientFactory(myIoClientFactory);
                        res.setTCP(true);
                        return res;
                    } catch (Exception ex) {
                        return null;
                    }
                });
          return answ;
         */
        SimpleResolver res = new SimpleResolver(resolver);
        res.setLocalAddress(new InetSocketAddress(0));
        res.setIoClientFactory(myIoClientFactory);
        res.setTCP(true);
        return res;

    }

    private static int tryWhile(Lookup l) {
        return tryWhile(l, MAX_TRIES);
    }

    private static int tryWhile(Lookup l, int maxTries) {
        int tries = 0;
        do {
            try {
                if (tries != 0) {
                    Thread.sleep(500);
                }
            } catch (InterruptedException ex) {
                log.error("Cannot sleep!", ex);
            }
            l.run();
            log.info("Try #{} - result: {} - error: {} ", tries, l.getResult(), l.getErrorString());
            tries++;
        } while (l.getResult() == Lookup.TRY_AGAIN && tries < maxTries);
        return (tries-1);
    }

    public static void main(String[] args) {
        System.out.println("Multi-Thread DNS Java Test");
        App me = new App();
        me.run();
    }

    public static <T> T pickRandomElement(List<T> list) {
        Random random = new Random();
        return list.get(random.nextInt(list.size()));
    }

    private int totTries = 0;

    public void run() {
        List<String> all = Arrays.asList(domains);
        long total = all
                .parallelStream()
                .filter(domain -> {
                    Thread
                            .currentThread()
                            .setName(domain);
                    boolean worked = false;
                    try {
                        List<ARecord> records = new ArrayList<>();
                        Lookup        l       = new Lookup(domain, Type.A, DClass.IN);
                        l.setCache(null);
                        l.setResolver(makeResolver(pickRandomElement(DEFAULT_DNS_SERVERS)));
                        int tries= tryWhile(l);
                        totTries += tries;
                        if (l.getResult() == Lookup.SUCCESSFUL) {
                            StringBuilder b = new StringBuilder();

                            for (Record mx : l.getAnswers()) {
                                b
                                        .append(mx.toString())
                                        .append("\n");
                                records.add((ARecord) mx);
                            }
                            log.info("Found existing records for {} to be {}", domain, b);
                            worked = true;
                        }
                    } catch (Exception ex) {
                        log.error("Could not find the MX record for {}", domain, ex);
                    }
                    return worked;
                })
                .count();
        int parallel = totTries;
        log.info("Parallel records for {} out of {} -- Total tries: {}", total, domains.length, totTries);

        totTries = 0;

        // Now in sequencial
        total = all
                .stream()
                .filter(domain -> {
                    Thread
                            .currentThread()
                            .setName(domain);
                    boolean worked = false;
                    try {
                        List<ARecord> records = new ArrayList<>();
                        Lookup        l       = new Lookup(domain, Type.A, DClass.IN);
                        l.setCache(null);
                        l.setResolver(makeResolver(pickRandomElement(DEFAULT_DNS_SERVERS)));
                        int tries= tryWhile(l);
                        totTries += tries;
                        if (l.getResult() == Lookup.SUCCESSFUL) {
                            StringBuilder b = new StringBuilder();

                            for (Record mx : l.getAnswers()) {
                                b
                                        .append(mx.toString())
                                        .append("\n");
                                records.add((ARecord) mx);
                            }
                            log.info("Found existing records for {} to be {}", domain, b);
                            worked = true;
                        }
                    } catch (Exception ex) {
                        log.error("Could not find the MX record for {}", domain, ex);
                    }
                    return worked;
                })
                .count();
        log.info("Sequencial records for {} out of {} -- Total tries: {} vs. parallel: {}", total, domains.length, totTries, parallel);


    }
}

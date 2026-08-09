package li.cil.oc.core.impl.util;

import com.google.common.net.InetAddresses;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InternetFilteringRule {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternetFilteringRule.class);
    private static final InternetFilteringRule[] defaultRules = {
            new InternetFilteringRule("deny private"),
            new InternetFilteringRule("deny bogon"),
            new InternetFilteringRule("allow all")
    };
    private static final InetAddressRange[] bogonMatchingRules = {
            InetAddressRange.parse("0.0.0.0", "8"), InetAddressRange.parse("10.0.0.0", "8"),
            InetAddressRange.parse("100.64.0.0", "10"), InetAddressRange.parse("127.0.0.0", "8"),
            InetAddressRange.parse("169.254.0.0", "16"), InetAddressRange.parse("172.16.0.0", "12"),
            InetAddressRange.parse("192.0.0.0", "24"), InetAddressRange.parse("192.0.2.0", "24"),
            InetAddressRange.parse("192.168.0.0", "16"), InetAddressRange.parse("198.18.0.0", "15"),
            InetAddressRange.parse("198.51.100.0", "24"), InetAddressRange.parse("203.0.113.0", "24"),
            InetAddressRange.parse("224.0.0.0", "3"), InetAddressRange.parse("::", "128"),
            InetAddressRange.parse("::1", "128"), InetAddressRange.parse("::ffff:0:0", "96"),
            InetAddressRange.parse("::", "96"), InetAddressRange.parse("100::", "64"),
            InetAddressRange.parse("2001:10::", "28"), InetAddressRange.parse("2001:db8::", "32"),
            InetAddressRange.parse("fc00::", "7"), InetAddressRange.parse("fe80::", "10"),
            InetAddressRange.parse("fec0::", "10"), InetAddressRange.parse("ff00::", "8"),
            InetAddressRange.parse("64:ff9b::", "96"), InetAddressRange.parse("64:ff9b:1::", "96"),
            InetAddressRange.parse("3fff::", "20"), InetAddressRange.parse("2002::", "16"),
            InetAddressRange.parse("2001:0::", "32")
    };
    private BiFunction<InetAddress, String, Boolean> validator;
    private boolean _invalid = false;

    public InternetFilteringRule(String ruleString) {
        try {
            String[] ruleParts = ruleString.split(" ");
            String action = ruleParts[0];
            switch (action) {
                case "allow":
                case "deny": {
                    boolean value = action.equals("allow");
                    List<BiFunction<InetAddress, String, Boolean>> predicates = new ArrayList<>();
                    for (int i = 1; i < ruleParts.length; i++) {
                        String f = ruleParts[i];
                        String[] filter = f.split(":", 2);
                        switch (filter[0]) {
                            case "default":
                                if (!value) {
                                    predicates.add((addr, host) -> false);
                                } else {
                                    predicates.add((inetAddress, host) -> {
                                        for (InternetFilteringRule r : defaultRules) {
                                            Boolean result = r.apply(inetAddress, host);
                                            if (result != null) return result;
                                        }
                                        return false;
                                    });
                                }
                                break;
                            case "private":
                                predicates.add((inetAddress, host) ->
                                        inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() ||
                                                inetAddress.isLinkLocalAddress() || inetAddress.isSiteLocalAddress());
                                break;
                            case "bogon":
                                predicates.add((inetAddress, host) -> {
                                    for (InetAddressRange range : bogonMatchingRules) {
                                        if (range.matches(inetAddress)) return true;
                                    }
                                    return false;
                                });
                                break;
                            case "ipv4":
                                predicates.add((inetAddress, host) -> inetAddress instanceof Inet4Address);
                                break;
                            case "ipv6":
                                predicates.add((inetAddress, host) -> inetAddress instanceof Inet6Address);
                                break;
                            case "ipv4-embedded-ipv6":
                                predicates.add((inetAddress, host) ->
                                        inetAddress instanceof Inet6Address &&
                                                InetAddresses.hasEmbeddedIPv4ClientAddress((Inet6Address) inetAddress));
                                break;
                            case "domain":
                                String domain = filter[1];
                                InetAddress[] addresses = InetAddress.getAllByName(domain);
                                predicates.add((inetAddress, host) -> {
                                    if (host.equals(domain)) return true;
                                    for (InetAddress a : addresses) {
                                        if (a.equals(inetAddress)) return true;
                                    }
                                    return false;
                                });
                                break;
                            case "ip":
                                String[] ipStringParts = filter[1].split("/", 2);
                                if (ipStringParts.length == 2) {
                                    InetAddressRange ipRange = InetAddressRange.parse(ipStringParts[0], ipStringParts[1]);
                                    predicates.add((inetAddress, host) -> ipRange.matches(inetAddress));
                                } else {
                                    InetAddress ipAddress = InetAddresses.forString(ipStringParts[0]);
                                    predicates.add((inetAddress, host) -> ipAddress.equals(inetAddress));
                                }
                                break;
                            case "all":
                                break;
                        }
                    }
                    this.validator = (inetAddress, host) -> {
                        for (BiFunction<InetAddress, String, Boolean> p : predicates) {
                            if (!p.apply(inetAddress, host)) return null;
                        }
                        return value;
                    };
                    break;
                }
                case "removeme":
                    this.validator = (addr, host) -> null;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown rule action: " + action);
            }
        } catch (Throwable t) {
            LOGGER.error("Invalid Internet filteringRules rule in configuration: \"{}\".", ruleString, t);
            _invalid = true;
            this.validator = (addr, host) -> false;
        }
    }

    public boolean invalid() {
        return _invalid;
    }

    public Boolean apply(InetAddress inetAddress, String host) {
        return validator.apply(inetAddress, host);
    }
}

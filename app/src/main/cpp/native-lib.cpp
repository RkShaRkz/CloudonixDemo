#include <jni.h>
#include <string>

// Required for parsing the network address from struct sockaddr into a std::string
#include <stdio.h>
#include <ifaddrs.h>
#include <netdb.h>

// Required for splitting the std::string representing address into octets
#include <iostream>
#include <string>
#include <sstream>
#include <vector>

/**
 * Utility method declarations here
 */
bool isIPv6GlobalUnicast(const std::string &addr);

bool isIPv4Private(const std::string &addr);

bool isIPv4PrivateCase1(const std::string &addr);

bool isIPv4PrivateCase2(const std::string &addr);

bool isIPv4PrivateCase3(const std::string &addr);

bool isIPv4PrivateLinkLocal(const std::string &addr);

extern "C" JNIEXPORT jstring

JNICALL
Java_com_example_cloudonix_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

JNICALL
extern "C"
jstring Java_com_example_cloudonix_MainActivity_getifaddrs(
        JNIEnv *env,
        jobject /* this */) {

    jstring retVal = nullptr;
    // To make things simple, this method **will not** instantiate Java objects
    // and return an int value. Instead, it will only return a jstring.
    //
    // It will first invoke the necessary getifaddrs() native method,
    // sift through it's returned values, filtering out what's interesting to
    // return, and just return that one address as a string. It won't bother
    // converting the ifaddr structs to actual classes or whatnot.
    //
    // Moreover, it will very crudely check whether the address is actually a private
    // address. It will also presume that, if the IP starts with "10." that it belongs to
    // a 10.x.x.x subnet without actually checking whether the IP is valid or not.

    /**
     * 1. If an IPv6 address is available, that is from the global unicast range - as per this document, return that address.
     * 2. If an IPv4 address is available that is public - i.e. not a “private address” according to the table here, or “link local” according to the specification here, return that address.
     * 3. Otherwise, return any one of the IPv4 addresses available.
     */

    // IPv6 global unicast range addresses start with 2000: so check for them

    /**
     * IPv4 private addresses can be one of the following three kinds:
     * 1. 10.0.0.0 - 10.255.255.255
     * 2. 172.16.0.0 - 172.31.255.255
     * 3. 192.168.0.0 - 192.168.255.255
     */

    /**
     * IPv4 link local addresses look like this:
     * 169.254.0.0 – 169.254.255.255
     */

    // First things first, fire the JNI call
    struct ifaddrs *ifaddr, *ifaddr_iterator;
    char addr[INET6_ADDRSTRLEN];

    if (getifaddrs(&ifaddr) == -1) {
        perror("getifaddrs error");
    }

    for (ifaddr_iterator = ifaddr;
         ifaddr_iterator != NULL; ifaddr_iterator = ifaddr_iterator->ifa_next) {
        // Iterate IPv6 addresses first
        if ((ifaddr_iterator->ifa_addr) && (ifaddr_iterator->ifa_addr->sa_family == AF_INET6)) {
            struct sockaddr_in6 *in6 = (struct sockaddr_in6 *) ifaddr_iterator->ifa_addr;   //TODO remove me
            getnameinfo(
                    ifaddr_iterator->ifa_addr,
                    sizeof(struct sockaddr_in6),
                    addr,
                    sizeof(addr),
                    NULL,
                    0,
                    NI_NUMERICHOST
            );
            std::string address(addr);
            if (isIPv6GlobalUnicast(address)) {
                retVal = env->NewStringUTF(address.c_str());
            }
        }
    }

    // If we found nothing, keep looking...
    if (retVal == nullptr) {
        for (ifaddr_iterator = ifaddr;
             ifaddr_iterator != NULL; ifaddr_iterator = ifaddr_iterator->ifa_next) {
            // Iterate IPv4 public addresses second
            if ((ifaddr_iterator->ifa_addr) && (ifaddr_iterator->ifa_addr->sa_family == AF_INET)) {
                struct sockaddr_in *in = (struct sockaddr_in *) ifaddr_iterator->ifa_addr;   //TODO remove me
                getnameinfo(
                        ifaddr_iterator->ifa_addr,
                        sizeof(struct sockaddr_in),
                        addr,
                        sizeof(addr),
                        NULL,
                        0,
                        NI_NUMERICHOST
                );
                std::string address(addr);
                if (isIPv4Private(address)) {
                    retVal = env->NewStringUTF(address.c_str());
                }
            }
        }
    }

    // If we found nothing, keep looking...
    if (retVal == nullptr) {
        //Fallback case, return any IPv4 address we found. So we can return the first one.
        for (ifaddr_iterator = ifaddr;
             ifaddr_iterator != NULL; ifaddr_iterator = ifaddr_iterator->ifa_next) {
            if ((ifaddr_iterator->ifa_addr) && (ifaddr_iterator->ifa_addr->sa_family == AF_INET)) {
                struct sockaddr_in *in = (struct sockaddr_in *) ifaddr_iterator->ifa_addr;   //TODO remove me
                getnameinfo(
                        ifaddr_iterator->ifa_addr,
                        sizeof(struct sockaddr_in6),
                        addr,
                        sizeof(addr),
                        NULL,
                        0,
                        NI_NUMERICHOST
                );

                std::string address(addr);
                retVal = env->NewStringUTF(address.c_str());
            }
        }
    }

    // Cleanup ifaddrs
    freeifaddrs(ifaddr);

    return retVal;
}

bool isIPv6GlobalUnicast(const std::string &addr) {
    return addr.find("2000:") == 0;
}

bool isIPv4Private(const std::string &addr) {
    return isIPv4PrivateCase1(addr)
           || isIPv4PrivateCase2(addr)
           || isIPv4PrivateCase3(addr)
           || isIPv4PrivateLinkLocal(addr);
}

bool isIPv4PrivateCase1(const std::string &addr) {
    // If the address starts with "10." it's on the 10.x.x.x. subnet, therefore it's private
    return addr.find("10.") == 0;
}

bool isIPv4PrivateCase2(const std::string &addr) {
    // Check for 172.16.0.0 - 172.31.255.255 case
    // We should actually extract the 'octets' or whatever the name was...
    // and then check if the first one is 172, and the second one is in 16-31 range

    // Split the string by '.'
    std::istringstream iss(addr);
    std::string token;
    std::vector<std::string> parts;
    while (std::getline(iss, token, '.')) {
        parts.push_back(token);
    }

    // Check if the first part is equal to 172
    if (parts[0] == "172") {
        // Check if the second part is in the range [16, 31]
        int num = std::stoi(parts[1]);
        if (num >= 16 && num <= 31) {
            return true;
        }
    }

    return false;
}

bool isIPv4PrivateCase3(const std::string &addr) {
    // Check for 192.168.0.0 - 192.168.255.255 case
    // We should actually extract the 'octets' or whatever the name was...
    // and then check if the first one is 192, and the second one is in 168 range

    // Split the string by '.'
    std::istringstream iss(addr);
    std::string token;
    std::vector<std::string> parts;
    while (std::getline(iss, token, '.')) {
        parts.push_back(token);
    }

    // Check if the first part is equal to 192
    // i figure casting it to int and checking is kinda the same as strcmp()ing it so...
    int num = std::stoi(parts[0]);
    if (num == 192) {
        // Check if the second part is 168
        num = std::stoi(parts[1]);
        if (num == 168) {
            return true;
        }
    }

    return false;
}

bool isIPv4PrivateLinkLocal(const std::string &addr) {
    // Check for 169.254.0.0 – 169.254.255.255
    // We should actually extract the 'octets' or whatever the name was...
    // and then check if the first one is 169, and the second one is 254

    // Split the string by '.'
    std::istringstream iss(addr);
    std::string token;
    std::vector<std::string> parts;
    while (std::getline(iss, token, '.')) {
        parts.push_back(token);
    }

    // Check if the first part is equal to 169
    // i figure casting it to int and checking is kinda the same as strcmp()ing it so...
    int num = std::stoi(parts[0]);
    if (num == 169) {
        // Check if the second part is 254
        num = std::stoi(parts[1]);
        if (num == 254) {
            return true;
        }
    }

    return false;
}

//
//  main.cpp
//  mto_tester
//
//  Created by Joris Borgdorff on 5/2/13.
//  Copyright (c) 2013 Joris Borgdorff. All rights reserved.
//

#include "../net/csocket.h"
#include "../net/async_cservice.h"
#include "../util/option_parser.hpp"

#include <iostream>
#include <cstring>
#include <cstdlib>
#include <arpa/inet.h>

using namespace std;

int failed = 0, total = 0;

void assert(bool passed, string msg)
{
    total++;
    if (passed)
    {
        cout << "[ OK     ] " << msg << endl;
    }
    else
    {
        cout << "[ FAILED ] " << msg << endl;
        failed++;
    }
}

template <typename T>
void assertEquals(T received, T expected, string msg)
{
    bool ok = (received == expected);
    if (!ok)
    {
        stringstream ss;
        ss << msg << " (" << expected << " expected but " << received << " given)";
        msg = ss.str();
    }
    assert(ok, msg);
}
template <>
void assertEquals<const char *>(const char *received, const char *expected, string msg)
{
    bool ok = strcmp(received, expected) == 0;
    if (!ok)
    {
        stringstream ss;
        ss << "(" << expected << " expected but " << received << " given) " << msg;
        msg = ss.str();
    }
    assert(ok, msg);
}
template <typename T>
ostream& vectorStr(ostream& os, const vector<T>& v)
{
    typename vector<T>::const_iterator it = v.begin();
    os << "[";
    if (it != v.end())
        os << *it;
    ++it;
    
    for (; it != v.end(); ++it)
    {
        os << ", " << *it;
    }
    os << "]";
    return os;
}

template <>
void assertEquals<vector<string> >(vector<string> received, vector<string> expected, string msg)
{
    bool ok = received == expected;
    if (!ok)
    {
        stringstream ss;
        ss << "(";
        vectorStr(ss, expected) << " expected but ";
        vectorStr(ss, received) << " given) " << msg;
        msg = ss.str();
    }
    assert(ok, msg);
}
void assertFalse(string msg) { assert(false, msg); }
void assertTrue(string msg) { assert(true, msg); }

class async_ss : public muscle::async_acceptlistener, muscle::async_recvlistener, muscle::async_sendlistener
{
public:
    muscle::async_service * const service;
    int i;
    const muscle::ServerSocket *ss;
    const muscle::ClientSocket *cs;
    const muscle::ClientSocket *as;
    async_ss(muscle::async_service *s, const muscle::ServerSocket *ssock) : service(s), i(0), ss(ssock) {}
    
    virtual void async_accept(size_t code, int flag, const muscle::ClientSocket *sock)
    {
        i++;
        if (i == 1) {
            cout << "connected socket" << endl;
            char *s = strdup("my string");
            cs = sock;
            cs->async_send(3, s, strlen(s)+1, this);
        } else {
            char *s = new char[20];
            cout << "accepted socket" << endl;
            as = sock;
            as->async_recv(4, s, 20, this);
            service->erase(ss);
        }
    }
    
    virtual void async_report_error(size_t,int,const muscle::muscle_exception&) {}
    virtual bool async_received(size_t code, int flag, void *data, size_t len, int final)
    {
        if (final == -1)
            assertFalse("Error during receiving data");
        else if (len > 0)
            cout << "received " << string((char *)data) << endl;
        
        delete [] (char *)data;
        
        return false;
    }
    virtual void async_sent(size_t code, int flag, void *data, size_t len, int final)
    {
        if (final == 1)
        {
            cout << "sent " << string((char *)data) << endl;
            free(data);
        }
        if (final == -1)
            assertFalse("Error during sending data");
    }
    virtual void async_done(size_t code, int flag)
    {
        if (flag == 3) delete cs;
        if (flag == 4) delete as;
    }
};

class async_func_call : public muscle::async_function
{
public:
    muscle::async_service * const service;
    const bool first;
    async_func_call(muscle::async_service *s, bool first) : service(s), first(first) {}
    virtual void async_report_error(size_t,int,const muscle::muscle_exception&) {}
    virtual void async_execute(size_t code, int flag, void *data)
    {
        if (first)
        {
            assertEquals(code, 1ul, "first call to async_service");
            assertEquals("test string", (const char *)data, "user data passes unmodified");
            assertEquals(flag, 1, "flag passes unmodified");
            service->erase_timer(code);
        }
        else
            assertEquals(code, 2ul, "second call to async_service");
    }
};

void testTime()
{
    cout << endl << "time" << endl << endl;
    
    muscle::time t = muscle::time::now();
    assert(t.is_past(), "current time is past");
    assert(!muscle::time::far_future().is_past(), "far future is not past");
    muscle::duration d(10,0);
    assert(!d.time_after().is_past(), "duration time_after has time in the future");
    assert(t < d.time_after(), "evaluation up to now takes less than 10 seconds");
    muscle::duration dnine(9,1);
    assert(dnine.time_after().duration_until() < d && dnine < d.time_after().duration_until(), "Evaluation of time and duration has the right ordering and takes less than a second");
    assertEquals(dnine.seconds(), 9l, "seconds match given");
    assertEquals(dnine.useconds(), 9000001u, "microseconds match given");
}

void testAsyncConnectServerSocket()
{    
    cout << endl << "async_connect/accept" << endl << endl;
    muscle::endpoint ep("localhost", 40104);
    muscle::socket_opts opts(10);
    muscle::async_cservice service;
    try
    {
        muscle::CServerSocket ssock(ep, &service, opts);
        async_ss ass(&service, &ssock);
        ssock.async_accept(1, &ass);
        muscle::ClientSocket::async_connect(&service, 1, ep, &ass);
        
        service.run();
    } catch (const muscle::muscle_exception& ex) {
        cout << "Can not bind to port, use a different one: " << string(ex.what()) << endl;
    }
}

void testAsyncTimer()
{    
    cout << endl << "async_cservice timer" << endl << endl;

    muscle::async_cservice service;
    muscle::time t = muscle::duration(0l, 100).time_after();
    const char *str = "test string";
    
    async_func_call func(&service, true);
    service.timer(1, t, &func, (void *)str);

    async_func_call func2(&service, false);
    muscle::time t2 = muscle::duration(1l, 0).time_after();
    service.timer(1, t2, &func2, (void *)str);
    
    service.run();
    
    try {
        service.update_timer(2, t2, (void *)str);
        service.run();
        assertTrue("update timer");
    } catch (...) {
        assertFalse("update timer");
    }

    try {
        service.update_timer(1, t2, (void *)str);
        service.run();
        assert(false, "can not update erased timer");
    } catch (...) {
        assertTrue("can not update erased timer");
    }
}

void testSocket()
{
    cout << endl << "csocket" << endl << endl;
    muscle::endpoint ep("napoli.science.uva.nl", 50022);
    
    try {
        muscle::CClientSocket sock(ep, NULL);
        assert(true, "Connection to existing host");
        char s[] = "some string";
        assert(sock.send(s, sizeof(s)) > 0, "Sending");
        assert(sock.isend(s, sizeof(s)) >= 0, "Sending (async)");
        ssize_t len;
        assert((len = sock.recv(s, sizeof(s)-1)) > 0, "Receiving");
        s[len] = '\0';
        cout << "\t\t(received: " << string(s) << ")" << endl;
        assert(sock.irecv(s, sizeof(s)) > 0, "Receiving (async)");
    } catch (const exception& ex) {
        assert(false, "Connection to existing host (" + string(ex.what()) + ")");
    }
    
    muscle::endpoint nep("XXXnapoli.science.uva.nl", 50022);
    try {
        muscle::CClientSocket sock(nep, NULL);
        assert(false, "Do not connect to non-existing host");
    } catch (const exception& ex) {
        assert(true, "Do not connect to non-existing host (" + string(ex.what()) + ")");
    }
}

void testParsing()
{
    cout << endl << "parsing" << endl << endl;
    vector<string> v;
    v.push_back("some"); v.push_back("text");
    assertEquals(split("some text", " "), v, "Split string by spaces");
    assertEquals(split(" some text", " "), v, "Left trim string by spaces");
    assertEquals(split("some text ", " "), v, "Right trim string by spaces");
    assertEquals(split("some=text", "="), v, "Split string by equal signs");
    assertEquals(split("some = text ", "= "), v, "Split string by spaces and equal signs");
    assertEquals(split("some = 	text ", "= \t"), v, "Split string by spaces, equal and tabs");
    assertEquals<string>(to_upper_ascii("ascii"), "ASCII", "Upper case");
    assertEquals<string>(to_upper_ascii("AsCIi"), "ASCII", "Mixed to upper case");
    assertEquals<string>(to_upper_ascii("AsC1=*20Ii"), "ASC1=*20II", "Mixed symbols to upper case");
}

void testOptions()
{
    cout << endl << "options" << endl << endl;
    option_parser opt;
    opt.add("left", "left side", false);
    opt.add("optA", "opt A");

    const char *argv[] = {"mto_tester", "--left", "--optA", "some"};
    int argc = 4;
    opt.load(argc, (char **)argv);
    assert(opt.has("left"), "flag option");
    assertEquals<string>(opt.get<string>("left", "default"), "", "flag option value");
    assertEquals<string>(opt.get<string>("optA", "default"), "some", "flag argument value");
    assertEquals<string>(opt.get<string>("optB", "default"), "default", "flag argument default value");
    assertEquals<string>(opt.forceGet<string>("optA"), "some", "force get argument value");
    try {
        opt.forceGet<string>("NonExisting");
        assertFalse("Throw exception non-existing mandatory key");
    } catch (...) {
        assertTrue("Throw exception non-existing mandatory key");
    }
}

void testEndpoint()
{
    cout << endl << "endpoint" << endl << endl;
    muscle::endpoint ep("napoli.science.uva.nl", 50022);
    
    assertEquals(ep.c_host(), "napoli.science.uva.nl", "c-host name is preserved");
    assertEquals(ep.getHost(), string("napoli.science.uva.nl"), "host name is preserved");
    assert(!ep.isResolved(), "do not resolve existing host without calling resolve");
    ep.resolve();
    assert(ep.isResolved(), "resolve existing host");
    assert(ep.isValid(), "validate existing host");
    assert(!ep.isIPv6(), "check IPv4 host");
    assertEquals<string>(ep.getHostFromAddress(), "146.50.56.52", "formatted IPv4 address matches");
    assertEquals<uint16_t>(ep.port, 50022, "host order port number matches");
    unsigned char *buf = new unsigned char[ep.getSize()];
    ep.serialize((char *)buf);
    assertEquals<int>(buf[0], MUSCLE_ENDPOINT_IPV4, "serialized protocol is IPv4");
    assertEquals<int>(buf[1], 146, "first IPv4 segment matches 146");
    assertEquals<int>(buf[2], 50, "second IPv4 segment matches 50");
    assertEquals<int>(buf[3], 56, "third IPv4 segment matches 56");
    assertEquals<int>(buf[4], 52, "last IPv4 segment matches 52");
    assertEquals(*(uint16_t *)&buf[17],htons(50022), "port matches 50022");
    // 146.50.56.52 = 146*256^3 + 50*256^2 + 56*256 + 52 = 2452764724
    
    muscle::endpoint bep((char *)buf);
    assertEquals(bep, ep, "Serialized data matches original data");
    
    const muscle::endpoint cep(ep);
    
    assert(cep.isResolved(), "const resolve existing host without calling resolve");
    assert(cep.isValid(), "const validate existing host");
    assert(!cep.isIPv6(), "const check IPv4 host");
    // 146.50.56.52 = 146*256^3 + 50*256^2 + 56*256 + 52 = 2452764724
    
    muscle::endpoint invEp("XXXnapoli.science.uva.nl", 5000);
    try {
        invEp.resolve();
        assertFalse("throw exception on resolve invalid hosts");
    }
    catch (const muscle::muscle_exception& ex)
    {
        assertTrue("throw exception on resolve invalid hosts");
    }
    assert(!invEp.isResolved(), "do not resolve invalid hosts");
}

int main(int argc, char * argv[])
{
    muscle::time t0 = muscle::time::now();

    try {
        testEndpoint();
        testSocket();
        testTime();
        testParsing();
        testOptions();
        testAsyncTimer();
        testAsyncConnectServerSocket();
        cout << endl;
        assertTrue("run tests without uncaught exceptions");
    } catch (const muscle::muscle_exception& ex) {
        assertFalse("run tests without uncaught exceptions (" + string(ex.what()) + ")");
    } catch (...) {
        assertFalse("run tests without uncaught exceptions");
    }
    
    cout << endl << failed << " of " << total << " failed (using " << t0.duration_since().str() << "s)" << endl;
    
    return failed;
}

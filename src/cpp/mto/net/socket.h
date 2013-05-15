//
//  socket.h
//  CMuscle
//
//  Created by Joris Borgdorff on 4/15/13.
//  Copyright (c) 2013 Joris Borgdorff. All rights reserved.
//

#ifndef __CMuscle__socket__
#define __CMuscle__socket__

#ifndef MUSCLE_SOCKET_TIMEOUT
#define MUSCLE_SOCKET_TIMEOUT (muscle::time(10,0))
#endif

#define MUSCLE_SOCKET_NONE 0
#define MUSCLE_SOCKET_R 1
#define MUSCLE_SOCKET_W 2
#define MUSCLE_SOCKET_RW 3
#define MUSCLE_SOCKET_ERR 4

#include "endpoint.h"
#include "async_description.h"
#include "../util/time.h"

namespace muscle {

class async_service;
    
struct socket_opts
{
    int keep_alive;
    int blocking_connect;
    int max_connections;
    ssize_t send_buffer_size;
    ssize_t recv_buffer_size;
    socket_opts() : keep_alive(-1), blocking_connect(-1), max_connections(-1), send_buffer_size(-1), recv_buffer_size(-1) {}
    socket_opts(int max_connections) : keep_alive(-1), blocking_connect(-1), max_connections(max_connections), send_buffer_size(-1), recv_buffer_size(-1) {}
    socket_opts(ssize_t r_bufsize, ssize_t s_bufsize) : keep_alive(-1), blocking_connect(-1), max_connections(-1), send_buffer_size(s_bufsize), recv_buffer_size(r_bufsize) {}
};

class ServerSocket;

class socket
{
public:
    virtual ~socket() {}

    // Check if the socket is readable / writable. Timeout is MUSCLE_SOCKET_TIMEOUT seconds.
    // Override MUSCLE_SOCKET_TIMEOUT to choose a different number of seconds
    virtual int select(int mask) const = 0;
    virtual int select(int mask, time timeout) const = 0;

    virtual std::string str() const;
    virtual std::string str();
    const endpoint& getAddress() const;
    const bool hasAddress;
    virtual int getSock() const = 0;
    virtual bool operator < (const socket & s1) const
    { return getSock() < s1.getSock(); }
    virtual bool operator == (const socket & s1) const
    { return getSock() == s1.getSock(); }
    async_service *getServer() const;
    virtual void async_cancel() const = 0;
protected:
    socket(endpoint& ep, async_service *service);
    socket(async_service *service);
    socket(const socket& other);
    socket();
    virtual void setBlocking (const bool) = 0;
    
    endpoint address;
    async_service *server;
}; // end class socket

class ClientSocket : virtual public socket
{
public:    
    static size_t async_connect(async_service *service, int user_flag, muscle::endpoint& ep, async_acceptlistener* accept);
    static size_t async_connect(async_service *service, int user_flag, muscle::endpoint& ep, socket_opts& opts, async_acceptlistener* accept);

    virtual int hasError() const { return 0; }

    // Data Transmission
    virtual ssize_t send (const void* s, size_t size) const = 0;
    virtual ssize_t recv (void* s, size_t size) const = 0;

    // Light-weight, non-blocking
    virtual ssize_t isend (const void* s, size_t size) const = 0;
    virtual ssize_t irecv (void* s, size_t size) const = 0;

    // asynchronous, light-weight, non-blocking
    virtual ssize_t async_send (int user_flag, const void* s, size_t size, async_sendlistener *send) const = 0;
    virtual ssize_t async_recv (int user_flag, void* s, size_t size, async_recvlistener *recv) const = 0;

    virtual void async_cancel() const;
    virtual ~ClientSocket() { async_cancel(); }
};

class ServerSocket : virtual public socket
{
public:
    virtual size_t async_accept(int user_flag, async_acceptlistener *accept) = 0;
    virtual void async_cancel() const;
    virtual ~ServerSocket() { async_cancel(); }
protected:
    ServerSocket(const socket_opts& opts);
    virtual void listen(int max_connections);
};

} // end namespace muscle

#endif /* defined(__CMuscle__socket__) */

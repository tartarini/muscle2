//
//  async_service.h
//  CMuscle
//
//  Created by Joris Borgdorff on 17-04-13.
//  Copyright (c) 2013 Joris Borgdorff. All rights reserved.
//

#ifndef __CMuscle__async_service__
#define __CMuscle__async_service__

#include "socket.h"

#include <queue>
#include <set>
#include <map>

namespace muscle {
    class async_service
    {
        typedef std::pair<time,async_description> timer_t;
        typedef std::map<const ClientSocket *, std::queue<async_description> > sockqueue_t;
        typedef std::map<const ServerSocket *, async_description> ssockdesc_t;
        typedef std::map<const ClientSocket *, async_description> csockdesc_t;
        typedef std::set<const ClientSocket *> csocks_t;
    public:
        async_service();
        virtual ~async_service() {};
        
        size_t send(int user_flag, const ClientSocket* socket, const void *data, size_t size, async_sendlistener* send);
        size_t receive(int user_flag, const ClientSocket* socket, void *data, size_t size, async_recvlistener* recv);
        size_t listen(int user_flag, const ServerSocket* socket, socket_opts *,async_acceptlistener* accept);
        size_t timer(int user_flag, time& t, async_function* func, void *user_data);
        virtual size_t connect(int user_flag, endpoint& ep, socket_opts *opts, async_acceptlistener* accept) = 0;

        void erase(const ClientSocket *socket);
        void erase(const ServerSocket *socket);
        void erase_timer(size_t);
        virtual void erase_connect(size_t) = 0;
        void *update_timer(size_t, time&, void *user_data);
        
        virtual void run();
        virtual void done();
        virtual bool isDone() const;
        virtual bool isShutdown() const;
        virtual void printDiagnostics();
    protected:
        virtual void run_timer(size_t timer);
        virtual void run_send(const ClientSocket *sender, bool hasErr);
        virtual void run_recv(const ClientSocket *receiver, bool hasErr);
        virtual void run_accept(const ServerSocket *listener, bool hasErr) = 0;
        virtual void run_connect(const ClientSocket *connect, bool hasErr) = 0;
        size_t getNextCode() { return _current_code++; }
        virtual int select(const ClientSocket **sender, const ClientSocket **receiver, const ServerSocket **listener, const ClientSocket **connect, duration& utimeout) const = 0;
        csocks_t recvSockets;
        csocks_t sendSockets;
        ssockdesc_t listenSockets;
        csockdesc_t connectSockets;
    private:
        size_t _current_code;
        bool is_done;
        bool is_shutdown;
        sockqueue_t recvQueues;
        sockqueue_t sendQueues;
        std::map<size_t,timer_t> timers;
        std::map<size_t,async_description> done_timers;
        size_t next_alarm();
    };
}
#endif /* defined(__CMuscle__async_service__) */

#include "connection.hpp"
#include "peercollection.h"
#include "peerconnectionhandler.hpp"
#include "localmto.h"

#include <cassert>

#define CONN_REMOTE_TO_LOCAL 1 // don't free
#define CONN_CONN_REMOTE 2 // do free
#define CONN_LOCAL_TO_REMOTE_R 3 // don't free
#define CONN_SEND_DATA 4 // don't free
#define CONN_TIMEOUT_CLOSE 5 // don't free

using namespace muscle;

/* from remote */
Connection::Connection(Header h, ClientSocket* s, PeerConnectionHandler* t, LocalMto *mto, bool remotePeerConnected)
: sock(s), header(h), closing(false), hasRemotePeer(remotePeerConnected), referenceCount(0), secondMto(t), mto(mto), closing_timer(0)
{
    assert(secondMto != NULL);
    if (hasRemotePeer)
    {
        logger::fine("Established client connection (%s), initiated by peer %s",
                      h.str().c_str(), secondMto->str().c_str());
        header.type=Header::Data;
        
        receive();
    }
    else
    {
        secondMto->send(header);
        
        logger::fine("Requesting connection to host %s from peer %s",
                      header.dst.str().c_str(), secondMto->str().c_str());
    }
}

void Connection::close()
{
    if(closing)
        return;

    referenceCount++;
    closing = true;
    logger::fine("Closing connection %s",
                  header.str().c_str());
    
    if(hasRemotePeer)
    {
        hasRemotePeer=false;
        header.type=Header::Close;
        if(secondMto)
            secondMto->send(header);
    }
    
    mto->conns.erase(header);
    muscle::time timer = duration(10l, 0).time_after();
    closing_timer = sock->getServer()->timer(CONN_TIMEOUT_CLOSE, timer, this, (void *)0);
    referenceCount--;
    tryClose();
}

void Connection::tryClose()
{
    if (closing && referenceCount == 0)
    {
        if (closing_timer)
            sock->getServer()->erase_timer(closing_timer);

        logger::fine("Connection %s closed",
                      header.str().c_str());
        delete this;
    }
}

Connection::~Connection()
{
    delete sock;
}

void Connection::async_execute(size_t code, int flag, void *user_data)
{
    logger::info("Connection %s did not close after timeout (%d connections running); forcing",
                  header.str().c_str(), referenceCount);
    sock->getServer()->printDiagnostics();
    sock->async_cancel();
    sock->getServer()->erase_timer(closing_timer);
}

void Connection::receive()
{
    if(closing)
        return;

    referenceCount++;
    sock->async_recv(CONN_LOCAL_TO_REMOTE_R, receiveBuffer, CONNECTION_BUFFER_SIZE, this);
}

bool Connection::async_received(size_t code, int user_flag, void *data, size_t count, int is_final)
{
    // Error occurred
    if(is_final == -1 || closing) return false;
    if (count == 0) return true;
	
	// Create new buffer: don't delete class-local data array receiveBuffer
    void *buffer = new char[count];
	memcpy(buffer, data, count);
	secondMto->send(header, buffer, count, new async_sendlistener_delete(2));
    
    receive();
    // Don't try to receive more information, we sent what we received
    return false;
}

void Connection::send(void *data, size_t length)
{
    if (closing)
        return;
    
	if (logger::isLoggable(MUSCLE_LOG_FINEST))
		logger::finest("[__W] Sending directly %u bytes on %s", length, sock->getAddress().str().c_str());
    
    referenceCount++;
    sock->async_send(CONN_REMOTE_TO_LOCAL, data, length, this);
}

void Connection::async_sent(size_t, int, void *data, size_t, int is_final)
{
    if (!is_final) return;
    
    if (data != receiveBuffer)
        delete [] (char *)data;
}

void Connection::async_report_error(size_t code, int user_flag, const muscle_exception &ex)
{
    if(closing)
        return;

    logger::severe("Error occurred in connection between %s (%s)",
                  header.str().c_str(), ex.what());
    
    close();
}

void Connection::async_done(size_t code, int flag)
{
    referenceCount--;
    tryClose();
}

void Connection::remoteConnected(Header h)
{
    if (closing)
        return;

    size_t response = h.length;

    h.type = Header::ConnectResponse;
    char *packet;
    size_t len = h.makePacket(&packet, response);
    referenceCount++;
    sock->async_send(CONN_CONN_REMOTE, packet, len, this);
    
    if(response)
    { // Fail
        logger::fine("Got negative response for connection request (%s)",
                      header.str().c_str());
        close();
    }
    else
    { // Success
        logger::fine("Remote connection succeeded (%s)",
                      header.str().c_str());
        header.type=Header::Data;
        hasRemotePeer = true;
        receive();
    }
}

void Connection::remoteClosed()
{
    hasRemotePeer = false;
    close();
}

void Connection::peerDied(PeerConnectionHandler* handler)
{
    if(secondMto == handler)
    {
        secondMto = mto->peers.get(header);
        
        if(secondMto == NULL)
            close();
    }
}

void Connection::replacePeer(PeerConnectionHandler* from, PeerConnectionHandler* to)
{
    if(secondMto == from)
        secondMto = to;
}

package com.feeyo.redis.net.backend;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import com.feeyo.redis.net.RedisConnection;
import com.feeyo.redis.net.backend.callback.BackendCallback;
import com.feeyo.redis.net.backend.callback.SelectDbCallback;
import com.feeyo.redis.net.backend.pool.PhysicalNode;
import com.feeyo.redis.nio.util.TimeUtil;

/**
 * REDIS 后端连接
 * 
 * @author zhuam
 *
 */
public class RedisBackendConnection extends RedisConnection {
	
    private BackendCallback callback;
    private PhysicalNode physicalNode;
    
    private volatile int db = 0;				//REDIS select database, default 0
    private volatile boolean borrowed = false;
    
	public RedisBackendConnection(SocketChannel channel) {
		super(channel);
	}
	
	public BackendCallback getCallback() {
		return callback;
	}

	public void setCallback(BackendCallback callback) {
		this.callback = callback;
	}

	public PhysicalNode getPhysicalNode() {
		return physicalNode;
	}

	public void setPhysicalNode(PhysicalNode node) {
		this.physicalNode = node;
	}
	
	public void release() {
		this.setBorrowed( false );
		this.physicalNode.releaseConnection(this);
	}

	public void setBorrowed(boolean borrowed) {
		this.borrowed = borrowed;
	}
	
	public boolean isBorrowed() {
        return this.borrowed;
    }

	public void setDb(int db) {
		this.db = db;
	}
	
	public boolean needSelectIf(int db) {
		if ( db == -1 && this.db == 0 ) {
			return false;			
		} else if ( db == this.db ) {
			return false;			
		} else {
			return true;
		}
	}
	
	
//	public void multi(BackendCallback callback) throws IOException {
//		/*
//		  2a 31 0d 0a 24 35 0d 0a     * 1 . . $ 5 . . 
//		  4d 55 4c 54 49 0d 0a        M U L T I . . 
//		 */
//		
//		this.callback = callback;
//		
//		StringBuffer sBuffer = new StringBuffer(34);
//		sBuffer.append("*1\r\n");
//		sBuffer.append("$5\r\n");
//		sBuffer.append("MULTI\r\n");
//		
//		write( sBuffer.toString().getBytes() );
//	}
	
	public void unwatch(BackendCallback callback) throws IOException {
		/*
		 2a 31 0d 0a 24 37 0d 0a     * 1 . . $ 7 . . 
		 55 4e 57 41 54 43 48 0d     U N W A T C H . 
		 0a                          . 
		 */
		
		this.callback = callback;
		
		StringBuffer sBuffer = new StringBuffer(34);
		sBuffer.append("*1\r\n");
		sBuffer.append("$7\r\n");
		sBuffer.append("UNWATCH\r\n");
		
		write( sBuffer.toString().getBytes() );
	}
	
	public void exec(BackendCallback callback) throws IOException {
		/*
		  2a 31 0d 0a 24 34 0d 0a     * 1 . . $ 4 . . 
		  45 58 45 43 0d 0a           E X E C . .  
		 */
		
		this.callback = callback;
		
		StringBuffer sBuffer = new StringBuffer(34);
		sBuffer.append("*1\r\n");
		sBuffer.append("$4\r\n");
		sBuffer.append("EXEC\r\n");
		
		write( sBuffer.toString().getBytes() );
		
	}
	
	public void discard(BackendCallback callback) throws IOException {
		/*
		  2a 31 0d 0a 24 37 0d 0a     * 1 . . $ 7 . . 
		  44 49 53 43 41 52 44 0d     D I S C A R D . 
		  0a                          . 
		 */
		
		this.callback = callback;
		
		StringBuffer sBuffer = new StringBuffer(34);
		sBuffer.append("*1\r\n");
		sBuffer.append("$7\r\n");
		sBuffer.append("DISCARD\r\n");
		
		write( sBuffer.toString().getBytes() );
	}
	
	public void unsubscribe(BackendCallback callback) throws IOException {
		
		this.callback = callback;
		
		/*
		2a 31 0d 0a 24 31 31 0d     * 1 . . $ 1 1 . 
		0a 55 4e 53 55 42 53 43     . U N S U B S C 
		52 49 42 45 0d 0a           R I B E . . 
		 */
		
		StringBuffer sBuffer = new StringBuffer(34);
		sBuffer.append("*1\r\n");
		sBuffer.append("$11\r\n");
		sBuffer.append("unsubscribe\r\n");
		
		write( sBuffer.toString().getBytes() );
	}
	
	public void select(int db, SelectDbCallback callback) throws IOException {
	   
	   this.callback = callback;
	   
		/*
		 2a 32 0d 0a 24 36 0d 0a     * 2 . . $ 6 . . 
		 73 65 6c 65 63 74 0d 0a     s e l e c t . . 
		 24 32 0d 0a 31 32 0d 0a     $ 2 . . 1 2 . . 
		 */		
		StringBuffer sBuffer = new StringBuffer(34);
		sBuffer.append("*2\r\n");
		sBuffer.append("$6\r\n");
		sBuffer.append("select\r\n");
		sBuffer.append("$").append( db < 10 ? 1 : 2 ).append("\r\n");
		sBuffer.append( db ).append("\r\n");
		
		write( sBuffer.toString().getBytes() );			
	}
	
	@Override
	public String toString() {
		StringBuffer sbuffer = new StringBuffer(100);
		sbuffer.append( "Connection [reactor=").append( reactor );
		sbuffer.append(", host=").append( host ).append("/").append( port );
		sbuffer.append(", id=").append( id );
		sbuffer.append(", borrowed=").append( borrowed );
		sbuffer.append(", startupTime=").append( TimeUtil.formatTimestamp(startupTime) );
		sbuffer.append(", lastReadTime=").append( TimeUtil.formatTimestamp(lastReadTime) );
		sbuffer.append(", lastWriteTime=").append( TimeUtil.formatTimestamp(lastWriteTime) );
		if ( isClosed.get() ) {
			sbuffer.append(", closedTime=").append( TimeUtil.formatTimestamp( closeTime) );
			sbuffer.append(", closeReason=").append( closeReason );
		}
		sbuffer.append(", isClosed=").append( isClosed );
		sbuffer.append("]");
		return  sbuffer.toString();
	}
	
}
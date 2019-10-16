package org.dragonli.service.general.netty.websocket;

import com.alibaba.fastjson.JSON;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.dragonli.service.nettyservice.websockethandle.NettySocketSession;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author freeangel
 *
 */
public class SocketPool {
	
	private int moduleId;
	public int getModuleId() {
		return moduleId;
	}

	public void setModuleId(int moduleId) {
		this.moduleId = moduleId;
	}

	protected final static ConcurrentMap<Channel, NettySocketSession> chnnels = new ConcurrentHashMap<Channel,NettySocketSession>();
	protected final static ConcurrentMap<Integer,SocketPool> socketPools = new ConcurrentHashMap<Integer,SocketPool>();
	private final ConcurrentMap<String,NettySocketSession> sessions = new ConcurrentHashMap<String,NettySocketSession>();
	
	public static SocketPool getModuleInstance(int id)
	{
		SocketPool pool = socketPools.get(id);
		SocketPool pool2;
		if( pool == null )
			pool = ( pool2 = socketPools.putIfAbsent(id, pool = new SocketPool(id)) ) == null ? pool : pool2;
		return pool;
	}
	
	public SocketPool(int id)
	{
		this.moduleId = id;
	}
	
	public NettySocketSession remove(String uuid)
	{
		System.out.println("remove sessions - start|uuid:"+uuid);
		NettySocketSession ss = getSessions().remove(uuid);
		if( ss == null ){
//			System.out.println("remove sessions - sessions is null|uuid:"+uuid);
			return null;
		}
		
		if(ss.getChannel() != null){
//			System.out.println("remove sessions - clear channel|uuid:"+uuid);
			socketPools.remove(ss.getChannel());
		}else{
//			System.out.println("remove sessions - channel is null|uuid:"+uuid);
		}
		
		ss.clear();
//		System.out.println("remove sessions - sessions|uuid:"+uuid);
		return ss;
	}
	
	public NettySocketSession[] where(Map<String,Object> where)
	{
		NettySocketSession one = null;
		if( where.containsKey("uuid") )
		{
			one = getSessions().get(where.get("uuid").toString());
			if( one != null )
				return new NettySocketSession[]{one};
			else
				return null;
		}
		
		Integer id = where.containsKey("uid") ? Integer.parseInt( where.get("uid").toString() ) : null;
		if( id == null )
			return null;
		
		NettySocketSession[] all = new NettySocketSession[0];
		getSessions().values().toArray(all);
		Queue<NettySocketSession> queue = new LinkedList<NettySocketSession>();
		for( int i = 0 ; i < all.length ; i++ )
		{
			one = all[i];
			if( one.getUid() == id.intValue() )
				queue.add(one);
		}
		
		return queue.toArray(new NettySocketSession[0]);
			
	}
	
	public NettySocketSession get(String uuid)
	{
		return getSessions().get(uuid);
	}
	
	public static NettySocketSession remove(Channel channel)
	{
		
		
		return chnnels.remove(channel);
	}
	
	public static NettySocketSession get(Channel channel)
	{
		return chnnels.get(channel);
	}
	
	public NettySocketSession put(NettySocketSession one)
	{
//		chnnels.put(one.getChannel(),one);
		NettySocketSession n;
		n = chnnels.putIfAbsent(one.getChannel(), one);
		if( n != null && n.getChannel() != null )
		{
			one.getChannel().close();
			one.clear();
		}
		n = getSessions().put(one.getUuid(), one);
		if(n != one && n != null && n.getChannel() != null )
		{
			
			writeCloseMsg(n.getChannel(),false);
//			n.getChannel().w
//			n.getChannel().writeAndFlush(new CloseWebSocketFrame(200,"1111"));
//			n.getChannel().w
//			n.getChannel().close();
//			n.clear();
		}
		return getSessions().get(one.getUuid());
	}

	private void writeCloseMsg(Channel channel,boolean reconnect){
		Map<String,Object> sendMap = new HashMap<String,Object>();
		sendMap.put("reconnect", reconnect);
		sendMap.put("messageKey", "close");
		channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(sendMap)));
	}
	
	
	public ConcurrentMap<String,NettySocketSession> getSessions() {
		return sessions;
	}
	
	
}

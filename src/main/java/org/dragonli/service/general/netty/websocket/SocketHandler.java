/**
 * 
 */
package org.dragonli.service.general.netty.websocket;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.apache.log4j.Logger;
import org.dragonli.service.general.interfaces.general.WebSocketService;
import org.dragonli.service.nettyservice.websockethandle.INettySocketCentral;
import org.dragonli.service.nettyservice.websockethandle.INettySocketHandler;
import org.dragonli.service.nettyservice.websockethandle.NettySocketSession;
import org.dragonli.tools.general.EncryptionUtil;
import org.dragonli.tools.general.GeneralUtil;
import org.dragonli.tools.general.HttpUtil;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author freeangel
 *
 */
@Component
public class SocketHandler implements INettySocketHandler {
	
	public static Logger logger = Logger.getLogger(SocketHandler.class);
	private static final AtomicInteger connectCount = new AtomicInteger(0);
	@Autowired
	private RedissonClient redissonClient;
	@Value("${service.micro-service.simple-netty-service.redisonlineKey}")
	private String redisOnlineKey;
	private String ip;
	private WebSocketService ws;
	public SocketHandler() throws Exception
	{
		this.ip = InetAddress.getLocalHost().getHostAddress();
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	private String key;
	private int minTime = 1000000;
	
	public int getMinTime() {
		return minTime;
	}

	public void setMinTime(int minTime) {
		this.minTime = minTime;
	}

	private INettySocketCentral central;
//	private ConcurrentMap<Integer, SocketPool> socketpools;
	@Value("${service.micro-service.simple-netty-service.privatekey}")
	private String privateKey;
	
	@Override
	public void setCentral(INettySocketCentral central) {
		// TODO Auto-generated method stub
		this.central = central;
	}

	/* (non-Javadoc)
	 * @see com.freewzq.core.sockethandle.INettySocketHandler#notifyConnected(com.freewzq.core.sockethandle.NettySocketSession)
	 */
	@Override
	public void notifyConnected(HttpRequest req,Channel one) throws Exception {
		// TODO Auto-generated method stub
		logger.info("userInfo");
		Map<String, Object> paras = HttpUtil.getUrlParams(req.uri());
		String unid = GeneralUtil.assertEmpty( paras.get("uuid").toString() );
		//int uid = Integer.parseInt( GeneralUtil.assertEmpty( paras.get("uid").toString() ) );
		//String serverName = GeneralUtil.assertEmpty( paras.get("serverName").toString() );
		//int moduleId = Integer.parseInt( GeneralUtil.assertEmpty( paras.get("moduleId").toString() ) );
		
		String ipAddr = null;
		FullHttpRequest request = (FullHttpRequest)req;
		if (request.headers().contains("X-Forwarded-For") ) {
			ipAddr = request.headers().get("X-Forwarded-For");
		}else{   
			InetSocketAddress insocket = (InetSocketAddress) one.remoteAddress();
			ipAddr = insocket.getAddress().getHostAddress();
		}
		NettySocketSession nss = new NettySocketSession();
		nss.setModuleId(1);
		//nss.setServerName(serverName);
		//nss.setUid(uid);
		nss.setUuid(unid);
		nss.setChannel(one);
		nss.setIp(ipAddr);
		SocketPool.getModuleInstance(1).put(nss);
		
		connectCount.incrementAndGet();
		
		//把自己注册到redis 里面 以便于查找我在哪socket服务器上
		
		
		//TODO 换成币为的redis写法 @biwei redis
		try {
			Map<String,String> uuidToIp = redissonClient.getMap(redisOnlineKey);
			uuidToIp.put(unid, InetAddress.getLocalHost().getHostAddress()+";");
			logger.info("userInfo "+unid+"has been layed on redis");
		} catch (Exception e) {
			logger.error("notifyConnected Error",e);
		}

	}

	/* (non-Javadoc)
	 * @see com.freewzq.core.sockethandle.INettySocketHandler#notifyCloseed(com.freewzq.core.sockethandle.NettySocketSession)
	 */
	@Override
	public void notifyClosed(Channel one) throws Exception {
		// TODO Auto-generated method stub
		
		NettySocketSession nss = SocketPool.remove(one);;
		if( nss == null ){
			logger.info("close socket warning: one is null");
			return ;
		}
		connectCount.decrementAndGet();	
		NettySocketSession currentNss = SocketPool.getModuleInstance(nss.getModuleId()).get(nss.getUuid());
		//若果当前记录 跟要断开的是同一个 那么删除 (这样会有多线程问题啊....)
		if(currentNss != null && currentNss.getChannel() == nss.getChannel()){
			SocketPool.getModuleInstance(nss.getModuleId()).remove(nss.getUuid());
			//删除redis 里面的在线用户,将用户id从redis中取消注册，换成币为的写法 @biwei redis
			Map<String,String> uuidToIp = redissonClient.getMap(redisOnlineKey);
			uuidToIp.remove(nss.getUuid());
			//删除本地map里保存的用户订阅信息
			ws.clearTopicById(nss.getUuid());
//			Jedis jedis = jedisPool.getResource();	
//			try {
//				String userMapKey = redisOnlineKey
//				jedis.hdel(userMapKey, nss.getUuid());
//			} catch (Exception e) {
//				logger.error("notifyClosed Error",e);
//			}finally {
//				jedis.close();
//			}
		}

//				}
	}

	/* (non-Javadoc)
	 * @see com.freewzq.core.sockethandle.INettySocketHandler#send(java.lang.Object, java.util.Map)
	 */
	@Override
	public boolean send(Object msg,int moduleId, Map<String, Object> para) {
		// TODO Auto-generated method stub
		NettySocketSession[] all = SocketPool.getModuleInstance(moduleId).where(para);
		if( all == null )
		{
			logger.info("socket send return:  msg:"+msg+";;;moduleId:"+moduleId+";;;para:"+para.get("uuid")+"/"+para.get("uuid")+"|||");
			return false;
		}
			
		for( int i = 0 ; i < all.length ;  )
		{
			NettySocketSession one = all[i];
			if( one.getChannel().isActive() && one.getChannel().isWritable() )
			{
				logger.info("socket send successed:  msg:"+msg+";;;moduleId:"+moduleId+";;;para:"+one.getUuid()+"/"+one.getUid());
				writeOneMsg(one.getChannel(),msg);
				return true;
			}
			else
			{
				logger.info("socket send faild:  msg:"+msg+";;;moduleId:"+moduleId+";;;para:"+one.getUuid()+"/"+one.getUid());
				return false;
			}
		}
		return false;
	}
	
	@Override
	public boolean send(String uuid,String msg,int moduleId)
	{
		NettySocketSession one = SocketPool.getModuleInstance(moduleId).get(uuid);
		if(one == null )
		{
			logger.info("socket by uuid can't find uuid:  msg:"+msg+";;;moduleId:"+moduleId+";;;para:"+uuid);
			return false;
		}
		if( !one.getChannel().isActive() || !one.getChannel().isWritable())
		{
			logger.info("socket by uuid send faild:  msg:"+msg+";;;moduleId:"+moduleId+";;;para:"+one.getUuid()+"/"+one.getUid());
			return false;
		}
		logger.info("socket by uuid send successed:  msg:"+msg+";;;moduleId:"+moduleId+";;;para:"+one.getUuid()+"/"+one.getUid());
		writeOneMsg(one.getChannel(),msg);
		return true;
	}


	
	

	private void writeOneMsg(Channel channel,Object msg)
	{
		//临时方案，将来去除if...
		if( msg instanceof String )
			channel.writeAndFlush(new TextWebSocketFrame((String) msg));
	}
	
	/* (non-Javadoc)
	 * @see com.freewzq.core.sockethandle.INettySocketHandler#close(java.util.Map)
	 */
	@Override
	public void close(int moduleId,Map<String, Object> para) {
		// TODO Auto-generated method stub
//		to be continue
		central.close(para);
		NettySocketSession[] all = SocketPool.getModuleInstance(moduleId).where(para);
		if( all == null )
			return ;
		for( int i = 0 ; i < all.length ; i++ )
		{
			NettySocketSession one = all[i];
			one.getChannel().close();
		}
	}
	
	@Override 
	public boolean valicodeSocket(HttpRequest req) throws Exception
	{
//		boolean f = true;
//		if( f )
//			return true;
		logger.info("test1 - 1:");
		Map<String, Object> paras = HttpUtil.getUrlParams(req.getUri());
		logger.info("test1 - 1:");

		String uuid = GeneralUtil.assertEmpty( paras.get("uuid").toString() );
//		String uid = GeneralUtil.assertEmpty( paras.get("uid").toString() );
//		String moduldId = GeneralUtil.assertEmpty( paras.get("moduleId").toString() );//待配置化
		String rKey = GeneralUtil.assertEmpty( paras.get("key").toString() );
		long time = Long.parseLong( GeneralUtil.assertEmpty( paras.get("time").toString() ) );
		//这个是有效期嘛???看之前的代码意思好像不是
		if( System.currentTimeMillis()  - time > minTime )
			return false;		
//		MessageDigest md = MessageDigest.getInstance("SHA-1");
//		String oKey = uuid + "|" + t  + "|" + key;
//		String digest = GeneralUtil.byteToString( md.digest( oKey.getBytes("UTF-8") ) );	
		String digest = EncryptionUtil.sha1(uuid+"|"+time+"|"+this.privateKey);
		return rKey.equals(digest) ;
	}
	
	@Override
	public int getConnectCount()
	{
		return connectCount.get();
	}

	@Override
	public int sendToAll(String msg, int moduleId) {
//		long time =System.currentTimeMillis();
//		Map map = JSONUtil.parseObject(msg);
//		map.put("systime", time);
//		msg = JSONUtil.toJSONString(map);
		ConcurrentMap<String,NettySocketSession> all = SocketPool.getModuleInstance(moduleId).getSessions();
		for (NettySocketSession ss : all.values()) {  
			writeOneMsg(ss.getChannel(),msg);  
		}  
		return all.size();
	}

	@Override
	public String receive(Channel channel, String msg) {
		// TODO Auto-generated method stub
		if(msg != null && msg.length()>100)
		{
			try {
				channel.close();
				notifyClosed(channel);
			}catch (Exception e){e.printStackTrace();}
			return null;
		}
		return "{\"PONG\":true}";
	}
	


}

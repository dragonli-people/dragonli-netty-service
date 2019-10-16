/**
 * 
 */
package org.dragonli.service.general.netty.websocket;

import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.dragonli.service.nettyservice.websockethandle.INettySocketHandler;
import org.dragonli.tools.zookeeper.ZookeeperClientUtil;

/**
 * @author dev
 *
 */
public class UpdateConnectHandler extends Thread {
	
	private static final Logger logger = Logger.getLogger(UpdateConnectHandler.class);
	
	
	
	private ZookeeperClientUtil zk;
	private INettySocketHandler socketHandler;
	private String prefix;
//	private String prefix2;
	private String ip;
	private String port;
	private String ipAndPort;
	
	public UpdateConnectHandler(
			ZookeeperClientUtil zk, INettySocketHandler socketHandler
			,String ipAndPort,int port,String prefix)
	{
		this.zk = zk;
		this.socketHandler = socketHandler;
		this.ip = ip;
		this.port = port+"";
		this.prefix = prefix;
		this.ipAndPort = ipAndPort;
//		this.prefix2 = prefix2;
	}
	
	@Override
	public void run()
	{
//		ZookeeperClientUtil zk = new ZookeeperClientUtil();
//		zk.setSessionTimeout(10000000);//Integer.MAX_VALUE);
//		zk.setServers("zookeeper.ijizhe.com:2181");
//		prefix = GlobalVars.properties.getProperty("websocket.zookeeper.online");
//		prefix2 = GlobalVars.properties.getProperty("websocket.zookeeper.port");
//		port = GlobalVars.properties.getProperty("websocket.service.port");;
//		ipAndPort = ip + ":" + port;
		
		
//		int serverId = 1;
//		System.out.println("get in ipAndPort!!!!!!"+ipAndPort);
		try {
			zk.createIfNotExist(prefix, Ids.OPEN_ACL_UNSAFE);
//			zk.createIfNotExist(prefix2, Ids.OPEN_ACL_UNSAFE);
			zk.setTextData(prefix+"/"+ipAndPort.split(";")[0] , "0"
					, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
//			zk.setTextData(prefix2+"/"+ip , port
//					, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		String kk = new String((zk.getAliveZk().getData("/ijizhe/services/general/websocket/online/"+group+"/"+InetAddress.getLocalHost().getHostAddress(), false, null)),"UTF-8");
		
//		System.out.println(kk);
		long last = zk.getLastConnectTime();
		int lastCount = socketHandler.getConnectCount();
		int i = 0;
		int ii = 0;
		while(true)
		{
			boolean log = (ii=ii%1000)==0;
			try
			{
				int count = socketHandler.getConnectCount();
				long lc = zk.getLastConnectTime(); 
				if( lc > last 
					|| count != lastCount 
					)
				{
					doUpdate(log);
					count = lastCount;
					last = lc;
				}
			}catch(Exception ee){ee.printStackTrace();}
//			System.out.println(zk.getAliveZk().getSessionTimeout());
			try {
				Thread.sleep(10L);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			i++;
			ii++;
			if( i > 100 )
			{
				try {
					doUpdate(log);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				i = 0;
			}
		}
	}
	
	private void doUpdate(boolean log) throws Exception
	{
//		System.out.println("get in doUpdate:"+ (prefix+"/"+ipAndPort));
		Integer count = socketHandler.getConnectCount(); 
		boolean f1 = zk.setTextData(prefix+"/"+ipAndPort.split(";")[0] , count.toString()
				, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
//		boolean f2 = zk.setTextData(prefix2+"/"+ip , port
//				, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
		if(!f1 )//|| !f2)
			logger.error("websocket["+ipAndPort+"] update online info faild");
		else if(log)
			logger.info("websocket["+ipAndPort+"] update online info successed!set count:["+count+"],set port["+port+"]");
	}

	public ZookeeperClientUtil getZk() {
		return zk;
	}

	public void setZk(ZookeeperClientUtil zk) {
		this.zk = zk;
	}

	public INettySocketHandler getSocketHandler() {
		return socketHandler;
	}

	public void setSocketHandler(INettySocketHandler socketHandler) {
		this.socketHandler = socketHandler;
	}
}

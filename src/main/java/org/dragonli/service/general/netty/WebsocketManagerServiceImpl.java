/**
 * 
 */
package org.dragonli.service.general.netty;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import org.apache.log4j.Logger;
import org.dragonli.service.dubbosupport.DubboBeanManager;
import org.dragonli.service.general.interfaces.general.WebSocketService;
import org.dragonli.service.general.netty.websocket.SocketHandler;
import org.dragonli.service.general.netty.websocket.UpdateConnectHandler;
import org.dragonli.service.nettyservice.websockethandle.INettySocketHandler;
import org.dragonli.service.nettyservice.websockethandle.NettyWebSocketStarter;
import org.dragonli.tools.general.EncryptionUtil;
import org.dragonli.tools.zookeeper.ZookeeperClientUtil;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author dev
 *
 */

@Service(interfaceClass = WebSocketService.class , register = true,timeout = 6000,retries=-1 ,delay=-1)
public class WebsocketManagerServiceImpl implements WebSocketService {

	@Autowired
	private RedissonClient redissonClient;
	public static Logger logger = Logger.getLogger(WebsocketManagerServiceImpl.class);
	public ZookeeperClientUtil zk;
	
	private static final ConcurrentMap<String, DubboBeanManager> managers = new ConcurrentHashMap<>();
	@Autowired
	private ApplicationConfig application;
	@Autowired
	private RegistryConfig registry;
	private volatile Map<String,Map<String,String>> ipDic1 = new HashMap<>();//外网为键
	private volatile Map<String,Map<String,String>> ipDic2 = new HashMap<>();//内网为键
	private volatile Map<String,Map<String,String>> ipDic3 = new HashMap<>();//内网为键
	private volatile Map<String,Set<String>> topicMap = new HashMap<>();//保存用户订阅的主题信息，主题为键
	private volatile Map<String,Integer> ipDic4 = new HashMap<>();
	@SuppressWarnings("unused")//暂时未用
	private volatile Map<String,String> allDic1= new HashMap<>(); ;//外网为键
	@SuppressWarnings("unused")//暂时未用
	private volatile Map<String,String> allDic2 = new HashMap<>(); ;//内网为键
	
	private UpdateConnectHandler updateConnectHandler;
	@Value("${spring.netty-service.configsStr}")
	private String configsStr;
	private int port;
//	private int serverId;
	private String ip;
	@Value("${spring.netty-service.onlinePrefix}")
	private String onlinePrefix;
	
	@Value("${spring.lock-cinfig.zookeeperAddr}")
	private String zkpAddr;
	@Value("${spring.lock-cinfig.zkpTimeout}")
	private int zkpClientTimeout;
	@Value("${spring.netty-service.protocol.port}")
	private int handlerDubboPort;
//	private String redisHost;
	@Value("${spring.netty-service.privatekey}")
	private String privatekey;
	private String ipAndPort;
	public List<String> serverConfigs;
	@Value("${spring.netty-service.redisonlineKey}")
	private String redisOnlineKey;
	@Autowired
	SocketHandler handler;
	@PostConstruct
	public void start() throws Exception
	{
//		if((1+2) > 0)
//			return;
//		this.ip = InetAddress.getLocalHost().getHostAddress();
		this.socketHandler = handler;
//		this.onlinePrefix = onlinePrefix;
//		this.handlerDubboPort = handlerDubboPort;
//		this.redisHost = redisHost;//用现有的？
//		this.privatekey = privatekey;
//		this.redisOnlineKey = redisOnlineKey;
		
		configsStr = configsStr.replaceAll("(^\\s+)|(\\s+$)", "");
		String[] configs = configsStr.split("\\s+");
		this.serverConfigs = new LinkedList<>();
		for( String s : configs )
			this.serverConfigs.add(s);
		
		this.zk = new ZookeeperClientUtil();
		this.zk.setServers(zkpAddr);
		this.zk.setSessionTimeout(zkpClientTimeout);
		this.zk.zkReconnect();
//		socketHandler.setWebSocketService(this);
		new Thread( new Runnable(){
			
			@Override
			public void run()
			{
				try {
					doRun();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			public void doRun() throws Exception {
				Thread.sleep(40000);//?? lin shi try 10 ?
				parseConfig();
				
				
				updateConnectHandler = new UpdateConnectHandler(zk,handler, ipAndPort,port,onlinePrefix);
				updateConnectHandler.start();

				NettyWebSocketStarter.startNetty(port,handler);
				// TODO Auto-generated method stub
				while(true)
				{
					try {
						refrenshHost(onlinePrefix);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					try {
						Thread.sleep(5000L);//将来是不是改成侦测zkp的变化来修改？暂方便起见，这么处理。运维如果要减服，需要先改配置，一段时间再真正把服停掉
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
		} ).start();
	}
	
	private INettySocketHandler socketHandler;
	
	public INettySocketHandler getSocketHandler() {
		return socketHandler;
	}

	public void setSocketHandler(INettySocketHandler socketHandler) {
		this.socketHandler = socketHandler;
	}

	
	public int sendTextMessageToAll(String content) {
		// TODO Auto-generated method stub
		//1将来可能废弃
		return socketHandler.sendToAll(content, 1);
	}
	
	public boolean sendTextMessage(String uniqueId, String content) {
		// TODO Auto-generated method stub
		return socketHandler.send(uniqueId,content, 1);
	}
	
	
	public void refrenshHost(String prefix) throws Exception
	{
		List<String> list1 = this.serverConfigs;//zk.getAliveZk().getChildren(prefix, false);
		logger.info("get in configs::"+JSON.toJSONString(list1));
		for (String key : list1) {
			String ke = key.split(";")[0];
			Integer count = 0;
			try{
			count = Integer.parseInt(new String(zk.getAliveZk().getData(prefix+"/"+ke, false, null),"UTF-8"));
			}catch(Exception e){//服务器挂掉的预案
				ipDic4.remove(key);
				logger.warn("socket ip :"+key+"can't be found in zookeeper");
				continue;
			}
			ipDic4.put(key, count);
			
		}
	}

	//包装票据，找空闲服务器
	@Override
	public Map<String,Object>recommendServerForUUid(String unid) throws Exception{
		String url = recommendServer();//找空闲服务器
		if(url == null) return null;
		Map<String,Object> map = new HashMap<String,Object>();		
		long time = System.currentTimeMillis();		
		String sha = EncryptionUtil.sha1(unid+"|"+time+"|"+this.privatekey);
		map.put("time", time);
		map.put("uuid", unid);
		map.put("key", sha);
		Map<String,Object> mapDta =new HashMap<String,Object>();		
		mapDta.put("url",url);
		mapDta.put("data", map);
		return mapDta;
		
	}
	

	
	@Override
	public String recommendServer()
	{
		try {

			List<String> keys = this.serverConfigs;
			if( keys == null || keys.size() == 0 )
				return null;
			int min = Integer.MAX_VALUE;
			String host = null;
			for( String key : keys )
			{
				//获取所有服务器的连接数据
//				int count = Integer.parseInt( new String( zk.getAliveZk().getData(baseKey+"/"+key, false,null) , "UTF-8" ) );
				Integer count = ipDic4.get(key);
				if(count!=null){
					if( min > count )
					{
						min = count;
						host = key.split(";")[1];
					}
					
				}
			}
			return host;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public ZookeeperClientUtil getZk() {
		return zk;
	}

	public void setZk(ZookeeperClientUtil zk) {
		this.zk = zk;
	}
	
	private boolean doSendTextMessage(String uniqueId, String content,String ip) {
		if(ip==null)
			return false;//配置之外的服务器
		
		DubboBeanManager manager = createBeanManager(ip,WebSocketService.class,"WebSocketManagerService-"+ip);
		if(!manager.isHadSuccessInit())
			return false;
		
		if( !manager.getBean().$echo("OK").equals("OK") )
			return false;//ping不通
		
		return ((WebSocketService)manager.getBean()).sendTextMessage(uniqueId, content);
	}
	
	@Override
	public boolean sendTextMessageByUniqueId(String unid, Map data,String key) {
		//封装消息
		Map<String,Object> message = new HashMap<>();
		message.put("key", key);
		message.put("data", data);
		message.put("servertime", System.currentTimeMillis());
		String content = JSON.toJSONString(message);
		//根据用户的id从redis中取出与之连接的ip
		RMap<String, String> map = redissonClient.getMap(redisOnlineKey);
		logger.info("redisson map:" + map.toString());
		String userInfo = map.get(unid);
		if(userInfo==null){
			logger.warn("can't find ip linked with user: "+unid);
			return false;
		}
		String userIp = userInfo.split(";")[0];

		return doSendTextMessage(unid, content,userIp);
	}

	private DubboBeanManager createBeanManager(String ip,Class<?> cls,String name )
	{
		DubboBeanManager beanManager = null;
		if( !managers.containsKey(ip) )
		{
			managers.putIfAbsent(ip, new DubboBeanManager());
		}
		beanManager = managers.get(ip);
		beanManager.init(cls,name,this.application,this.registry,ip+":"+handlerDubboPort);//端口可以将来可以改成从zookper上获取，不过似乎没有必要…
		return beanManager;
	}

	public UpdateConnectHandler getUpdateConnectHandler() {
		return updateConnectHandler;
	}

	public void setUpdateConnectHandler(UpdateConnectHandler updateConnectHandler) {
		this.updateConnectHandler = updateConnectHandler;
	}

	private String serviceVersion;

	public String getServiceVersion() {
		return serviceVersion;
	}

	public void setServiceVersion(String serviceVersion) {
		this.serviceVersion = serviceVersion;
	}
	//从配置列表找自己的端口号
	public void parseConfig() throws Exception
	{
		ZookeeperClientUtil zkp = this.zk;
		this.ip = ((InetAddress) InetAddress.getLocalHost()).getHostAddress();
		logger.info("host:"+ ip);
		List<String> list = this.serverConfigs;
//		List<String> list = zkp.getAliveZk().getChildren("/btcdo/configs/services/general/websocket/servers/id/user", false);
		for(String k:list)
		{
			System.out.println("get in k======"+k);
			if(k.startsWith(ip+":")||k.startsWith(ip+";")){
				ipAndPort = k;
				String ipName1 = k.split(";")[0];
				String ipName2 = k.split(";")[1];
				port = ipName1.split(":").length > 1 ? Integer.parseInt(ipName1.split(":")[1])
						: Integer.parseInt(ipName2.split(":")[ipName2.length()-1]);
				logger.info("socketHost ip zookeeper:"+ k);
				return;
			}

		}
		throw new Exception("no config with this ip");
	}

	/**
	 * 根据用户id获取长连接服务对象
	 */
	public WebSocketService getWebSocketServiceByUnid(String unid){
		//从redis拿到用户长连接的服务器
//		RMap<String, String> map = redissonClient.getMap(redisOnlineKey);
//		String userInfo = map.get(unid);
//		String userIp = userInfo.split(";")[0];
//		if(userIp==null){//说明还没有建立长连接
//			return null;
//		}
//		DubboBeanManager manager = createBeanManager(userIp,WebSocketService.class,"WebSocketManagerService-"+ip);
//		if(!manager.isHadSuccessInit()){
//			return null;
//		}
//		if( !manager.getBean().$echo("OK").equals("OK") ){
//			return null;//ping不通
//		}
//		return (WebSocketService)manager.getBean();
		return this;
	}
	
	@Override
	public boolean subscribe(String[] topics,String unid){
//		if(topics==null || topics.length==0){
//			return false;
//		}
//		//从redis获取用户订阅信息
//		RMap<String, String> map = redissonClient.getMap(redisOnlineKey);
//		String userInfo = map.get(unid);
//		String userIp = userInfo.split(";")[0];
//		String[] topicsArr = null;
//		if(!userInfo.endsWith(";")){
//			topicsArr = userInfo.split(";")[1].split(",");
//			
//		}
//		//保存redis端客户的订阅信息
//		Set<String> redisTopics = new HashSet<>();
//		if(topicsArr!=null && topicsArr.length!=0){
//			for (String tp : topicsArr) {
//				redisTopics.add(tp);
//			}
//		}
//		//保存订阅信息到本地map
//		getWebSocketServiceByUnid(unid).updateTopicMap(topics,unid);
//		//更新订阅信息到redis
//		for (String topic : topics) {
//			redisTopics.add(topic);
//		}
//		String topicStr = "";
//		if(redisTopics!=null && redisTopics.size()!=0){
//			for (String topic : redisTopics) {
//				topicStr = topicStr + topic + ",";
//			}
//		}
//		userInfo = userIp + ";" + topicStr;
//		map.put(unid, userInfo);
//		logger.info("user:"+unid+" topic update to redis succeed!");
		return true;
	}

	@Override
	public boolean unsubscribeByUnid(String topic,String unid){
		return getWebSocketServiceByUnid(unid).unsubscribe(topic, unid);
	}
	
	@Override
	public boolean unsubscribe(String topic,String unid){
		//删除redis中的记录
//		RMap<String, String> map = redissonClient.getMap(redisOnlineKey);
//		String userInfo = map.get(unid);
//		if(userInfo.contains(topic)){
//			userInfo = userInfo.replaceAll(topic+",","");
//		}else{
//			logger.warn("topic "+topic+" doesn't exist or no user,can't be unsubscribe!");
//		}
//		map.put(unid, userInfo);
//		//删除本地map里的用户订阅信息
//		Set<String> set = topicMap.get(topic);
//		if(set==null||set.size()==0){
//			logger.warn("topic "+topic+" doesn't exist or no user,can't be unsubscribe!");
//			return true;
//		}
//		set.remove(unid);
		return true;
	}
	
	@Override
	public void updateTopicMap(String[] topics, String unid) {
		for (String topic : topics) {
			Set<String> set = topicMap.get(topic);
			if(set == null){
				set = new HashSet<>();
			}
			set.add(unid);
			topicMap.put(topic, set);
		}
	}

	@Override
	public void clearTopicById(String unid) {
		Set<String> topics = topicMap.keySet();
		if(topics!=null && topics.size()!=0){
			for (String topic : topics) {
				Set<String> ids = topicMap.get(topic);
				if(ids.contains(unid)){
					ids.remove(unid);
				}
			}
		}
		
	}

	@Override
	public void sendTextMessageByTopic(String topic, Map data,String key) {
		//封装消息
		Map<String,Object> message = new HashMap<>();
		message.put("key", key);
		message.put("data", data);
		String content = JSON.toJSONString(message);
		//批量推送消息
		Set<String> set = topicMap.get(topic);
		if(set==null||set.size()==0){
			logger.info("topic "+topic+" doesn't exist!");
			return;
		}
		for (String unid : set) {
			sendTextMessage(unid, content);
		}
	}
}

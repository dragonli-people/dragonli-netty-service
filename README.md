# dragonli-netty-service
* service.micro-service.simple-netty-service.onlinePrefix 这里定义的路径，需要在zookpeer上定义该目录。可通过--NETTY_ONLINE_ZOOKEEPER_PATH_PREFIX覆盖此值
* service.micro-service.simple-netty-service.configsStr: 可在配置中心修改。也可通过--NETTY_CONFIG_STR覆盖此项配置（尤其在本地调试之时）
    * 每一段格式示例：192.168.7.101:8100;ws://192.168.7.101:8100
    * 整个配置项可以包含若干个上述的段，以空白字符（比如回车）分隔即可。整个配置项将被当成一个大字符串处理，字符串两端的空白字符将被忽略
    * 每一段的解释：
        * ;之前的半段，为服务器内网ip:指定的netty服务的端口号 
        * ;之后的半段，是提供给外网的完整访问地址（尤其请考虑生产环境有nginx反向代理的情况）
    * 在开发环境中，如果需要调试，可在yml文件中临时配置成自己开发机的ip
* privatekey：这个参数最好通过 --CONFIG_PRIVATE_KEY 覆盖成私有订制值。是加密验证uniqueId是否合法的私钥。
* 您可以修改service.micro-service.simple-netty-service来修改服务所需的配置
* 与所有服务一样，您可以设置 --MICRO_SERVICE_PORT、--MICRO_SERVICE_GROUP、--MICRO_SERVICE_HTTP_PORT分别用来覆盖微服务端口号、分组、http端口号（尤其是在端口号冲突之时）

package org.dragonli.service.general.netty;

import org.apache.log4j.Logger;
import org.dragonli.service.dubbosupport.DubboApplicationBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import com.alibaba.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication( exclude={DataSourceAutoConfiguration.class},scanBasePackages={"org.dragonli"})
@DubboComponentScan(basePackages = "org.dragonli.service.general.netty")
public class CryptoNettyApplication extends DubboApplicationBase {

	public CryptoNettyApplication(
			@Value("${service.micro-service.simple-netty-service.application-name}") String applicationName,
			@Value("${service.micro-service.common.registry-address}") String registryAddr,
			@Value("${service.micro-service.simple-netty-service.protocol-name}") String protocolName,
			@Value("${service.micro-service.simple-netty-service.protocol-port}") Integer protocolPort,
			@Value("${service.micro-service.simple-netty-service.scan}") String registryId,
			@Value("${service.micro-service.simple-netty-service.http-port}")  int port
		)
	{

		super(applicationName, registryAddr, protocolName, protocolPort, registryId, port);
	}

	@SuppressWarnings(value = "unused")
	final Logger logger = Logger.getLogger(getClass());

	public static void main(String[] args) {
		SpringApplication.run(CryptoNettyApplication.class, args);
	}
}

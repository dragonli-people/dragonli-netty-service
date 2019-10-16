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
			@Value("${spring.netty-service.application.name}") String applicationName,
			@Value("${spring.common.registry.address}") String registryAddr,
			@Value("${spring.netty-service.protocol.name}") String protocolName,
			@Value("${spring.netty-service.protocol.port}") Integer protocolPort,
			@Value("${spring.netty-service.scan}") String registryId,
			@Value("${micro-service-port.netty-service-port}") int port
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

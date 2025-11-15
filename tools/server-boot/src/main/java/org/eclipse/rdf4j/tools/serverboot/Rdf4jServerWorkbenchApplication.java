package org.eclipse.rdf4j.tools.serverboot;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.catalina.Context;
import org.eclipse.rdf4j.common.webapp.filters.PathFilter;
import org.eclipse.rdf4j.workbench.proxy.CacheFilter;
import org.eclipse.rdf4j.workbench.proxy.CookieCacheControlFilter;
import org.eclipse.rdf4j.workbench.proxy.WorkbenchGateway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import com.github.ziplet.filter.compression.CompressingFilter;

@SpringBootApplication
public class Rdf4jServerWorkbenchApplication {

	public static void main(String[] args) {
		SpringApplication.run(Rdf4jServerWorkbenchApplication.class, args);
	}

	@Bean(destroyMethod = "close")
	WebappResourceExtractor webappResourceExtractor() {
		return new WebappResourceExtractor();
	}

	@Bean
	TomcatServletWebServerFactory tomcatFactory(WebappResourceExtractor extractor) {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
		factory.addContextCustomizers(workbenchResourcesCustomizer(extractor));
		return factory;
	}

	private TomcatContextCustomizer workbenchResourcesCustomizer(WebappResourceExtractor extractor) {
		return (Context context) -> context.setDocBase(extractor.getServerDocBase().toFile().getAbsolutePath());
	}

	@Bean
	ServletRegistrationBean<DispatcherServlet> rdf4jServerServlet(ApplicationContext parentContext) {
		DispatcherServlet dispatcherServlet = new DispatcherServlet();
		dispatcherServlet.setContextClass(ServerXmlWebApplicationContext.class);
		dispatcherServlet.setContextConfigLocation(String.join(",",
				"classpath:/rdf4j/server-webapp/WEB-INF/common-webapp-servlet.xml",
				"classpath:/rdf4j/server-webapp/WEB-INF/common-webapp-system-servlet.xml",
				"classpath:/rdf4j/server-webapp/WEB-INF/rdf4j-http-server-servlet.xml"));
		ServletRegistrationBean<DispatcherServlet> registration = new ServletRegistrationBean<>(dispatcherServlet,
				"/protocol/*", "/repositories/*", "*.view", "*.form");
		registration.setName("rdf4jServer");
		registration.setLoadOnStartup(1);
		return registration;
	}

	@Bean
	ServletRegistrationBean<WorkbenchGateway> rdf4jWorkbenchServlet() {
		WorkbenchGateway servlet = new WorkbenchGateway();
		ServletRegistrationBean<WorkbenchGateway> registration = new ServletRegistrationBean<>(servlet,
				"/rdf4j-workbench/repositories/*");
		registration.setName("rdf4jWorkbench");
		registration.setLoadOnStartup(2);
		registration.setInitParameters(workbenchInitParameters());
		return registration;
	}

	@Bean
	FilterRegistrationBean<ServerPrefixForwardFilter> serverPrefixForwardFilter() {
		FilterRegistrationBean<ServerPrefixForwardFilter> registration = new FilterRegistrationBean<>(
				new ServerPrefixForwardFilter());
		registration.addUrlPatterns("/rdf4j-server", "/rdf4j-server/*");
		registration.setName("ServerPrefixForwardFilter");
		registration.setOrder(-20);
		return registration;
	}

	@Bean
	FilterRegistrationBean<CompressingFilter> compressingFilter() {
		FilterRegistrationBean<CompressingFilter> registration = new FilterRegistrationBean<>(new CompressingFilter());
		registration.addUrlPatterns("/rdf4j-server/*");
		registration.setName("CompressingFilter");
		registration.setOrder(-10);
		registration.addInitParameter("excludeContentTypes",
				"application/x-binary-rdf,application/x-binary-rdf-results-table");
		return registration;
	}

	@Bean
	FilterRegistrationBean<UrlRewriteFilter> urlRewriteFilter() {
		FilterRegistrationBean<UrlRewriteFilter> registration = new FilterRegistrationBean<>(new UrlRewriteFilter());
		registration.addUrlPatterns("/rdf4j-server", "/rdf4j-server/");
		registration.setName("UrlRewriteFilter");
		registration.setOrder(-9);
		registration.addInitParameter("logLevel", "commons");
		registration.addInitParameter("statusEnabled", "false");
		return registration;
	}

	@Bean
	FilterRegistrationBean<PathFilter> pathFilter() {
		FilterRegistrationBean<PathFilter> registration = new FilterRegistrationBean<>(new PathFilter());
		registration.addUrlPatterns("*.css");
		registration.setName("PathFilter");
		registration.setOrder(-8);
		return registration;
	}

	private Map<String, String> workbenchInitParameters() {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("transformations", "/transformations");
		params.put("default-server", "/rdf4j-server");
		params.put("accepted-server-prefixes", "file: http: https:");
		params.put("change-server-path", "/NONE/server");
		params.put("cookie-max-age", "2592000");
		params.put("no-repository-id", "NONE");
		params.put("default-path", "/NONE/repositories");
		params.put("default-command", "/summary");
		params.put("default-limit", "100");
		params.put("default-queryLn", "SPARQL");
		params.put("default-infer", "true");
		params.put("default-Accept", "application/rdf+xml");
		params.put("default-Content-Type", "application/rdf+xml");
		params.put("/summary", "org.eclipse.rdf4j.workbench.commands.SummaryServlet");
		params.put("/info", "org.eclipse.rdf4j.workbench.commands.InfoServlet");
		params.put("/information", "org.eclipse.rdf4j.workbench.commands.InformationServlet");
		params.put("/repositories", "org.eclipse.rdf4j.workbench.commands.RepositoriesServlet");
		params.put("/create", "org.eclipse.rdf4j.workbench.commands.CreateServlet");
		params.put("/delete", "org.eclipse.rdf4j.workbench.commands.DeleteServlet");
		params.put("/namespaces", "org.eclipse.rdf4j.workbench.commands.NamespacesServlet");
		params.put("/contexts", "org.eclipse.rdf4j.workbench.commands.ContextsServlet");
		params.put("/types", "org.eclipse.rdf4j.workbench.commands.TypesServlet");
		params.put("/explore", "org.eclipse.rdf4j.workbench.commands.ExploreServlet");
		params.put("/query", "org.eclipse.rdf4j.workbench.commands.QueryServlet");
		params.put("/saved-queries", "org.eclipse.rdf4j.workbench.commands.SavedQueriesServlet");
		params.put("/export", "org.eclipse.rdf4j.workbench.commands.ExportServlet");
		params.put("/add", "org.eclipse.rdf4j.workbench.commands.AddServlet");
		params.put("/remove", "org.eclipse.rdf4j.workbench.commands.RemoveServlet");
		params.put("/clear", "org.eclipse.rdf4j.workbench.commands.ClearServlet");
		params.put("/update", "org.eclipse.rdf4j.workbench.commands.UpdateServlet");
		return params;
	}

	@Bean
	FilterRegistrationBean<WorkbenchRootRedirectFilter> redirectFilter() {
		FilterRegistrationBean<WorkbenchRootRedirectFilter> registration = new FilterRegistrationBean<>(
				new WorkbenchRootRedirectFilter());
		registration.addUrlPatterns("/rdf4j-workbench", "/rdf4j-workbench/", "/rdf4j-workbench/*");
		registration.setName("workbenchRootRedirect");
		registration.setOrder(0);
		return registration;
	}

	@Bean
	FilterRegistrationBean<CookieCacheControlFilter> cookieCacheFilter() {
		FilterRegistrationBean<CookieCacheControlFilter> registration = new FilterRegistrationBean<>(
				new CookieCacheControlFilter());
		registration.addUrlPatterns("/rdf4j-workbench/repositories/*");
		registration.setName("cache");
		registration.setOrder(1);
		return registration;
	}

	@Bean
	FilterRegistrationBean<CacheFilter> cacheFilter() {
		FilterRegistrationBean<CacheFilter> registration = new FilterRegistrationBean<>(new CacheFilter());
		registration.addUrlPatterns("/rdf4j-workbench/*");
		registration.setName("CacheFilter");
		registration.setOrder(2);
		registration.addInitParameter("Cache-Control", "600");
		return registration;
	}

	static class ServerXmlWebApplicationContext extends XmlWebApplicationContext {
		ServerXmlWebApplicationContext() {
			setAllowBeanDefinitionOverriding(true);
			setClassLoader(Rdf4jServerWorkbenchApplication.class.getClassLoader());
		}

		@Override
		protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
			super.postProcessBeanFactory(beanFactory);
			beanFactory.addBeanPostProcessor(new SimpleUrlHandlerMappingPostProcessor());
		}

		@Override
		protected void onRefresh() {
			super.onRefresh();
			getBeansOfType(AbstractHandlerMapping.class).values()
					.forEach(mapping -> mapping.setAlwaysUseFullPath(false));
		}

		private static class SimpleUrlHandlerMappingPostProcessor implements BeanPostProcessor {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (bean instanceof SimpleUrlHandlerMapping) {
					SimpleUrlHandlerMapping mapping = (SimpleUrlHandlerMapping) bean;
					mapping.setAlwaysUseFullPath(false);
				}
				return bean;
			}
		}
	}
}

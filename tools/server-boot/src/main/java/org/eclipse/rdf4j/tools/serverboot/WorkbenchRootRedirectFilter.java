package org.eclipse.rdf4j.tools.serverboot;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class WorkbenchRootRedirectFilter implements Filter {

	private static final String WORKBENCH_PREFIX = "/rdf4j-workbench";
	private static final String TARGET = "/rdf4j-workbench/repositories";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			chain.doFilter(request, response);
			return;
		}

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		String contextPath = httpRequest.getContextPath();
		String requestUri = httpRequest.getRequestURI();
		String workbenchPrefix = contextPath + WORKBENCH_PREFIX;

		if (requestUri.equals(workbenchPrefix) || requestUri.equals(workbenchPrefix + "/")) {
			httpResponse.sendRedirect(contextPath + TARGET);
			return;
		}

		chain.doFilter(request, response);
	}
}

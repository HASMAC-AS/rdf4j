package org.eclipse.rdf4j.tools.serverboot;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class ServerPrefixForwardFilter implements Filter {

	private static final String SERVER_PREFIX = "/rdf4j-server";

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
		String serverPrefix = contextPath + SERVER_PREFIX;

		if (requestUri.equals(serverPrefix) || requestUri.equals(serverPrefix + "/")) {
			httpResponse.sendRedirect(serverPrefix + "/overview.view");
			return;
		}

		if (requestUri.startsWith(serverPrefix + "/")) {
			String remainder = requestUri.substring(serverPrefix.length());
			if (remainder.isEmpty()) {
				remainder = "/";
			}
			RequestDispatcher dispatcher = request.getRequestDispatcher(remainder);
			dispatcher.forward(request, response);
			return;
		}

		chain.doFilter(request, response);
	}
}

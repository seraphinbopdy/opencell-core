package org.meveo.security.filters;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.meveo.util.view.PageAccessHandler;

@WebFilter(urlPatterns = "/*")
public class AuthorizationFilter implements Filter {

    @Inject
    private PageAccessHandler pageAccessHandler;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        boolean isPermited = pageAccessHandler.isCurrentURLAccesible();

        if (!isPermited) {
            ((HttpServletResponse) response).setStatus(403);

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String url = httpRequest.getRequestURL().toString();
            String contextPath = httpRequest.getContextPath();
            url = url.substring(url.indexOf(contextPath) + contextPath.length());

            if (!url.startsWith("/api")) {
                String page = "/errors/403.jsf";
                RequestDispatcher dispatcher = httpRequest.getRequestDispatcher(page);
                dispatcher.forward(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
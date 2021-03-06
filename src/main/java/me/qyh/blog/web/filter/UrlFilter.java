/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.util.UrlUtils;
import me.qyh.blog.web.Webs;

/**
 * 配置多域名访问时，用来将space.abc.com请求转发至abc.com/space<br>
 * 没有配置多域名时候，用来将space.ab.com重定向至abc.com/space
 * 
 * @author Administrator
 *
 */
public class UrlFilter extends OncePerRequestFilter {

	private UrlHelper urlHelper;
	private static final Logger LOGGER = LoggerFactory.getLogger(UrlFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain fc)
			throws ServletException, IOException {
		if (Webs.isAction(req)) {
			String host = req.getServerName();
			if (urlHelper.maybeSpaceDomain(req)) {
				String space = host.split("\\.")[0];
				LOGGER.debug("从空间域名请求中获取空间名:" + space);
				// 如果开启了域名
				if (urlHelper.isEnableSpaceDomain()) {
					String requestUrl = buildForwardUrl(req, space);
					LOGGER.debug(UrlUtils.buildFullRequestUrl(req) + "转发请求到:" + requestUrl);
					req.getRequestDispatcher(requestUrl).forward(req, resp);
					return;
				} else {
					String requestURI = req.getRequestURI();
					String queryString = req.getQueryString();
					StringBuilder sb = new StringBuilder();
					sb.append(urlHelper.getUrl()).append("/space/").append(space).append(requestURI);
					if (queryString != null) {
						sb.append("?").append(queryString);
					}
					resp.setStatus(301);
					resp.setHeader("Location", sb.toString());
					resp.setHeader("Connection", "close");
					return;
				}
			} else {
				String url = UrlUtils.buildFullRequestUrl(req);
				if (!url.startsWith(urlHelper.getUrl())) {
					String requestURI = req.getRequestURI();
					String queryString = req.getQueryString();
					StringBuilder sb = new StringBuilder();
					sb.append(urlHelper.getUrl()).append(requestURI);
					if (queryString != null) {
						sb.append("?").append(queryString);
					}
					resp.setStatus(301);
					resp.setHeader("Location", sb.toString());
					resp.setHeader("Connection", "close");
					return;
				}
			}
		}
		fc.doFilter(req, resp);
	}

	@Override
	protected void initFilterBean() throws ServletException {
		urlHelper = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext())
				.getBean(UrlHelper.class);
	}

	private String buildForwardUrl(HttpServletRequest request, String space) {
		return "/space/" + space + request.getRequestURI().substring(request.getContextPath().length());
	}
}

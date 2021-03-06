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
package me.qyh.blog.evt.ping;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;

import me.qyh.blog.entity.Article;
import me.qyh.blog.evt.ArticleEvent;
import me.qyh.blog.evt.EventType;
import me.qyh.blog.exception.SystemException;

/**
 * 简单的ping管理器
 * 
 * @author Administrator
 *
 */
public class SimplePingManager implements ApplicationListener<ArticleEvent>, InitializingBean {

	protected static final Logger LOGGER = LoggerFactory.getLogger(SimplePingManager.class);

	private List<PingService> pingServices = new ArrayList<>();
	private final String blogName;

	@Autowired
	private ThreadPoolTaskExecutor taskExecutor;

	public SimplePingManager(String blogName) {
		super();
		this.blogName = blogName;
	}

	@Async
	@Override
	public void onApplicationEvent(ArticleEvent event) {
		List<Article> articles = event.getArticles();
		articles.stream().filter(article -> needPing(event.getEventType(), article)).forEach(this::ping);
	}

	private boolean needPing(EventType eventType, Article article) {
		return ((EventType.INSERT.equals(eventType) || EventType.UPDATE.equals(eventType)) && article.isPublished()
				&& !article.hasLock() && !article.isPrivate());

	}

	private void ping(Article article) {
		pingServices.stream().forEach(pingService -> CompletableFuture.runAsync(() -> {
			try {
				pingService.ping(article, blogName);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		}, taskExecutor));
	}

	public void setPingServices(List<PingService> pingServices) {
		this.pingServices = pingServices;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (CollectionUtils.isEmpty(pingServices)) {
			throw new SystemException("ping服务不能为空");
		}
	}

}

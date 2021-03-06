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
package me.qyh.blog.comment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.mail.MailSender;
import me.qyh.blog.mail.MailSender.MessageBean;
import me.qyh.blog.message.Messages;
import me.qyh.blog.util.FileUtils;
import me.qyh.blog.util.Resources;
import me.qyh.blog.util.SerializationUtils;

/**
 * 用来向管理员发送评论|回复通知邮件
 * <p>
 * <strong>删除评论不会对邮件的发送造成影响，即如果发送队列中或者待发送列表中的一条评论已经被删除，那么它将仍然被发送</strong>
 * </p>
 * 
 * @author Administrator
 *
 */
public class CommentEmailNotifySupport implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(CommentEmailNotifySupport.class);
	private ConcurrentLinkedQueue<Comment> toProcesses = new ConcurrentLinkedQueue<>();
	private List<Comment> toSend = Collections.synchronizedList(new ArrayList<>());
	private MailTemplateEngine mailTemplateEngine = new MailTemplateEngine();
	private Resource mailTemplateResource;
	private String mailTemplate;
	private String mailSubject;

	private File toSendSdfile = new File(FileUtils.getHomeDir(), "toSendSdfile.dat");
	private File toProcessesSdfile = new File(FileUtils.getHomeDir(), "toProcessesSdfile.dat");

	/**
	 * 每隔5秒从评论队列中获取评论放入待发送列表
	 */
	private static final Integer MESSAGE_PROCESS_SEC = 5;

	/**
	 * 如果待发送列表中有10或以上的评论，立即发送邮件
	 */
	private static final Integer MESSAGE_TIP_COUNT = 10;

	/**
	 * 如果发送列表中存在待发送评论，但是数量始终没有达到10条，那么每隔300秒会发送邮件同时清空发送列表
	 */
	private static final Integer MESSAGE_PROCESS_PERIOD_SEC = 300;

	private int messageProcessSec = MESSAGE_PROCESS_SEC;
	private int messageProcessPeriodSec = MESSAGE_PROCESS_PERIOD_SEC;
	private int messageTipCount = MESSAGE_TIP_COUNT;

	/**
	 * 邮件发送，用来提示管理员有新的回复或者评论
	 */
	@Autowired
	private MailSender mailSender;
	@Autowired
	private UrlHelper urlHelper;
	@Autowired
	private Messages messages;
	@Autowired
	private ThreadPoolTaskScheduler threadPoolTaskScheduler;

	/**
	 * 系统关闭时序列化待发送列表和待处理列表
	 */
	public final void shutdown() throws Exception {
		if (!toSend.isEmpty()) {
			SerializationUtils.serialize(toSend, toSendSdfile);
		}
		if (!toProcesses.isEmpty()) {
			SerializationUtils.serialize(toProcesses, toProcessesSdfile);
		}
	}

	private void sendMail(List<Comment> comments, String to) {
		Context context = new Context();
		context.setVariable("urls", urlHelper.getUrls());
		context.setVariable("comments", comments);
		context.setVariable("messages", messages);
		String text = mailTemplateEngine.process(mailTemplate, context);
		MessageBean mb = new MessageBean(mailSubject, true, text);
		if (to != null) {
			mb.setTo(to);
		}
		mailSender.send(mb);
	}

	private final class MailTemplateEngine extends TemplateEngine {
		public MailTemplateEngine() {
			setTemplateResolver(new StringTemplateResolver());
		}
	}

	@Override
	public final void afterPropertiesSet() throws Exception {
		if (mailSubject == null) {
			throw new SystemException("邮件标题不能为空");
		}

		if (mailTemplateResource == null) {
			mailTemplateResource = new ClassPathResource("resources/page/defaultMailTemplate.html");
		}

		mailTemplate = Resources.readResourceToString(mailTemplateResource);

		if (messageProcessPeriodSec <= 0) {
			messageProcessPeriodSec = MESSAGE_PROCESS_PERIOD_SEC;
		}
		if (messageProcessSec <= 0) {
			messageProcessSec = MESSAGE_PROCESS_SEC;
		}
		if (messageTipCount <= 0) {
			messageTipCount = MESSAGE_TIP_COUNT;
		}

		if (toSendSdfile.exists()) {
			this.toSend = SerializationUtils.deserialize(toSendSdfile);
			if (!FileUtils.deleteQuietly(toSendSdfile)) {
				LOGGER.warn("删除文件:" + toSendSdfile.getAbsolutePath() + "失败，这会导致邮件重复发送");
			}
		}

		if (toProcessesSdfile.exists()) {
			this.toProcesses = SerializationUtils.deserialize(toProcessesSdfile);
			if (!FileUtils.deleteQuietly(toProcessesSdfile)) {
				LOGGER.warn("删除文件:" + toProcessesSdfile.getAbsolutePath() + "失败，这会导致邮件重复发送");
			}
		}

		threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
			synchronized (toSend) {
				int size = toSend.size();
				for (Iterator<Comment> iterator = toProcesses.iterator(); iterator.hasNext();) {
					Comment toProcess = iterator.next();
					toSend.add(toProcess);
					size++;
					iterator.remove();
					if (size >= messageTipCount) {
						LOGGER.debug("发送列表尺寸达到" + messageTipCount + "立即发送邮件通知");
						sendMail(toSend, null);
						toSend.clear();
						break;
					}
				}
			}
		}, messageProcessSec * 1000L);

		threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
			synchronized (toSend) {
				if (!toSend.isEmpty()) {
					LOGGER.debug("待发送列表不为空，将会发送邮件，无论发送列表是否达到" + messageTipCount);
					sendMail(toSend, null);
					toSend.clear();
				}
			}
		}, messageProcessPeriodSec * 1000L);
	}

	public void setMailTemplateResource(Resource mailTemplateResource) {
		this.mailTemplateResource = mailTemplateResource;
	}

	public void setMailSubject(String mailSubject) {
		this.mailSubject = mailSubject;
	}

	public void setMessageProcessSec(int messageProcessSec) {
		this.messageProcessSec = messageProcessSec;
	}

	public void setMessageProcessPeriodSec(int messageProcessPeriodSec) {
		this.messageProcessPeriodSec = messageProcessPeriodSec;
	}

	public void setMessageTipCount(int messageTipCount) {
		this.messageTipCount = messageTipCount;
	}

	public final void handle(Comment comment) {
		Comment parent = comment.getParent();
		// 如果在用户登录的情况下评论，一律不发送邮件
		// 如果回复了管理员
		if (!comment.getAdmin() && (parent == null || parent.getAdmin())) {
			toProcesses.add(comment);
		}
		// 如果父评论不是管理员的评论
		// 如果回复是管理员
		if (parent != null && parent.getEmail() != null && !parent.getAdmin() && comment.getAdmin()) {
			// 直接邮件通知被回复对象
			sendMail(Arrays.asList(comment), comment.getParent().getEmail());
		}
	}

}

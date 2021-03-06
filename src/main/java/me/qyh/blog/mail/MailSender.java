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
package me.qyh.blog.mail;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import me.qyh.blog.config.Constants;
import me.qyh.blog.config.UserConfig;
import me.qyh.blog.util.FileUtils;
import me.qyh.blog.util.SerializationUtils;
import me.qyh.blog.util.Validators;

/**
 * 邮件发送服务
 * 
 * @author Administrator
 *
 */
public class MailSender implements InitializingBean, ApplicationListener<ContextClosedEvent> {

	@Autowired
	private JavaMailSender javaMailSender;
	@Autowired
	private ThreadPoolTaskScheduler threadPoolTaskScheduler;
	@Autowired
	private ThreadPoolTaskExecutor threadPoolTaskExecutor;

	private ConcurrentLinkedQueue<MessageBean> queue = new ConcurrentLinkedQueue<>();

	private static final Logger LOGGER = LoggerFactory.getLogger(MailSender.class);

	/**
	 * 应用关闭时未发送的信息存入文件中
	 */
	private final File sdfile = new File(FileUtils.getHomeDir(), "message_shutdown.dat");

	/**
	 * 处理队列的时间(ms)
	 */
	private int processQueueDelay = 1000;

	/**
	 * 将邮件加入发送队列
	 * 
	 * @param mb
	 *            邮件对象
	 */
	public void send(MessageBean mb) {
		queue.add(mb);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (sdfile.exists()) {
			LOGGER.debug("发现序列化文件，执行反序列化操作");
			queue = SerializationUtils.deserialize(sdfile);
			if (!FileUtils.deleteQuietly(sdfile)) {
				LOGGER.warn("删除序列文件失败");
			}
		}
		threadPoolTaskScheduler.scheduleWithFixedDelay(() -> {
			MessageBean mb = queue.poll();
			if (mb != null) {
				sendMail(mb);
			}
		}, processQueueDelay);
	}

	private void sendMail(final MessageBean mb) {
		threadPoolTaskExecutor.execute(() -> {
			try {
				String email = mb.to;
				if (Validators.isEmptyOrNull(email, true)) {
					email = UserConfig.get().getEmail();
				}
				if (Validators.isEmptyOrNull(email, true)) {
					LOGGER.error("接受人邮箱为空，无法发送邮件");
					return;
				}
				javaMailSender.send((mimeMessage) -> {
					MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, mb.html, Constants.CHARSET.name());
					helper.setText(mb.text, mb.html);
					helper.setTo(Validators.isEmptyOrNull(mb.to, true) ? UserConfig.get().getEmail() : mb.to);
					helper.setSubject(mb.subject);
					mimeMessage.setFrom();
				});
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		});
	}

	/**
	 * 
	 * @author Administrator
	 *
	 */
	public static final class MessageBean implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private final String subject;
		private final boolean html;
		private final String text;
		private String to;

		/**
		 * 
		 * @param subject
		 *            主题
		 * @param html
		 *            是否是html
		 * @param text
		 *            内容
		 */
		public MessageBean(String subject, boolean html, String text) {
			super();
			this.subject = subject;
			this.html = html;
			this.text = text;
		}

		public void setTo(String to) {
			this.to = to;
		}

		@Override
		public String toString() {
			return "MessageBean [subject=" + subject + ", html=" + html + ", text=" + text + "]";
		}

	}

	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
		if (!queue.isEmpty()) {
			LOGGER.debug("队列中存在未发送邮件，序列化到本地:" + sdfile.getAbsolutePath());
			try {
				SerializationUtils.serialize(queue, sdfile);
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

	public void setProcessQueueDelay(int processQueueDelay) {
		this.processQueueDelay = processQueueDelay;
	}

}

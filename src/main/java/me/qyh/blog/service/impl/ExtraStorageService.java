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
package me.qyh.blog.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import me.qyh.blog.util.FileUtils;
import me.qyh.blog.util.SerializationUtils;

/**
 * 额外的存储，以键值对的形式用来存放一些<b>文本数据</b>
 * <p>
 * 仅供管理员调用<br>
 * <b>首先会存储在内存中，当服务关闭时，会保存到extra.dat文件中，当服务启动时，会从extra.dat读取数据</b><br>
 * <b>不适合存储大量数据</b>
 * </p>
 * 
 * @author Administrator
 *
 */
@Service
public class ExtraStorageService implements InitializingBean {

	private final File dataFile = new File(FileUtils.getHomeDir(), "extra.dat");

	private Map<String, String> dataMap = Collections.synchronizedMap(new HashMap<>());

	/**
	 * 存储键值对数据
	 * 
	 * @param k
	 * @param v
	 */
	public void store(String k, String v) {
		dataMap.put(k, v);
	}

	/**
	 * 根据key查询数据
	 * 
	 * @param k
	 * @return
	 */
	public Optional<String> get(String k) {
		return Optional.ofNullable(dataMap.get(k));
	}

	/**
	 * 删除指定key
	 * 
	 * @param k
	 */
	public void remove(String k) {
		dataMap.remove(k);
	}

	@EventListener
	public void handleCloseEvent(ContextClosedEvent evt) throws IOException {
		synchronized (dataMap) {
			SerializationUtils.serialize(dataMap, dataFile);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// load map from dataFile
		if (dataFile.exists()) {
			dataMap = SerializationUtils.deserialize(dataFile);
		}
	}
}

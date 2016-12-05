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
package me.qyh.blog.ui;

import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.ui.data.DataBind;
import me.qyh.blog.ui.fragment.Fragment;

/**
 * 
 * @author Administrator
 *
 */
public final class TemplateParser {

	private static final String DATA_TAG = "data";
	private static final String FRAGEMENT = "fragment";
	private static final String NAME_ATTR = "name";

	public interface DataQuery {
		/**
		 * 根据用户的widget标签查询对应的widgetTpl
		 * 
		 * @param dataTag
		 *            widget标签，不会为null
		 * @return
		 * @throws LogicException
		 * @throws MissParamException
		 */
		DataBind<?> query(DataTag dataTag) throws LogicException;
	}

	public interface FragmentQuery {

		Fragment query(String name);
	}

	public DataTag parse(String dataTagStr) {
		Element body = Jsoup.parseBodyFragment(dataTagStr).body();
		Elements eles = body.children();
		if (eles.size() == 1) {
			Element ele = eles.get(0);
			if (DATA_TAG.equals(ele.tagName())) {
				String name = ele.attr(NAME_ATTR);
				if (name != null) {
					DataTag dataTag = new DataTag(name);
					Attributes attributes = ele.attributes();
					if (attributes != null) {
						for (Attribute attribute : attributes) {
							dataTag.put(attribute.getKey(), attribute.getValue());
						}
					}
					return dataTag;
				}
			}
		}
		return null;
	}

	public ParseResult parse(String tpl, DataQuery dquery, FragmentQuery fquery) throws LogicException {
		ParseResult result = new ParseResult();
		Document doc = Jsoup.parse(tpl);
		clean(doc);
		Elements dataEles = doc.getElementsByTag(DATA_TAG);
		Map<DataTag, DataBind<?>> cache = Maps.newLinkedHashMap();
		for (Element dataEle : dataEles) {
			String name = dataEle.attr(NAME_ATTR);
			DataTag tag = new DataTag(name);
			Attributes attributes = dataEle.attributes();
			if (attributes != null) {
				for (Attribute attribute : attributes) {
					tag.put(attribute.getKey(), attribute.getValue());
				}
			}
			DataBind<?> bind = !cache.containsKey(tag) ? dquery.query(tag) : cache.get(tag);
			if (bind != null) {
				cache.put(tag, bind);
				dataEle.removeAttr(name);
			} else {
				result.addUnkownData(tag);
				removeElement(dataEle);
			}
		}
		Elements fragmentEles = doc.getElementsByTag(FRAGEMENT);
		for (Element fragmentEle : fragmentEles) {
			String name = fragmentEle.attr(NAME_ATTR);
			Fragment fragment = fquery.query(name);
			if (fragment == null) {
				result.addUnkownFragment(name);
				removeElement(fragmentEle);
			} else {
				result.putFragment(name, fragment);
			}
		}
		result.setBinds(Lists.newArrayList(cache.values()));
		return result;
	}

	public static String buildFragmentTag(String name, Map<String, String> atts) {
		Tag tag = Tag.valueOf(FRAGEMENT);
		Attributes attributes = new Attributes();
		if (!CollectionUtils.isEmpty(atts))
			for (Map.Entry<String, String> it : atts.entrySet())
				attributes.put(it.getKey(), it.getValue());
		attributes.put(NAME_ATTR, name);
		Element ele = new Element(tag, "", attributes);
		return ele.toString();
	}

	private void clean(Document doc) {
		// 删除包含挂件自标签的挂件标签
		doc.select("data:has(data)").remove();
		doc.select("fragment:has(fragment)").remove();
		// 删除没有包含name属性的标签
		doc.select("data:not([name])").remove();
		doc.select("fragment:not([name])").remove();
		// 删除属性为空的标签
		doc.select("data[name~=^$]").remove();
		doc.select("fragment[name~=^$]").remove();
	}

	private void removeElement(Element e) {
		try {
			e.remove();
		} catch (Exception ex) {

		}
	}
}

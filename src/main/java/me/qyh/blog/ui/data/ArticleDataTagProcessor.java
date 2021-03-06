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
package me.qyh.blog.ui.data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleFrom;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.Editor;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.ui.ContextVariables;

/**
 * 文章详情数据数据器
 * 
 * @author Administrator
 *
 */
public class ArticleDataTagProcessor extends DataTagProcessor<Article> {

	@Autowired
	private ArticleService articleService;

	private static final String ID_OR_ALIAS = "idOrAlias";

	public ArticleDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected Article buildPreviewData(Attributes attributes) {
		Article article = new Article();
		article.setComments(0);
		article.setEditor(Editor.MD);
		article.setContent("这是预览内容");
		article.setEditor(Editor.HTML);
		article.setFrom(ArticleFrom.ORIGINAL);
		article.setHits(10);
		article.setId(1);
		article.setIsPrivate(false);
		article.setLastModifyDate(Timestamp.valueOf(LocalDateTime.now()));
		article.setPubDate(Timestamp.valueOf(LocalDateTime.now()));
		article.setAllowComment(true);

		article.setSpace(getSpace());

		article.setStatus(ArticleStatus.PUBLISHED);
		article.setSummary("这是预览内容");
		article.setTitle("预览内容");
		Set<Tag> tags = new HashSet<>();
		tags.add(new Tag("预览标签"));

		article.setTags(tags);
		return article;
	}

	@Override
	protected Article query(ContextVariables variables, Attributes attributes) throws LogicException {
		// 首先从属性中获取
		String idOrAlias = attributes.get(ID_OR_ALIAS);
		if (idOrAlias == null) {
			// 从PathVariable中获取
			Object variable = variables.getPathVariable(ID_OR_ALIAS);
			if (variable != null) {
				idOrAlias = variable.toString();
			}
		}
		if (idOrAlias == null) {
			// 从参数中获取
			idOrAlias = variables.getParam(ID_OR_ALIAS);
		}
		if (idOrAlias == null) {
			throw new LogicException("article.notExists", "文章不存在");
		}
		return articleService.getArticleForView(idOrAlias)
				.orElseThrow(() -> new LogicException("article.notExists", "文章不存在"));
	}

}

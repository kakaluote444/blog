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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.bean.ArticleSpaceFile;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.ui.ContextVariables;

public class ArticleSpaceFilesDataTagProcessor extends DataTagProcessor<List<ArticleSpaceFile>> {

	@Autowired
	private ArticleService articleService;

	public ArticleSpaceFilesDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected List<ArticleSpaceFile> buildPreviewData(Attributes attributes) {
		List<ArticleSpaceFile> files = new ArrayList<>();

		ArticleSpaceFile file1 = new ArticleSpaceFile();
		file1.setSpace(getSpace());
		file1.setCount(1);
		files.add(file1);

		return files;
	}

	@Override
	protected List<ArticleSpaceFile> query(ContextVariables variables, Attributes attributes) throws LogicException {
		return articleService.queryArticleSpaceFiles();
	}

}

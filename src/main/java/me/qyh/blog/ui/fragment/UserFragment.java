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
package me.qyh.blog.ui.fragment;

import java.sql.Timestamp;
import java.util.Objects;

import me.qyh.blog.entity.Space;
import me.qyh.blog.util.Validators;

public class UserFragment extends Fragment {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Integer id;
	private String description;
	private Timestamp createDate;
	private Space space;
	private boolean global;

	public UserFragment() {
		super();
	}

	public UserFragment(UserFragment fragment) {
		super(fragment);
		this.id = fragment.id;
		this.description = fragment.description;
		this.createDate = fragment.createDate;
		this.space = fragment.space;
		this.global = fragment.global;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Timestamp getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Timestamp createDate) {
		this.createDate = createDate;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public boolean hasId() {
		return id != null;
	}

	public Space getSpace() {
		return space;
	}

	public void setSpace(Space space) {
		this.space = space;
	}

	public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getName());
	}

	@Override
	public boolean equals(Object obj) {
		if (Validators.baseEquals(this, obj)) {
			UserFragment other = (UserFragment) obj;
			return Objects.equals(this.getName(), other.getName());
		}
		return false;
	}

}

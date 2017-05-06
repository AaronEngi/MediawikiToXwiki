/*
 * Copyright (c) 2010 mkirst(at portolancs dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xwiki.migration.mediawiki.xmldump.model;

import java.util.ArrayList;
import java.util.List;

/**
 * SIM "Special Intermediate Model"
 * 
 * There are many {@link SimTemplateDefinition}s per one name space.
 * Each {@link SimNamespace} is unique because of its attribute 'namespace'.
 * 
 * @author mkirst(at portolancs dot com)
 */
public class SimNamespace {

	private String namespace;
	private String key;
	private List<SimTemplateDefinition> texts = new ArrayList<SimTemplateDefinition>();

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public int sizeDefinitions() {
		return texts.size();
	}

	public boolean add(SimTemplateDefinition e) {
		return texts.add(e);
	}

	public SimTemplateDefinition get(int index) {
		return texts.get(index);
	}


}

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
package org.xwiki.migration.mediawiki.xmldump;

import org.xwiki.migration.mediawiki.xmldump.model.SimPage;
import org.xwiki.migration.mediawiki.xmldump.model.SimNamespace;

/**
 * This filtering happens before transformation of MediaWiki pages
 * 
 * @author mkirst(at portolancs dot com)
 */
public interface IPreTransformFilter {

	/**
	 * Do your filtering stuff on this concrete page.
	 * Templates also can be used or modified.
	 *
	 * @param page
	 * @param templates
	 */
	public abstract void filterPage(SimPage page, SimNamespace... templates);

}

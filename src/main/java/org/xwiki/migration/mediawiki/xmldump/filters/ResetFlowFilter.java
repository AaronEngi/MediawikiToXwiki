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
package org.xwiki.migration.mediawiki.xmldump.filters;

import org.xwiki.migration.mediawiki.xmldump.IPreTransformFilter;
import org.xwiki.migration.mediawiki.xmldump.model.SimPage;
import org.xwiki.migration.mediawiki.xmldump.model.SimNamespace;


/**
 * In MediaWiki you can use {{-}} to stop floating elements.
 * For example, you've inserted a picture, which is left aligned and
 * have let the text float on the right side. But the next line
 * should be rendered below the image. This is the use case for {{-}}.
 * 
 * There is no such thing like this in XWiki, thus filter it out.
 * 
 * @author mkirst(at portolancs dot com)
 */
public class ResetFlowFilter implements IPreTransformFilter {

	/* (non-Javadoc)
	 * @see de.portolancs.mediawiki.xmldump.IPageFilter#filterPage(de.portolancs.mediawiki.xmldump.model.Page, de.portolancs.mediawiki.xmldump.model.Template[])
	 */
	@Override
	public void filterPage(SimPage page, SimNamespace... templates) {
		String text = page.getText();
		page.setText(text.replaceAll("\\{\\{-\\}\\}", ""));
	}

}

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.migration.mediawiki.xmldump.IPreTransformFilter;
import org.xwiki.migration.mediawiki.xmldump.model.SimPage;
import org.xwiki.migration.mediawiki.xmldump.model.SimNamespace;


/**
 * Filters all German occurrences of '[[Bild:???]]'
 * and change it to '[[Image:???]]'. Not case sensitive ;-)
 * 
 * @author mkirst(at portolancs dot com)
 */
public class BildFilter implements IPreTransformFilter {

	/* (non-Javadoc)
	 * @see de.portolancs.mediawiki.xmldump.IPageFilter#filterPage(de.portolancs.mediawiki.xmldump.model.Page, de.portolancs.mediawiki.xmldump.model.Template[])
	 */
	@Override
	public void filterPage(SimPage page, SimNamespace... templates) {
		String text = page.getText();
		StringBuilder source = new StringBuilder(text);
		Pattern p = Pattern.compile("\\[\\[(\\s*bild\\s*:\\s*)[^\\]]*?\\]\\]", Pattern.CASE_INSENSITIVE);
		for (Matcher m = p.matcher(source); m.find(); m = p.matcher(source)) {
			int start = m.start(1);
			int end = m.end(1);
			source.replace(start, end, "Image:");
		}
		p = Pattern.compile("\\[\\[(\\s*image\\s*:\\s*)[^\\]]*?\\]\\]");
		for (Matcher m = p.matcher(source); m.find(); m = p.matcher(source)) {
			int start = m.start(1);
			int end = m.end(1);
			source.replace(start, end, "Image:");
		}
		page.setText(source.toString());
	}

}

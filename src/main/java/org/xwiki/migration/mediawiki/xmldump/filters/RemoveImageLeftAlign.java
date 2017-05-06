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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.migration.mediawiki.xmldump.IPostTransformFilter;
import org.xwiki.migration.mediawiki.xmldump.model.SimPage;

/**
 * Removes all align="left" statements for images, cause looks ugly ;-)  
 * 
 * @author mkirst(at portolancs dot com)
 */
public class RemoveImageLeftAlign implements IPostTransformFilter {

	private final static String KILLIT = "align=\"left\"";
	
	@Override
	public void filterPage(SimPage page) {
		StringBuilder sb = new StringBuilder(page.getText());
		String regex = "\\[\\[image:[^\\]]*]]";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(sb);
		List<String> doimages = new ArrayList<String>();
		while (m.find()) {
			String s = m.group();
			if (s.indexOf(KILLIT) > -1) {
				doimages.add(s);
			}
		}
		for (String s : doimages) {
			int start = sb.indexOf(s);
			int end = start + s.length();
			String shorts = s.replace(KILLIT, "");
			sb.replace(start, end, shorts);
		}
		page.setText(sb.toString());
	}
	
}

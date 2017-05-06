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
import org.xwiki.migration.mediawiki.xmldump.model.SimTemplateDefinition;


/**
 * Filters all German occurrences of '{{Vorlage:???}}'
 * and change it to '{{Template:???}}'. Not case sensitive ;-)
 * 
 * @author mkirst(at portolancs dot com)
 */
public class VorlageFilter implements IPreTransformFilter {

	private static final String VORLAGE = "vorlage";
	private static final String TEMPLATE = "Template";

	/* (non-Javadoc)
	 * @see de.portolancs.mediawiki.xmldump.IPageFilter#filterPage(de.portolancs.mediawiki.xmldump.model.Page, de.portolancs.mediawiki.xmldump.model.Template[])
	 */
	@Override
	public void filterPage(SimPage page, SimNamespace... templates) {
		// clean templates name space first
		for (SimNamespace t : templates) {
			filterTemplate(t);
		}
		// clean the page
		String text = page.getText();
		for (SimNamespace t : templates) {
			final String ns = t.getNamespace();
			if (TEMPLATE.equals(ns)) {
				// unify and clean all 'Vorlage' to the one and only name space 'Template' 
				for (int i=0, len=t.sizeDefinitions(); i<len; i++) {
					final SimTemplateDefinition td = t.get(i);
					final String name = td.getTitle().substring(ns.length()+1);
					// sample: {{Vorlage:Hint}} OR {{Template:Hint}}
					// \{\{(?:Vorlage|Template):Hint\}\}   -- CASE_INSENSITIVE!!!
					String regex = "\\{\\{(?:Vorlage|" + ns + "):" + name + "\\}\\}";
					final Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
					final Matcher m = p.matcher(text);
					text = m.replaceAll("{{" + ns + ":" + name + "}}");
				}
			}
		}
		page.setText(text);
	}

	private void filterTemplate(SimNamespace template) {
		final String ns = template.getNamespace();
		if (VORLAGE.equals(ns.toLowerCase())) {
			template.setNamespace(TEMPLATE);
			for (int i=0, len=template.sizeDefinitions(); i<len; i++) {
				final SimTemplateDefinition td = template.get(i);
				final String title = td.getTitle();
				td.setTitle(title.replace(ns, TEMPLATE));
			}
		}
	}

}

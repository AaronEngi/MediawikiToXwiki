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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xwiki.migration.mediawiki.xmldump.filters.BildFilter;
import org.xwiki.migration.mediawiki.xmldump.filters.KategorieFilter;
import org.xwiki.migration.mediawiki.xmldump.filters.ResetFlowFilter;
import org.xwiki.migration.mediawiki.xmldump.filters.VorlageFilter;
import org.xwiki.migration.mediawiki.xmldump.model.SimNamespace;
import org.xwiki.migration.mediawiki.xmldump.model.SimPage;
import org.xwiki.migration.mediawiki.xmldump.model.SimTemplateDefinition;


/**
 * <strong>Features</strong>
 * <ul>
 * <li>Able to parse MediaWiki XML dump files.</li>
 * <li>Resolves Templates within page texts.</li>
 * </ul>
 *
 * <strong>Limits:</strong>
 * <ul>
 * <li>Does NOT support multiple revisions of articles!</li>
 * <li>Does NOT support nested templates!</li>
 * </ul>
 * 
 * <strong>Usage:</strong>
 * <ol>
 * <li>call {@link #parse(String)}</li>
 * <li>call {@link #filterPages(IPreTransformFilter...)} OPTIONAL, but recommended</li>
 * <li>call {@link #resolveTemplates()} OPTIONAL, but recommended</li>
 * <li>call {@link #getPages()}</li>
 * </ol>
 * 
 * NOT A REAL MAIN CLASS but for testing ;-)
 * 
 * @author mkirst(at portolancs dot com)
 */
public class DumpParser {

	private List<SimNamespace> templates;
	private List<SimPage> pages;
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// SAMPLE usage
		final DumpParser main = new DumpParser();
		try {
			main.parse("sample-data/PortoWiki-20100730160513.xml");
			main.filterPages(new VorlageFilter(), new BildFilter(), new KategorieFilter(), new ResetFlowFilter());
			main.resolveTemplates();
			main.getPages();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @throws IllegalStateException
	 */
	public void resolveTemplates() {
		if (pages == null || templates == null) {
			throw new IllegalStateException("Nothing parsed yet. Parse an XML dump file first!");
		}
		for (SimPage page : pages) {
			String text = page.getText();
			for (SimNamespace template : templates) {
				for (int i=0, len=template.sizeDefinitions(); i<len; i++) {
					final SimTemplateDefinition td = template.get(i);
					text = replaceTemplate(text, td);
				}
			}
			page.setText(text);
		}
	}

	/**
	 * @param source
	 * @param td
	 * @return
	 */
	private String replaceTemplate (final String source, SimTemplateDefinition td) {
		// "\{\{(?:Template:)?XXX[^}]*\}\}"
		// assuming that templates/macros can be used with or without name space,
		// means: {{Template:foo|param1}} is the same like {{foo|param1}}
		StringBuilder text = new StringBuilder(source);
		String tname = td.getTitle();
		if (tname.toLowerCase().startsWith("template:")) {
			tname = tname.substring("template:".length());
		}
		String regex = "\\{\\{(?:Template:)?"+ tname +"[^}]*\\}\\}";
		final Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		for (Matcher m = p.matcher(text); m.find(); m = p.matcher(text)) {
			int start = m.start();
			int end = m.end();
			String reference = text.substring(start, end);
			String t = resolveTemplateParameters(reference, td.getText());
			text = text.replace(start, end, t);
		}
		return text.toString();
	}
	
	/**
	 * @param reference
	 * @param definition
	 * @return
	 */
	private String resolveTemplateParameters(String reference, String definition) {
		String result = definition;
		if (reference.length() > 4 && reference.startsWith("{{") && reference.endsWith("}}")) {
            // template with named and unnamed parameters
            final String macro = reference.substring(2, reference.length()-2);
            String[] parts = macro.split("[|]");
            // String macroname = parts[0];
            for (int i=1; i<parts.length; i++) {
                String key = Integer.toString(i);
                String value = parts[i];
                int equidx = parts[i].indexOf('=');
                if (equidx > 0) {
                    key = parts[i].substring(0, equidx);
                    value = parts[i].substring(equidx+1);
                }
                result = result.replaceAll("\\{\\{\\{"+key+"\\}\\}\\}", value);
            }
		}
		return result;
	}
	
	/**
	 * @return
	 * @throws IllegalStateException
	 */
	public List<SimPage> getPages() {
		if (pages == null || templates == null) {
			throw new IllegalStateException("Nothing parsed yet. Parse an XML dump file first!");
		}
		return pages;
	}
	
	/**
	 * Parses a MediaWiki XML dump file.
	 *
	 * @param filename
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public void parse(String filename) throws ParserConfigurationException, SAXException, IOException {
		File file = new File(filename);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file);
		final Element docelem = doc.getDocumentElement();
		templates = extractTemplates(docelem);
		logger.fine("read " + templates.size() + " template name space.");
		if (logger.isLoggable(Level.FINE)) {
			for (SimNamespace t : templates) {
				logger.fine(" > Template name space " + t.getNamespace() + " contains " + t.sizeDefinitions() + " templates");
			}
		}
		pages = extractAllPages(docelem, templates);
		logger.fine("read " + pages.size() + " pages.");
	}

	/**
	 * MediaWiki supports localization of template names.
	 * For example, it's legal to use {{{template:foo}}} or German
	 * version {{{vorlage:foo}}}. Both will result in the same valid usage
	 * of the equal template.<br>
	 * The target should be to eliminate all translations and
	 * only have one resulting name for each template.
	 *
	 * @param filters
	 * @throws IllegalStateException
	 */
	public void filterPages(IPreTransformFilter... filters) {
		if (pages == null || templates == null) {
			throw new IllegalStateException("Nothing parsed yet. Parse an XML dump file first!");
		}
		if (filters == null || filters.length < 1) {
			return;
		}
		final SimNamespace[] templatearray = templates.toArray(new SimNamespace[templates.size()]);
		for (SimPage page : pages) {
			for (IPreTransformFilter filter : filters) {
				filter.filterPage(page, templatearray);
			}
		}
	}

	/**
	 * Returns only pages, no templates ...
	 *
	 * @param docelem
	 * @param templates
	 * @return
	 */
	private List<SimPage> extractAllPages(Element docelem, List<SimNamespace> templates) {
		final NodeList pages = docelem.getElementsByTagName("page");
		List<SimPage> result = new ArrayList<SimPage>();
		for (int i=0, len=pages.getLength(); i<len; i++) {
			final Element page = (Element) pages.item(i);
			final SimPage p = new SimPage();
			p.setTitle(getTitleFromElement(page));
			p.setText(getTextFromElement(page));
			p.setOriginal(p.getText());
			p.setUsername(getUsernameFromElement(page));
			p.setRevision(getTimestampFromElement(page));
			// do not add templates !
			boolean istemplate = false;
			for (SimNamespace t : templates) {
				if (p.getTitle().startsWith(t.getNamespace() + ":")) {
					istemplate = true;
					break;
				}
			}
			if (!istemplate) {
				result.add(p);
			}
		}
		return result;
	}

	/**
	 * Loads all the templates from the dump.
	 *
	 * @param docelem
	 */
	private List<SimNamespace> extractTemplates(Element docelem) {
		final NodeList siteinfos = docelem.getElementsByTagName("siteinfo");
		List<SimNamespace> result = new ArrayList<SimNamespace>();
		// search for Template declarations
		if (siteinfos != null && siteinfos.getLength() > 0) {
			final Element siteinfo = (Element) siteinfos.item(0);
			final NodeList namespaces = siteinfo.getElementsByTagName("namespaces");
			if (namespaces != null && namespaces.getLength() > 0) {
				final Element namespace = (Element) namespaces.item(0);
				final NodeList namespacelist = namespace.getElementsByTagName("namespace");
				for (int i=0, len=namespacelist.getLength(); i<len; i++) {
					final Element ns = (Element) namespacelist.item(i);
					final SimNamespace t = new SimNamespace();
					t.setKey(ns.getAttribute("key"));
					t.setNamespace(ns.getTextContent());
					result.add(t);
				}
			}
		}
		// fill in additional Template definitions (Text)
		for (SimNamespace t : result) {
			final NodeList pages = docelem.getElementsByTagName("page");
			for (int i=0, len=pages.getLength(); i<len; i++) {
				final Element page = (Element) pages.item(i);
				final String title = getTitleFromElement(page);
				if (title != null && title.startsWith(t.getNamespace() + ":")) {
					final SimTemplateDefinition td = new SimTemplateDefinition();
					td.setText(getTextFromElement(page));
					td.setTitle(title);
					t.add(td);
				}
			}
		}
		// cleanup empty declarations
		final Iterator<SimNamespace> tit = result.iterator();
		while (tit.hasNext()) {
			SimNamespace t = tit.next();
			if (t.sizeDefinitions() < 1) {
				tit.remove();
			}
		}
		return result;
	}

	/**
	 * Searches for &lt;title&gt;TEXT_TITLE_TEXT&lt;/title&gt;
	 *
	 * @param element
	 * @return the text content of the title element
	 */
	private static String getTitleFromElement(Element element) {
		final String title = element.getElementsByTagName("title").item(0).getTextContent();
		return title;
	}

	/**
	 * Searches for &lt;text&gt;TEXT&lt;/text&gt;
	 *
	 * @param element
	 * @return the text content of the this element
	 */
	private static String getTextFromElement(Element element) {
		final String text = element.getElementsByTagName("text").item(0).getTextContent();
		return text;
	}

	/**
	 * Searches for &lt;timestamp&gt;2009-09-09T10:45:25Z&lt;/timestamp&gt;
	 *
	 * @param element
	 * @return the text content of the this timestamp element
	 */
	private static String getTimestampFromElement(Element element) {
		final String text = element.getElementsByTagName("timestamp").item(0).getTextContent();
		return text;
	}

	/**
	 * Searches for &lt;username&gt;ASchwille&lt;/username&gt;
	 *
	 * @param element
	 * @return the text content of the this username element
	 */
	private static String getUsernameFromElement(Element element) {
		final String text = element.getElementsByTagName("username").item(0).getTextContent();
		return text;
	}

	/*
	 * ========================================================================
	 */
	public void setLogLevel(Level level) {
		this.logger.setLevel(level);
	}
}

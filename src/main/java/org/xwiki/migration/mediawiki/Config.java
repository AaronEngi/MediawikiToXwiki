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
package org.xwiki.migration.mediawiki;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;

import org.xwiki.migration.mediawiki.xmldump.INameSpaceResolver;
import org.xwiki.migration.mediawiki.xmldump.IPostTransformFilter;
import org.xwiki.migration.mediawiki.xmldump.IPreTransformFilter;

/**
 * @author mkirst(at portolancs dot com)
 */
public class Config {

	private final static String CONF_FILE = "mediawiki2xwiki.properties";

	private static final String CFG_LOG_LEVEL = "logging.level";
	private final static String CFG_SOURCE_DUMP = "source.dump.file";
	private static final String CFG_DOWNLOAD_URL = "source.download.url";
	private static final String CFG_TARGET_PATH = "target.path";
	private static final String CFG_XWIKI_DEFSPACE = "xwiki.namespace.default";
	private static final String CFG_XWIKI_USER = "xwiki.user";
	private static final String CFG_XWIKI_PASS = "xwiki.pass";
	private static final String CFG_XWIKI_COMMENT = "xwiki.comment.create";
	private static final String CFG_XWIKI_COMMENT_MSG = "xwiki.comment.message";
	private static final String CFG_XWIKI_ATTACH_ORIG = "xwiki.attach.original";
	private static final String CFG_FILTERS_PRE = "filters.pre.transform";
	private static final String CFG_FILTERS_POST = "filters.post.transform";
	private static final String CFG_XWIKI_NS_RESOLVERS = "resolvers.namespace";

	private final Properties props = new Properties();
	
	private final HashMap<Integer, Object> objectCache = new HashMap<Integer, Object>();

	public Config() {
		loadSettings();
	}

	private void loadSettings() {
		// fallback first ....
		props.put(CFG_LOG_LEVEL, "fine");
		props.put(CFG_SOURCE_DUMP, "sample-data/PortoWiki-20100730160513.xml");
		props.put(CFG_TARGET_PATH, "wiki-pages");
		props.put(CFG_XWIKI_USER, "Admin");
		props.put(CFG_XWIKI_PASS, "admin");
		props.put(CFG_XWIKI_DEFSPACE, "Sandbox");
		props.put(CFG_DOWNLOAD_URL,
				"wget \"http://subdev.portolancs.intra/portowiki/images/{0}\" -O \"{1}\"");
		props.put(CFG_XWIKI_COMMENT, "true");
		props.put(CFG_XWIKI_COMMENT_MSG, "Last modified from {1} on {0}.");
		props.put(CFG_XWIKI_ATTACH_ORIG, "true");
		props.put(CFG_FILTERS_PRE, "");
		props.put(CFG_FILTERS_POST, "");
		props.put(CFG_XWIKI_NS_RESOLVERS, "");

		try {
			FileInputStream fis = new FileInputStream(CONF_FILE);
			props.load(fis);
		} catch (Exception e1) {
			try {
				props.load(this.getClass().getClassLoader()
						.getResourceAsStream(CONF_FILE));
			} catch (Exception e2) {
				// than the fallback is active ...
			}
		}
	}

	public String getSourceDump() {
		return props.getProperty(CFG_SOURCE_DUMP).trim();
	}

	public String getTargetPath() {
		return props.getProperty(CFG_TARGET_PATH).trim();
	}

	public String getXWikiUser() {
		return props.getProperty(CFG_XWIKI_USER).trim();
	}

	public String getXWikiPass() {
		return props.getProperty(CFG_XWIKI_PASS).trim();
	}

	public String getXWikiDefaultSpace() {
		return props.getProperty(CFG_XWIKI_DEFSPACE).trim();
	}

	public String getSourceDownloadURL() {
		return props.getProperty(CFG_DOWNLOAD_URL).trim();
	}

	public boolean isCreateXwikiComment() {
		return Boolean
				.parseBoolean(props.getProperty(CFG_XWIKI_COMMENT).trim());
	}

	public String getCommentMessage() {
		return props.getProperty(CFG_XWIKI_COMMENT_MSG).trim();
	}

	public boolean isAttachOriginal() {
		return Boolean.parseBoolean(props.getProperty(CFG_XWIKI_ATTACH_ORIG)
				.trim());
	}

	public Level logLevel() {
		return Level.parse(props.getProperty(CFG_LOG_LEVEL).trim()
				.toUpperCase());
	}

	public IPreTransformFilter[] getPreFilters() {
		String[] classes = props.getProperty(CFG_FILTERS_PRE).split("[,]");
		return loadObjectArray(classes, new IPreTransformFilter[classes.length]);
	}

	public IPostTransformFilter[] getPostFilters() {
		String[] classes = props.getProperty(CFG_FILTERS_POST).split("[,]");
		return loadObjectArray(classes,	new IPostTransformFilter[classes.length]);
	}

	public INameSpaceResolver[] getNameSpaceResolvers() {
		String[] classes = props.getProperty(CFG_XWIKI_NS_RESOLVERS).split("[,]");
		return loadObjectArray(classes, new INameSpaceResolver[classes.length]);
	}
	
	/*
	 * ------------------------------------------------------------------------
	 */

	/**
	 * @param <T>
	 * @param clazznames
	 * @param target
	 * @return
	 */
	private <T> T[] loadObjectArray(String[] clazznames, T[] target) {
		final ClassLoader cl = this.getClass().getClassLoader();
		int counter = 0;
		for (String classname : clazznames) {
			classname = classname.trim();
			Integer key = new Integer(classname.hashCode());
			if ( objectCache.containsKey(key) ) {
				target[counter] = (T) objectCache.get(key);
				counter++;
			} else {
				try {
					if (!"".equals(classname)) {
						Class<?> clazz = cl.loadClass(classname);
						Object obj = clazz.newInstance();
						target[counter] = (T) obj;
						counter++;
						objectCache.put(key, obj);
					}
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
			}
		}
		if (counter < target.length) {
			return Arrays.copyOf(target, counter);
		}
		return target;
	}
	
	/*
	 * ========================================================================
	 */

	public String fileOriginal() {
		return "_original.txt";
	}

	public String fileXwikiText() {
		return "_text.txt";
	}

	public String fileCategories() {
		return "_categories.txt";
	}

	public String fileFiles() {
		return "_files.txt";
	}

	public String fileWget() {
		return "_wget_files.cmd";
	}

	public String fileTitle() {
		return "_title.txt";
	}

	public String fileTimestamp() {
		return "_timestamp.txt";
	}

	public String fileUser() {
		return "_user.txt";
	}
}

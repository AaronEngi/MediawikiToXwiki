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

import java.util.ArrayList;
import java.util.List;

import org.wikimodel.wem.EmptyWemListener;
import org.wikimodel.wem.WikiReference;

/**
 * Attention: the lists may contain duplicates.
 * 
 * @author mkirst(at portolancs dot com)
 */
public class CollectReferencesListener extends EmptyWemListener {

    public static final String PREFIX_IMAGE = "^(?:i|I)mage:.*";
    public static final String PREFIX_FILE  = "^(?:f|F)ile:.*";
    public static final String PREFIX_MEDIA  = "^(?:m|M)edia:.*";
    public static final String PREFIX_DATEI  = "^(?:d|D)atei:.*";
    
    public static final String PREFIX_CATEGORY  = "^(?:c|C)ategory:.*";
    
	private List<String> files = new ArrayList<String>();
	private List<String> categories = new ArrayList<String>();
	
	@Override
	public void onReference(String ref) {
		if (ref.matches(PREFIX_IMAGE) || ref.matches(PREFIX_FILE)
				|| ref.matches(PREFIX_MEDIA) || ref.matches(PREFIX_DATEI)) {
			final String target = ref.substring(ref.indexOf(':')+1);
			if (target.trim().length() > 0) {
				files.add(target);
			}
		}
		if (ref.matches(PREFIX_CATEGORY)) {
			final String target = ref.substring(ref.indexOf(':')+1);
			if (target.trim().length() > 0) {
				categories.add(target);
			}
		}
	}

	@Override
	public void onReference(WikiReference ref) {
		onReference(ref.getLink());
	}
	
	@Override
	public void onImage(String ref) {
		files.add(ref);
	}

	@Override
	public void onImage(WikiReference ref) {
		files.add(ref.getLink());
	}

	/**
	 * @return all file references 
	 */
	public String[] getFiles() {
		return files.toArray(new String[files.size()]);
	}
	
	/**
	 * @return all categories 
	 */
	public String[] getCategories() {
		return categories.toArray(new String[categories.size()]);
	}
}

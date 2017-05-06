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

import java.util.Calendar;
import java.util.Date;

import org.xwiki.migration.mediawiki.xmldump.INameSpaceResolver;
import org.xwiki.migration.mediawiki.xmldump.model.SimPage;

/**
 * Every page which is older than {@link #DEADLINE} will moved
 * to name space 'Archiv'.
 * 
 * @author mkirst(at portolancs dot com)
 */
public class ArchiveNameSpaceResolver implements INameSpaceResolver {

	public final static Date DEADLINE;
	
	static {
		Calendar cal = Calendar.getInstance();
		cal.set(2007, 12, 31);
		DEADLINE = cal.getTime();
	}
	
	/* (non-Javadoc)
	 * @see org.xwiki.migration.mediawiki.xmldump.INameSpaceResolver#determineNameSpace(org.xwiki.migration.mediawiki.xmldump.model.SimPage)
	 */
	@Override
	public String determineNameSpace(SimPage page) {
		if (page.getRevision().before(DEADLINE)) {
			return "Archiv";
		}
		return null;
	}

}

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

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SIM "Special Intermediate Model"
 * 
 * Each page does have some attributes:
 * title, revision, user name, text and a {@link Set} of categories.
 * 
 * @author mkirst(at portolancs dot com)
 */
public class SimPage {

	private String title;
	private Date revision;
	private String username;
	private String text;
	private String original;
	
	private Set<String> categories = new HashSet<String>();
	private Set<String> files = new HashSet<String>();
	
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the revision
	 */
	public Date getRevision() {
		return revision;
	}
	/**
	 * FORMAT: &lt;timestamp&gt;2009-07-23T10:45:25Z&lt;/timestamp&gt;
	 *
	 * @param revision the revision to set
	 */
	public void setRevision(String revision) {
		final Pattern p = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})Z");
		final Matcher m = p.matcher(revision);
		if (m.matches()) {
			final int year  = Integer.parseInt(m.group(1));
			final int month = Integer.parseInt(m.group(2)) - 1; // 0 = January
			final int date  = Integer.parseInt(m.group(3));
			final int hrs   = Integer.parseInt(m.group(4));
			final int min   = Integer.parseInt(m.group(5));
			final int sec   = Integer.parseInt(m.group(6));
			final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
			cal.set(year, month, date, hrs, min, sec);
			this.revision = cal.getTime();
		}
	}
	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}
	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}
	/**
	 * @param text the text to set
	 */
	public void setText(String text) {
		this.text = text;
	}
	/**
	 * @return
	 * @see java.util.Set#size()
	 */
	public int sizeCategory() {
		return categories.size();
	}
	/**
	 * @param category
	 * @return
	 * @see java.util.Set#add(java.lang.Object)
	 */
	public boolean addCategory(String category) {
		return categories.add(category);
	}
	/**
	 * @param category
	 * @return
	 * @see java.util.Set#remove(java.lang.Object)
	 */
	public boolean removeCategory(String category) {
		return categories.remove(category);
	}

	/**
	 * @return all categories
	 */
	public String[] getCategories() {
		return categories.toArray(new String[categories.size()]);
	}
	/**
	 * @return
	 * @see java.util.Set#size()
	 */
	public int sizeFiles() {
		return files.size();
	}
	/**
	 * @param filename
	 * @return
	 * @see java.util.Set#add(java.lang.Object)
	 */
	public boolean addFile(String filename) {
		return files.add(filename);
	}
	/**
	 * @return all files 
	 */
	public String[] getFiles() {
		return files.toArray(new String[files.size()]);
	}
	/**
	 * @return the original
	 */
	public String getOriginal() {
		return original;
	}
	/**
	 * @param original the original to set
	 */
	public void setOriginal(String original) {
		this.original = original;
	}
	
}

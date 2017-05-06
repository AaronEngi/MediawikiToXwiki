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
package org.xwiki.migration.xwiki;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xmlrpc.XmlRpcException;
import org.codehaus.swizzle.confluence.Attachment;
import org.codehaus.swizzle.confluence.Comment;
import org.codehaus.swizzle.confluence.ConfluenceException;
import org.codehaus.swizzle.confluence.Page;
import org.codehaus.swizzle.confluence.SwizzleException;
import org.wikimodel.wem.xwiki.xwiki20.XWikiSerializer2;
import org.xwiki.migration.mediawiki.Config;
import org.xwiki.xmlrpc.XWikiXmlRpcClient;
import org.xwiki.xmlrpc.model.XWikiObject;
import org.xwiki.xmlrpc.model.XWikiObjectSummary;

/**
 * MAIN CLASS
 * 
 * @author mkirst(at portolancs dot com)
 */
public class XWikiUploader {
    
    private final Config cfg = new Config();
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    
    public static void main(String[] args) throws MalformedURLException {

        XWikiUploader main = new XWikiUploader();
        try {
        	main.logger.setLevel(main.cfg.logLevel());
            main.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void run() throws Exception {
        // URL of the xwiki instance
        String url = "http://119.29.101.119:10007/xwiki/xmlrpc/confluence";

        // Perform Login & Authentication using above url address

        XWikiXmlRpcClient rpc = new XWikiXmlRpcClient(url);
        rpc.login(cfg.getXWikiUser(), cfg.getXWikiPass());

        File targetpath = new File(cfg.getTargetPath());
        File[] folders = targetpath.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                File f = new File(dir, name);
                return f.isDirectory();
            }
        });
        for (File namespacepath : folders) {
        	uploadNameSpace(rpc, namespacepath);
        }

        rpc.logout();
        logger.info("Done.");
    }

	/**
	 * @param rpc
	 * @param namespacepath
	 * @throws SwizzleException
	 * @throws ConfluenceException
	 * @throws IOException
	 * @throws XmlRpcException
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	private void uploadNameSpace(XWikiXmlRpcClient rpc, File namespacepath)
					throws SwizzleException, ConfluenceException, IOException,
					XmlRpcException, FileNotFoundException,
					UnsupportedEncodingException {
		String namespace = namespacepath.getName();
		logger.info("Uploading name space " + namespace + " ...");
		File[] folders = namespacepath.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                File f = new File(dir, name);
                return f.isDirectory();
            }
        });
        
        for (File pagefolder : folders ) {
            Page page = storePage(rpc, namespace, pagefolder);
            storeAttachments(rpc, pagefolder, page);
            storeTags(rpc, pagefolder, page);
            storeOriginalTextAsAttachment(rpc, pagefolder, page);
            createImportComment(rpc, pagefolder, page);
        }
	}

    /**
     * @param rpc
     * @param pagefolder
     * @param page
     * @throws XmlRpcException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    private void createImportComment(XWikiXmlRpcClient rpc, File pagefolder, Page page) throws XmlRpcException, FileNotFoundException, UnsupportedEncodingException, IOException {
    	if (!cfg.isCreateXwikiComment()) {
    		return; // nothing to do.
    	}
    	Comment comment = new Comment();
    	comment.setPageId(page.getId());
    	DateFormat dformater = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
		String d = dformater.format(getTimestampFromPage(pagefolder));
		String u = getUserFromPage(pagefolder);
		comment.setContent(MessageFormat.format(cfg.getCommentMessage(), d, u));
		rpc.addComment(comment);
	}

	/**
     * @param rpc
     * @param namespace
     * @param pagefolder
     * @throws SwizzleException
     * @throws ConfluenceException
     * @throws IOException 
     * @throws XmlRpcException 
     */
    private Page storePage(XWikiXmlRpcClient rpc, String namespace, File pagefolder) throws SwizzleException, ConfluenceException, IOException, XmlRpcException {
    	
    	File textfile = new File(pagefolder, cfg.fileXwikiText());
        File titlefile = new File(pagefolder, cfg.fileTitle());
        StringBuilder text = loadTextFile(textfile);
        StringBuilder title = loadTextFile(titlefile);
        
    	final Date timestamp = getTimestampFromPage(pagefolder);
        
        Page page = new Page();
        String pageId = namespace+"."+XWikiSerializer2.clearName(title.toString(), true, true);
        
        logger.fine("storing page " + pageId);
        
        page.setId(pageId);
        page.setSpace(namespace);
        page.setTitle(title.toString());
        page.setContent(text.toString());
        page.setCreated(timestamp);
        page.setModified(timestamp);
        page.setCreator(getUserFromPage(pagefolder));
        page.setModifier(getUserFromPage(pagefolder));
        page.setParentId(namespace+".WebHome");
        // Store the page object into XWiki
        rpc.storePage(page);
        return page;
    }

    /**
     * @param rpc
     * @param pagefolder
     * @param page
     * @throws SwizzleException
     * @throws ConfluenceException
     * @throws IOException
     * @throws XmlRpcException
     */
    private void storeAttachments(XWikiXmlRpcClient rpc, File pagefolder, Page page) throws SwizzleException, ConfluenceException, IOException, XmlRpcException {
        File ffile = new File(pagefolder, cfg.fileFiles());
        if (!ffile.exists()) {
            return; // nothing to do ;-)
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ffile), "UTF-8"));
        for (String filename = br.readLine(); filename != null; filename = br.readLine()) {
        	logger.fine("storring attachment " + filename +" for page " + page.getId());
            File fa = new File(pagefolder, filename);
            if (!fa.canRead() || fa.length() < 1) {
            	continue; // skip not existing files
            }
            FileInputStream fis = new FileInputStream(fa);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; 
            int read = -1;
            while ((read = fis.read(buffer)) > 0) {
                baos.write(buffer, 0, read);
            }
            fis.close();
            
            org.codehaus.swizzle.confluence.Attachment a = new org.codehaus.swizzle.confluence.Attachment();
            a.setFileName(filename);
            a.setFileSize(Long.toString(fa.length()));
            a.setPageId(page.getId());
            rpc.addAttachment(new Integer(filename.hashCode()), a, baos.toByteArray());
            
        }
    }
    
    /**
     * @param rpc
     * @param pagefolder
     * @param page
     * @throws SwizzleException
     * @throws ConfluenceException
     * @throws IOException
     * @throws XmlRpcException
     */
    private void storeOriginalTextAsAttachment(XWikiXmlRpcClient rpc, File pagefolder, Page page) throws SwizzleException, ConfluenceException, IOException, XmlRpcException {
    	if (!cfg.isAttachOriginal()) {
    		return; // nothing to do
    	}
    	logger.fine("attaching original text for page " + page.getId());
    	
    	File ofile = new File(pagefolder, cfg.fileOriginal());
    	if (!ofile.canRead() || ofile.length() < 1) return; // nothing to do ;-)
    	
    	final StringBuilder original = loadTextFile(ofile);
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	OutputStreamWriter osw = new OutputStreamWriter(baos);
    	osw.write(original.toString());
    	osw.close();
    	
    	final Date timestamp = getTimestampFromPage(pagefolder);
    	
    	Attachment a = new Attachment();
    	String filename = "mediawiki_original.txt";
		a.setFileName(filename);
    	a.setFileSize(Long.toString(ofile.length()));
    	a.setPageId(page.getId());
    	a.setCreated(timestamp);
    	a.setCreator(getUserFromPage(pagefolder));
    	rpc.addAttachment(new Integer(filename.hashCode()), a, baos.toByteArray());
    }

	/**
	 * @param folder
	 * @return
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	private Date getTimestampFromPage(File folder) throws FileNotFoundException, UnsupportedEncodingException, IOException {
		File tfile = new File(folder, cfg.fileTimestamp());
    	if (!tfile.canRead()) throw new IllegalStateException("Error, can't read from: " + tfile);
		final StringBuilder timestr = loadTextFile(tfile);
    	return new Date(Long.parseLong(timestr.toString().trim()));
	}
	
	/**
	 * @param folder
	 * @return
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	private String getUserFromPage(File folder) throws FileNotFoundException, UnsupportedEncodingException, IOException {
		File ufile = new File(folder, cfg.fileUser());
    	if (!ufile.canRead()) throw new IllegalStateException("Error, can't read from: " + ufile);
		final StringBuilder userstr = loadTextFile(ufile);
    	return userstr.toString().trim();
	}

    
    /**
     * @param rpc
     * @param pagefolder
     * @param page
     * @throws SwizzleException
     * @throws ConfluenceException
     * @throws IOException
     * @throws XmlRpcException
     */
    private void storeTags(XWikiXmlRpcClient rpc, File pagefolder, Page page) throws SwizzleException, ConfluenceException, IOException, XmlRpcException {
        File ctgfile = new File(pagefolder, cfg.fileCategories());
        if (!ctgfile.exists()) {
            return; // nothing to do ;-)
        }
        
        // read the categories from file ...
        List<String> newtags = new ArrayList<String>();
        InputStreamReader isr = new InputStreamReader(new FileInputStream(ctgfile), "UTF-8");
		BufferedReader br = new BufferedReader(isr);
        for (String tagname = br.readLine(); tagname != null; tagname = br.readLine()) {
        	newtags.add(tagname);
        }
        br.close();
        
        if (logger.isLoggable(Level.FINE)) {
        	logger.fine("placing tags " + Arrays.toString(newtags.toArray())+ " for page " + page.getId());
        }
        
    	// retrieve current page object informations
    	XWikiObjectSummary xosum = new XWikiObjectSummary();
    	xosum.setPageId(page.getId());
    	xosum.setClassName("XWiki.TagClass");
		try {
			// append tags
			XWikiObject xwo = rpc.getObject(xosum);
			List<String> curtags = (List<String>) xwo.getProperty("tags");
			for (String newtag : newtags) {
				// avoid duplicates
				if (!curtags.contains(newtag)) curtags.add(newtag);
			}
	    	rpc.storeObject(xwo);
		} catch (XmlRpcException  e) {
			// most likely bug:
			// http://jira.xwiki.org/jira/browse/XWIKI-5396
			// Thus create new tags
			XWikiObject xwo = new XWikiObject();
			xwo.setClassName("XWiki.TagClass");
			xwo.setPageId(page.getId());
			xwo.setProperty("tags", newtags);
			rpc.storeObject(xwo);
		}
		
    }

    /**
     * @param file
     * @return
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    private StringBuilder loadTextFile(File file) throws FileNotFoundException, UnsupportedEncodingException, IOException {
        FileInputStream fis = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        StringBuilder text = new StringBuilder((int)file.length());
        char[] buf = new char[4096];
        int read = -1;
        while ((read = isr.read(buf)) > 0) {
            text.append(buf, 0, read);
        }
        isr.close();
        return text;
    }
}
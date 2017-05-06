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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.wikimodel.wem.CompositeListener;
import org.wikimodel.wem.IWikiPrinter;
import org.wikimodel.wem.WikiParserException;
import org.wikimodel.wem.mediawiki.MediaWikiParser;
import org.wikimodel.wem.xwiki.xwiki20.XWikiSerializer2;
import org.xml.sax.SAXException;
import org.xwiki.migration.mediawiki.xmldump.DumpParser;
import org.xwiki.migration.mediawiki.xmldump.INameSpaceResolver;
import org.xwiki.migration.mediawiki.xmldump.IPostTransformFilter;
import org.xwiki.migration.mediawiki.xmldump.model.SimPage;


/**
 * MAIN CLASS
 * 
 * @author mkirst(at portolancs dot com)
 */
public class MediaWikiConverter {

	final static String NL = System.getProperty("line.separator");
	
    private final Config cfg = new Config();
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private final Set<File> wgetcmds = new LinkedHashSet<File>();
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MediaWikiConverter main = new MediaWikiConverter();
		try {
			main.logger.setLevel(main.cfg.logLevel());
			main.convert();
//			main.testWikiSource();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    /**
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws WikiParserException 
	 */
	private void convert() throws ParserConfigurationException, SAXException, IOException, WikiParserException {
	    String dumpfile = cfg.getSourceDump();
	    String targetpath = cfg.getTargetPath();
		final File dumpf = new File(dumpfile);
		if (!dumpf.exists()) {
			return;
		}
		final File outpf = new File(targetpath);
		if (!outpf.exists()) {
			outpf.mkdirs();
		}
		final DumpParser dparser = new DumpParser();
		dparser.setLogLevel(logger.getLevel());
		
		logger.info("parsing dumpfile ...");
		dparser.parse(dumpfile);
		
		logger.info("pre filtering ...");
		dparser.filterPages(cfg.getPreFilters());
		
		logger.info("resolving templates ...");
		dparser.resolveTemplates();
		List<SimPage> pages = dparser.getPages();
		
		logger.info("saving pages ...");
		for (SimPage p : pages) {
			final String namespace = determineNameSpace(p);
			final File namespacef = new File(targetpath + File.separatorChar + namespace);
			if (!namespacef.exists()) namespacef.mkdir();
			final String pagename = XWikiSerializer2.clearName(p.getTitle(), true, true);
			final File pagefolder = new File(namespacef, pagename);
			if (!pagefolder.exists()) pagefolder.mkdir();
			
			transform(p);
			filterPostPageTransform(p);
			writeText(p, pagefolder);
			writeOriginalText(p, pagefolder);
			writeTitle(p, pagefolder);
			writeCategories(p, pagefolder);
			writeFiles(p, pagefolder);
			writeTimestamp(p, pagefolder);
			writeUser(p, pagefolder);
		}
		writeWgetSummaryFile();
		logger.info("Done.");
	}

	/**
	 * Determines name space before the transformation
	 * 
	 * @param page
	 * @return
	 */
	private String determineNameSpace(SimPage page) {
		String namespace = cfg.getXWikiDefaultSpace();
		for (INameSpaceResolver nsr : cfg.getNameSpaceResolvers()) {
			String ns = nsr.determineNameSpace(page);
			if (ns != null) {
				namespace = ns;
				break;
			}
		}
		return namespace;
	}

	/**
	 * @param page
	 * @throws WikiParserException
	 * @throws IOException
	 */
	private void transform(SimPage page) throws WikiParserException, IOException {
	    final CollectReferencesListener pagerefs = new CollectReferencesListener();
	    final String xwikitext = transformWikiText(page.getText(), pagerefs);
	    if (pagerefs.getFiles().length > 0) {
	        for (String f : pagerefs.getFiles()) {
				page.addFile(f);
			}
		}
		if (pagerefs.getCategories().length > 0) {
			for (String c : pagerefs.getCategories()) {
				page.addCategory(c);
			}
		}
		page.setText(xwikitext);
	}
	
	/**
	 * @param page
	 */
	private void filterPostPageTransform(SimPage page) {
		for (IPostTransformFilter postfilter : cfg.getPostFilters()) {
			postfilter.filterPage(page);
		}
	}

	/**
	 * @param page
	 * @param pagefolder
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void writeOriginalText(SimPage page, File pagefolder) throws IOException, FileNotFoundException {
		File originalf = new File(pagefolder, cfg.fileOriginal());
        FileOutputStream fos = new FileOutputStream(originalf);
		OutputStreamWriter foriginal = new OutputStreamWriter(fos,"UTF-8");
        foriginal.write(page.getOriginal());
        foriginal.close();
	}
	
	/**
	 * @param page
	 * @param pagefolder
	 * @throws IOException 
	 */
	private void writeText(SimPage page, File pagefolder) throws IOException {
		logger.fine("saving page " + page.getTitle() + " ...");
		File textf = new File(pagefolder, cfg.fileXwikiText());
		final FileOutputStream fos = new FileOutputStream(textf);
		final OutputStreamWriter w = new OutputStreamWriter(fos, "UTf-8");
		w.write(page.getText());
		w.close();
	}
	
	/**
	 * @param page
	 * @param pagefolder
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void writeTitle(SimPage page, File pagefolder) throws IOException, FileNotFoundException {
        FileOutputStream fos = new FileOutputStream(new File(pagefolder, cfg.fileTitle()));
		OutputStreamWriter titlef = new OutputStreamWriter(fos, "UTF-8");
        titlef.write(page.getTitle());
        titlef.close();
	}
	
	/**
	 * @param page
	 * @param pagefolder
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void writeCategories(SimPage page, File pagefolder) throws IOException, FileNotFoundException {
	    // categories ...
	    if (page.getCategories().length > 0) {
	    	final File ctgfile = new File(pagefolder, cfg.fileCategories());
	    	final FileOutputStream fos = new FileOutputStream(ctgfile);
	    	final OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
	    	for (String category : page.getCategories()) {
	    		osw.write(category + NL);
	    	}
	    	osw.close();
	    }
	}
	
	/**
	 * @param page
	 * @param pagefolder
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void writeTimestamp(SimPage page, File pagefolder) throws IOException, FileNotFoundException {
        FileOutputStream fos = new FileOutputStream(new File(pagefolder, cfg.fileTimestamp()));
		OutputStreamWriter titlef = new OutputStreamWriter(fos,"UTF-8");
        titlef.write(Long.toString(page.getRevision().getTime()));
        titlef.close();
	}
	
	/**
	 * @param page
	 * @param pagefolder
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void writeUser(SimPage page, File pagefolder) throws IOException, FileNotFoundException {
        FileOutputStream fos = new FileOutputStream(new File(pagefolder, cfg.fileUser()));
		OutputStreamWriter userf = new OutputStreamWriter(fos,"UTF-8");
        userf.write(page.getUsername());
        userf.close();
	}

	/**
	 * @param page
	 * @param pagefolder
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void writeFiles(SimPage page, File pagefolder) throws IOException, FileNotFoundException {
	    if (page.sizeFiles() > 0) {
	        final FileOutputStream fos = new FileOutputStream(new File(pagefolder, cfg.fileFiles()));
			final OutputStreamWriter ffiles = new OutputStreamWriter(fos,"UTF-8");
	        File wgetfile = new File(pagefolder, cfg.fileWget());
			final FileOutputStream ffos = new FileOutputStream(wgetfile);
	        final OutputStreamWriter osw = new OutputStreamWriter(ffos);
	        for (String f : page.getFiles()) {
	            osw.write(MessageFormat.format(cfg.getSourceDownloadURL(), f, XWikiSerializer2.clearName(f)) + NL);
	            ffiles.write(XWikiSerializer2.clearName(f) + NL);
	        }
	        osw.close();
	        ffiles.close();
	        wgetcmds.add(wgetfile);
	    }
	}

	private void writeWgetSummaryFile() throws IOException {
		if (wgetcmds.size() > 0) {
			File wgetsf = new File(new File(cfg.getTargetPath()), cfg.fileWget());
			logger.info("writing wget summary file " + wgetsf.getPath() + " ...");
			FileOutputStream fos = new FileOutputStream(wgetsf);
			OutputStreamWriter osw = new OutputStreamWriter(fos,"UTF-8");
			for (File f : wgetcmds) {
				f = f.getAbsoluteFile();
				// X:
				osw.write(f.getAbsolutePath().substring(0,2) + NL );
				// CD target folder && call _wget_files.cmd
				osw.write("CD " + f.getParent() + " && CALL " + cfg.fileWget() + NL);
			}
			osw.close();
		}
	}
	
	/**
	 * @param source
	 * @param fileListener
	 * @return
	 * @throws WikiParserException
	 */
	private String transformWikiText(String source, CollectReferencesListener fileListener) throws WikiParserException {
		final StringReader reader = new StringReader(source);
		final MediaWikiParser parser = new MediaWikiParser();
		IWikiPrinter wprnt = new org.wikimodel.wem.WikiPrinter();
		final XWikiSerializer2 iwlistener = new XWikiSerializer2(wprnt);
		parser.parse(reader, new CompositeListener(iwlistener, fileListener));
		return wprnt.toString();
	}

	private void testWikiSource() throws Exception {
		String source = loadTextFile(new File("sample-data/mediawiki.04.txt")).toString();
		final CollectReferencesListener filrefs = new CollectReferencesListener();
		System.out.println(transformWikiText(source, filrefs));
		printFiles(filrefs);
	}
	
	private void printFiles(CollectReferencesListener colrefs) {
		final String[] xx = colrefs.getFiles();
		if (xx.length > 0 ) {
			System.out.println(Arrays.toString(xx));
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

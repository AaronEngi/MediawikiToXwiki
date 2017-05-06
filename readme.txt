{{include document="XWiki.ExtensionClassSheet20"/}}

== Introduction ==

This extension, or better call it "Migration Toolkit", was build to migrate a grown MediaWiki intranet installation (230 pages, 570 attachments, 150MB data). The idea was to automatically convert all the pages to the XWiki 2.0 syntax using wikimodel (http://code.google.com/p/wikimodel/). Additionally the toolkit should automatically download all existing files and upload them to the according XWiki pages.

Having this in mind, I've started to write this migration toolkit.

== How It Works ==

=== Step 1 - Parsing & Preparing ===

[[image:howitworksparser.png]]

* **XML-Dump** This is the huge XML file you've got from your MediaWiki export function. See http://meta.wikimedia.org/wiki/Help:Export
* **XML-Parser** Reads the XML into an internal 'SIM' model (special intermediate model), representing pages, templates and their meta informations (user, date, etc.)
* **PRE-Filtering-Engine** You can define one ore more filters, operating on 'SIM' to fix some things, the wiki syntax parser couldn't understand. For example, there are filters correcting localized tags (ex. German //Bild// fixed to //Image//). Additionally, filters are able to add meta informations, like marking pages for target name spaces.
* **Template-Resolver** This process scans all pages for occurrences of templates/macros. Each one found will looked up (templates are also within the dump file) and replaced by the according wiki text. Parameters are also replaced.
* **Transform** This is the core transformation, where the wikimodel's MediaWikiParser is used to read each page and a XWiki 2.0 serializer will create new XWiki 2.0 syntax text. The parser also collects important things like attachment links. This collection will be used later.
* **POST-Filtering-Engine** Here are some filters applied which are able to modify transformed text. For example all links to categories are removed, cause XWiki doesn't know categories, there are only tags available.
* **Output-Writer** Now the 'SIM' data is written to the file system. As you can see in the screenshot, there are 1st level folder representing name spaces. The 2nd level folder representing pages within each name space. Each page contains a lot of .TXT files where the filtered text is saved. Each page folder contains these text files.

=== Step 2 - Downloading & Arranging (optional) ===

Now you have to download all images and attachments from your existing MediaWiki. Don't be afraid, there were already made some nice preparations for you. In step 1 there where .CMD files created, one per page. This said, just by clicking them, you the download is made for you (by default using the wget tool). Additionally there is one .CMD file which catches them all up at once.

Arranging is an optional step. You are now free to take your favorite file manager and text editor to do some manual changes. This way you can move pages to other spaces/folder (ex. 'archive' or 'main'). There are also some tricky syntax or design errors which can't be covered by regular expressions.

Having huge migrations in mind, it's more clever to look at the pre-filtering code, to make these tasks more automated. But you are still very free to manipulate your data easily on the file system in text files, than in any other intermediate format (XML, database or whatnot).

=== Last Step 3 - Uploading ===

[[image:howitworksuploader.png]]

* **Input-Reader** Reads the files and structure (pages and name spaces)
* **WXikiClient** Stores it via XMLRPC-API directly into the XWiki instance

== How To Use ==

{{warning}}
You need at least some basic skills in Java, Eclipse, SVN and how to apply patches.
{{/warning}}

=== Setup ===

This setup describes an independent ready to test setup,
which is highly recommended to go you first steps.

1. Download ##xwiki-enterprise-jetty-hsqldb-2.4.zip##, unzip it and fire up this pre-build installation.
1. Open up Eclipse and setup a fresh workspace
1. Import the attached project
1*. Copy/Fill in the missing JAR files (see BuildPath), see on this sides:
1**. [[platform:Features.XMLRPCJavaExamples]]
1**. [[platform:Features.XMLRPC]]
1. Switch to SVN repository exploring perspective and add the repository URL http://wikimodel.googlecode.com/svn
1. Checkout the projects for wikimodel:
1*. //trunk/org.wikimodel.javacc//
1*. //trunk/org.wikimodel.wem//
1. Apply patch ##wikimodel_xwiki2_serializer.patch## to the project ##org.wikimodel.wem##
1*. This patch was created against revision 479, there is no need to apply it once it's in the official trunk committed
1. At this point, there should be no more errors in the 3 projects

=== Using it ===

* After reading how it works, look at the configuration file ##mediawiki2xwiki.properties## first.
* Now you need to configure you're MediaWiki export dump XML file
* Check and adjust the other setting there too
* The two main classes are
*1. ##org.xwiki.migration.mediawiki.MediaWikiConverter##
*1. ##org.xwiki.migration.xwiki.XWikiUploader##
* Run them as regular Java applications.

As said, the target audience of this toolkit are developers ;-)

== Features & Limitations ==

This toolkit was used for converting a home grown MediaWiki containing about 230 articles.
There where a couple of additional plugins in use (for example 'Timeline') and a few templates.
All articles where written in German and thus also some template/macro names. 

Features:

* Transforming MediaWiki to XWiki 2.0 Syntax
* Most common syntax stuff is working 
* Support for localized macro names and name spaces (see IPageFilter)
* Automatic downloading and uploading of attachments & images (needs public read access; using WGET)
* Automatic attachment & images name transformation to XWiki 2.0 style
* Support for XWiki name spaces
* Support for Templates/Macros (but limited)
* International support, everything is kept as UTF8 
* Configurable via mediawiki2xwiki.properties file

Limitations:

* Only migration of the latest revision of each page is supported (Thus no support of history)
* Only one level of name spaces is supported, no nested name spaces
* No nested templates are supported
* MediaWiki Name spaces are not supported. It's assumed that there is only one.
* MediaWiki plugins are not supported
* Special table/row/cell attributes are not supported
* Currently all scripts are only created for Windows platform (.CMD files)

== Final thoughts ==

This migration toolkit works for the usual 80% of all pages. By design it's really complicated to do such migration. Just think about additional plugins like the famous 'TimelinePlugin' where there are not related things in the XWiki world. The fact, that MediaWiki also supports localized variant of keywords or magic words makes it not easier for syntax parser. This toolkit was only tested with German ones, but is designed to
be enhanceable. So, if you're not migration German text, you have to write your own pre-filters.

As I'm not getting paid for writing such toolkits, I've stopped myself after nearly two weeks. But when you want to start a similar migration project, use this as a start point so I've saved this time for you ;-) 

==== A Word On Templates ====

When transforming Wiki source from one format into another it's difficult to handle templates correctly. In theory you should open a new parser  for any occurrence of a template. When there are nested templates you have to open a new parser within the parser.

This toolkit takes a simpler approach, it reads the dump and does a simple 'search and replace' (based on regular expressions) on the whole article. So, parameters are supported but not nested templates. This way, also localized macros are supported.
For example: ~{~{Template:foo}}and ~{~{Vorlage:foo}} (German) will give the same result on MediaWiki. 

== Troubleshooting ==

=== Missing XWikiXmlRpcClient class ===

See XWiki documentation where to get the missing JARs:
http://platform.xwiki.org/xwiki/bin/view/Features/XMLRPCJavaExamples

=== Using alternative 'XWiki' class doesn't work ===

When you substitute 'XWikiXmlRpcClient' class with 'XWiki' class,
your compiler will let you go. Even the login into your XWiki will work.
But unluckily when you follow the examples on xwiki:"XMLRPCJavaExamples"
and want to create a page, following error occurs.
The solution seems to be using 'XWikiXmlRpcClient' ...
{{code language="none"}}org.codehaus.swizzle.confluence.ConfluenceException: Null values aren't supported, if isEnabledForExtensions() == false
 at org.codehaus.swizzle.confluence.Confluence.call(Confluence.java:808)
 at org.codehaus.swizzle.confluence.Confluence.call(Confluence.java:765)
 at org.codehaus.swizzle.confluence.Confluence.storePage(Confluence.java:215)
 at de.portolancs.xwiki.XWikiUploader.storePage(XWikiUploader.java:96)
 at de.portolancs.xwiki.XWikiUploader.run(XWikiUploader.java:54)
 at de.portolancs.xwiki.XWikiUploader.main(XWikiUploader.java:29){{/code}}
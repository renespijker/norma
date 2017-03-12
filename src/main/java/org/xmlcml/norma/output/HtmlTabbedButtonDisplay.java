package org.xmlcml.norma.output;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xmlcml.html.HtmlDiv;
import org.xmlcml.html.HtmlElement;
import org.xmlcml.html.HtmlH1;
import org.xmlcml.html.HtmlHtml;
import org.xmlcml.html.HtmlScript;
import org.xmlcml.html.HtmlStyle;
import org.xmlcml.norma.Norma;
import org.xmlcml.xml.XMLUtil;

import nu.xom.Element;

/** creates a tabbed display for multiple HTML files using buttons
 * 
 * The HTML files MUST have:
 * a unique ID
 * a class attribute on the content to be displayed
 * and SHOULD have a unique title attribute
 * 
 * Example:
 * <div id="table1" class="tabcontent" title="Table 1">
     <table xmlns="http://www.w3.org/1999/xhtml">
     ...
     </table>
   </div>
 * 
 * I believe they can have any displayable HTML content
 * 
 * the tabDisplay must have a title or titleElement to be displayed above the tabs
 * 
 * For each file the class constructs:
 *  a list of buttons linked to the content files
 *  titles from the content using getTitle()
 *  
<h1>title (e.g. 10.1016_j.pain.2014.08.023)</h1>
 <div class="tab">
  <button class="tablinks" onclick="openTab(event, 'table1', 'tabcontent')">title (e.g. Table1)</button>
  <button class="tablinks" onclick="openTab(event, 'table2', 'tabcontent')">Table2</button>
  <button class="tablinks" onclick="openTab(event, 'table3', 'tabcontent')">Table3</button>
  <button class="tablinks" onclick="openTab(event, 'table4', 'tabcontent')">Table4</button>
 </div>

getTitle() will look for (in order)
 title attribute
 title element
 id

(Not yet fully tested)

 *  
 * 
 * 
 * The tool 
 * 
 * @author pm286
 *
 */
public class HtmlTabbedButtonDisplay extends HtmlHtml {
	private static final Logger LOG = Logger.getLogger(HtmlTabbedButtonDisplay.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}

//	public final static HtmlScript BUTTON_SCRIPT = new HtmlScript(); 
//	public final static HtmlStyle BUTTON_STYLE = new HtmlStyle(); 
	
	public final static String BUTTON_STYLE_RESOURCE = Norma.NORMA_OUTPUT_RESOURCE+"/"+"tabButton.css"; 
	public final static String BUTTON_SCRIPT_RESOURCE = Norma.NORMA_OUTPUT_RESOURCE+"/"+"tabButton.js";
	
	private static final String TAB = "tab";
	private static final String TABCONTENTDIV = "tabcontentdiv";
	
	private static HtmlStyle readButtonStyle(String resource) {
		HtmlStyle htmlStyle = null;
		String buttonStyleContent = readStringContent(resource);
		if (buttonStyleContent != null) {
			htmlStyle = new HtmlStyle();
			htmlStyle.addCss(buttonStyleContent);
		}
		return htmlStyle;
	}

	private static HtmlScript readButtonScript(String resource) {
		HtmlScript htmlScript = null;
		String buttonScriptContent = readStringContent(resource);
		if (buttonScriptContent != null) {
			htmlScript = new HtmlScript();
			htmlScript.setContent(buttonScriptContent);
		}
		return htmlScript;
	}

	private static String readStringContent(String resource) {
		String buttonStyleContent = null;
		InputStream buttonStyleStream = HtmlTabbedButtonDisplay.class.getResourceAsStream(resource);
		if (buttonStyleStream == null) {
			throw new RuntimeException("null input stream: "+resource);
		}
		try {
			buttonStyleContent = IOUtils.toString(buttonStyleStream);
		} catch (IOException e) {
			LOG.debug("Cannot read "+resource+" in "+HtmlTabbedButtonDisplay.class+"; "+e.getMessage());
		}
		return buttonStyleContent;
	}
	
	private static Element readXMLContent(String resource) {
		Element xmlContent = null;
		InputStream buttonStyleStream = HtmlTabbedButtonDisplay.class.getResourceAsStream(resource);
		xmlContent = XMLUtil.parseQuietlyToDocument(buttonStyleStream).getRootElement();
		return xmlContent;
	}
	
	private String title;
	
	public HtmlTabbedButtonDisplay() {
		
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	/** add files to be displayed.
	 * 
	 * @param htmlFiles
	 */
	public HtmlTabbedButtonDisplay(List<File> htmlFiles) {
		createButtonsFromHtmlFiles(htmlFiles);
	}

	/** add files to be displayed.
	 * 
	 * @param files
	 */
	public void createButtonsFromHtmlFiles(List<File> files) {
		getOrCreateH1Title();
		getOrCreateScript();
		getOrCreateStyle();
		HtmlDiv tabDiv = getTabDiv();
		if (tabDiv == null) {
			tabDiv = getOrCreateTabDiv();
			getOrCreateBody().appendChild(tabDiv);
		}
		HtmlDiv contentDiv = getContentDiv();
		if (contentDiv == null) {
			contentDiv = getOrCreateContentDiv();
			getOrCreateBody().appendChild(contentDiv);
		}
		for (File file : files) {
			HtmlElement htmlElement = HtmlElement.create(XMLUtil.parseQuietlyToDocument(file).getRootElement());
			HtmlTabbedButton htmlButtonTab = HtmlTabbedButton.createButtonFromHtmlFile(file, htmlElement);
			if (htmlButtonTab != null) {
				tabDiv.appendChild(htmlButtonTab);
			}
			
			List<Element> tabElements = XMLUtil.getQueryElements(
					htmlElement, "//*[@class='"+HtmlTabbedButton.TABCONTENT+"']");
			HtmlElement contentElement = tabElements == null ? null : (HtmlElement) tabElements.get(0);
			if (contentElement != null) {
				contentDiv.appendChild(contentElement);
			}
		}
	}

	public HtmlElement getOrCreateH1Title() {
		HtmlH1 h1 = null;
		Element element = XMLUtil.getSingleElement(getOrCreateBody(), ".//*[local-name()='"+HtmlH1.TAG+"']");
		if (element == null) {
			h1 = new HtmlH1();
			h1.setContent(title);
			getOrCreateBody().appendChild(h1);
		} else {
			h1 = (HtmlH1) HtmlElement.create(element); 
		}
		
		return h1;
	}

	private HtmlDiv getOrCreateTabDiv() {
		HtmlDiv tabDiv = getTabDiv();
		if (tabDiv == null) {
			tabDiv = new HtmlDiv();
			tabDiv.setClassAttribute(TAB);
		}
		return tabDiv;
	}

	private HtmlDiv getTabDiv() {
		Element element = XMLUtil.getSingleElement(getOrCreateBody(), ".//*[local-name()='"+HtmlDiv.TAG+"' and @class='"+TAB+"']");
		return element == null ? null : (HtmlDiv) HtmlElement.create(element);
	}

	private HtmlDiv getOrCreateContentDiv() {
		HtmlDiv contentDiv = getContentDiv();
		if (contentDiv == null) {
			contentDiv = new HtmlDiv();
			contentDiv.setClassAttribute(TABCONTENTDIV);
		}
		return contentDiv;
	}

	private HtmlDiv getContentDiv() {
		Element element = XMLUtil.getSingleElement(getOrCreateBody(), 
				".//*[local-name()='"+HtmlDiv.TAG+"' and @class='"+TABCONTENTDIV+"']");
		return element == null ? null : (HtmlDiv) HtmlElement.create(element);
	}

	/* this is crude - only one variant.
	 * 
	 */
	private HtmlScript getOrCreateScript() {
		HtmlScript script = null;
		Element element = XMLUtil.getSingleElement(getOrCreateHead(), "./*[local-name()='"+HtmlScript.TAG+"']");
		if (element != null) {
			script = (HtmlScript) HtmlElement.create(element);
		} else {
			HtmlScript buttonScript = readButtonScript(BUTTON_SCRIPT_RESOURCE);
			LOG.debug(buttonScript.getValue());
			getOrCreateHead().appendChild(buttonScript);
			LOG.debug("BUTT"+getOrCreateHead().getOrCreateScript().getValue());
			
		}
		return script;
	}
	
	/* this is crude - only one variant.
	 * 
	 */
	private HtmlStyle getOrCreateStyle() {
		HtmlStyle style = null;
		Element element = XMLUtil.getSingleElement(getOrCreateHead(), "./*[local-name()='"+HtmlStyle.TAG+"']");
		if (element != null) {
			style = (HtmlStyle) HtmlElement.create(element);
		} else {
			HtmlStyle buttonStyle = readButtonStyle(BUTTON_STYLE_RESOURCE);
			getOrCreateHead().appendChild(buttonStyle);
		}
		return style;
	}
	
}
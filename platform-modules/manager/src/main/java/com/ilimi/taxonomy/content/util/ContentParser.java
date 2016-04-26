package com.ilimi.taxonomy.content.util;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.NotDirectoryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ilimi.common.exception.ClientException;
import com.ilimi.taxonomy.content.common.ContentErrorMessageConstants;
import com.ilimi.taxonomy.content.entity.Content;
import com.ilimi.taxonomy.content.entity.Controller;
import com.ilimi.taxonomy.content.entity.Manifest;
import com.ilimi.taxonomy.content.entity.Media;
import com.ilimi.taxonomy.content.entity.Plugin;
import com.ilimi.taxonomy.content.enums.ContentWorkflowPipelineParams;
import com.ilimi.taxonomy.enums.ContentErrorCodes;

public class ContentParser {

	private static final Map<String, String> nonPluginTags = new HashMap<String, String>();

	private static final Map<String, String> eventTags = new HashMap<String, String>();

	private static final Map<String, String> actionTags = new HashMap<String, String>();

	{
		nonPluginTags.put(ContentWorkflowPipelineParams.param.name(), ContentWorkflowPipelineParams.param.name());
		nonPluginTags.put(ContentWorkflowPipelineParams.data.name(), ContentWorkflowPipelineParams.data.name());
		nonPluginTags.put(ContentWorkflowPipelineParams.audioSprite.name(),ContentWorkflowPipelineParams.audioSprite.name());
		nonPluginTags.put(ContentWorkflowPipelineParams.action.name(), ContentWorkflowPipelineParams.action.name());
		nonPluginTags.put(ContentWorkflowPipelineParams.event.name(), ContentWorkflowPipelineParams.event.name());
		nonPluginTags.put(ContentWorkflowPipelineParams.manifest.name(), ContentWorkflowPipelineParams.manifest.name());
		nonPluginTags.put(ContentWorkflowPipelineParams.media.name(), ContentWorkflowPipelineParams.media.name());
		nonPluginTags.put(ContentWorkflowPipelineParams.theme.name(), ContentWorkflowPipelineParams.theme.name());

		eventTags.put(ContentWorkflowPipelineParams.event.name(), ContentWorkflowPipelineParams.event.name());
		actionTags.put(ContentWorkflowPipelineParams.action.name(), ContentWorkflowPipelineParams.action.name());

	}

	public void parseContent(String xml) {
		DocumentBuilderFactory factory = null;
		DocumentBuilder builder = null;
		Document document = null;
		try {
			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
			document = builder.parse(new InputSource(new StringReader(xml)));
			document.getDocumentElement().normalize();
			Element root = document.getDocumentElement();
			processContentDocument(root);
		} catch (ParserConfigurationException e) {
			throw new ClientException(ContentErrorCodes.ERR_CONTENT_BLANK_TAXONOMY_ID.name(),
					ContentErrorMessageConstants.XML_PARSE_CONFIG_ERROR);
		} catch (SAXException e) {
			throw new ClientException(ContentErrorCodes.ERR_CONTENT_BLANK_TAXONOMY_ID.name(),
					ContentErrorMessageConstants.XML_NOT_WELL_FORMED_ERROR);
		} catch (IOException e) {
			throw new ClientException(ContentErrorCodes.ERR_CONTENT_BLANK_TAXONOMY_ID.name(),
					ContentErrorMessageConstants.XML_IO_ERROR);
		} finally {
			if(document != null){
				document = null;
	        }
		}
	}
	
	private void processContentDocument(Element root) {
		Content content = new Content();
		if (null != root && root.hasChildNodes()) {
			content.setManifest(getContentManifest(root.getElementsByTagName(ContentWorkflowPipelineParams.manifest.name())));
			content.setControllers(getControllers(root.getElementsByTagName(ContentWorkflowPipelineParams.controller.name())));
			content.setPlugins(getPlugins(root));
		}
	}
	
	private Manifest getContentManifest(NodeList manifestNodes) {
		Manifest manifest = new Manifest();
		if (null != manifestNodes && manifestNodes.getLength() > 0) {
			List<Media> medias = new ArrayList<Media>();
			for (int i = 0; i < manifestNodes.getLength(); i++) {
				if (manifestNodes.item(i).getNodeType() == Node.ELEMENT_NODE && 
						StringUtils.equalsIgnoreCase(manifestNodes.item(i).getNodeName(), ContentWorkflowPipelineParams.media.name()))
					medias.add(getContentMedia(manifestNodes.item(i)));
			}
			manifest.setMedias(medias);
		}
		return manifest;
	}
	
	private Media getContentMedia(Node mediaNode) {
		Media media = new Media();
		if (null != mediaNode) {
			Map<String, String> data = getAttributeMap(mediaNode);
			data.put(ContentWorkflowPipelineParams.tag_name.name(), ContentWorkflowPipelineParams.media.name());
			media.setData(data);
			media.setChildrenData(getChildrenData(mediaNode));
		}
		return media;
	}
	
	private Map<String, String> getAttributeMap(Node node) {
		Map<String, String> attributes = new HashMap<String, String>();
		if (node.hasAttributes()) {
			NamedNodeMap attribute = node.getAttributes();
			for (int i = 0; i < attribute.getLength(); i++) {
				if (!StringUtils.isBlank(attribute.item(i).getNodeName()) && 
						!StringUtils.isBlank(attribute.item(i).getNodeValue()))
					attributes.put(attribute.item(i).getNodeName(), attribute.item(i).getNodeValue());
			}
		}
		return attributes;
	}
	
	public List<Map<String, String>> getChildrenData(Node node) {
		List<Map<String, String>> childrenTags = new ArrayList<Map<String, String>>();
		NodeList nodeList = ((Document) node).getElementsByTagName("*");
	    for (int i = 0; i < nodeList.getLength(); i++) {
	        Node childNode = nodeList.item(i);
	        if (childNode.getNodeType() == Node.ELEMENT_NODE) {
	            Map<String, String> map = getAttributeMap(childNode);
	            map.put(ContentWorkflowPipelineParams.tag_name.name(), childNode.getNodeName());
	            map.put(ContentWorkflowPipelineParams.group_tag_name.name(), childNode.getParentNode().getNodeName());
	            childrenTags.add(map);
	        }
	    }
	    return childrenTags;
	}
	
	private List<Controller> getControllers(NodeList controllerNodes) {
		List<Controller> controllers = new ArrayList<Controller>();
		if (null != controllerNodes && controllerNodes.getLength() > 0) {
			for (int i = 0; i < controllerNodes.getLength(); i++) {
				Controller controller = new Controller();
				if (controllerNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
					Map<String, String> map = getAttributeMap(controllerNodes.item(i));
					map.put(ContentWorkflowPipelineParams.tag_name.name(), ContentWorkflowPipelineParams.controller.name());
					controller.setData(map);
					controller.setcData(getCData(controllerNodes.item(i)));
				}
				controllers.add(controller);
			}
		}
		return controllers;
	}
	
	private String getCData(Node node) {
		String cData = "";
		if (null != node && node.hasChildNodes()) {
			NodeList childrenNodes = node.getChildNodes();
			for (int i = 0; i < childrenNodes.getLength(); i++) {
				if (childrenNodes.item(i).getNodeType() == Node.CDATA_SECTION_NODE) {
					cData = childrenNodes.item(i).getNodeValue();
				}
			}
		}
		return cData;
	}
	
	private List<Plugin> getPlugins(Node node) {
		List<Plugin> plugins = new ArrayList<Plugin>();
		
		return plugins;
	}
	
	private Node getPluginViewofDocument(Node node) {
		if (null != node) {
			//Remove Manifest From Document
			NodeList manifestNodeList = ((Document) node).getElementsByTagName(ContentWorkflowPipelineParams.manifest.name());
			for (int i = 0; i < manifestNodeList.getLength(); i++)
				manifestNodeList.item(i).getParentNode().removeChild(manifestNodeList.item(i));
			// Remove Controllers From Document
			NodeList controllerNodeList = ((Document) node).getElementsByTagName(ContentWorkflowPipelineParams.controller.name());
			for (int i = 0; i < controllerNodeList.getLength(); i++)
				controllerNodeList.item(i).getParentNode().removeChild(controllerNodeList.item(i));
			// Normalize the DOM tree, puts all text nodes in the
			// full depth of the sub-tree underneath this node
			node.normalize();
		}
		return node;
	} 

	private boolean isPlugin(String elementName) {
		if (null == nonPluginTags.get(elementName))
			return true;
		return false;
	}

	private boolean isEvent(String elementName) {
		if (null == eventTags.get(elementName))
			return true;
		return false;
	}

	private boolean isAction(String elementName) {
		if (null == actionTags.get(elementName))
			return true;
		return false;
	}

}

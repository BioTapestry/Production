/*
 **    Copyright (C) 2003-2017 Institute for Systems Biology 
 **                            Seattle, Washington, USA. 
 **
 **    This library is free software; you can redistribute it and/or
 **    modify it under the terms of the GNU Lesser General Public
 **    License as published by the Free Software Foundation; either
 **    version 2.1 of the License, or (at your option) any later version.
 **
 **    This library is distributed in the hope that it will be useful,
 **    but WITHOUT ANY WARRANTY; without even the implied warranty of
 **    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 **    Lesser General Public License for more details.
 **
 **    You should have received a copy of the GNU Lesser General Public
 **    License along with this library; if not, write to the Free Software
 **    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.systemsbiology.biotapestry.web;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.app.WebServerApplication;
import org.systemsbiology.biotapestry.app.WebServerApplication.WebClientState;
import org.systemsbiology.biotapestry.app.WebServerApplication.CommandResult;
import org.systemsbiology.biotapestry.app.WebServerApplication.CommandResult.ResultType;
import org.systemsbiology.biotapestry.app.WebServerApplication.WebClientState.NetModuleObject;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.nav.XPlatModelTree;
import org.systemsbiology.biotapestry.ui.LinkSegmentID;
import org.systemsbiology.biotapestry.ui.menu.XPlatGenericMenu;

import org.systemsbiology.biotapestry.web.serialization.BioTapSerializerFactory;
import org.systemsbiology.biotapestry.web.serialization.ExcludeTransformer;
import org.systemsbiology.biotapestry.web.serialization.LinkSegmentIDTransformer;

import flexjson.JSONSerializer;

/****************************************************************************
 **
 ** Servlet for BioTapestry Web Applications
 */

public class BioTapServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	private static final int MAGIC = -42;

	////////////////////////////////////////////////////////////////////////////
	//
	// PUBLIC STATIC CONSTANTS
	//
	////////////////////////////////////////////////////////////////////////////   

	////////////////////////////////////////////////////////////////////////////
	//
	// PRIVATE MEMBERS
	//
	////////////////////////////////////////////////////////////////////////////   

	private final String DEFAULT_MODEL_FILE_ = "/WEB-INF/data/DefaultModel.btp";
	private final String PLUGINS_DIR_ = "/WEB-INF/plugins/";
	private final String BTP_DIR_ = "/WEB-INF/data/";
	private final String CONFIG_FILE_ = "/WEB-INF/configuration.txt";
	private final String MODEL_LIST_KEY_ = "modellistfile";
	private final String MODEL_FILE_KEY_ = "modelfile";
	
	private WebServerApplication wsa_;
	
    // In general, all responses should be UTF-8 encoded to ensure proper
    // handling of special characters
	private final String charEncoding_ = "UTF-8";
	
	private String modelListFile_ = null;
	private String modelFilename_ = null;
	private String servicesDir_ = null;
	
	////////////////////////////////////////////////////////////////////////////
	//
	// PUBLIC CONSTRUCTORS
	//
	////////////////////////////////////////////////////////////////////////////    

	public BioTapServlet() {
		super();
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// INIT
	//
	////////////////////////////////////////////////////////////////////////////    

	@Override
	public void init() {
		try { 
			Properties configProps = new Properties();
			ServletContext myContext = this.getServletContext();

			configProps.load(myContext.getResourceAsStream(this.CONFIG_FILE_));

			if(configProps.getProperty(this.MODEL_LIST_KEY_) != null) {
				this.modelListFile_ = configProps.getProperty(this.MODEL_LIST_KEY_);
			}
			
			if(new File(myContext.getRealPath("/") + this.PLUGINS_DIR_).listFiles().length > 0) {
				System.out.println("[STATUS] Loading plugins from " + this.PLUGINS_DIR_);
				this.servicesDir_ = this.PLUGINS_DIR_;
			}
			
			String fileMsg = null;
			if(configProps.getProperty(this.MODEL_FILE_KEY_) == null) {
				System.out.println("[STATUS] A model file was not supplied in configuration.txt! Loading default file: " + this.DEFAULT_MODEL_FILE_);
				modelFilename_ = this.DEFAULT_MODEL_FILE_;
				fileMsg = "[WARNING] No model file entry was found in configuration.txt! Defaulting to " + this.DEFAULT_MODEL_FILE_;
			} else {
				modelFilename_ = (!configProps.getProperty(this.MODEL_FILE_KEY_).startsWith(this.BTP_DIR_) ? this.BTP_DIR_ : "") + configProps.getProperty("modelfile");
				fileMsg = "[STATUS] Model file is now " + configProps.getProperty(this.MODEL_FILE_KEY_);
			}
			System.out.println(fileMsg);
			
			wsa_ = new WebServerApplication(
				"/"+this.getServletConfig().getServletName(),
				null,
				myContext.getRealPath("/"),
				this.servicesDir_
			);    

		} catch(IOException e) {
			System.err.println("IOException during Servlet init():");
			e.printStackTrace();
		} catch(WebServerApplication.GeneratorException e) {
			System.err.println("WebServerApplication.GeneratorException during Servlet init():");
			e.printStackTrace();
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	//
	// GET
	//
	////////////////////////////////////////////////////////////////////////////    

	@Override  
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				
		OutputStream os = null;
		Scanner scanner = null;
		
		try {
			// Thou shalt not cache
			response.setHeader("Cache-Control", "no-cache");
	        response.setDateHeader("Expires", 0);
	        response.setHeader("Pragma", "no-cache");
	        response.setDateHeader("Max-Age", 0);
			
			RequestTargetType target = RequestTargetType.getRequestTargetType(request.getParameter("target"));
			if(target == null) {
				System.out.println("[WARNING] Target came back null for " + request.getParameter("target"));
				target = RequestTargetType.UNKNOWN;
			}
			
			BTState appState = getBTStateForSession(target,request,response);
			
			if(appState == null) {
				System.out.println("[WARNING] Session isn't valid in GET for " + target + "! Sending session restart.");
				response.setContentType("application/json");
				response.setCharacterEncoding(charEncoding_);
				os = response.getOutputStream();
				os.write("{\"result\": \"NEW_SESSION\"}".getBytes());
				return;
			}
			
      BTState.AppSources aSrc = appState.getAppSources();
			
			JSONSerializer serializer = null;
			
			if(target == RequestTargetType.INIT) {
				serializer = new JSONSerializer().include("*").transform(new ExcludeTransformer(), void.class)
					.transform(new LinkSegmentIDTransformer(),LinkSegmentID.class);
				response.setContentType("application/json");
				response.setCharacterEncoding(charEncoding_);
				os = response.getOutputStream();
				os.write(this.getInitResponse(appState, aSrc.uics, aSrc.tSrc, serializer).getBytes());
				return;				
			} else {
				serializer = new JSONSerializer().exclude("*.class").include("*")
					.transform(new ExcludeTransformer(), void.class)
					.transform(new LinkSegmentIDTransformer(),LinkSegmentID.class);
			}

			String nodeID = null;
			
			String tab = request.getParameter("currentTab"); 
			
			os = response.getOutputStream();
			
			switch(target) {
				case LINKS_TO_INTERSECTIONS:
					response.setContentType("application/json");
					WebServerApplication.HSRWrapper hsrw = new WebServerApplication.HSRWrapper(request);
					CommandResult results = new CommandResult(ResultType.SUCCESS);
					results.addResult("LINKS", wsa_.mapLinksToIntersections(appState, hsrw));
					os.write(serializer.deepSerialize(results).getBytes());
					break;
				case MODEL_IMAGE:
					nodeID = request.getParameter("modelID");
					response.setContentType("image/png");
					wsa_.getImage(appState, nodeID, os); 				
					break;
		        case MODEL_ANNOT_IMAGE:
		        	nodeID = request.getParameter("modelID");
					 String annotMime = wsa_.getImageType(appState, nodeID, "MODEL");
					 if (annotMime != null) {
						 response.setContentType(annotMime);
						 wsa_.getAnnotationImage(appState, tab, nodeID, os);
					 }
					 break;
		        case GROUP_NODE_IMAGE:
		        	nodeID = request.getParameter("nodeID");
					// The "type" parameter determines which image is returned from the Group Node.
					// "image": The group node image
					// "map":   The mask image
					 String imgMime = wsa_.getImageType(appState, nodeID, "GROUP_NODE");
					 if (imgMime != null) {
						 response.setContentType(imgMime);
						 wsa_.getGroupNodeImage(appState, tab, nodeID, request.getParameter("type"), os);
					 }
					 break;		        	
				case NODE_JSON:
					nodeID = request.getParameter("nodeID");
					String nodeType = request.getParameter("nodeType");
					response.setContentType("application/json");
					response.setCharacterEncoding(charEncoding_);
								        			        					
					Map<String,Object> modelMap = wsa_.getNodeMap(appState, tab,nodeID, nodeType);
					
					JSONSerializer modelMapSerializer = BioTapSerializerFactory.getModelMapTransformer();					
					os.write(modelMapSerializer.deepSerialize(modelMap).getBytes(charEncoding_));
					break;
				
				case MODEL_TREE:
					XPlatModelTree xpmt = wsa_.getModelTree(appState,tab);  
					response.setContentType("application/json");
					response.setCharacterEncoding(charEncoding_);
					os.write(serializer.deepSerialize(xpmt).getBytes(charEncoding_));
					break;
					
				case ICON:
					String name = request.getParameter("iconName");
					response.setContentType("image/png");
					wsa_.getIcon(appState, name, os);
					break;
					
				case MENU_DEF:
					hsrw = new WebServerApplication.HSRWrapper(request);
					XPlatGenericMenu xpgm = wsa_.getMenuDefinition(appState, tab,hsrw);
					response.setContentType("application/json");
					response.setCharacterEncoding(charEncoding_);
					os.write(serializer.deepSerialize(xpgm).getBytes(charEncoding_));
					break;
					
				case COMMAND:
				case SET_NODE:
				case SESSION_NEVER_EXPIRES:
				case SESSION_EXPIRES_IN:				
					//throw new IllegalArgumentException("[ERROR] " + request.getParameter("target") + " must be requested with POST");
					
					doPost(request, response);
					break; 
				
				default:
					// TODO: sometimes the target is spontaneously not being parsed right; need to figure out why
					System.out.println("[STATUS] Target " + request.getParameter("target") + " [" + target.toString() + "] not recognized, defaulting");
					response.setContentType("text/html");
					basicPage(os); 
					break;
			}
					
		} catch (Exception ex) {
			response.setContentType("application/json");
			response.setStatus(400);
			String errBack = "{\"errormsg\": \"" + ex.getMessage() + "\"}";
			if(os != null) {
				os.write(errBack.getBytes());
			} else {
				response.getWriter().write(errBack);	
			}
			System.err.println("Exception in GET: " + ex.getMessage());
			ex.printStackTrace();   	
		} finally {
			if(scanner != null) {
				scanner.close();
			}
		}

		return;
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// POST
	//
	////////////////////////////////////////////////////////////////////////////

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
				
		OutputStream os = null;
		String errMsg = null;
		RequestTargetType target = null;
		
		try {	
			
			// Caching is disabled
			response.setHeader("Cache-Control", "no-cache");
	        response.setDateHeader("Expires", 0);
	        response.setHeader("Pragma", "no-cache");
	        response.setDateHeader("Max-Age", 0);
			
			target = RequestTargetType.getRequestTargetType(request.getParameter("target"));
			
			if(target == null) {
				System.out.println("[WARNING] Target came back null for " + request.getParameter("target"));
				target = RequestTargetType.UNKNOWN;
			}
			
			BTState appState = getBTStateForSession(target,request,response);
			
			if(appState == null) {
				System.out.println("[WARNING] Session isn't valid in POST for " + target + "!");
				response.setContentType("application/json");
				response.setCharacterEncoding(charEncoding_);
				os = response.getOutputStream();
				os.write("{\"result\": \"NEW_SESSION\"}".getBytes());
				return;
			}
			
			BTState.AppSources aSrc = appState.getAppSources();
			
			JSONSerializer serializer = new JSONSerializer().exclude("*.class").include("*")
					.transform(new ExcludeTransformer(), void.class)
					.transform(new LinkSegmentIDTransformer(),LinkSegmentID.class);
			
			if(target == RequestTargetType.INIT) {
				response.setContentType("application/json");
				response.setCharacterEncoding(charEncoding_);
				os = response.getOutputStream();
				os.write(this.getInitResponse(appState, aSrc.uics, aSrc.tSrc, serializer).getBytes());
				return;				
			}
			
			WebServerApplication.HSRWrapper hsrw = null;
			os = response.getOutputStream();
			
			String reply = null;
						
			switch(target) {
				case SESSION_NEVER_EXPIRES:
					sessionNeverExpires(request);
					response.setContentType("application/json");
					reply = "{\"reply\":\"session expiration disabled\"}";
					os.write(reply.getBytes(charEncoding_));					
					break;
				case SESSION_EXPIRES_IN:
					setSessionExpiry(request,Integer.parseInt(request.getParameter("expiry")));
					response.setContentType("application/json");			 
					reply = "{\"reply\":\"session expiration set to " + request.getParameter("expiry") + "\"}";
					os.write(reply.getBytes(charEncoding_));					
					break;					
				case COMMAND:
					// Commands may need to send over classes which will be deserialized; we need to include
					// class information in these cases.
					hsrw = new WebServerApplication.HSRWrapper(request);
					CommandResult result = wsa_.processCommand(appState, hsrw); 
					if (result == null) {
						response.setContentType("text/html"); 			  
						basicPage(os); 
					} else {
						if(result.resultNeedsClassAttr()) {
							// if an outgoing result is sending out classes which will need class information,
							// we need to make a different serializer which will not exclude the class attribute
							serializer = new JSONSerializer().include("*")
								.transform(new ExcludeTransformer(), void.class)
								.transform(new LinkSegmentIDTransformer(),LinkSegmentID.class);
						}
						response.setContentType("application/json");
						response.setCharacterEncoding(charEncoding_);
						switch(result.getResultType()) {
							case PARAMETER_ERROR:
							case PROCESSING_ERROR:
							case WAITING_FOR_CLICK:
							case ILLEGAL_CLICK_PROCESSED:
							case LEGAL_CLICK_PROCESSED:
							case SUCCESS:
							case CANCEL:
							case XPLAT_DIALOG:
							case XPLAT_FRAME:
							case STACK_PAGE:
								os.write(serializer.deepSerialize(result).getBytes(charEncoding_));
								break;			
								
						  default:
				              errMsg = "The CommandResult type " + result.getResultType() + " is not recognized.";
				              System.err.println(errMsg);
				              throw new IllegalArgumentException(errMsg);
					  }
					} 					
					break;
				case SET_NODE:					
					System.err.println("[ERROR] Use target=command to set tree nodes!");
					throw new Exception(" Use target=command to set tree nodes!!");
				case MENU_DEF:
					hsrw = new WebServerApplication.HSRWrapper(request);
					XPlatGenericMenu xpgm = wsa_.getMenuDefinition(appState, hsrw);
					response.setContentType("application/json");
					response.setCharacterEncoding(charEncoding_);
					os.write(serializer.deepSerialize(xpgm).getBytes(charEncoding_));
					break;					
				default:
					errMsg = "[ERROR] The POST request target " + request.getParameter("target") + " is not recognized.";
					System.err.println(errMsg);
					throw new IllegalArgumentException(errMsg);
			} 
		} catch (Exception ex) {
			errMsg = "{\"errormsg\": \"" + ex.getMessage() + "\", \"status\":\"400\"}";
			if(target == RequestTargetType.UPLOAD_FILE) {
				response.setContentType("text/html");
				errMsg = "<html><body><textarea>" + errMsg + "</textarea></body></html>";
			} else {
				response.setContentType("application/json");
			}
			response.setCharacterEncoding(charEncoding_);
			response.setStatus(400);
			os.write(errMsg.getBytes());

			System.err.println("Exception in POST: " + ex.getMessage());
			ex.printStackTrace(); 
		}
		return;
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// PRIVATE METHODS
	//
	////////////////////////////////////////////////////////////////////////////
		
	private void setSessionExpiry(HttpServletRequest request, int expiry) {
		HttpSession session = request.getSession(false);
		if(session != null) {
			session.setMaxInactiveInterval(expiry);
		}
	}
	
	private void sessionNeverExpires(HttpServletRequest request) {
		setSessionExpiry(request,MAGIC);
	}
	
	private String getInitResponse(BTState appState, UIComponentSource uics, TabSource tSrc, JSONSerializer serializer) throws Exception {
		Set<Object> emptySelex = new HashSet<Object>();
		Set<NetModuleObject> emptyMods = new HashSet<NetModuleObject>();
		
		String initResponse = 
			"{\"result\": \"SESSION_READY\", \"clientMode\": \"" + (uics.getIsEditor() ? "Editor" : "Viewer") + "\"" 
			+ ",\"stateObject\":" + serializer.deepSerialize(new WebClientState(emptySelex," ",emptyMods," "))
			+ ",\"supportedMenus\":" + serializer.deepSerialize(wsa_.getSupportedMenuRequests(appState))  
			+ ",\"tabs\":" + serializer.deepSerialize(tSrc.getTabs())
			+ ",\"currentTab\":" + tSrc.getCurrentTabIndex()
				
			+ "}";
		
		return initResponse;
	}
	
	

 /****************************************************************************
  **
  ** Handle appState init
  */
  
  private BTState initNewBTState(String sessionID) throws WebServerApplication.GeneratorException, IOException {
	  HashMap<String, Object> args = new HashMap<String, Object>();
	  BTState appState = new BTState(sessionID, args, true, true);
	  wsa_.initNewState(appState, getServletContext().getResourceAsStream(modelFilename_));
	  return (appState);
  }
  
  /****************************************************************************
  **
  ** Handle getting per-session appState. Newly synchronized: had a race condition
  */
  
  private synchronized BTState getBTStateForSession(
	  RequestTargetType target,HttpServletRequest request,HttpServletResponse response
  ) throws WebServerApplication.GeneratorException, IOException {

		HttpSession session = request.getSession(false);
					
		BTState appState = ((session != null) ? (BTState)session.getAttribute("btState") : null);
		
		if(target != RequestTargetType.INIT && (appState==null)){
			return null;
		}
					
		if (session == null) {
			session = request.getSession(true);
		}
		
		if(appState == null) {
			appState = initNewBTState(session.getId());
			BTState.AppSources aSrc = appState.getAppSources();
			session.setAttribute("btState", appState);
			if(this.modelListFile_ != null) {
				aSrc.pafs.setServerBtpFileList(this.modelListFile_);
			}
		}
		return (appState);
  }


	private void basicPage(OutputStream os) throws IOException {

		PrintWriter out = new PrintWriter(os);
		
		out.println("<html>\n");
		out.println("<head>\n");
		out.println("<title>BioTapestry</title>\n");
		out.println("</head>\n");
		out.println("<body>\n");
		out.println("  <center>");
		out.println("  <img src=\"/BTapAsServlet/BioTapServlet?target=modelImage\" border=\"3\" height=\"540\" width=\"707\"/>");
		out.println("  <p>");
		out.println("  <form method=\"get\" action=\"/BTapAsServlet/BioTapServlet\">\n");
		out.println("    <input type=\"submit\" name=\"formButton\" value=\"Add Gene\">");
		out.println("    <input type=\"hidden\" name=\"target\" value=\"command\">");   
		out.println("    <input type=\"hidden\" name=\"cmdClass\" value=\"main\">");   
		out.println("    <input type=\"hidden\" name=\"cmdKey\" value=\"" + FlowMeister.MainFlow.ADD + "\">");
		out.println("  </form>");
		out.println("  <form method=\"get\" action=\"/BTapAsServlet/BioTapServlet\">\n");
		out.println("    <input type=\"submit\" name=\"formButton\" value=\"Search\">");
		out.println("    <input type=\"hidden\" name=\"target\" value=\"command\">");   
		out.println("    <input type=\"hidden\" name=\"cmdClass\" value=\"main\">");   
		out.println("    <input type=\"hidden\" name=\"cmdKey\" value=\"" + FlowMeister.MainFlow.NETWORK_SEARCH + "\">");
		out.println("  </form>");    
		out.println("  </p>");
		out.println("  </center>");
		out.println("</body>");
		out.println("</html>"); 
		
		out.flush();

		return;
	}

	
	///////////////////////
	// RequestTargetType
	//////////////////////
	//
	// Types of requests handled by GET and POST
	// 
	public enum RequestTargetType {
		SET_NODE,
		NODE_JSON,
		MODEL_IMAGE,
		GROUP_NODE_IMAGE,
		COMMAND,
		MODEL_TREE,
		MENU_DEF,
		ICON,
		UPLOAD_FILE,
		FILE_LIST,
		SESSION_NEVER_EXPIRES,
		SESSION_EXPIRES_IN,
		MODEL_ANNOT_IMAGE,
		INIT,
		LINKS_TO_INTERSECTIONS,
		UNKNOWN;
		
		private static Map<String, RequestTargetType> stringToType = null;
		
		public static RequestTargetType getRequestTargetType(String typeAsString) {
			if(stringToType == null) {
				stringToType = new Hashtable<String, RequestTargetType>();
				stringToType.put("init", RequestTargetType.INIT);
				stringToType.put("setnode", RequestTargetType.SET_NODE);
				stringToType.put("nodejson", RequestTargetType.NODE_JSON);
				stringToType.put("modelimage", RequestTargetType.MODEL_IMAGE);
				stringToType.put("command", RequestTargetType.COMMAND);
				stringToType.put("modeltree", RequestTargetType.MODEL_TREE);
				stringToType.put("menudef", RequestTargetType.MENU_DEF);
				stringToType.put("uploadfile", RequestTargetType.UPLOAD_FILE);
				stringToType.put("iconimage", RequestTargetType.ICON);
				stringToType.put("filelist", RequestTargetType.FILE_LIST);
				stringToType.put("disablesessionexpiry", RequestTargetType.SESSION_NEVER_EXPIRES);
				stringToType.put("setsessionexpiry", RequestTargetType.SESSION_EXPIRES_IN);
				stringToType.put("modelannotimage", RequestTargetType.MODEL_ANNOT_IMAGE);
				stringToType.put("linkstointersections", RequestTargetType.LINKS_TO_INTERSECTIONS);
				stringToType.put("groupnodeimage", RequestTargetType.GROUP_NODE_IMAGE);
			}
			if(typeAsString == null) {
				throw new IllegalArgumentException("String value of request type was null!");
			}
			return stringToType.get(typeAsString.toLowerCase());
		}
	}
}

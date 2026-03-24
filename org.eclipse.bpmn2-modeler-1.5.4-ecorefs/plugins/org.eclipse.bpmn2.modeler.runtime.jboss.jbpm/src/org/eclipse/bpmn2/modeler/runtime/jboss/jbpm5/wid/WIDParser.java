/*******************************************************************************
 * Copyright (c) 2011, 2012, 2013, 2014 Red Hat, Inc.
 *  All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 *
 * @author Brian Fitzpatrick
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.wid;

import java.util.ArrayList;
import java.util.List;

/**
 * @author bfitzpat
 *
 */
public class WIDParser {
	
	public enum Section {
	    PARAMETERS, RESULTS, DEPENDENCIES 
	}
	
    /**
     * Takes in the String content of a *.wid/*.conf file and 
     * parses it into a HashMap of WorkItemDefinition classes.
     * 
     * @param content the String content of a WID definition.
     * @return a HashMap of the parsed WID definition, indexed by the custom
     * work item task names.
     * @throws WIDException
     */
    public static List<WorkItemDefinition> parse(String content) throws WIDException {

    	List<WorkItemDefinition> widMap = new ArrayList<WorkItemDefinition>();
    	
    	if (content == null) {
        	  WIDException widException = 
        			  new WIDException(
        					  "No data passed to WIDHandler.processWorkDefinitionsContent method"); //$NON-NLS-1$
        	  throw widException;
          }
          if (widMap != null) {
        	  widMap.clear();
          }
          
          String strings[] = content.split("[\n]+"); //$NON-NLS-1$
    	  int openBrackets = 0;
    	  WorkItemDefinition currentWid = new WorkItemDefinitionImpl();
    	  
    	  Section current = Section.PARAMETERS;
    	  
          for (int i = 0; i < strings.length; i++) {
        	  String trim = strings[i].trim();
        	  if (trim.length() == 0) continue;
        	  if (trim.startsWith("import ") || trim.startsWith("import\t")) {
        		  // collect "import" statements so we can resolve fully qualified class names
        		  // in Parameters and Results types
        		  String fullyQualifiedClassName = trim.substring(7, trim.length()-1);
        		  currentWid.addImport(fullyQualifiedClassName);
        	  }
        	  
        	  if (trim.startsWith("[") || trim.endsWith("[") || trim.endsWith(":")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        		  if (trim.endsWith(":") && i < strings.length - 1) { //$NON-NLS-1$
        			  trim = strings[i].trim() + strings[i+1].trim();
        		  } else {
        			  openBrackets++;
        		  }
        		  String[] nameValue = trim.split("[:]+"); //$NON-NLS-1$
        		  if (nameValue.length == 2) {
        			  String name = nameValue[0].replace('"', ' ').trim();
        			  if (name.equalsIgnoreCase("parameters")) { //$NON-NLS-1$
	    				  current = Section.PARAMETERS;
	    			  } else if (name.equalsIgnoreCase("results")) { //$NON-NLS-1$
	    				  current = Section.RESULTS;
	    			  } else if (name.equalsIgnoreCase("dependencies")) { //$NON-NLS-1$
	    				  current = Section.DEPENDENCIES;
	    			  }
        		  }
        	  }
        	  if (trim.startsWith("]") || trim.endsWith("]")) { //$NON-NLS-1$ //$NON-NLS-2$
        		  openBrackets--;
        		  if (openBrackets == 1) {
	    			  if (currentWid != null && currentWid.getName() != null) {
	    				  widMap.add(currentWid);
	    			  }
	    			  currentWid = new WorkItemDefinitionImpl();
        		  }
        	  }
        	  if (trim.contains(":")) { //$NON-NLS-1$
        		  String[] nameValue = trim.split("[:]+"); //$NON-NLS-1$
        		  if (nameValue.length == 2 || nameValue.length == 3) {
        			  String name = nameValue[0].replace('"', ' ').trim();
        			  int valueIndex = 1;
        			  if (nameValue.length == 3) {
        				  valueIndex = 2;
        				  name = name + ':' + nameValue[1].replace('"', ' ').trim();
        			  }
        			  String stringValue = nameValue[valueIndex].replace('"', ' ').replace(',', ' ').
        					  replace('[',' ').trim();
        			  if (openBrackets == 2 && stringValue.trim().length() > 0) {
        				  if (name.equalsIgnoreCase("name")) { //$NON-NLS-1$
        					  currentWid.setName(stringValue);
        				  } else if (name.equalsIgnoreCase("displayName")) { //$NON-NLS-1$
        					  currentWid.setDispalyName(stringValue);
						  } else if (name.equalsIgnoreCase("description")) { //$NON-NLS-1$
							  currentWid.setDescription(stringValue);
						  } else if (name.equalsIgnoreCase("category")) { //$NON-NLS-1$
							  currentWid.setCategory(stringValue);
        				  } else if (name.equalsIgnoreCase("icon")) { //$NON-NLS-1$
        					  currentWid.setIcon(stringValue);
        				  } else if (name.equalsIgnoreCase("customEditor")) { //$NON-NLS-1$
        					  currentWid.setCustomEditor(stringValue);
        				  } else if (name.equalsIgnoreCase("eclipse:customEditor")) { //$NON-NLS-1$
        					  currentWid.setEclipseCustomEditor(stringValue);
        				  }
        			  } else if (openBrackets == 3 && stringValue.trim().length() > 0) {
        				  WorkItemDefinition.Parameter parameter = new WorkItemDefinition.Parameter();
        				  if (stringValue.startsWith("new") && stringValue.indexOf("(")>0) { //$NON-NLS-1$ //$NON-NLS-2$
        					  int index = stringValue.indexOf("("); //$NON-NLS-1$
        					  stringValue = stringValue.substring(3,index).trim();
        					  // look up the DataType in the registry and replace the DataType
        					  // name with its Java type equivalent name
        					  String fqn = currentWid.findImport(stringValue);
        					  if (fqn!=null)
        						  parameter.type = fqn;
        					  else
        						  parameter.type = stringValue;
        					  // the parameter's value (a drools-core DataType object) will be constructed
        					  // at the time the WorkEditor is created. We can't do it here because we don't
        					  // have access to the JavaProject in which the WorkEditor java class is defined.
        				  }
        				  else {
        					  parameter.type = "java.lang.String";
        					  parameter.value = stringValue;
        				  }
        				  if (current == Section.PARAMETERS)
        					  currentWid.getParameters().put(name, parameter);
        				  else if (current == Section.RESULTS)
        					  currentWid.getResults().put(name, parameter);
        			  }
        		  }
        	  }
          }
          
          return widMap;
     }

}
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
 * @author Bob Brodt
 ******************************************************************************/

package org.eclipse.bpmn2.modeler.core.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 *
 */
public class FileUtils {

	/**
	 * 
	 */
	public FileUtils() {
	}

	public static IFile getFile(URI uri) {
		if (uri == null) {
			return null;
		}
	
		final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
	
		// File URIs
		final String filePath = getWorkspaceFilePath(uri.trimFragment());
		if (filePath == null) {
			final IPath location = Path.fromOSString(uri.toString());
			final IFile file = workspaceRoot.getFileForLocation(location);
			if (file != null) {
				return file;
			}
			return null;
		}
	
		// Platform resource URIs
		else {
			final IResource workspaceResource = workspaceRoot.findMember(filePath);
			return (IFile) workspaceResource;
		}
	}

	public static String getWorkspaceFilePath(URI uri) {
		if (uri.isPlatform()) {
			return uri.toPlatformString(true);
		}
		return null;
	}

	public static File createTempFile(String name) {
		return createTempFile(name,null);
	}

	public static File createTempFile(String name, InputStream istream) {
		File tempFile = null;
		try {
			tempFile = File.createTempFile(name, ".bpmn"); //$NON-NLS-1$
			if (istream!=null) {
				OutputStream ostream = new FileOutputStream(tempFile);
	
				int read = 0;
				byte[] bytes = new byte[1024];
	
				while ((read = istream.read(bytes)) != -1) {
					ostream.write(bytes, 0, read);
				}
	
				istream.close();
	
				ostream.flush();
				ostream.close();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tempFile;
	}

	public static String createTempName(String name) {
		String tempDir = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$
		if (!tempDir.endsWith(File.separator))
			tempDir += File.separator;
		String tempName = tempDir + name + "." + EcoreUtil.generateUUID(); //$NON-NLS-1$
		return tempName;
	}

	public static void deleteTempFile(URI uri) {
		File file = new File(uri.toFileString());
		if (file.exists())
			file.delete();
	}

	public static boolean isTempFile(URI uri) {
		String tempDir = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$
		String uriDir = uri.trimFragment().trimSegments(1).devicePath();
		return tempDir!=null && tempDir.compareToIgnoreCase(uriDir)==0;
	}

	public static byte[] readStream(InputStream inputstream) throws IOException {
	    byte bytes[] = (byte[]) null;
	    int i = 0;
	    byte tempBytes[] = new byte[1024];
	    for (int j = inputstream.read(tempBytes); j != -1; j = inputstream.read(tempBytes)) {
	        byte tempBytes2[] = new byte[i + j];
	        if (i > 0) {
	            System.arraycopy(bytes, 0, tempBytes2, 0, i);
	        }
	        System.arraycopy(tempBytes, 0, tempBytes2, i, j);
	        bytes = tempBytes2;
	        i += j;
	    }
	
	    return bytes;
	}
}

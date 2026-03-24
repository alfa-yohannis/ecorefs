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

import java.util.Stack;
import java.util.StringTokenizer;

/**
 * This class augments the {@code java.util.StringTokenizer} class by providing methods
 * to peek at the next token, and push tokens back into the token stream.
 */
public class ExtendedStringTokenizer extends StringTokenizer {

	Stack<String> stack = new Stack<String>();
	
	/* (non-Javadoc)
	 * @see java.util.StringTokenizer#StringTokenizer()
	 */
	public ExtendedStringTokenizer(String str, String delim, boolean returnDelims) {
		super(str, delim, returnDelims);
	}
	
	public void pushToken(String token) {
		stack.push(token);
	}
	
	public String peekToken() {
		String token = nextToken();
		stack.push(token);
		return token;
	}
	
	/* (non-Javadoc)
	 * @see java.util.StringTokenizer#nextToken()
	 */
	@Override
	public String nextToken() {
		if (!stack.isEmpty())
			return stack.pop();
		return super.nextToken();
	}
}
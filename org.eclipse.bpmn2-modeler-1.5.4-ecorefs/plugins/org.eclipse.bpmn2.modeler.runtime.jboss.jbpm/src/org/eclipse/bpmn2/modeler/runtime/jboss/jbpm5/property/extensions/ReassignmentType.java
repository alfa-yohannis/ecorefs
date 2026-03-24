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

package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.property.extensions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.Enumerator;

public enum ReassignmentType implements Enumerator {
	NOT_STARTED_REASSIGN(0, "NotStartedReassign", "NotStartedReassign"), //$NON-NLS-1$ //$NON-NLS-2$
    NOT_COMPLETED_REASSIGN(1, "NotCompletedReassign", "NotCompletedReassign"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final int NOT_STARTED_REASSIGN_VALUE = 0;
    public static final int NOT_COMPLETED_REASSIGN_VALUE = 1;

    private static final ReassignmentType[] VALUES_ARRAY = new ReassignmentType[] { NOT_STARTED_REASSIGN, NOT_COMPLETED_REASSIGN };

    /**
     * A public read-only list of all the '<em><b>Gateway Direction</b></em>' enumerators.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public static final List<ReassignmentType> VALUES = Collections.unmodifiableList(Arrays
            .asList(VALUES_ARRAY));

    /**
     * Returns the '<em><b>Gateway Direction</b></em>' literal with the specified literal value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public static ReassignmentType get(String literal) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            ReassignmentType result = VALUES_ARRAY[i];
            if (result.toString().equals(literal)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Gateway Direction</b></em>' literal with the specified name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public static ReassignmentType getByName(String name) {
        for (int i = 0; i < VALUES_ARRAY.length; ++i) {
            ReassignmentType result = VALUES_ARRAY[i];
            if (result.getName().equals(name)) {
                return result;
            }
        }
        return null;
    }

    /**
     * Returns the '<em><b>Gateway Direction</b></em>' literal with the specified integer value.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public static ReassignmentType get(int value) {
        switch (value) {
        case NOT_STARTED_REASSIGN_VALUE:
            return NOT_STARTED_REASSIGN;
        case NOT_COMPLETED_REASSIGN_VALUE:
            return NOT_COMPLETED_REASSIGN;
        }
        return null;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    private final int value;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    private final String name;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    private final String literal;

    /**
     * Only this class can construct instances.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    private ReassignmentType(int value, String name, String literal) {
        this.value = value;
        this.name = name;
        this.literal = literal;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public String getLiteral() {
        return literal;
    }

    @Override
    public String toString() {
        return literal;
    }

}
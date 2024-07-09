/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
* VERSION:4.4:FA:FA-2108:04/10/2019:[PATRIUS] Incoherence hash code/equals dans ComparableInterval
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 *
 *
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * @history 07/03/2012
 */
package fr.cnes.sirius.patrius.math.interval;

/**
 * <p>
 * - Describes the type of an interval endpoint : OPENED or CLOSED.
 * </p>
 * 
 * @useSample <p>
 *            An interval of Doubles ] 1.0 ; 2.0 [ is created this way :
 *            </p>
 *            <p>
 *            <code>
 *            Interval<Double> interval = new Interval<Double>(IntervalEndpointType.OPEN, 1.0, 2.0,
 *            IntervalEndpointType.OPEN);
 *            </code>
 *            </p>
 * 
 * @concurrency unconditionally thread-safe
 * 
 * @author Pierre Cardoso
 * 
 * @version $Id: IntervalEndpointType.java 18083 2017-10-02 16:54:39Z bignon $
 * 
 * @since 3.0
 * 
 * @see GenericInterval
 * 
 */
public enum IntervalEndpointType {

    /**
     * the end point is not in the interval.
     */
    OPEN,

    /**
     * the end point is in the interval.
     */
    CLOSED;
    
    /** hashcode for OPEN */
    private static final int OPEN_HASHCODE = 13;
    
    /** hashcode for CLOSED */
    private static final int CLOSED_HASHCODE = 37;

    /**
     * Returns OPEN if the instance is CLOSED and CLOSED if the instance is OPEN.
     * 
     * @return an {@link IntervalEndpointType}
     */
    public IntervalEndpointType getOpposite() {
        if (this == OPEN) {
            return CLOSED;
        } else {
            return OPEN;
        }
    }
    
    /**
     * Computes hash code for the instance (13 if the instance is OPEN and 37 if the instance is CLOSED)
     * 
     * @return the hashcode
     */
    public int computeHashCode() {
        if (this == OPEN) {
            return OPEN_HASHCODE;
        } else {
            return CLOSED_HASHCODE;
        }
    }
}

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * HISTORY
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
 * @history 26/07/2011
 */
package fr.cnes.sirius.patrius.math.interval;

/**
 * <p>
 * - very simple class to represent an interval only by its ending point nature : this is what all intervals have in
 * common.
 * </p>
 * <p>
 * - This class is abstract : it can't be instanced.
 * </p>
 * <p>
 * - It contains no method.
 * </p>
 * See DV-MATHS_50, DV-DATES_150
 * 
 * @concurrency immutable
 * 
 * @author Thomas TRAPIER
 * 
 * @version $Id: AbstractInterval.java 17584 2017-05-10 13:26:39Z bignon $
 * 
 * @since 1.0
 * 
 */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class AbstractInterval {

    /** upper end point */
    private IntervalEndpointType upperEndPoint;

    /** lower end point */
    private IntervalEndpointType lowerEndPoint;

    /**
     * @param upEndPoint
     *        the upperEndPoint to set
     */
    protected final void setUpperEndPoint(final IntervalEndpointType upEndPoint) {
        this.upperEndPoint = upEndPoint;
    }

    /**
     * @param lowEndPoint
     *        the lowerEndPoint to set
     */
    protected final void setLowerEndPoint(final IntervalEndpointType lowEndPoint) {
        this.lowerEndPoint = lowEndPoint;
    }

    /**
     * @return the upperEndPoint
     */
    public final IntervalEndpointType getUpperEndPoint() {
        return this.upperEndPoint;
    }

    /**
     * @return the lowerEndPoint
     */
    public final IntervalEndpointType getLowerEndPoint() {
        return this.lowerEndPoint;
    }

}

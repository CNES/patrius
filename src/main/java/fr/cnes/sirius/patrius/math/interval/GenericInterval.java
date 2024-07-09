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
 * @history 23/09/2011
 */
package fr.cnes.sirius.patrius.math.interval;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * - Generic class to describe an interval.
 * </p>
 * <p>
 * - The generic element is the nature of the data defining the upper and lower boundaries.
 * </p>
 * <p>
 * - This class can be extended ; toString may be overriden.
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
 * @param <T>
 *        the nature of ending points.<br>
 *        It is HIGHLY recommended this class be immutable!
 * 
 * @concurrency matches the concurrency of the parameter type : using an immutable type is HIGHLY recommended
 * 
 * @author Pierre Cardoso
 * 
 * @version $Id: GenericInterval.java 18083 2017-10-02 16:54:39Z bignon $
 * 
 * @since 3.0
 * 
 */
public class GenericInterval<T> implements Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 4134192354278539190L;

    /** Interval type for the lower end of the interval. */
    private final IntervalEndpointType lowerEndpoint;

    /** Interval type for the upper end of the interval. */
    private final IntervalEndpointType upperEndpoint;

    /** lower end data value. */
    private final T lowerData;
    /** upper end data value. */
    private final T upperData;

    /**
     * Constructor.<br>
     * The input parameters have to be not null.<br>
     * Otherwise a <code>MathIllegalArgumentException</code> is thrown.
     * 
     * @param lowerDataIn
     *        lower end data value
     * @param upperDataIn
     *        upper end data value
     * @param lowerEndpointIn
     *        lower end point state
     * @param upperEndpointIn
     *        upper end point state
     * @throws MathIllegalArgumentException
     *         when the interval is invalid (unchecked exception)
     */
    public GenericInterval(final IntervalEndpointType lowerEndpointIn, final T lowerDataIn, final T upperDataIn,
        final IntervalEndpointType upperEndpointIn) {
        if (this.genericIntervalIsOK(lowerEndpointIn, lowerDataIn, upperDataIn, upperEndpointIn)) {
            // We simply set the attributes
            this.lowerData = lowerDataIn;
            this.upperData = upperDataIn;
            this.lowerEndpoint = lowerEndpointIn;
            this.upperEndpoint = upperEndpointIn;
        } else {
            throw new MathIllegalArgumentException(PatriusMessages.ARGUMENT_OUTSIDE_DOMAIN);
        }
    }

    /**
     * @return the lowerEndpoint
     */
    public final IntervalEndpointType getLowerEndpoint() {
        return this.lowerEndpoint;
    }

    /**
     * @return the lowerData
     */
    public final T getLowerData() {
        return this.lowerData;
    }

    /**
     * @return the upperData
     */
    public final T getUpperData() {
        return this.upperData;
    }

    /**
     * @return the upperEndpoint
     */
    public final IntervalEndpointType getUpperEndpoint() {
        return this.upperEndpoint;
    }

    /**
     * Returns true if the parameters describe a valid interval.<br>
     * Here it only means they have to be not null.
     * 
     * @param lowerDataIn
     *        lower end data value
     * @param upperDataIn
     *        upper end data value
     * @param lowerEndpointIn
     *        lower end point state
     * @param upperEndpointIn
     *        upper end point state
     * 
     * @return true when the interval represented by the parameters is valid
     */
    private boolean genericIntervalIsOK(final IntervalEndpointType lowerEndpointIn, final T lowerDataIn,
                                        final T upperDataIn, final IntervalEndpointType upperEndpointIn) {
        boolean validFlag = true;

        // Always needed check : no null parameters
        if (lowerEndpointIn == null || upperEndpointIn == null) {
            validFlag = false;
        } else {
            if (lowerDataIn == null || upperDataIn == null) {
                validFlag = false;
            }
        }
        return validFlag;
    }

    /**
     * This method returns a String representing the interval, with boundaries as brackets and the
     * lower/upper values.<br>
     * Example : "] 0.0 , 1.2534 [" for an open interval with doubles.<br>
     * toString is called on the values.<br>
     * Warning : this representation is subject to change.<br>
     * This method may be overriden if convenient.
     * 
     * @return a String with boundary brackets and values.
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public final String toString() {
        // Example of a toString representation for this class :
        // "] 0.0 ; 3.1415 ["
        final String leftB = "[";
        final String rightB = "]";
        final String spc = " ";
        // Starting and ending brackets determined here
        final String startB = (this.lowerEndpoint.equals(IntervalEndpointType.CLOSED)) ? leftB : rightB;
        final String endB = (this.upperEndpoint.equals(IntervalEndpointType.CLOSED)) ? rightB : leftB;
        // Values added here
        return startB + spc + this.lowerData.toString() + " ; " + this.upperData.toString() + spc + endB;
    }

}

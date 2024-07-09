/**
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
 * @history created 01/02/2012
 */
package fr.cnes.sirius.patrius.utils;

import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * Epsilon constants
 * 
 * @concurrency immutable
 * 
 * @author antonj
 * 
 * @version $Id: UtilsPatrius.java 17584 2017-05-10 13:26:39Z bignon $
 * 
 * @since 1.1
 * 
 */
public final class UtilsPatrius {

    /** Smallest positive number such that 1 - EPSILON is not numerically equal to 1. */
    public static final double EPSILON = Precision.EPSILON;

    /** Epsilon used for doubles relative comparison */
    public static final double DOUBLE_COMPARISON_EPSILON = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon for the geometry aspects. */
    public static final double GEOMETRY_EPSILON = 1e-10;

    /**
     * default constructor
     */
    private UtilsPatrius() {

    }
}

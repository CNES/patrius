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
 * @history created 15/02/2016
 *
 * HISTORY
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

/**
 * Enum class for elements type (mean or osculating).
 * 
 * @author Emmanuel Bignon
 * @since 3.2
 * @version $Id: ParametersType.java 18092 2017-10-02 17:12:58Z bignon $
 */

public enum ParametersType {

    /** Mean elements. */
    MEAN,

    /**
     * Osculating elements.
     */
    OSCULATING;
}

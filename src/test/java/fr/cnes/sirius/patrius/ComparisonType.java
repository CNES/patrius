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
 * HISTORY
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */
package fr.cnes.sirius.patrius;

/**
 * Comparison type: absolute or relative.
 * 
 * @author Emmanuel Bignon
 * @version $Id: ComparisonType.java 18088 2017-10-02 17:01:51Z bignon $
 */
public enum ComparisonType {

    /** Absolute difference. */
    ABSOLUTE,

    /** Relative difference. */
    RELATIVE;
}

/**
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
 */
package fr.cnes.sirius.patrius.attitudes;

/**
 * This interface has been created to represent a generic attitude provider without an interval of validity:
 * the attitude can be computed at any date.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: AttitudeLaw.java 18065 2017-10-02 16:42:02Z bignon $
 * 
 * @since 1.3
 * 
 */
public interface AttitudeLaw extends AttitudeProvider {

}

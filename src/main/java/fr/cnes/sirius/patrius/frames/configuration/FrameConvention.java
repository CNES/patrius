/**
 * Copyright 2011-2022 CNES
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
 * @history creation 24/09/2016
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration;

/**
 * IERS conventions enumeration.
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: FrameConvention.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 * @since 3.3
 */
public enum FrameConvention {

    /** IERS 2003. */
    IERS2003,

    /** IERS 2010. */
    IERS2010,

    /** STELA. */
    STELA,

    /** Undefined. */
    NONE;
}

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
 * @history creation 02/01/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:657:29/08/2016:Thread-specific correction models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.libration;

/**
 * Factory for predefined models.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: LibrationCorrectionModelFactory.java 18073 2017-10-02 16:48:07Z bignon $
 */
public final class LibrationCorrectionModelFactory {

    /** Ignore the libration effects. */
    public static final LibrationCorrectionModel NO_LIBRATION = new LibrationCorrectionPerThread(){
        /** {@inheritDoc} */
        @Override
        protected LibrationCorrectionModel buildModel() {
            return new NoLibrationCorrection();
        }
    };

    /** IERS 2010. */
    public static final LibrationCorrectionModel LIBRATION_IERS2010 = new LibrationCorrectionPerThread(){
        /** {@inheritDoc} */
        @Override
        protected LibrationCorrectionModel buildModel() {
            return new IERS2010LibrationCorrection();
        }
    };

    /** Private constructor. */
    private LibrationCorrectionModelFactory() {
    }
}

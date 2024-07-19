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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:657:29/08/2016:Thread-specific correction models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.tides;

/**
 * Factory for predefined models.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: TidalCorrectionModelFactory.java 18073 2017-10-02 16:48:07Z bignon $
 */
public final class TidalCorrectionModelFactory {

    /** Ignore tidal effects. */
    public static final TidalCorrectionModel NO_TIDE = new TidalCorrectionPerThread(){
         /** Serializable UID. */
        private static final long serialVersionUID = -1185445303259341548L;

        /** {@inheritDoc} */
        @Override
        protected TidalCorrectionModel buildModel() {
            return new NoTidalCorrection();
        }
    };
    /** IERS 2010 with interpolation. */
    public static final TidalCorrectionModel TIDE_IERS2010_INTERPOLATED = new TidalCorrectionPerThread(){
         /** Serializable UID. */
        private static final long serialVersionUID = -1717124821423280741L;

        /** {@inheritDoc} */
        @Override
        protected TidalCorrectionModel buildModel() {
            return new TidalCorrectionCache(new IERS2010TidalCorrection());
        }
    };
    /** IERS 2003 with interpolation. */
    public static final TidalCorrectionModel TIDE_IERS2003_INTERPOLATED = new TidalCorrectionPerThread(){
         /** Serializable UID. */
        private static final long serialVersionUID = 4877198582702633996L;

        /** {@inheritDoc} */
        @Override
        protected TidalCorrectionModel buildModel() {
            return new TidalCorrectionCache(new IERS2003TidalCorrection());
        }
    };
    /** IERS 2010 without interpolation. */
    public static final TidalCorrectionModel TIDE_IERS2010_DIRECT = new TidalCorrectionPerThread(){
         /** Serializable UID. */
        private static final long serialVersionUID = -6492114487748000060L;

        /** {@inheritDoc} */
        @Override
        protected TidalCorrectionModel buildModel() {
            return new IERS2010TidalCorrection();
        }
    };
    /** IERS 2003 without interpolation. */
    public static final TidalCorrectionModel TIDE_IERS2003_DIRECT = new TidalCorrectionPerThread(){
         /** Serializable UID. */
        private static final long serialVersionUID = 2508215133587871477L;

        /** {@inheritDoc} */
        @Override
        protected TidalCorrectionModel buildModel() {
            return new IERS2003TidalCorrection();
        }
    };

    /** Private constructor. */
    private TidalCorrectionModelFactory() {
        // Nothing to do
    }
}

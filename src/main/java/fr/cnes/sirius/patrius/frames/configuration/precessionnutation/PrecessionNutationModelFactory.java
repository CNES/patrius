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
 * VERSION::DM:317:15/04/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::FA:657:29/08/2016:Thread-specific correction models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

/**
 * Factory for predefined models.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: PrecessionNutationModelFactory.java 18073 2017-10-02 16:48:07Z bignon $
 */
public final class PrecessionNutationModelFactory {

    /** No precession Nutation. */
    public static final PrecessionNutationModel NO_PN = new PrecessionNutationPerThread(){
        /** Serializable UID. */
        private static final long serialVersionUID = -494548369846137420L;

        /** {@inheritDoc} */
        @Override
        protected PrecessionNutationModel buildModel() {
            return new NoPrecessionNutation();
        }
    };

    /** IERS 2010 with interpolation. */
    public static final PrecessionNutationModel PN_IERS2010_INTERPOLATED_NON_CONSTANT =
        new PrecessionNutationPerThread(){
            /** Serializable UID. */
            private static final long serialVersionUID = -220156158703680110L;

            /** {@inheritDoc} */
            @Override
            protected PrecessionNutationModel buildModel() {
                return new PrecessionNutationCache(new IERS20032010PrecessionNutation(
                    PrecessionNutationConvention.IERS2010, true));
            }
        };

    /** IERS 2003 with interpolation. */
    public static final PrecessionNutationModel PN_IERS2003_INTERPOLATED_NON_CONSTANT =
        new PrecessionNutationPerThread(){
            /** Serializable UID. */
            private static final long serialVersionUID = -2998703343309329349L;

            /** {@inheritDoc} */
            @Override
            protected PrecessionNutationModel buildModel() {
                return new PrecessionNutationCache(new IERS20032010PrecessionNutation(
                    PrecessionNutationConvention.IERS2003, true));
            }
        };

    /** IERS 2010 without interpolation. */
    public static final PrecessionNutationModel PN_IERS2010_DIRECT_NON_CONSTANT = new PrecessionNutationPerThread(){
        /** Serializable UID. */
        private static final long serialVersionUID = -5722899548936614875L;

        /** {@inheritDoc} */
        @Override
        protected PrecessionNutationModel buildModel() {
            return new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2010, true);
        }
    };

    /** IERS 2003 without interpolation. */
    public static final PrecessionNutationModel PN_IERS2003_DIRECT_NON_CONSTANT = new PrecessionNutationPerThread(){
        /** Serializable UID. */
        private static final long serialVersionUID = 8954405667455039871L;

        /** {@inheritDoc} */
        @Override
        protected PrecessionNutationModel buildModel() {
            return new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2003, true);
        }
    };

    /** IERS 2010 with interpolation. */
    public static final PrecessionNutationModel PN_IERS2010_INTERPOLATED_CONSTANT = new PrecessionNutationPerThread(){
        /** Serializable UID. */
        private static final long serialVersionUID = 5079525775916679200L;

        /** {@inheritDoc} */
        @Override
        protected PrecessionNutationModel buildModel() {
            return new PrecessionNutationCache(new IERS20032010PrecessionNutation(
                PrecessionNutationConvention.IERS2010, false));
        }
    };

    /** IERS 2003 with interpolation. */
    public static final PrecessionNutationModel PN_IERS2003_INTERPOLATED_CONSTANT = new PrecessionNutationPerThread(){
        /** Serializable UID. */
        private static final long serialVersionUID = 8543092609400851266L;

        /** {@inheritDoc} */
        @Override
        protected PrecessionNutationModel buildModel() {
            return new PrecessionNutationCache(new IERS20032010PrecessionNutation(
                PrecessionNutationConvention.IERS2003, false));
        }
    };

    /** IERS 2010 without interpolation. */
    public static final PrecessionNutationModel PN_IERS2010_DIRECT_CONSTANT = new PrecessionNutationPerThread(){
        /** Serializable UID. */
        private static final long serialVersionUID = -6411588258065110648L;

        /** {@inheritDoc} */
        @Override
        protected PrecessionNutationModel buildModel() {
            return new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2010, false);
        }
    };

    /** IERS 2003 without interpolation. */
    public static final PrecessionNutationModel PN_IERS2003_DIRECT_CONSTANT = new PrecessionNutationPerThread(){
        /** Serializable UID. */
        private static final long serialVersionUID = -3269976387225868020L;

        /** {@inheritDoc} */
        @Override
        protected PrecessionNutationModel buildModel() {
            return new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2003, false);
        }
    };

    /** Stela model. */
    public static final PrecessionNutationModel PN_STELA = new PrecessionNutationPerThread(){
        /** Serializable UID. */
        private static final long serialVersionUID = 1193331595903382424L;

        /** {@inheritDoc} */
        @Override
        protected PrecessionNutationModel buildModel() {
            return new StelaPrecessionNutationModel();
        }
    };

    /** Private constructor. */
    private PrecessionNutationModelFactory() {
    }
}

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
 * @history creation 29/08/2016
 *
 * HISTORY
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.13:DM:DM-108:08/12/2023:[PATRIUS] Modele d'obliquite et de precession de la Terre
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:657:29/08/2016:Thread-specific correction models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Provides per-thread PrecessionNutationCorrectionModel.
 * This class is to be used for GCRF to CIRF transformation.
 *
 * @author Marc Madaule
 *
 * @version $Id: PrecessionNutationPerThread.java 18073 2017-10-02 16:48:07Z bignon $
 *
 * @since 3.3
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class PrecessionNutationPerThread implements PrecessionNutationModel {
    // CHECKSTYLE: resume AbstractClassName check

     /** Serializable UID. */
    private static final long serialVersionUID = -6657989747584674879L;

    /**
     * Will hold all thread-specific model instances.
     */
    private final ThreadLocal<PrecessionNutationModel> threadLocalModel =
        new ThreadLocal<PrecessionNutationModel>(){
            /** {@inheritDoc} */
            @Override
            protected PrecessionNutationModel initialValue() {
                // delegate to overridable factory method
                return PrecessionNutationPerThread.this.buildModel();
            }
        };

        /** {@inheritDoc} */
    @Override
    public CIPCoordinates getCIPCoordinates(final AbsoluteDate date) {
        return this.getThreadLocalModel().getCIPCoordinates(date);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirect() {
        return this.getThreadLocalModel().isDirect();
    }

    /** {@inheritDoc} */
    @Override
    public FrameConvention getOrigin() {
        return this.getThreadLocalModel().getOrigin();
    }

    /**
     * Factory method to create a model.
     * This method is called once for each thread using this object.
     *
     * @return a model
     */
    protected abstract PrecessionNutationModel buildModel();

    /**
     * Returns the thread-specific model instance.
     *
     * @return a model
     */
    private PrecessionNutationModel getThreadLocalModel() {
        return this.threadLocalModel.get();
    }
}

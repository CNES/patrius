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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:657:29/08/2016:Thread-specific correction models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.libration;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.frames.configuration.eop.PoleCorrection;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Provides per-thread LibrationCorrectionModel.
 * 
 * @author Marc Madaule
 * 
 * @version $Id: LibrationCorrectionPerThread.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 * @since 3.3
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class LibrationCorrectionPerThread implements LibrationCorrectionModel {
    // CHECKSTYLE: resume AbstractClassName check

    /** Serializable UID. */
    private static final long serialVersionUID = 5187461817288608329L;

    /**
     * Will hold all thread-specific model instances.
     */
    private final ThreadLocal<LibrationCorrectionModel> threadLocalModel =
        new ThreadLocal<LibrationCorrectionModel>(){
            /** {@inheritDoc} */
            @Override
            protected LibrationCorrectionModel initialValue() {
                // delegate to overridable factory method
                return LibrationCorrectionPerThread.this.buildModel();
            }
        };

    /** {@inheritDoc} */
    @Override
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) throws PatriusException {
        return this.getThreadLocalModel().getPoleCorrection(date);
    }

    /** {@inheritDoc} */
    @Override
    public double getUT1Correction(final AbsoluteDate date) {
        return this.getThreadLocalModel().getUT1Correction(date);
    }

    /**
     * Factory method to create a model.
     * This method is called once for each thread using this object.
     * 
     * @return a model
     */
    protected abstract LibrationCorrectionModel buildModel();

    /**
     * Returns the thread-specific model instance.
     * 
     * @return a model
     */
    private LibrationCorrectionModel getThreadLocalModel() {
        return this.threadLocalModel.get();
    }

    /** {@inheritDoc} */
    @Override
    public FrameConvention getOrigin() {
        return this.getThreadLocalModel().getOrigin();
    }
}

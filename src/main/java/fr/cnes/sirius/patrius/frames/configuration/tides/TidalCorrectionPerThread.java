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
 *
 * @history creation 29/08/2016
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:657:29/08/2016:Thread-specific correction models
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.tides;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.frames.configuration.eop.PoleCorrection;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Provides per-thread TidalCorrectionModel.
 * 
 * @author Marc Madaule
 * 
 * @version $Id: TidalCorrectionPerThread.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 * @since 3.3
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class TidalCorrectionPerThread implements TidalCorrectionModel {
    // CHECKSTYLE: resume AbstractClassName check

    /** Serial UID. */
    private static final long serialVersionUID = -4181381052182963281L;
    /**
     * Will hold all thread-specific model instances.
     */
    private final ThreadLocal<TidalCorrectionModel> threadLocalModel =
        new ThreadLocal<TidalCorrectionModel>(){
            /** {@inheritDoc} */
            @Override
            protected TidalCorrectionModel initialValue() {
                // delegate to overridable factory method
                return TidalCorrectionPerThread.this.buildModel();
            }
        };

    /** {@inheritDoc} */
    @Override
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) {
        return this.getThreadLocalModel().getPoleCorrection(date);
    }

    /** {@inheritDoc} */
    @Override
    public double getUT1Correction(final AbsoluteDate date) {
        return this.getThreadLocalModel().getUT1Correction(date);
    }

    /** {@inheritDoc} */
    @Override
    public double getLODCorrection(final AbsoluteDate date) {
        return this.getThreadLocalModel().getLODCorrection(date);
    }

    /**
     * Factory method to create a model.
     * This method is called once for each thread using this object.
     * 
     * @return a model
     */
    protected abstract TidalCorrectionModel buildModel();

    /**
     * Returns the thread-specific model instance.
     * 
     * @return a model
     */
    private TidalCorrectionModel getThreadLocalModel() {
        return this.threadLocalModel.get();
    }

    /** {@inheritDoc} */
    @Override
    public FrameConvention getOrigin() {
        return this.getThreadLocalModel().getOrigin();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirect() {
        return this.getThreadLocalModel().isDirect();
    }
}

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
 * @history creation 28/06/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This class contains the precession nutation model used within the
 * {@link fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationImplementation} class.
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment thread safe if the underlying PrecessionNutationModel is thread safe
 * 
 * @author Julie Anton, Rami Houdroge
 * 
 * @version $Id: PrecessionNutation.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 * @since 1.2
 */
public class PrecessionNutation implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -3700748706951357410L;

    /** Precession Nutation model. */
    private final PrecessionNutationModel model;

    /** Flag for use of EOP data (nutation corrections). */
    private final boolean useEop;

    /**
     * Constructor. Create an instance with given Precession Nutation model and nutation corrections flag.
     * 
     * @param useEopData
     *        true if nutation corrections from EOP data are to be used, false otherwise.
     * @param iersPrecessionNutation
     *        model of precession nutation to use.
     */
    public PrecessionNutation(final boolean useEopData, final PrecessionNutationModel iersPrecessionNutation) {
        this.model = iersPrecessionNutation;
        this.useEop = useEopData;
    }

    /**
     * Compute the CIP pole coordinates at given date.
     * 
     * @param date
     *        date at which to compute CIP coordinates
     * @return the CIP pole coordinates
     */
    public double[] getCIPMotion(final AbsoluteDate date) {
        return this.model.getCIPMotion(date);
    }

    /**
     * Compute the CIP pole coordinate derivatives at given date.
     * 
     * @param date
     *        date at which to compute CIP coordinate derivatives
     * @return the CIP pole coordinates
     */
    public double[] getCIPMotionTimeDerivative(final AbsoluteDate date) {
        return this.model.getCIPMotionTimeDerivative(date);
    }

    /**
     * Use EOP data for nutation correction.
     * 
     * @return true if EOP data is to be used, flase if not.
     */
    public boolean useEopData() {
        return this.useEop;
    }

    /**
     * @return the precession nutation model
     */
    public PrecessionNutationModel getPrecessionNutationModel() {
        return this.model;
    }
}

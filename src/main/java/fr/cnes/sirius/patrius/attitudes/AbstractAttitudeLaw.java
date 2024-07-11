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
 */
/*
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:403:20/10/2015:Improving ergonomic
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;


/**
 * This abstract class gather all common features to classes implementing the {@link AttitudeLaw} interface. <br>
 * 
 * @author chabaudp
 * @version $Id: AbstractAttitudeLaw.java 18065 2017-10-02 16:42:02Z bignon $
 * @since 1.3
 */

public abstract class AbstractAttitudeLaw implements AttitudeLaw {

    /** Serial UID. */
    private static final long serialVersionUID = -1910701919077112225L;

    /** Flag to indicate if spin derivation computation is activated. */
    private boolean spinDerivativesComputation = false;

    /** {@inheritDoc} */
    @Override
    public void setSpinDerivativesComputation(final boolean computeSpinDerivatives) {
        this.spinDerivativesComputation = computeSpinDerivatives;
    }

    /**
     * Get the value of the flag indicating if spin derivation computation is activated.
     * 
     * @return true if the spin derivative have to be computed,
     *         false otherwise
     */
    @SuppressWarnings("PMD.BooleanGetMethodName")
    public boolean getSpinDerivativesComputation() {
        return this.spinDerivativesComputation;
    }
}

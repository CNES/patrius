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
 * Copyright 2010-2011 Centre National d'Études Spatiales
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11.1:DM:DM-95:30/06/2023:[PATRIUS] Utilisation de types gen. dans les classes internes d'interp. de AbstractEOPHistory
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:completed Javadoc concerning the partial derivatives parameters
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:280:08/10/2014:propagator modified in order to use the mu of gravitational forces
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Force model for Newtonian central body attraction.
 * 
 * <p>
 * The implementation of this class enables the computation of partial derivatives with respect to the <b>central
 * attraction coefficient</b>.
 * </p>
 * 
 * @author Luc Maisonobe
 */

public class NewtonianGravityModel extends AbstractHarmonicGravityModel {

    /** Serializable UID. */
    private static final long serialVersionUID = -7754312556095545327L;

    /**
     * Simple constructor for Earth-centered computations: uses GCRF.
     * 
     * @param mu
     *        central attraction coefficient (m^3/s^2)
     */
    public NewtonianGravityModel(final double mu) {
        this(FramesFactory.getGCRF(), mu);
    }

    /**
     * Simple constructor.<br>
     * Note that the body frame is used for translation only. Therefore it shall be as high as possible in the tree of
     * frames so as to avoid unnecessary calculations.
     * 
     * @param centralBodyFrame
     *        body frame of the central body
     * @param mu
     *        central attraction coefficient (m^3/s^2)
     */
    public NewtonianGravityModel(final Frame centralBodyFrame, final double mu) {
        this(centralBodyFrame, mu, true);
    }

    /**
     * Simple constructor.<br>
     * Note that the body frame is used for translation only. Therefore it shall be as high as possible in the tree of
     * frames so as to avoid unnecessary calculations.
     * 
     * @param centralBodyFrame
     *        body frame of the central body
     * @param mu
     *        central attraction coefficient (m^3/s^2)
     * @param computePD
     *        true if partial derivatives wrt position have to be computed
     */
    public NewtonianGravityModel(final Frame centralBodyFrame, final double mu, final boolean computePD) {
        this(centralBodyFrame, new Parameter(MU, mu), computePD);
    }

    /**
     * Simple constructor using {@link Parameter}.<br>
     * Note that the body frame is used for translation only. Therefore it shall be as high as possible in the tree of
     * frames so as to avoid unnecessary calculations.
     * 
     * @param centralBodyFrame
     *        body frame of the central body
     * @param mu
     *        parameter representing central attraction coefficient (m^3/s^2)
     */
    public NewtonianGravityModel(final Frame centralBodyFrame, final Parameter mu) {
        this(centralBodyFrame, mu, true);
    }

    /**
     * Simple constructor using {@link Parameter}.<br>
     * Note that the body frame is used for translation only. Therefore it shall be as high as possible in the tree of
     * frames so as to avoid unnecessary calculations.
     * 
     * @param centralBodyFrame
     *        body frame of the central body
     * @param mu
     *        parameter representing central attraction coefficient (m^3/s^2)
     * @param computePD
     *        true if partial derivatives wrt position have to be computed
     */
    public NewtonianGravityModel(final Frame centralBodyFrame, final Parameter mu, final boolean computePD) {
        super(centralBodyFrame, mu);
    }

    /** {@inheritDoc}. */
    @Override
    public Vector3D computeNonCentralTermsAcceleration(final Vector3D positionInBodyFrame, final AbsoluteDate date)
        throws PatriusException {
        // The newtonian gravity model does not have any non-central terms
        return Vector3D.ZERO;
    }

    /** {@inheritDoc}. */
    @Override
    public final double[][] computeNonCentralTermsDAccDPos(final Vector3D positionInBodyFrame,
                                                           final AbsoluteDate date)
        throws PatriusException {
        // The Newtonian gravity model does not have any non-central terms, so the acceleration derivatives with respect
        // to the position for the non-central term is zero
        return new double[3][3];
    }
}

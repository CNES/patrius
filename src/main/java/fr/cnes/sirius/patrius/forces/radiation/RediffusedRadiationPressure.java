/**
 *
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
 * @history creation 12/03/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.13:DM:DM-101:08/12/2023:[PATRIUS] Harmonisation des eclipses pour les evenements et pour la PRS
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:completed Javadoc concerning the partial derivatives parameters
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:318:05/11/2014:anomalies correction for class RediffusedFlux
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::FA:461:11/06/2015:Corrected partial derivatives computation for rediffused PRS
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.radiation;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.models.RediffusedRadiativeModel;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.JacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.ParameterUtils;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * Class that represents a rediffused radiative force.
 * </p>
 * <p>
 * The implementation of this class enables the computation of partial derivatives with respect to <b>K0 albedo global
 * coefficient</b>, <b>K0 infrared global coefficient</b>, <b>absorption</b>, <b>specular reflection</b> or <b>diffusion
 * reflection coefficients</b>.
 * </p>
 *
 * @concurrency conditionally thread-safe
 *
 * @concurrency.comment thread-safe if the CelestialBody attribute is.
 *
 * @author clauded
 *
 * @version $Id: RediffusedRadiationPressure.java 17582 2017-05-10 12:58:16Z bignon $
 *
 * @since 1.2
 *
 */
public final class RediffusedRadiationPressure extends JacobiansParameterizable implements
    ForceModel, GradientModel {

    /** Serializable UID. */
    private static final long serialVersionUID = -6974428498597145839L;
    /** Sun model. */
    private final CelestialPoint sun;
    /** body frame */
    private final CelestialBodyFrame bodyFrame;
    /** number of corona. */
    private final int nCorona;
    /** number of meridian. */
    private final int nMeridian;
    /** emissivity model */
    private final IEmissivityModel emissivityModel;
    /** rediffused radiative pressure model. */
    private final RediffusedRadiationSensitive radiativeModel;

    /** spacecraftstate stored in cache after acceleration computation */
    private SpacecraftState spaceCraftStateComputationKey;
    /**
     * True if acceleration partial derivatives with respect to position have to be computed.
     */
    private final boolean computePartialDerivativesWrtPosition;

    /**
     * Constructor.
     *
     * @param inSun coordinate of sun
     * @param inBodyFrame boby frame
     * @param inCorona number of corona
     * @param inMeridian number of meridian
     * @param inEmissivityModel emissivity model
     * @param inModel redistributed radiative model
     */
    public RediffusedRadiationPressure(final CelestialPoint inSun, final CelestialBodyFrame inBodyFrame,
            final int inCorona, final int inMeridian, final IEmissivityModel inEmissivityModel,
            final RediffusedRadiationSensitive inModel) {
        this(inSun, inBodyFrame, inCorona, inMeridian, inEmissivityModel, inModel, true);
    }

    /**
     * Constructor.
     *
     * @param inSun coordinate of sun
     * @param inBodyFrame boby frame
     * @param inCorona number of corona
     * @param inMeridian number of meridian
     * @param inEmissivityModel emissivity model
     * @param inModel redistributed radiative model
     * @param computePD true if partial derivatives wrt position have to be computed
     */
    public RediffusedRadiationPressure(final CelestialPoint inSun, final CelestialBodyFrame inBodyFrame,
            final int inCorona, final int inMeridian, final IEmissivityModel inEmissivityModel,
            final RediffusedRadiationSensitive inModel, final boolean computePD) {
        super();
        this.addJacobiansParameter(inModel.getJacobianParameters());
        ParameterUtils.addFieldToParameters(getParameters(), StandardFieldDescriptors.FORCE_MODEL, this.getClass());
        this.sun = inSun;
        this.bodyFrame = inBodyFrame;
        this.nCorona = inCorona;
        this.nMeridian = inMeridian;
        this.emissivityModel = inEmissivityModel;
        this.radiativeModel = inModel;
        this.computePartialDerivativesWrtPosition = computePD;
    }

    /**
     * Creates a new instance from the data in another one but with a different assembly.
     *
     * @param otherInstance the other instance
     * @param assembly the new assembly
     */
    public RediffusedRadiationPressure(final RediffusedRadiationPressure otherInstance, final Assembly assembly) {
        this(otherInstance.getInSun(), otherInstance.getInBodyFrame(), otherInstance.getInCorona(),
                otherInstance.getInMeridian(), otherInstance.getInEmissivityModel(),
                new RediffusedRadiativeModel(otherInstance.isAlbedoComputed(),
                    otherInstance.isIRComputed(), otherInstance.getK0Albedo(),
                    otherInstance.getK0Ir(), assembly));
    }

    /** {@inheritDoc} */
    @Override
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
        throws PatriusException {
        // compute relative acceleration
        final Vector3D gamma = this.computeAcceleration(s);

        // add contribution to the ODE second member
        adder.addXYZAcceleration(gamma.getX(), gamma.getY(), gamma.getZ());
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {
        this.radiativeModel.initDerivatives();

        // computing elementary rediffused flux in bodyFrame frame
        final RediffusedFlux redistributedFlux = new RediffusedFlux(this.nCorona, this.nMeridian, this.bodyFrame,
            this.sun, s.getOrbit(), s.getDate(), this.emissivityModel, this.radiativeModel.getFlagIr(),
            this.radiativeModel.getFlagAlbedo());

        final ElementaryFlux[] flux =
            redistributedFlux.getFlux(this.bodyFrame.getTransformTo(s.getFrame(), s.getDate()));

        final Vector3D acc = this.radiativeModel.rediffusedRadiationPressureAcceleration(s, flux);

        this.spaceCraftStateComputationKey = s;
        return acc;
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDState(final SpacecraftState s, final double[][] dAccdPos,
                              final double[][] dAccdVel)
        throws PatriusException {

        if (this.computeGradientPosition()) {
            // Compute acceleration and derivatives value
            if (!s.equals(this.spaceCraftStateComputationKey)) {
                this.computeAcceleration(s);
            }
            // Acceleration derivatives of the model
            this.radiativeModel.addDAccDStateRediffusedRadiativePressure(s, dAccdPos, dAccdVel);
        }
    }

    /**
     * {@inheritDoc} <br>
     * K0ALBEDO_COEFFICIENT -->derivatives with respect to the K0 albedo global coefficient <br>
     * K0IR_COEFFICIENT --> derivatives with respect to the K0 infrared global coefficient <br>
     * ABSORPTION_COEFFICIENT --> derivatives with respect to the absorption coefficient <br>
     * SPECULAR_COEFFICIENT --> derivatives with respect to the specular reflection coefficient <br>
     * DIFFUSION_COEFFICIENT --> derivatives with respect to the diffusion reflection coefficient <br>
     */
    @Override
    public void addDAccDParam(final SpacecraftState s, final Parameter param,
                              final double[] dAccdParam)
        throws PatriusException {
        if (!this.supportsJacobianParameter(param)) {
            throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param);
        }
        if (!s.equals(this.spaceCraftStateComputationKey)) {
            this.computeAcceleration(s);
        }
        this.radiativeModel.addDAccDParamRediffusedRadiativePressure(s, param, dAccdParam);
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientPosition() {
        return this.computePartialDerivativesWrtPosition;
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientVelocity() {
        return false;
    }

    /**
     * Getter for the coordinate of sun used at construction.
     *
     * @return the coordinate of sun.
     */
    public CelestialPoint getInSun() {
        return this.sun;
    }

    /**
     * Getter for the boby frame used at construction.
     *
     * @return the boby frame.
     */
    public CelestialBodyFrame getInBodyFrame() {
        return this.bodyFrame;
    }

    /**
     * Getter for the number of corona used at construction.
     *
     * @return the number of corona.
     */
    public int getInCorona() {
        return this.nCorona;
    }

    /**
     * Getter for the number of meridian used at construction.
     *
     * @return the number of meridian.
     */
    public int getInMeridian() {
        return this.nMeridian;
    }

    /**
     * Getter for the emissivity model used at construction.
     *
     * @return the emissivity model.
     */
    public IEmissivityModel getInEmissivityModel() {
        return this.emissivityModel;
    }

    /**
     * Getter for the albedo indicator used at construction.
     *
     * @return the albedo indicator.
     */
    public boolean isAlbedoComputed() {
        return this.radiativeModel.getFlagAlbedo();
    }

    /**
     * Getter for the infrared indicator used at construction.
     *
     * @return the infrared indicator.
     */
    public boolean isIRComputed() {
        return this.radiativeModel.getFlagIr();
    }

    /**
     * Getter for the albedo global multiplicative factor used at construction.
     *
     * @return the albedo global multiplicative factor.
     */
    public double getK0Albedo() {
        return this.radiativeModel.getK0Albedo().getValue();
    }

    /**
     * Getter for the infrared global multiplicative factor used at construction.
     *
     * @return the infrared global multiplicative factor.
     */
    public double getK0Ir() {
        return this.radiativeModel.getK0Ir().getValue();
    }

    /**
     * Getter for the assembly used at construction.
     *
     * @return the assembly.
     */
    public Assembly getAssembly() {
        return this.radiativeModel.getAssembly();
    }

    /** {@inheritDoc} */
    @Override
    public void checkData(final AbsoluteDate start, final AbsoluteDate end) {
        // Nothing to do
    }
}

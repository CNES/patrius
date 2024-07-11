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
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.JacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
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

    /** Serial UID. */
    private static final long serialVersionUID = -6974428498597145839L;
    /** Sun model. */
    private final CelestialBody sun;
    /** body frame */
    private final Frame bodyFrame;
    /** number of corona. */
    private final int nCorona;
    /** number of meridian. */
    private final int nMeridian;
    /** emissivity model */
    private final IEmissivityModel emissivityModel;
    /** rediffused radiative pressure model. */
    private final RediffusedRadiationSensitive radiativeModel;
    /** computing of acceleration */
    private int computingAcc;
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
     * @throws PatriusException thrown if no radiative properties found
     */
    public RediffusedRadiationPressure(final CelestialBody inSun, final Frame inBodyFrame,
        final int inCorona, final int inMeridian, final IEmissivityModel inEmissivityModel,
        final RediffusedRadiationSensitive inModel) throws PatriusException {
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
     * @throws PatriusException thrown if no radiative properties found
     */
    public RediffusedRadiationPressure(final CelestialBody inSun, final Frame inBodyFrame,
        final int inCorona, final int inMeridian, final IEmissivityModel inEmissivityModel,
        final RediffusedRadiationSensitive inModel, final boolean computePD)
        throws PatriusException {
        super();
        this.addJacobiansParameter(inModel.getJacobianParameters());
        this.enrichParameterDescriptors();
        this.sun = inSun;
        this.bodyFrame = inBodyFrame;
        this.nCorona = inCorona;
        this.nMeridian = inMeridian;
        this.emissivityModel = inEmissivityModel;
        this.radiativeModel = inModel;
        this.computingAcc = 0;
        this.computePartialDerivativesWrtPosition = computePD;
    }

    /**
     * Creates a new instance from the data in another one but with a different assembly.
     *
     * @param otherInstance the other instance
     * @param assembly the new assembly
     * @throws PatriusException if no radiative properties found
     */
    public RediffusedRadiationPressure(final RediffusedRadiationPressure otherInstance, final Assembly assembly)
        throws PatriusException {
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

        Vector3D acc = Vector3D.ZERO;
        this.radiativeModel.initDerivatives();

        // computing elementary rediffused flux in bodyFrame frame
        final RediffusedFlux redistributedFlux = new RediffusedFlux(this.nCorona, this.nMeridian, this.bodyFrame,
            this.sun, s.getOrbit(), s.getDate(), this.emissivityModel, this.radiativeModel.getFlagIr(),
            this.radiativeModel.getFlagAlbedo());
        final ElementaryFlux[] flux = redistributedFlux.getFlux();

        final Transform t = this.bodyFrame.getTransformTo(s.getFrame(), s.getDate());

        // flux iteration
        for (final ElementaryFlux element : flux) {
            // computing acceleration in SpacecraftState frame
            final ElementaryFlux transformedFlux = new ElementaryFlux(t.transformVector(element
                .getDirFlux()), element.getAlbedoPressure(), element.getInfraRedPressure());
            acc = acc.add(this.radiativeModel
                .rediffusedRadiationPressureAcceleration(s, transformedFlux));
        }
        this.computingAcc = s.hashCode();
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
                              final double[][] dAccdVel) throws PatriusException {

        if (this.computeGradientPosition()) {
            // Compute acceleration and derivatives value
            if (this.computingAcc != s.hashCode()) {
                this.computeAcceleration(s);
            }
            // Acceleration derivatives of the model
            final double[][] dAccdPosModel = new double[3][3];
            final double[][] dAccdVelModel = new double[3][3];
            this.radiativeModel
                .addDAccDStateRediffusedRadiativePressure(s, dAccdPosModel, dAccdVelModel);

            // jacobian with respect to position
            dAccdPos[0][0] += dAccdPosModel[0][0];
            dAccdPos[0][1] += dAccdPosModel[0][1];
            dAccdPos[0][2] += dAccdPosModel[0][2];
            dAccdPos[1][0] += dAccdPosModel[1][0];
            dAccdPos[1][1] += dAccdPosModel[1][1];
            dAccdPos[1][2] += dAccdPosModel[1][2];
            dAccdPos[2][0] += dAccdPosModel[2][0];
            dAccdPos[2][1] += dAccdPosModel[2][1];
            dAccdPos[2][2] += dAccdPosModel[2][2];
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
                              final double[] dAccdParam) throws PatriusException {
        if (!this.supportsJacobianParameter(param)) {
            throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param);
        }
        if (this.computingAcc != s.hashCode()) {
            this.computeAcceleration(s);
        }
        final double[] dAccdParamModel = new double[3];
        this.radiativeModel.addDAccDParamRediffusedRadiativePressure(s, param, dAccdParamModel);
        dAccdParam[0] += dAccdParamModel[0];
        dAccdParam[1] += dAccdParamModel[1];
        dAccdParam[2] += dAccdParamModel[2];
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
    public CelestialBody getInSun() {
        return this.sun;
    }

    /**
     * Getter for the boby frame used at construction.
     *
     * @return the boby frame.
     */
    public Frame getInBodyFrame() {
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
    public void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        // Nothing to do
    }
}

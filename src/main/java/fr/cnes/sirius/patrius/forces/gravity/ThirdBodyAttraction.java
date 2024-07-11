/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8.1:FA:FA-2947:07/12/2021:[PATRIUS] Problemes lies au coefficient multiplicatif des forces grav.
 * VERSION:4.8:FA:FA-2947:15/11/2021:[PATRIUS] Problemes lies au coefficient multiplicatif des forces gravitationnelles 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.7:DM:DM-2685:18/05/2021:Prise en compte d un modele de gravite complexe pour un troisieme corps
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:completed Javadoc concerning the partial derivatives parameters
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:767:24/04/2018: Creation of ForceModelsData to collect the models data for a force computation
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.JacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.ParameterUtils;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Third body attraction force model.
 * 
 * <p>
 * The implementation of this class enables the computation of partial derivatives by finite differences with respect to
 * the <b>central attraction coefficient</b>.
 * </p>
 * 
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */
@SuppressWarnings("PMD.NullAssignment")
// Reason: code clarity and simplicity
public class ThirdBodyAttraction extends JacobiansParameterizable implements ForceModel, GradientModel {

    /** Serializable UID. */
    private static final long serialVersionUID = -1703641239448217284L;

    /** -1.5 */
    private static final double C_N1DOT5 = -1.5;

    /** -2 */
    private static final int C_N2 = -2;

    /** String. */
    private static final String ATTRACTION_COEF = " attraction coefficient";
    
    /** The body to consider. */
    private final CelestialBody body;

    /** Central attraction coefficient parameter. */
    private Parameter paramMu = null;

    /** True if acceleration partial derivatives with respect to position have to be computed. */
    private final boolean computePartialDerivativesWrtPosition;

    /** Multiplicative coefficient. */
    private double k;

    /**
     * Simple constructor.
     * Partial derivative computation is set to true by default.
     * Only central term of third body is considered. 
     * 
     * @param bodyIn
     *        the third body to consider (ex: {@link fr.cnes.sirius.patrius.bodies.CelestialBodyFactory#getSun()} or
     *        {@link fr.cnes.sirius.patrius.bodies.CelestialBodyFactory#getMoon()})
     */
    public ThirdBodyAttraction(final CelestialBody bodyIn) {
        this(bodyIn, true);
    }

    /**
     * <p>
     * Simple constructor. Only central term of third body is considered.
     * </p>
     * 
     * @param bodyIn
     *        the third body to consider (ex: {@link fr.cnes.sirius.patrius.bodies.CelestialBodyFactory#getSun()} or
     *        {@link fr.cnes.sirius.patrius.bodies.CelestialBodyFactory#getMoon()})
     * @param computePD
     *        true if partial derivatives have to be computed
     */
    public ThirdBodyAttraction(final CelestialBody bodyIn, final boolean computePD) {
        super();
        this.paramMu = new Parameter(bodyIn.getName() + ATTRACTION_COEF, bodyIn.getGM());
        this.addParameter(this.paramMu);
        ParameterUtils.addFieldToParameters(getParameters(), StandardFieldDescriptors.FORCE_MODEL, this.getClass());
        this.body = bodyIn;
        this.computePartialDerivativesWrtPosition = computePD;
        this.k = 1.;
    }

    /** {@inheritDoc} */
    @Override
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder) throws PatriusException {
        // compute relative acceleration
        final Vector3D gamma = this.computeAcceleration(s);

        // add contribution to the ODE second member
        adder.addXYZAcceleration(gamma.getX(), gamma.getY(), gamma.getZ());
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
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

    /** {@inheritDoc}. */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {
        // Central term
        Vector3D gamma = this.computeCentralAcceleration(s.getPVCoordinates(), s.getFrame(), s.getDate());

        // Harmonics if defined
        if (this.body.getAttractionModel() != null
                && !(this.body.getAttractionModel() instanceof NewtonianAttractionModel)) {
            final SpacecraftState frameCenter = new SpacecraftState(new CartesianOrbit(
                PVCoordinates.ZERO, s.getFrame(), s.getDate(), s.getMu()));

            // Direct effect
            final Vector3D gamma1 = this.body.getAttractionModel().computeAcceleration(s);
            // Effect on frame center (= position of central body) that needs to be subtracted from the acceleration
            final Vector3D gamma2 = this.body.getAttractionModel().computeAcceleration(frameCenter);

            gamma = gamma.add(gamma1);
            gamma = gamma.subtract(gamma2);

        }
        
        return gamma;
    }

    /**
     * <p>
     * Method to compute the acceleration. This method has been implemented in order to validate the force model only.
     * The reason is that for the validation context, we do not want to set up an instance of the SpacecraftState object
     * to avoid the inertial frame of the spacecraft orbit.
     * </p>
     * <p>
     * (see Story #V85 and Feature #34 on https://www.orekit.org/forge/issues/34)
     * </p>
     * <p>
     * Out of the validation context, one must use the method Vector3D computeAcceleration(final SpacecraftState s)
     * </p>
     * 
     * @param pv
     *        PV coordinates of the spacecraft
     * @param frame
     *        frame in which the acceleration is computed
     * @param date
     *        date
     * @throws PatriusException
     *         if an Orekit error occurs
     * @return acceleration vector
     */
    public Vector3D computeCentralAcceleration(final PVCoordinates pv, final Frame frame,
                                        final AbsoluteDate date) throws PatriusException {
        // compute bodies separation vectors and squared norm
        final Vector3D centralToBody = this.body.getPVCoordinates(date, frame).getPosition();
        final double r2Central = Vector3D.dotProduct(centralToBody, centralToBody);
        final Vector3D satToBody = centralToBody.subtract(pv.getPosition());
        final double r2Sat = Vector3D.dotProduct(satToBody, satToBody);

        // compute relative acceleration
        return new Vector3D(this.paramMu.getValue() * this.k * MathLib.pow(r2Sat, C_N1DOT5),
            satToBody, -this.paramMu.getValue() * this.k * MathLib.pow(r2Central, C_N1DOT5), centralToBody);
    }

    /** {@inheritDoc} */
    @Override
    public final void addDAccDState(final SpacecraftState s, final double[][] dAccdPos,
                                    final double[][] dAccdVel) throws PatriusException {

        if (this.computeGradientPosition()) {
            // Derivative wrt position
            // No derivative wrt velocity
            final double[][] dAdP = this.computeCentralDAccDPos(s.getPVCoordinates(), s.getFrame(), s.getDate());

            // the only non-null contribution for this force is dAcc/dPos
            dAccdPos[0][0] += this.k * dAdP[0][0];
            dAccdPos[0][1] += this.k * dAdP[0][1];
            dAccdPos[0][2] += this.k * dAdP[0][2];
            dAccdPos[1][0] += this.k * dAdP[1][0];
            dAccdPos[1][1] += this.k * dAdP[1][1];
            dAccdPos[1][2] += this.k * dAdP[1][2];
            dAccdPos[2][0] += this.k * dAdP[2][0];
            dAccdPos[2][1] += this.k * dAdP[2][1];
            dAccdPos[2][2] += this.k * dAdP[2][2];

            // Harmonics are defined
            if (this.body.getAttractionModel() != null
                    && !(this.body.getAttractionModel() instanceof NewtonianAttractionModel)) {
                this.body.getAttractionModel().addDAccDState(s, dAccdPos, dAccdVel);

//                // Subtract impact on frame center
//                final SpacecraftState frameCenter = new SpacecraftState(new CartesianOrbit(
//                    PVCoordinates.ZERO, s.getFrame(), s.getDate(), s.getMu()));
//                final double[][] dAccdPosFS = new double[3][3];
//                final double[][] dAccdVelFS = new double[3][3];
//                this.body.getAttractionModel().addDAccDState(frameCenter, dAccdPosFS, dAccdVelFS);
//                for (int i = 0; i < 3; i++) {
//                    for (int j = 0; j < 3; j++) {
//                        dAccdPos[i][j] -= dAccdPosFS[i][j];
//                    }
//                }
                
            }
        }
    }

    /**
     * <p>
     * Method to compute acceleration derivatives with respect to spacecraft position. This method has been implemented
     * for the validation only.
     * </p>
     * <p>
     * 
     * @param pv
     *        PV coordinates of the spacecraft.
     * @param frame
     *        frame in which the PV coordinates are expressed.
     * @param date
     *        date.
     * @return dAdP
     *         acceleration derivatives with respect to position.
     * @throws PatriusException
     *         if an Orekit error occurs.
     *         </p>
     */
    private double[][] computeCentralDAccDPos(final PVCoordinates pv, final Frame frame,
                                       final AbsoluteDate date) throws PatriusException {

        // position of the spacecraft
        final Vector3D sat = pv.getPosition();
        // compute satToBody vector
        final Vector3D centralToBody = this.body.getPVCoordinates(date,
            frame).getPosition();
        final Vector3D satToBody = centralToBody.subtract(sat);
        final double r2Sat = Vector3D.dotProduct(satToBody, satToBody);

        // intermediate variables
        final double x2 = sat.getX() * sat.getX();
        final double y2 = sat.getY() * sat.getY();
        final double z2 = sat.getZ() * sat.getZ();
        final double u2 = centralToBody.getX() * centralToBody.getX();
        final double v2 = centralToBody.getY() * centralToBody.getY();
        final double w2 = centralToBody.getZ() * centralToBody.getZ();
        final double ux = centralToBody.getX() * sat.getX();
        final double vy = centralToBody.getY() * sat.getY();
        final double wz = centralToBody.getZ() * sat.getZ();
        final double satToBodyXY = satToBody.getX() * satToBody.getY();
        final double satToBodyXZ = satToBody.getX() * satToBody.getZ();
        final double satToBodyYZ = satToBody.getY() * satToBody.getZ();
        final double prefix = this.paramMu.getValue() * MathLib.pow(r2Sat, -2.5);

        // acceleration derivatives
        final double[][] dAdP = new double[3][3];

        // the only non-null contribution for this force is dAcc/dPos
        dAdP[0][0] = -prefix * (C_N2 * u2 + 4 * ux + v2 - 2 * vy + w2 - 2 * wz - 2 * x2 + y2 + z2);
        dAdP[0][1] = 3 * prefix * satToBodyXY;
        dAdP[0][2] = 3 * prefix * satToBodyXZ;
        dAdP[1][0] = 3 * prefix * satToBodyXY;
        dAdP[1][1] = -prefix * (u2 - 2 * ux - 2 * v2 + 4 * vy + w2 - 2 * wz + x2 - 2 * y2 + z2);
        dAdP[1][2] = 3 * prefix * satToBodyYZ;
        dAdP[2][0] = 3 * prefix * satToBodyXZ;
        dAdP[2][1] = 3 * prefix * satToBodyYZ;
        dAdP[2][2] = -prefix * (u2 - 2 * ux + v2 - 2 * vy - 2 * w2 + 4 * wz + x2 + y2 - 2 * z2);

        // Return result
        //
        return dAdP;
    }

    /**
     * {@inheritDoc} <br>
     * No parameter is supported by this force model.
     */
    @Override
    public final void addDAccDParam(final SpacecraftState s, final Parameter param,
                                    final double[] dAccdParam) throws PatriusException {
        throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param);
    }

    /**
     * Return the celestial body.
     * 
     * @return the celestial body
     */
    public CelestialBody getCelestialBody() {
        return this.body;
    }
    
    /** {@inheritDoc} */
    @Override
    public void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        // Harmonics if defined
        if (this.body.getAttractionModel() != null) {
            this.body.getAttractionModel().checkData(start, end);
        }
    }

    /**
     * Get the force multiplicative factor.
     * 
     * @return the force multiplicative factor
     */
    public double getMultiplicativeFactor() {
        return this.body.getAttractionModel().getMultiplicativeFactor();
    }

    /**
     * Set the multiplicative factor.
     * 
     * @param factor the factor to set.
     */
    public void setMultiplicativeFactor(final double factor) {
        this.k = factor;
        this.body.getAttractionModel().setMultiplicativeFactor(factor);
    }
}

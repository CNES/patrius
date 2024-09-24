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
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2795:18/05/2021:Evolution du package orbits 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.7:DM:DM-2590:18/05/2021:Configuration des TransformProvider 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:FA:FA-2468:27/05/2020:Robustesse methode shiftedBy de la classe Orbit
 * VERSION:4.5:FA:FA-2464:27/05/2020:Anomalie dans le calcul du vecteur rotation des LOF
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.4:FA:FA-2134:04/10/2019:Modifications mineures d'api - corrections
 * VERSION:4.4:DM:DM-2135:04/10/2019:[PATRIUS] Methodes equals() et hashCode() non implementees dans la classe TLE
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2082:15/05/2019:Modifications mineures d'api
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS/COLOSUS] Mise en conformite code avec nouveau standard codage DYNVOL
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:426:30/10/2015: Make orbit instantiation in non inertial frame possible
 * VERSION::DM:481:05/10/2015: new method to compute the jacobian related to the conversion between 2 orbital types
 * VERSION::DM:482:02/11/2015:Covariance Matrix Propagation
 * VERSION::DM:489:17/12/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:559:26/02/2016:minor corrections
 * VERSION::FA:1281:30/08/2017: Javadoc correction
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.DecompositionSolver;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.QRDecomposition;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.IOrbitalParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeInterpolable;
import fr.cnes.sirius.patrius.time.TimeShiftable;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

//CHECKSTYLE: stop ModifiedControlVariable check
//Reason: Orekit code kept as such

/**
 * This class handles orbital parameters.
 * 
 * <p>
 * For user convenience, both the Cartesian and the equinoctial elements are provided by this class, regardless of the
 * canonical representation implemented in the derived class (which may be classical keplerian elements for example).
 * </p>
 * <p>
 * The parameters are defined in a frame specified by the user. It is important to make sure this frame is consistent:
 * it probably is inertial and centered on the central body. This information is used for example by some force models.
 * </p>
 * <p>
 * The object <code>OrbitalParameters</code> is guaranteed to be immutable.
 * </p>
 * 
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings({"PMD.AbstractNaming", "PMD.NullAssignment"})
public abstract class Orbit
    implements TimeStamped, TimeShiftable<Orbit>, TimeInterpolable<Orbit>,
    PVCoordinatesProvider {
    // CHECKSTYLE: resume AbstractClassName check

    /** Orbit types for which the Keplerian transition matrix is applicable. */
    private static final List<OrbitType> KEPLERIAN_TRANSITION_TYPES;

    static {
        final List<OrbitType> valids = new ArrayList<>();
        valids.add(OrbitType.CIRCULAR);
        valids.add(OrbitType.KEPLERIAN);
        valids.add(OrbitType.EQUATORIAL);
        valids.add(OrbitType.EQUINOCTIAL);
        KEPLERIAN_TRANSITION_TYPES = Collections.unmodifiableList(valids);
    }

    /** Serializable UID. */
    private static final long serialVersionUID = 438733454597999578L;

    /** Size of the transition matrix. */
    private static final int STATE_DIM = 6;

    /** Frame in which are defined the orbital parameters. */
    private final Frame frame;

    /** Date of the orbital parameters. */
    private final AbsoluteDate date;

    /** Value of mu used to compute position and velocity (m<sup>3</sup>/s<sup>2</sup>). */
    private final double mu;

    /** Computed PVCoordinates. */
    private PVCoordinates pvCoordinates;

    /** Jacobian of the orbital parameters with mean angle with respect to the Cartesian coordinates. */
    private transient double[][] jacobianMeanWrtCartesian;

    /** Jacobian of the Cartesian coordinates with respect to the orbital parameters with mean angle. */
    private transient double[][] jacobianWrtParametersMean;

    /** Jacobian of the orbital parameters with eccentric angle with respect to the Cartesian coordinates. */
    private transient double[][] jacobianEccentricWrtCartesian;

    /** Jacobian of the Cartesian coordinates with respect to the orbital parameters with eccentric angle. */
    private transient double[][] jacobianWrtParametersEccentric;

    /** Jacobian of the orbital parameters with true angle with respect to the Cartesian coordinates. */
    private transient double[][] jacobianTrueWrtCartesian;

    /** Jacobian of the Cartesian coordinates with respect to the orbital parameters with true angle. */
    private transient double[][] jacobianWrtParametersTrue;

    /**
     * Default constructor.
     * Build a new instance with arbitrary default elements.
     * 
     * @param frameIn
     *        the frame in which the parameters are defined
     *        (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param dateIn
     *        date of the orbital parameters
     * @param muIn
     *        central attraction coefficient (m^3/s^2)
     */
    protected Orbit(final Frame frameIn, final AbsoluteDate dateIn, final double muIn) {

        this.date = dateIn;
        this.mu = muIn;
        this.pvCoordinates = null;
        this.frame = frameIn;
        this.jacobianMeanWrtCartesian = null;
        this.jacobianWrtParametersMean = null;
        this.jacobianEccentricWrtCartesian = null;
        this.jacobianWrtParametersEccentric = null;
        this.jacobianTrueWrtCartesian = null;
        this.jacobianWrtParametersTrue = null;
    }

    /**
     * Set the orbit from Cartesian parameters.
     * 
     * @param pvCoordinatesIn
     *        the position and velocity in the inertial frame
     * @param frameIn
     *        the frame in which the {@link PVCoordinates} are defined
     * @param dateIn
     *        date of the orbital parameters
     * @param muIn
     *        central attraction coefficient (m^3/s^2)
     */
    protected Orbit(final PVCoordinates pvCoordinatesIn, final Frame frameIn,
        final AbsoluteDate dateIn, final double muIn) {

        this.date = dateIn;
        this.mu = muIn;
        if (pvCoordinatesIn.getAcceleration() == null || pvCoordinatesIn.getAcceleration().getNormSq() == 0) {
            // the acceleration was not provided,
            // compute it from Newtonian attraction
            final double r2 = pvCoordinatesIn.getPosition().getNormSq();
            final double r3 = r2 * MathLib.sqrt(r2);
            this.pvCoordinates = new PVCoordinates(pvCoordinatesIn.getPosition(),
                pvCoordinatesIn.getVelocity(),
                new Vector3D(-muIn / r3, pvCoordinatesIn.getPosition()));
        } else {
            this.pvCoordinates = pvCoordinatesIn;
        }
        this.frame = frameIn;
    }

    /**
     * Get underlying orbital parameters.
     * 
     * @return orbital parameters
     */
    public abstract IOrbitalParameters getParameters();

    /**
     * Get the orbit type.
     * 
     * @return orbit type
     */
    public abstract OrbitType getType();

    /**
     * Ensure the defining frame is a pseudo-inertial frame.
     * 
     * @param frame
     *        frame to check
     * @exception IllegalArgumentException
     *            if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     */
    protected static void ensurePseudoInertialFrame(final Frame frame) {
        if (!frame.isPseudoInertial()) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.NON_PSEUDO_INERTIAL_FRAME_NOT_SUITABLE_FOR_DEFINING_ORBITS,
                frame.getName());
        }
    }

    /**
     * Get the frame in which the orbital parameters are defined.
     * 
     * @return frame in which the orbital parameters are defined
     */
    public Frame getFrame() {
        return this.frame;
    }

    /**
     * Get the semi-major axis.
     * <p>
     * Note that the semi-major axis is considered negative for hyperbolic orbits.
     * </p>
     * 
     * @return semi-major axis (m)
     */
    public abstract double getA();

    /**
     * Get the mean motion.
     *
     * @return mean motion (1/s)
     */
    public abstract double getN();

    /**
     * Get the first component of the equinoctial eccentricity vector.
     * 
     * @return first component of the equinoctial eccentricity vector
     */
    public abstract double getEquinoctialEx();

    /**
     * Get the second component of the equinoctial eccentricity vector.
     * 
     * @return second component of the equinoctial eccentricity vector
     */
    public abstract double getEquinoctialEy();

    /**
     * Get hx = ix / (2 * cos(i/2)), where ix is the first component of the inclination vector.
     * Another formulation is hx = tan(i/2) cos(&Omega;)
     * 
     * @return first component of the inclination vector
     */
    public abstract double getHx();

    /**
     * Get hy = iy / (2 * cos(i/2)), where iy is the second component of the inclination vector.
     * Another formulation is hy = tan(i/2) sin(&Omega;)
     * 
     * @return second component of the inclination vector
     */
    public abstract double getHy();

    /**
     * Get the eccentric longitude argument.
     * 
     * @return eccentric longitude argument (rad)
     */
    public abstract double getLE();

    /**
     * Get the true longitude argument.
     * 
     * @return true longitude argument (rad)
     */
    public abstract double getLv();

    /**
     * Get the mean longitude argument.
     * 
     * @return mean longitude argument (rad)
     */
    public abstract double getLM();

    // Additional orbital elements

    /**
     * Get the eccentricity.
     * 
     * @return eccentricity
     */
    public abstract double getE();

    /**
     * Get the inclination.
     * 
     * @return inclination (rad)
     */
    public abstract double getI();

    /**
     * Get the central acceleration constant.
     * 
     * @return central acceleration constant
     */
    public double getMu() {
        return this.mu;
    }

    /**
     * Get the keplerian period.
     * <p>
     * The keplerian period is computed directly from semi major axis and central acceleration constant.
     * </p>
     * 
     * @return keplerian period in seconds, or positive infinity for hyperbolic orbits
     */
    public double getKeplerianPeriod() {
        final double a = this.getA();
        return (a < 0) ? Double.POSITIVE_INFINITY : 2.0 * FastMath.PI * a * MathLib.sqrt(a / this.mu);
    }

    /**
     * Get the keplerian mean motion.
     * <p>
     * The keplerian mean motion is computed directly from semi major axis and central acceleration constant.
     * </p>
     * 
     * @return keplerian mean motion in radians per second
     */
    public double getKeplerianMeanMotion() {
        final double absA = MathLib.abs(this.getA());
        return MathLib.sqrt(this.mu / absA) / absA;
    }

    /**
     * Get the date of orbital parameters.
     * 
     * @return date of the orbital parameters
     */
    @Override
    public AbsoluteDate getDate() {
        return this.date;
    }

    /**
     * Get the {@link PVCoordinates} in a specified frame.
     * 
     * @param outputFrame
     *        frame in which the position/velocity coordinates shall be computed
     * @return pvCoordinates in the specified output frame
     * @exception PatriusException
     *            if transformation between frames cannot be computed
     * @see #getPVCoordinates()
     */
    public PVCoordinates getPVCoordinates(final Frame outputFrame) throws PatriusException {
        if (this.pvCoordinates == null) {
            this.pvCoordinates = this.initPVCoordinates();
        }

        // If output frame requested is the same as definition frame,
        // PV coordinates are returned directly
        if (outputFrame == this.frame) {
            return this.pvCoordinates;
        }

        // Else, PV coordinates are transformed to output frame
        final Transform t = this.frame.getTransformTo(outputFrame, this.date);
        return t.transformPVCoordinates(this.pvCoordinates);
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate otherDate,
                                          final Frame otherFrame) throws PatriusException {
        return this.shiftedBy(otherDate.durationFrom(this.getDate())).getPVCoordinates(otherFrame);
    }

    /**
     * Get the {@link PVCoordinates} in definition frame.
     * 
     * @return pvCoordinates in the definition frame
     * @see #getPVCoordinates(Frame)
     */
    public PVCoordinates getPVCoordinates() {
        if (this.pvCoordinates == null) {
            this.pvCoordinates = this.initPVCoordinates();
        }
        return this.pvCoordinates;
    }

    /**
     * Compute the position/velocity coordinates from the canonical parameters.
     * 
     * @return computed position/velocity coordinates
     */
    protected abstract PVCoordinates initPVCoordinates();

    /**
     * Get a time-shifted orbit.
     * <p>
     * The orbit can be slightly shifted to close dates. This shift is based on a simple keplerian model. It is
     * <em>not</em> intended as a replacement for proper orbit and attitude propagation but should be sufficient for
     * small time shifts or coarse accuracy.
     * </p>
     * 
     * @param dt
     *        time shift in seconds
     * @return a new orbit, shifted with respect to the instance (which is immutable)
     */
    protected abstract Orbit orbitShiftedBy(final double dt);

    /**
     * Call the method {@link #orbitShiftedBy(double)} implemented in inherited classes of Orbit.
     * 
     * @param dt
     *        time shift in seconds
     * @return a new orbit, shifted with respect to the instance (which is immutable)
     */
    @Override
    public Orbit shiftedBy(final double dt) {

        final Orbit shiftedOrbit;
        if (dt == 0.) { 
            shiftedOrbit = this; 
        } else {
            ensurePseudoInertialFrame(this.frame);
            shiftedOrbit = this.orbitShiftedBy(dt); 
        }
        return shiftedOrbit;
    }

    /**
     * Compute the Jacobian of the orbital parameters with respect to the Cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with respect to Cartesian coordinate
     * j. This means each row correspond to one orbital parameter whereas columns 0 to 5 correspond to the Cartesian
     * coordinates x, y, z, xDot, yDot and zDot.
     * </p>
     * 
     * @param type
     *        type of the position angle to use
     * @param jacobian
     *        placeholder 6x6 (or larger) matrix to be filled with the Jacobian, if matrix
     *        is larger than 6x6, only the 6x6 upper left corner will be modified
     */
    public void getJacobianWrtCartesian(final PositionAngle type, final double[][] jacobian) {

        // array to cache the Jacobian
        final double[][] cachedJacobian;
        synchronized (this) {
            switch (type) {
                case MEAN:
                    if (this.jacobianMeanWrtCartesian == null) {
                        // first call, we need to compute the jacobian and cache it
                        this.jacobianMeanWrtCartesian = this.computeJacobianMeanWrtCartesian();
                    }
                    cachedJacobian = this.jacobianMeanWrtCartesian;
                    break;
                case ECCENTRIC:
                    if (this.jacobianEccentricWrtCartesian == null) {
                        // first call, we need to compute the jacobian and cache it
                        this.jacobianEccentricWrtCartesian = this.computeJacobianEccentricWrtCartesian();
                    }
                    cachedJacobian = this.jacobianEccentricWrtCartesian;
                    break;
                case TRUE:
                    if (this.jacobianTrueWrtCartesian == null) {
                        // first call, we need to compute the jacobian and cache it
                        this.jacobianTrueWrtCartesian = this.computeJacobianTrueWrtCartesian();
                    }
                    cachedJacobian = this.jacobianTrueWrtCartesian;
                    break;
                default:
                    // Exception unkown type
                    throw PatriusException.createInternalError(null);
            }
        }

        // fill the user provided array
        for (int i = 0; i < cachedJacobian.length; ++i) {
            System.arraycopy(cachedJacobian[i], 0, jacobian[i], 0, cachedJacobian[i].length);
        }

    }

    /**
     * Compute the Jacobian of the Cartesian parameters with respect to the orbital parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with respect to Cartesian coordinate
     * j. This means each row correspond to one orbital parameter whereas columns 0 to 5 correspond to the Cartesian
     * coordinates x, y, z, xDot, yDot and zDot.
     * </p>
     * 
     * @param type
     *        type of the position angle to use
     * @param jacobian
     *        placeholder 6x6 (or larger) matrix to be filled with the Jacobian, if matrix
     *        is larger than 6x6, only the 6x6 upper left corner will be modified
     */
    public void getJacobianWrtParameters(final PositionAngle type, final double[][] jacobian) {

        // array to cache the Jacobian
        final double[][] cachedJacobian;
        synchronized (this) {
            switch (type) {
                case MEAN:
                    if (this.jacobianWrtParametersMean == null) {
                        // first call, we need to compute the jacobian and cache it
                        this.jacobianWrtParametersMean = this.createInverseJacobian(type);
                    }
                    cachedJacobian = this.jacobianWrtParametersMean;
                    break;
                case ECCENTRIC:
                    if (this.jacobianWrtParametersEccentric == null) {
                        // first call, we need to compute the jacobian and cache it
                        this.jacobianWrtParametersEccentric = this.createInverseJacobian(type);
                    }
                    cachedJacobian = this.jacobianWrtParametersEccentric;
                    break;
                case TRUE:
                    if (this.jacobianWrtParametersTrue == null) {
                        // first call, we need to compute the jacobian and cache it
                        this.jacobianWrtParametersTrue = this.createInverseJacobian(type);
                    }
                    cachedJacobian = this.jacobianWrtParametersTrue;
                    break;
                default:
                    // Exception unknown type
                    throw PatriusException.createInternalError(null);
            }
        }

        // fill the user-provided array
        for (int i = 0; i < cachedJacobian.length; ++i) {
            System.arraycopy(cachedJacobian[i], 0, jacobian[i], 0, cachedJacobian[i].length);
        }

    }

    /**
     * Create an inverse Jacobian.
     * 
     * @param type
     *        type of the position angle to use
     * @return inverse Jacobian
     */
    protected double[][] createInverseJacobian(final PositionAngle type) {

        // get the direct Jacobian
        final double[][] directJacobian = new double[6][6];
        this.getJacobianWrtCartesian(type, directJacobian);

        // invert the direct Jacobian
        final RealMatrix matrix = MatrixUtils.createRealMatrix(directJacobian, false);
        final DecompositionSolver solver = new QRDecomposition(matrix).getSolver();
        return solver.getInverse().getData(false);

    }

    /**
     * Compute the Jacobian of the orbital parameters with mean angle with respect to the Cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with respect to Cartesian coordinate
     * j. This means each row correspond to one orbital parameter whereas columns 0 to 5 correspond to the Cartesian
     * coordinates x, y, z, xDot, yDot and zDot.
     * </p>
     * 
     * @return 6x6 Jacobian matrix
     * @see #computeJacobianEccentricWrtCartesian()
     * @see #computeJacobianTrueWrtCartesian()
     */
    protected abstract double[][] computeJacobianMeanWrtCartesian();

    /**
     * Compute the Jacobian of the orbital parameters with eccentric angle with respect to the Cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with respect to Cartesian coordinate
     * j. This means each row correspond to one orbital parameter whereas columns 0 to 5 correspond to the Cartesian
     * coordinates x, y, z, xDot, yDot and zDot.
     * </p>
     * 
     * @return 6x6 Jacobian matrix
     * @see #computeJacobianMeanWrtCartesian()
     * @see #computeJacobianTrueWrtCartesian()
     */
    protected abstract double[][] computeJacobianEccentricWrtCartesian();

    /**
     * Compute the Jacobian of the orbital parameters with true angle with respect to the Cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with respect to Cartesian coordinate
     * j. This means each row correspond to one orbital parameter whereas columns 0 to 5 correspond to the Cartesian
     * coordinates x, y, z, xDot, yDot and zDot.
     * </p>
     * 
     * @return 6x6 Jacobian matrix
     * @see #computeJacobianMeanWrtCartesian()
     * @see #computeJacobianEccentricWrtCartesian()
     */
    protected abstract double[][] computeJacobianTrueWrtCartesian();

    /**
     * Fill a Jacobian half row with a single vector.
     * 
     * @param a
     *        coefficient of the vector
     * @param v
     *        vector
     * @param row
     *        Jacobian matrix row
     * @param j
     *        index of the first element to set (row[j], row[j+1] and row[j+2] will all be set)
     */
    protected static void fillHalfRow(final double a, final Vector3D v, final double[] row, final int j) {
        row[j] = a * v.getX();
        row[j + 1] = a * v.getY();
        row[j + 2] = a * v.getZ();
    }

    /**
     * Fill a Jacobian half row with a linear combination of vectors.
     * 
     * @param a1
     *        coefficient of the first vector
     * @param v1
     *        first vector
     * @param a2
     *        coefficient of the second vector
     * @param v2
     *        second vector
     * @param row
     *        Jacobian matrix row
     * @param j
     *        index of the first element to set (row[j], row[j+1] and row[j+2] will all be set)
     */
    protected static void fillHalfRow(final double a1, final Vector3D v1, final double a2, final Vector3D v2,
                                      final double[] row, final int j) {
        row[j] = a1 * v1.getX() + a2 * v2.getX();
        row[j + 1] = a1 * v1.getY() + a2 * v2.getY();
        row[j + 2] = a1 * v1.getZ() + a2 * v2.getZ();
    }

    /**
     * Fill a Jacobian half row with a linear combination of vectors.
     * 
     * @param a1
     *        coefficient of the first vector
     * @param v1
     *        first vector
     * @param a2
     *        coefficient of the second vector
     * @param v2
     *        second vector
     * @param a3
     *        coefficient of the third vector
     * @param v3
     *        third vector
     * @param row
     *        Jacobian matrix row
     * @param j
     *        index of the first element to set (row[j], row[j+1] and row[j+2] will all be set)
     */
    protected static void fillHalfRow(final double a1, final Vector3D v1, final double a2, final Vector3D v2,
                                      final double a3, final Vector3D v3,
                                      final double[] row, final int j) {
        row[j] = a1 * v1.getX() + a2 * v2.getX() + a3 * v3.getX();
        row[j + 1] = a1 * v1.getY() + a2 * v2.getY() + a3 * v3.getY();
        row[j + 2] = a1 * v1.getZ() + a2 * v2.getZ() + a3 * v3.getZ();
    }

    /**
     * Fill a Jacobian half row with a linear combination of vectors.
     * 
     * @param a1
     *        coefficient of the first vector
     * @param v1
     *        first vector
     * @param a2
     *        coefficient of the second vector
     * @param v2
     *        second vector
     * @param a3
     *        coefficient of the third vector
     * @param v3
     *        third vector
     * @param a4
     *        coefficient of the fourth vector
     * @param v4
     *        fourth vector
     * @param row
     *        Jacobian matrix row
     * @param j
     *        index of the first element to set (row[j], row[j+1] and row[j+2] will all be set)
     */
    protected static void fillHalfRow(final double a1, final Vector3D v1, final double a2, final Vector3D v2,
                                      final double a3, final Vector3D v3, final double a4, final Vector3D v4,
                                      final double[] row, final int j) {
        row[j] = a1 * v1.getX() + a2 * v2.getX() + a3 * v3.getX() + a4 * v4.getX();
        row[j + 1] = a1 * v1.getY() + a2 * v2.getY() + a3 * v3.getY() + a4 * v4.getY();
        row[j + 2] = a1 * v1.getZ() + a2 * v2.getZ() + a3 * v3.getZ() + a4 * v4.getZ();
    }

    /**
     * Fill a Jacobian half row with a linear combination of vectors.
     * 
     * @param a1
     *        coefficient of the first vector
     * @param v1
     *        first vector
     * @param a2
     *        coefficient of the second vector
     * @param v2
     *        second vector
     * @param a3
     *        coefficient of the third vector
     * @param v3
     *        third vector
     * @param a4
     *        coefficient of the fourth vector
     * @param v4
     *        fourth vector
     * @param a5
     *        coefficient of the fifth vector
     * @param v5
     *        fifth vector
     * @param row
     *        Jacobian matrix row
     * @param j
     *        index of the first element to set (row[j], row[j+1] and row[j+2] will all be set)
     */
    protected static void fillHalfRow(final double a1, final Vector3D v1, final double a2, final Vector3D v2,
                                      final double a3, final Vector3D v3, final double a4, final Vector3D v4,
                                      final double a5, final Vector3D v5,
                                      final double[] row, final int j) {
        row[j] = a1 * v1.getX() + a2 * v2.getX() + a3 * v3.getX() + a4 * v4.getX() + a5 * v5.getX();
        row[j + 1] = a1 * v1.getY() + a2 * v2.getY() + a3 * v3.getY() + a4 * v4.getY() + a5 * v5.getY();
        row[j + 2] = a1 * v1.getZ() + a2 * v2.getZ() + a3 * v3.getZ() + a4 * v4.getZ() + a5 * v5.getZ();
    }

    /**
     * Fill a Jacobian half row with a linear combination of vectors.
     * 
     * @param a1
     *        coefficient of the first vector
     * @param v1
     *        first vector
     * @param a2
     *        coefficient of the second vector
     * @param v2
     *        second vector
     * @param a3
     *        coefficient of the third vector
     * @param v3
     *        third vector
     * @param a4
     *        coefficient of the fourth vector
     * @param v4
     *        fourth vector
     * @param a5
     *        coefficient of the fifth vector
     * @param v5
     *        fifth vector
     * @param a6
     *        coefficient of the sixth vector
     * @param v6
     *        sixth vector
     * @param row
     *        Jacobian matrix row
     * @param j
     *        index of the first element to set (row[j], row[j+1] and row[j+2] will all be set)
     */
    protected static void fillHalfRow(final double a1, final Vector3D v1, final double a2, final Vector3D v2,
                                      final double a3, final Vector3D v3, final double a4, final Vector3D v4,
                                      final double a5, final Vector3D v5, final double a6, final Vector3D v6,
                                      final double[] row, final int j) {
        row[j] = a1 * v1.getX() + a2 * v2.getX() + a3 * v3.getX() + a4 * v4.getX() + a5 * v5.getX() + a6 * v6.getX();
        row[j + 1] = a1 * v1.getY() + a2 * v2.getY() + a3 * v3.getY() + a4 * v4.getY() + a5 * v5.getY() + a6
            * v6.getY();
        row[j + 2] = a1 * v1.getZ() + a2 * v2.getZ() + a3 * v3.getZ() + a4 * v4.getZ() + a5 * v5.getZ() + a6
            * v6.getZ();
    }

    /**
     * @param jacobianWrtParametersMeanIn
     *        the jacobianWrtParametersMean to set
     */
    protected void setJacobianWrtParametersMean(final double[][] jacobianWrtParametersMeanIn) {
        if (this.jacobianWrtParametersMean == null) {
            this.jacobianWrtParametersMean = jacobianWrtParametersMeanIn.clone();
        }
    }

    /**
     * @return the jacobianWrtParametersMean
     */
    protected double[][] getJacobianWrtParametersMean() {
        if (this.jacobianWrtParametersMean == null) {
            return null;
        }
        return this.jacobianWrtParametersMean.clone();
    }

    /**
     * @param jacobianWrtParametersEccentricIn
     *        the jacobianWrtParametersEccentric to set
     */
    protected void setJacobianWrtParametersEccentric(final double[][] jacobianWrtParametersEccentricIn) {
        if (this.jacobianWrtParametersEccentric == null) {
            this.jacobianWrtParametersEccentric = jacobianWrtParametersEccentricIn.clone();
        }
    }

    /**
     * @return the jacobianWrtParametersEccentric
     */
    protected double[][] getJacobianWrtParametersEccentric() {
        if (this.jacobianWrtParametersEccentric == null) {
            return null;
        }
        return this.jacobianWrtParametersEccentric.clone();
    }

    /**
     * @param jacobianWrtParametersTrueIn
     *        the jacobianWrtParametersTrue to set
     */
    protected void setJacobianWrtParametersTrue(final double[][] jacobianWrtParametersTrueIn) {
        if (this.jacobianWrtParametersTrue == null) {
            this.jacobianWrtParametersTrue = jacobianWrtParametersTrueIn.clone();
        }
    }

    /**
     * @return the jacobianWrtParametersTrue
     */
    protected double[][] getJacobianWrtParametersTrue() {
        if (this.jacobianWrtParametersTrue == null) {
            return null;
        }
        return this.jacobianWrtParametersTrue.clone();
    }

    /**
     * Get coordinate conversion jacobian. The position is set to MEAN by default.
     * 
     * @param numerator Numerator parameters.
     * @param denominator Denominator parameters.
     * @return Jacobian matrix numerator / denominator.
     */
    public RealMatrix getJacobian(final OrbitType numerator, final OrbitType denominator) {
        return this.getJacobian(numerator, denominator, PositionAngle.MEAN);
    }

    /**
     * Get coordinate conversion jacobian.
     * 
     * @param numerator Numerator parameters.
     * @param denominator Denominator parameters.
     * @param positionAngle Position Angle.
     * @return Jacobian matrix numerator / denominator.
     */
    public RealMatrix getJacobian(final OrbitType numerator, final OrbitType denominator,
                                  final PositionAngle positionAngle) {
        try {
            return getJacobian(this.frame, this.frame, denominator, numerator, positionAngle, positionAngle);
        } catch (final PatriusException e) {
            // Cannot happen since no conversion is performed
            throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, e);
        }
    }

    /**
     * Get keplerian transition matrix.
     * 
     * <p>
     * The transition matrix for those different orbit types are equal : keplerian, equinoctial, equatorial and
     * circular ; and is defined by dMda0 * IdMatrix. 
     * </p>
     * 
     * <p>
     * To compute the transition matrix for the other orbit types, we need
     * to convert it using the Jacobian matrix between the equinoctial orbit type (or we could use keplerian,
     * equatorial or circular orbit type as well, as there are identical) and the current orbit type.
     * </p>
     * 
     * @param dt Propagation interval.
     * @return Transition matrix given in the coordinates type of the input orbit (only valid for a
     *         position angle set to MEAN).
     */
    public RealMatrix getKeplerianTransitionMatrix(final double dt) {
        try {
            return getJacobian(dt, this.frame, this.frame, getType(), getType(), PositionAngle.MEAN,
                PositionAngle.MEAN);
        } catch (final PatriusException e) {
            // Cannot happen since no conversion is performed
            throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, e);
        }
    }

    /**
     * Computes the Jacobian of the transformation between the specified frames, orbit types and
     * position angle types with respect to this orbit.
     *
     * <p>
     * The transformation is computed at the date of definition of the specified orbit. Beware that non Cartesian
     * coordinates types are incompatible with the use of some frames (e.g. local orbital frames).
     * </p>
     *
     * @param orbit
     *        the orbit of reference
     * @param initFrame
     *        the initial frame
     * @param destFrame
     *        the destination frame
     * @param initOrbitType
     *        the initial orbit type
     * @param destOrbitType
     *        the destination orbit type
     * @param initAngleType
     *        the initial position angle type
     * @param destAngleType
     *        the destination position angle type
     *
     * @return the jacobian of the transformation [6x6]
     *
     * @throws PatriusException
     *         if the orbit type conversion failed of jacobian frame conversion failed
     */
    private static RealMatrix getJacobian(final Orbit orbit,
            final Frame initFrame,
            final Frame destFrame,
            final OrbitType initOrbitType,
            final OrbitType destOrbitType,
            final PositionAngle initAngleType,
            final PositionAngle destAngleType) throws PatriusException {

        // Initialize the Jacobian matrix (identity)
        RealMatrix jacobian = MatrixUtils.createRealIdentityMatrix(STATE_DIM);

        // Temporary array used to store the data of intermediate Jacobian matrices
        final double[][] workingArray = new double[STATE_DIM][STATE_DIM];
        final Array2DRowRealMatrix workingMatrix = new Array2DRowRealMatrix(workingArray, false);

        // If the destination frame, coordinates type and position
        // angle are the same as the initial ones, there is nothing to do.
        // Otherwise, compute the Jacobian matrix of the transformation.
        if (!destFrame.equals(initFrame) || !destOrbitType.equals(initOrbitType)
                || !destAngleType.equals(initAngleType)) {

            // Transformation to Cartesian space
            // (using the initial position angle type)
            if (!initOrbitType.equals(OrbitType.CARTESIAN)) {
                // Ensure the orbit is in the initial frame and coordinates type.
                final Orbit newOrbit = initOrbitType.convertOrbit(orbit, initFrame);

                // Compute the Jacobian matrix and combine it with
                // the previous transformations.
                newOrbit.getJacobianWrtParameters(initAngleType, workingArray);
                jacobian = jacobian.preMultiply(workingMatrix);
            }

            // Transformation to the new frame
            if (!destFrame.equals(initFrame)) {
                jacobian = jacobian.preMultiply(initFrame.getTransformJacobian(destFrame, orbit.getDate()));
            }

            // Transformation Cartesian to the new coordinates type
            // (using the new position angle type)
            if (!destOrbitType.equals(OrbitType.CARTESIAN)) {
                // Transform the reference orbit to wanted frame and coordinates type.
                final Orbit newOrbit = destOrbitType.convertOrbit(orbit, destFrame);

                // Compute the Jacobian matrix and combine it with
                // the previous transformations.
                newOrbit.getJacobianWrtCartesian(destAngleType, workingArray);
                jacobian = jacobian.preMultiply(workingMatrix);
            }
        }

        return jacobian;
    }

    /**
     * Computes the Keplerian transition matrix for the specified time shift.
     *
     * <p>
     * The Keplerian transition matrix is the identity matrix with the exception of the last element of the 1st column,
     * which is set to <i>-3/2 * dt * n/a</i> (with <i>n</i> the Keplerian mean motion and <i>a</i> the semi-major
     * axis).
     * </p>
     *
     * @param dt
     *        the time shift
     * @param orbit
     *        the orbit of reference
     *
     * @return the Keplerian transition matrix
     */
    private static RealMatrix getKeplerianTransitionMatrix(final double dt,
            final Orbit orbit) {
        final double dMda0 = -3. / 2. * dt * orbit.getKeplerianMeanMotion() / orbit.getA();
        final RealMatrix transitionMatrix = MatrixUtils.createRealIdentityMatrix(STATE_DIM, false);
        transitionMatrix.setEntry(5, 0, dMda0);
        return transitionMatrix;
    }

    /**
     * Computes the Jacobian of the transformation between the specified frames, orbit types and
     * position angle types with respect to this orbit.
     *
     * <p>
     * The transformation is computed at the date of definition of the specified orbit. Since the Keplerian transition
     * matrix is expressed in non-Cartesian coordinates, it cannot be computed for some frames (e.g. local orbital
     * frames).
     * </p>
     *
     * @param initFrame
     *        the initial frame
     * @param destFrame
     *        the destination frame
     * @param initOrbitType
     *        the initial orbit type
     * @param destOrbitType
     *        the destination orbit type
     * @param initAngleType
     *        the initial position angle
     * @param destAngleType
     *        the destination position angle
     *
     * @return the jacobian of the transformation [6x6]
     *
     * @throws PatriusException
     *         if the orbit type conversion failed of jacobian frame conversion failed
     */
    public RealMatrix getJacobian(final Frame initFrame,
            final Frame destFrame,
            final OrbitType initOrbitType,
            final OrbitType destOrbitType,
            final PositionAngle initAngleType,
            final PositionAngle destAngleType) throws PatriusException {
        return getJacobian(0., initFrame, destFrame, initOrbitType, destOrbitType, initAngleType, destAngleType);
    }

    /**
     * Computes the Jacobian of the transformation between the specified frames, orbit types and
     * position angle types with respect to this orbit and specified time shift.
     *
     * <p>
     * The transformation is computed at the date of definition of the specified orbit. Since the Keplerian transition
     * matrix is expressed in non-Cartesian coordinates, it cannot be computed for some frames (e.g. local orbital
     * frames).
     * </p>
     *
     * @param dt
     *        the time shift
     * @param initFrame
     *        the initial frame
     * @param destFrame
     *        the destination frame
     * @param initOrbitType
     *        the initial orbit type
     * @param destOrbitType
     *        the destination orbit type
     * @param initAngleType
     *        the initial position angle
     * @param destAngleType
     *        the destination position angle
     *
     * @return the jacobian of the transformation [6x6]
     *
     * @throws PatriusException
     *         if the orbit type conversion failed of jacobian frame conversion failed
     */
    public RealMatrix getJacobian(final double dt,
            final Frame initFrame,
            final Frame destFrame,
            final OrbitType initOrbitType,
            final OrbitType destOrbitType,
            final PositionAngle initAngleType,
            final PositionAngle destAngleType) throws PatriusException {

        // Jacobian matrix
        RealMatrix jacobian;

        // If dt = 0, simply return the Jacobian of the transformation.
        if (dt == 0) {
            jacobian = getJacobian(this, initFrame, destFrame, initOrbitType, destOrbitType, initAngleType,
                    destAngleType);
        } else {
            // Coordinates type used for the transition:
            // The Keplerian transition matrix is only applicable to some orbital elements.
            // If the initial or the destination orbit types are not among of them, transit
            // through Equinoctial elements.
            final OrbitType transitionType;

            if (KEPLERIAN_TRANSITION_TYPES.contains(initOrbitType)) {
                transitionType = initOrbitType;
            } else if (KEPLERIAN_TRANSITION_TYPES.contains(destOrbitType)) {
                transitionType = destOrbitType;
            } else {
                transitionType = OrbitType.EQUINOCTIAL;
            }

            // Initialize the Jacobian matrix (6x6 identity matrix)
            jacobian = MatrixUtils.createRealIdentityMatrix(STATE_DIM);

            // Transformation to the transition coordinates type and mean position angle
            if (!transitionType.equals(initOrbitType) || !initAngleType.equals(PositionAngle.MEAN)) {
                jacobian = jacobian.preMultiply(getJacobian(this, initFrame, initFrame, initOrbitType,
                        transitionType, initAngleType, PositionAngle.MEAN));
            }

            // Apply the Keplerian transition matrix.
            // (only applicable in non-Cartesian coordinates and a mean position angle)
            jacobian = jacobian.preMultiply(getKeplerianTransitionMatrix(dt, this));

            // Transformation to the wanted frame, coordinate type and position angle type
            if (!destFrame.equals(initFrame) || !destOrbitType.equals(transitionType)
                    || !destAngleType.equals(PositionAngle.MEAN)) {
                jacobian = jacobian.preMultiply(getJacobian(this.shiftedBy(dt), initFrame, destFrame,
                        transitionType, destOrbitType, PositionAngle.MEAN, destAngleType));
            }
        }

        // Return the Jacobian matrix
        return jacobian;
    }

    /**
     * Test for the equality of two orbits.
     * <p>
     * Orbits are considered equals if they have the same type and all their attributes are equals.
     * In particular, the orbits frame are considered equals if they represent the same instance.
     * If they have the same attributes but are not the same instance, the method will return false. 
     * </p>
     * 
     * @param object
     *        Object to test for equality to this
     * @return true if two orbits are equal
     */
    @Override
    public abstract boolean equals(final Object object); 
    
    /**
     * Get a hashCode for the orbit.
     *
     * @return a hash code value for this object
     */
    @Override
    public abstract int hashCode();

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate dateIn,
            final Frame frameIn) throws PatriusException {
        return this.frame;
    }

    // CHECKSTYLE: resume ModifiedControlVariable check
}

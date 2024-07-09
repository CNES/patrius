/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.5:FA:FA-2468:27/05/2020:Robustesse methode shiftedBy de la classe Orbit
* VERSION:4.4:DM:DM-2135:04/10/2019:[PATRIUS] Methodes equals() et hashCode() non implementees dans la classe TLE
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS/COLOSUS] Mise en conformite code avec nouveau standard codage DYNVOL
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:426:30/10/2015: allow orbit instantiation in non inertial frame
 * VERSION::DM:489:17/12/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:675:01/09/2016:corrected anomalies reducing the performances
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::FA:836:17/02/2017:Code optimization for getA(), orbitShiftedBy()
 * VERSION::FA:1281:30/08/2017: Javadoc correction
 * VERSION::DM:1798:10/12/2018: Add getN() after AlternateEquinoctialParameters creation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.IOrbitalParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.TimeStampedPVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.CartesianDerivativesFilter;

/**
 * This class holds cartesian orbital parameters.
 * <p>
 * The parameters used internally are the cartesian elements (see {@link CartesianParameters} for more information.
 * </p>
 * <p>
 * The instance <code>CartesianOrbit</code> is guaranteed to be immutable.
 * </p>
 * 
 * @see Orbit
 * @see KeplerianOrbit
 * @see CircularOrbit
 * @see EquinoctialOrbit
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */
public final class CartesianOrbit extends Orbit {

    /** Serializable UID. */
    private static final long serialVersionUID = -5411308212620896302L;
    
    /** Root int for hash code. */
    private static final int ROOTINT = 353;

    /** 6 x 6 identity matrix. */
    private static final double[][] JACOBIAN = { { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
        { 0.0, 1.0, 0.0, 0.0, 0.0, 0.0 }, { 0.0, 0.0, 1.0, 0.0, 0.0, 0.0 },
        { 0.0, 0.0, 0.0, 1.0, 0.0, 0.0 }, { 0.0, 0.0, 0.0, 0.0, 1.0, 0.0 },
        { 0.0, 0.0, 0.0, 0.0, 0.0, 1.0 } };

    /** Maximum number of iterations for convergence algorithms. */
    private static final double MAX_ITERATIONS = 50;

    /** Threshold for convergence algorithms. */
    private static final double THRESHOLD = 1.0e-12;

    /** Orbital parameters. */
    private final CartesianParameters parameters;

    /**
     * Creates a new instance.
     * 
     * @param parametersIn
     *        orbital parameters
     * @param frame
     *        the frame in which the parameters are defined
     * @param date
     *        date of the orbital parameters
     */
    public CartesianOrbit(final IOrbitalParameters parametersIn, final Frame frame,
        final AbsoluteDate date) {
        super(frame, date, parametersIn.getMu());
        this.parameters = parametersIn.getCartesianParameters();
    }

    /**
     * Constructor from cartesian parameters.
     * 
     * @param pvCoordinates
     *        the position and velocity of the satellite.
     * @param frame
     *        the frame in which the {@link PVCoordinates} are defined
     * @param date
     *        date of the orbital parameters
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    public CartesianOrbit(final PVCoordinates pvCoordinates, final Frame frame,
        final AbsoluteDate date, final double mu) {
        super(pvCoordinates, frame, date, mu);
        this.parameters = new CartesianParameters(pvCoordinates, mu);
    }

    /**
     * Constructor from any kind of orbital parameters.
     * 
     * @param op
     *        orbital parameters to copy
     */
    public CartesianOrbit(final Orbit op) {
        super(op.getPVCoordinates(), op.getFrame(), op.getDate(), op.getMu());
        this.parameters = op.getParameters().getCartesianParameters();
    }

    /** {@inheritDoc} */
    @Override
    public IOrbitalParameters getParameters() {
        return this.parameters;
    }

    /**
     * Getter for underlying circular parameters.
     * 
     * @return circular parameters
     */
    public CartesianParameters getCartesianParameters() {
        return this.parameters;
    }

    /** {@inheritDoc} */
    @Override
    public OrbitType getType() {
        return OrbitType.CARTESIAN;
    }

    /** {@inheritDoc} */
    @Override
    public double getA() {
        // preliminary computations
        final PVCoordinates pvCoordinates = this.parameters.getPVCoordinates();
        final double r = pvCoordinates.getPosition().getNorm();
        final double v2 = pvCoordinates.getVelocity().getNormSq();
        final double rV2OnMu = r * v2 / this.getMu();

        // compute semi-major axis (will be negative for hyperbolic orbits)
        return r / (2 - rV2OnMu);
    }

    /** {@inheritDoc} */
    @Override
    public double getE() {
        return this.parameters.getKeplerianParameters().getE();
    }

    /** {@inheritDoc} */
    @Override
    public double getI() {
        return this.parameters.getKeplerianParameters().getI();
    }

    /** {@inheritDoc} */
    @Override
    public double getEquinoctialEx() {
        return this.parameters.getEquinoctialParameters().getEquinoctialEx();
    }

    /** {@inheritDoc} */
    @Override
    public double getEquinoctialEy() {
        return this.parameters.getEquinoctialParameters().getEquinoctialEy();
    }

    /** {@inheritDoc} */
    @Override
    public double getHx() {
        return this.parameters.getEquinoctialParameters().getHx();
    }

    /** {@inheritDoc} */
    @Override
    public double getHy() {
        return this.parameters.getEquinoctialParameters().getHy();
    }

    /** {@inheritDoc} */
    @Override
    public double getLv() {
        return this.parameters.getEquinoctialParameters().getLv();
    }

    /** {@inheritDoc} */
    @Override
    public double getLE() {
        return this.parameters.getEquinoctialParameters().getLE();
    }

    /** {@inheritDoc} */
    @Override
    public double getLM() {
        return this.parameters.getEquinoctialParameters().getLM();
    }

    /**
     * Get the mean motion.
     * 
     * @return mean motion (1/s)
     */
    @Override
    public double getN() {
        return this.parameters.getAlternateEquinoctialParameters().getN();
    }

    /** {@inheritDoc} */
    @Override
    protected PVCoordinates initPVCoordinates() {
        return this.parameters.getPVCoordinates();
    }

    /** {@inheritDoc} */
    @Override
    protected CartesianOrbit orbitShiftedBy(final double dt) {
        // 2 cases : elliptic or hyperbolic orbit
        final double a = this.getA();
        final PVCoordinates shiftedPV = (a < 0) ? this.shiftPVHyperbolic(dt, a) : this.shiftPVElliptic(dt, a);
        return new CartesianOrbit(shiftedPV, this.getFrame(), this.getDate().shiftedBy(dt), this.getMu());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation ensuring velocity remains the exact
     * derivative of position.
     * </p>
     */
    @Override
    public CartesianOrbit interpolate(final AbsoluteDate date, final Collection<Orbit> sample) {
        final List<TimeStampedPVCoordinates> datedPV = new ArrayList<TimeStampedPVCoordinates>(
            sample.size());
        for (final Orbit o : sample) {
            datedPV.add(new TimeStampedPVCoordinates(o.getDate(), o.getPVCoordinates()
                .getPosition(), o.getPVCoordinates().getVelocity(), o.getPVCoordinates()
                .getAcceleration()));
        }
        final TimeStampedPVCoordinates interpolated = TimeStampedPVCoordinates.interpolate(date,
            CartesianDerivativesFilter.USE_PVA, datedPV);
        return new CartesianOrbit(interpolated, this.getFrame(), date, this.getMu());
    }

    /**
     * Get the keplerian mean motion.
     * <p>
     * The keplerian mean motion is computed directly from semi major axis and central acceleration constant.
     * </p>
     * 
     * @param a
     *        semi-major axis (m)
     * @return keplerian mean motion in radians per second
     */
    private double getKeplerianMeanMotion(final double a) {
        final double absA = MathLib.abs(a);
        return MathLib.sqrt(this.getMu() / absA) / absA;
    }

    /**
     * Compute shifted position and velocity in elliptic case.
     * 
     * @param dt
     *        time shift
     * @param a
     *        semi-major axis (m)
     * @return shifted position and velocity
     */
    private PVCoordinates shiftPVElliptic(final double dt, final double a) {

        // preliminary computation
        final Vector3D pvP = this.getPVCoordinates().getPosition();
        final Vector3D pvV = this.getPVCoordinates().getVelocity();
        final double r = pvP.getNorm();
        final double rV2OnMu = r * pvV.getNormSq() / this.getMu();
        final double eSE = Vector3D.dotProduct(pvP, pvV) / MathLib.sqrt(this.getMu() * a);
        final double eCE = rV2OnMu - 1;
        final double e2 = eCE * eCE + eSE * eSE;
        final double oMe2 = 1 - e2;

        // we can use any arbitrary reference 2D frame in the orbital plane
        // in order to simplify some equations below, we use the current position as the u axis
        final Vector3D u = pvP.normalize();
        final Vector3D v = Vector3D.crossProduct(this.getPVCoordinates().getMomentum(), u).normalize();

        // the following equations rely on the specific choice of u explained above,
        // some coefficients that vanish to 0 in this case have already been removed here
        final double ex = (eCE - e2) * a / r;
        final double ey = -MathLib.sqrt(MathLib.max(0.0, oMe2)) * eSE * a / r;
        final double beta = 1 / (1 + MathLib.sqrt(MathLib.max(0.0, oMe2)));
        final double thetaE0 = MathLib.atan2(ey + eSE * beta * ex, r / a + ex - eSE * beta * ey);
        final double thetaM0 = thetaE0 - ex * MathLib.sin(thetaE0) + ey * MathLib.cos(thetaE0);

        // compute in-plane shifted eccentric argument
        final double thetaM1 = thetaM0 + this.getKeplerianMeanMotion(a) * dt;
        final double thetaE1 = this.meanToEccentric(thetaM1, ex, ey);
        final double[] sincosThetaE1 = MathLib.sinAndCos(thetaE1);
        final double cTE = sincosThetaE1[1];
        final double sTE = sincosThetaE1[0];

        // compute shifted in-plane cartesian coordinates
        final double exey = ex * ey;
        final double exCeyS = ex * cTE + ey * sTE;
        final double x = a * ((1 - beta * ey * ey) * cTE + beta * exey * sTE - ex);
        final double y = a * ((1 - beta * ex * ex) * sTE + beta * exey * cTE - ey);
        final double factor = MathLib.sqrt(this.getMu() / a) / (1 - exCeyS);
        final double xDot = factor * (-sTE + beta * ey * exCeyS);
        final double yDot = factor * (cTE - beta * ex * exCeyS);

        final Vector3D shiftedP = new Vector3D(x, u, y, v);
        final double r2 = x * x + y * y;
        final Vector3D shiftedV = new Vector3D(xDot, u, yDot, v);
        final Vector3D shiftedA = new Vector3D(-this.getMu() / (r2 * MathLib.sqrt(r2)), shiftedP);

        return new PVCoordinates(shiftedP, shiftedV, shiftedA);

    }

    /**
     * Compute shifted position and velocity in hyperbolic case.
     * 
     * @param dt
     *        time shift
     * @param a
     *        semi-major axis (m)
     * @return shifted position and velocity
     */
    private PVCoordinates shiftPVHyperbolic(final double dt, final double a) {

        // Get PV
        final PVCoordinates pv = this.getPVCoordinates();
        final Vector3D pvP = pv.getPosition();
        final Vector3D pvV = pv.getVelocity();
        final Vector3D pvM = pv.getMomentum();

        // Intermediate variables
        final double r = pvP.getNorm();
        final double rV2OnMu = r * pvV.getNormSq() / this.getMu();
        final double muA = this.getMu() * a;
        final double e = MathLib.sqrt(MathLib.max(0.0, 1 - Vector3D.dotProduct(pvM, pvM) / muA));
        final double sqrt = MathLib.sqrt(MathLib.max(0.0, (e + 1) / (e - 1)));

        // compute mean anomaly
        final double eSH = Vector3D.dotProduct(pvP, pvV) / MathLib.sqrt(-muA);
        final double eCH = rV2OnMu - 1;
        final double h0 = MathLib.log((eCH + eSH) / (eCH - eSH)) / 2;
        final double m0 = e * MathLib.sinh(h0) - h0;

        // find canonical 2D frame with p pointing to perigee
        final double v0 = 2 * MathLib.atan(sqrt * MathLib.tanh(h0 / 2));
        final Vector3D p = new Rotation(pvM, -v0).applyTo(pvP).normalize();
        final Vector3D q = Vector3D.crossProduct(pvM, p).normalize();

        // compute shifted eccentric anomaly
        final double m1 = m0 + this.getKeplerianMeanMotion(a) * dt;
        final double h1 = this.meanToHyperbolicEccentric(m1, e);

        // compute shifted in-plane cartesian coordinates
        final double[] sinhcosh = MathLib.sinhAndCosh(h1);
        final double sH = sinhcosh[0];
        final double cH = sinhcosh[1];
        final double sE2m1 = MathLib.sqrt(MathLib.max(0.0, (e - 1) * (e + 1)));

        // coordinates of position and velocity in the orbital plane
        final double x = a * (cH - e);
        final double y = -a * sE2m1 * sH;
        final double factor = MathLib.sqrt(this.getMu() / -a) / (e * cH - 1);
        final double xDot = -factor * sH;
        final double yDot = factor * sE2m1 * cH;

        final Vector3D shiftedP = new Vector3D(x, p, y, q);
        final double r2 = x * x + y * y;
        final Vector3D shiftedV = new Vector3D(xDot, p, yDot, q);
        final Vector3D shiftedA = new Vector3D(-this.getMu() / (r2 * MathLib.sqrt(r2)), shiftedP);

        // Return result
        return new PVCoordinates(shiftedP, shiftedV, shiftedA);

    }

    /**
     * Computes the eccentric in-plane argument from the mean in-plane argument.
     * 
     * @param thetaM
     *        = mean in-plane argument (rad)
     * @param ex
     *        first component of eccentricity vector
     * @param ey
     *        second component of eccentricity vector
     * @return the eccentric in-plane argument.
     */
    private double meanToEccentric(final double thetaM, final double ex, final double ey) {
        // Generalization of Kepler equation to in-plane parameters
        // with thetaE = eta + E and
        // thetaM = eta + M = thetaE - ex.sin(thetaE) + ey.cos(thetaE)
        // and eta being counted from an arbitrary reference in the orbital plane
        double thetaE = thetaM;
        double shift = 0.0;
        double thetaEMthetaM = 0.0;
        final double[] sincos = MathLib.sinAndCos(thetaE);
        double cosThetaE = sincos[1];
        double sinThetaE = sincos[0];
        int iter = 0;
        do {
            final double f2 = ex * sinThetaE - ey * cosThetaE;
            final double f1 = 1.0 - ex * cosThetaE - ey * sinThetaE;
            final double f0 = thetaEMthetaM - f2;

            final double f12 = 2.0 * f1;
            shift = f0 * f12 / (f1 * f12 - f0 * f2);

            thetaEMthetaM -= shift;
            thetaE = thetaM + thetaEMthetaM;
            final double[] sincosThetaE = MathLib.sinAndCos(thetaE);
            cosThetaE = sincosThetaE[1];
            sinThetaE = sincosThetaE[0];

        } while ((++iter < MAX_ITERATIONS) && (MathLib.abs(shift) > THRESHOLD));

        return thetaE;

    }

    /**
     * Computes the hyperbolic eccentric anomaly from the mean anomaly.
     * <p>
     * The algorithm used here for solving hyperbolic Kepler equation is a naive initialization and classical Halley
     * method for iterations.
     * </p>
     * 
     * @param m
     *        mean anomaly (rad)
     * @param e
     *        eccentricity
     * @return the true anomaly
     */
    private double meanToHyperbolicEccentric(final double m, final double e) {

        // resolution of hyperbolic Kepler equation for keplerian parameters
        double h = -m;
        double shift = 0.0;
        double hpM = 0.0;
        int iter = 0;
        do {
            // Loop until convergence
            final double[] sinhcosh = MathLib.sinhAndCosh(h);
            final double sinh = sinhcosh[0];
            final double cosh = sinhcosh[1];
            final double f2 = e * sinh;
            final double f1 = e * cosh - 1;
            final double f0 = f2 - hpM;

            final double f12 = 2 * f1;
            // Update loop variable
            shift = f0 * f12 / (f1 * f12 - f0 * f2);
            hpM -= shift;
            h = hpM - m;

        } while ((++iter < MAX_ITERATIONS) && (MathLib.abs(shift) > THRESHOLD));

        // Return result
        return h;

    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    protected double[][] computeJacobianMeanWrtCartesian() {
        return JACOBIAN;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    protected double[][] computeJacobianEccentricWrtCartesian() {
        return JACOBIAN;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    protected double[][] computeJacobianTrueWrtCartesian() {
        return JACOBIAN;
    }

    /** {@inheritDoc} */
    @Override
    protected void orbitAddKeplerContribution(final PositionAngle type, final double gm,
                                              final double[] pDot) {

        // Get PV
        final PVCoordinates pv = this.getPVCoordinates();

        // position derivative is velocity
        final Vector3D velocity = pv.getVelocity();
        pDot[0] += velocity.getX();
        pDot[1] += velocity.getY();
        pDot[2] += velocity.getZ();

        // velocity derivative is Newtonian acceleration
        final Vector3D position = pv.getPosition();
        final double r2 = position.getNormSq();
        final double coeff = -gm / (r2 * MathLib.sqrt(r2));
        pDot[3] += coeff * position.getX();
        pDot[4] += coeff * position.getY();
        pDot[5] += coeff * position.getZ();

    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.parameters.toString();
    }
    
    /** {@inheritDoc} */
    @Override
	public boolean equals(final Object object) {
		// parameters : date, frame, type, mu,
		//              x, y, z, xDot, yDot, zDot
        boolean isEqual = true;
		
        if (object == this) { 
            isEqual = true; 
        } else if (object instanceof CartesianOrbit) {
            final CartesianOrbit other = (CartesianOrbit) object;
        	
            // Compare date and frame
            isEqual &= (this.getDate().equals(other.getDate()));
            isEqual &= (this.getFrame().equals(other.getFrame()));
            // Compare Cartesian parameters
            isEqual &= (this.parameters.equals(other.parameters));
        	
        	
        } else {
            isEqual = false;
        }
		
        return isEqual;
    }
	
	/** {@inheritDoc} */
    @Override 
    public int hashCode() { 
    	
        // A not zero random "root int"
        int result = ROOTINT;
        // An efficient multiplier (JVM optimizes 31 * i as (i << 5) - 1 )
        final int effMult = 31;
        // Good hashcode : it's the same
        // for "equal" orbits, but
        // reasonably sure it's different otherwise.
        result = effMult * result + this.getDate().hashCode();
        result = effMult * result + this.getFrame().hashCode();
        result = effMult * result + this.parameters.hashCode();
 
        return result; 
    }

}
